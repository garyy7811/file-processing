package org.pubanatomy.awsUtils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult
import com.amazonaws.services.autoscaling.model.Tag
import com.amazonaws.util.Base64
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

public class CreateEcsAutoScalingLaunchConfig extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( CreateEcsAutoScalingLaunchConfig.class );


    @Input
    public String launchConfigName
    @Input
    public String launchConfigImgId
    @Input
    public String launchConfigSecurityGroups
    @Input
    public String launchConfigKeyName
    @Input
    public String launchConfigInstanceType
    @Input
    public String launchConfigIamInstanceProfile
    @Input
    public String launchConfigDockerRegistryPull;
    @Input
    public String launchConfigEcsEtcEcsConfigS3Bucket;
    @Input
    public String launchConfigEcsEtcEcsConfigS3Path;
    @Input
    public File launchConfigUserDataYaml;

    @Input
    public String awsRegion = "us-east-1";


    @Input
    public String autoScalingGroupName

    @Input
    public String logGroupName

    @Input
    public String efsArgs

    @Input
    public String efsDomainName

    @Input
    public String vpcSubnets;
    @Input
    public int autoScalingMin;
    @Input
    public int autoScalingMax;
    @Input
    public int autoScalingDesired;

    @Input
    public Boolean launchConfigPublicIp = false;

    @TaskAction
    public void doTheTask() throws IOException{
        def autoScalingClient = new AmazonAutoScalingClient( new DefaultAWSCredentialsProviderChain() )
        autoScalingClient.setRegion( RegionUtils.getRegion( awsRegion ) )

        DescribeLaunchConfigurationsResult desLaunchConfig = autoScalingClient.describeLaunchConfigurations(
                new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames( launchConfigName ) )
        if( desLaunchConfig.launchConfigurations.size() == 0 ){
            CreateLaunchConfigurationRequest request = new CreateLaunchConfigurationRequest()
            request.setLaunchConfigurationName( launchConfigName )
            request.setImageId( launchConfigImgId )
            request.setSecurityGroups( Arrays.asList( launchConfigSecurityGroups.split( ',' ) ) )
            request.setKeyName( launchConfigKeyName )
            request.setInstanceType( launchConfigInstanceType )
            request.setIamInstanceProfile( launchConfigIamInstanceProfile )

            // leave default value unless setting AssociatePublicIp to true
            if (launchConfigPublicIp) {
                request.setAssociatePublicIpAddress( launchConfigPublicIp );
            }

            String userDataStr = launchConfigUserDataYaml.text
            userDataStr = userDataStr.replaceAll( "\\@\\@dockerRegistryPull\\@\\@", launchConfigDockerRegistryPull )
            userDataStr = userDataStr.replaceAll( "\\@\\@logGroupName\\@\\@", logGroupName )
            userDataStr = userDataStr.replaceAll( "\\@\\@mountEfsArgs\\@\\@", efsArgs )
            userDataStr = userDataStr.replaceAll( "\\@\\@efsDomainName\\@\\@", efsDomainName )
            userDataStr = userDataStr.replaceAll( "\\@\\@ecsEtcEcsConfigS3Path\\@\\@",
                    launchConfigEcsEtcEcsConfigS3Bucket + "/" + launchConfigEcsEtcEcsConfigS3Path )
            request.setUserData( Base64.encodeAsString( userDataStr.bytes ) )
            autoScalingClient.createLaunchConfiguration( request )
            logger.info( "new launch config created:" + launchConfigName )
        }
        else{
            logger.warn( "Existing launchConfig:" + launchConfigName + " found!" )
        }


        DescribeAutoScalingGroupsResult lstScalingGroupRslt = autoScalingClient.describeAutoScalingGroups(
                new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames( launchConfigName ) )

        if( lstScalingGroupRslt.autoScalingGroups.size() > 0 ){
            logger.warn( "Existing Auto Scaling Group:" + launchConfigName + " found!" )

        }
        else{
            CreateAutoScalingGroupResult rslt = autoScalingClient.createAutoScalingGroup(
                    new CreateAutoScalingGroupRequest().withLaunchConfigurationName( launchConfigName ).
                            withAutoScalingGroupName( autoScalingGroupName ).withVPCZoneIdentifier( vpcSubnets ).
                            withMinSize( autoScalingMin ).withMaxSize( autoScalingMax ).withDesiredCapacity( autoScalingDesired ).
                            withTags( new Tag().withKey( "Name" ).withValue( autoScalingGroupName + "--EcsInst" ).
                                    withPropagateAtLaunch( true ) ) )

            logger.info( "new Auto Scaling Group created:" + rslt )
        }

    }

}
