//add your own package structure

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

// Intial code created by AAU.  
// Extended and re-written with AWS "bulder" methods by: Alexander Lercher

public class Main {
    static String createdInstanceId="test";
	static KeyPair keyPair;
	static AmazonEC2 ec2;
	static final String IMAGE_ID = "ami-00aa4671cbf840d82";

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException, IllegalStateException {

        /***************** Load the credentials ****************/
        AWSCredentialsProvider credentials = null;
        try {
            credentials = new ProfileCredentialsProvider("default");
        } catch (Exception e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (/home/dragi/.aws/credentials), and is in valid format.", e);
        }

        /***************** Set an AWS region ****************/
        ec2 = AmazonEC2ClientBuilder
                .standard()
                .withCredentials(credentials)
                .withRegion(Regions.EU_CENTRAL_1)
                .build();

        /***************** List the availability zones ****************/
        //you should extend this part of the code

        /***************** Set a filter on available AMIs/VMIs ****************/
        //you should extend this part of the code

        /***************** Create new security group ****************/
        String gid = null;
        String groupNamePrefix = "JavaSecurityGroup";

        // check for exiting group first
        DescribeSecurityGroupsRequest req = new DescribeSecurityGroupsRequest();
        req.getFilters().add(new Filter().withName("group-name").withValues(groupNamePrefix + "*"));
        DescribeSecurityGroupsResult res = ec2.describeSecurityGroups(req);
        List<SecurityGroup> groups = res.getSecurityGroups();

        if (groups.size() > 0) {
            System.out.println("security group(s) already exist(s):");
            for (SecurityGroup securityGroup : groups) {
                System.out.println("\t" + securityGroup.getGroupName());
            }
            // take first existing one
            gid = groups.get(0).getGroupId();
        } else {
            // create new group
            try {
                CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
                csgr.withGroupName(groupNamePrefix + (Math.random())).withDescription("Proseminar security group");
                CreateSecurityGroupResult resultsc = ec2.createSecurityGroup(csgr);
                System.out.println(String.format("Security group created: [%s]", resultsc.getGroupId()));
                gid = resultsc.getGroupId();
            } catch (AmazonServiceException ase) {
                // gid could not be assigned
                throw new IllegalStateException("security group could not be created: " + ase.getMessage());
            }
        }

        List<String> groupNames = new ArrayList<String>();
        groupNames.add(gid);

        System.out.println("used security group: " + gid);

        /***************** Set incoming traffic policy ****************/
        IpPermission ipPermission = new IpPermission();

        IpRange ipRange1 = new IpRange().withCidrIp(getPublicIp() + "/32");

        ipPermission.withIpv4Ranges(Arrays.asList(new IpRange[]{ipRange1}))
                .withIpProtocol("tcp")
                .withFromPort(22)
                .withToPort(22);

        // TODO check if http is needed
//		IpRange ipRange2 = new IpRange().withCidrIp("0.0.0.0/0");
//		ipPermission.withIpv4Ranges(Arrays.asList(new IpRange[] { ipRange2 }))
//		.withIpProtocol("tcp")
//		.withFromPort(80)
//		.withToPort(80);

        /***************** Authorize ingress traffic ****************/
        try {
            AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest();

            ingressRequest.withGroupId(gid).withIpPermissions(ipPermission);
            ec2.authorizeSecurityGroupIngress(ingressRequest);
            System.out.println(String.format("Ingress port authroized: [%s]", ipPermission.toString()));
        } catch (AmazonServiceException ase) {
            // catches duplicate rule creation
            System.out.println(ase.getMessage());
        }

        /***************** create a key for the VM ****************/
        CreateKeyPairRequest newKReq = new CreateKeyPairRequest();
        newKReq.setKeyName("Proseminar2" + (Math.random()));
        CreateKeyPairResult kresult = ec2.createKeyPair(newKReq);
        keyPair = kresult.getKeyPair();
        System.out.println("Key for the VM was created  = " + keyPair.getKeyName()
                + "\nthe fingerprint is=" + keyPair.getKeyFingerprint()
                + "\nthe material is= \n" + keyPair.getKeyMaterial());

        /***************** store the key in a .pem file ****************/
        String keyname = keyPair.getKeyName();
        // using local path in workspace
        String fileName = keyname + ".pem";
        File distFile = new File(fileName);
        BufferedReader bufferedReader = new BufferedReader(new StringReader(keyPair.getKeyMaterial()));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(distFile));
        char buf[] = new char[1024];
        int len;
        while ((len = bufferedReader.read(buf)) != -1) {
            bufferedWriter.write(buf, 0, len);
        }
        bufferedWriter.flush();
        bufferedReader.close();
        bufferedWriter.close();

        /***************** Start a given free tier instance ****************/
        // You should extend this part of the code
        RunInstancesRequest run_request = new RunInstancesRequest()
                .withImageId(IMAGE_ID)
                .withInstanceType(InstanceType.T2Micro)
                .withMaxCount(1)
                .withMinCount(1);

        RunInstancesResult run_response = ec2.runInstances(run_request);

        createdInstanceId = run_response.getReservation().getInstances().get(0).getInstanceId();

        System.out.printf(
                "Successfully started EC2 instance %s based on AMI %s\n",
                createdInstanceId, IMAGE_ID);

        /***************** Create EBS volume for the instance ****************/
        DescribeInstanceStatusRequest disr = new DescribeInstanceStatusRequest().withInstanceIds(createdInstanceId);
        DescribeInstanceStatusResult instStatusResult = ec2.describeInstanceStatus(disr);
        List<InstanceStatus> statuses = instStatusResult.getInstanceStatuses();
        if (statuses.size() != 1)
            throw new IllegalStateException("not exactly one instance status result for id "
                    + createdInstanceId + "(yields " + statuses.size() + ")");

        String zoneOfInstance = statuses.get(0).getAvailabilityZone();

        CreateVolumeRequest cvr = new CreateVolumeRequest()
                .withVolumeType(VolumeType.Gp2)
                .withSize(8)
                .withAvailabilityZone(zoneOfInstance) // has to be in the same zone
                .withEncrypted(false);
        CreateVolumeResult createRes = ec2.createVolume(cvr);
        String volumeId = createRes.getVolume().getVolumeId();

        System.out.println("Sleep");
        Thread.sleep(30000); // wait for volume to be available

        AttachVolumeRequest attachVolReq = new AttachVolumeRequest(volumeId, createdInstanceId, "/dev/sdb");
        ec2.attachVolume(attachVolReq);

        System.out.println("created and attached volume: " + volumeId);

        /***************** Print the public IP and DNS of the instance ****************/
        // You should extend this part of the code

        /*****************
         * Terminate the instance after given time period
         ****************/
        System.out.println("Sleep");
        Thread.sleep(60000);
// You should extend this part of the code	
    }
	/**
	 * Returns the public ip.
	 * @return
	 * @throws IOException
	 */
	private static String getPublicIp() throws IOException {
		URL whatismyip = new URL("http://checkip.amazonaws.com");
		BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));

		return in.readLine();
	}
}
