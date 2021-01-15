package org.pubanatomy.awsUtils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest
import com.amazonaws.services.lambda.model.VpcConfig
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * User: flashflexpro@gmail.com
 * Date: 4/11/2016
 * Time: 3:23 PM*/
public class AddIntoVpcLambda extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( AddIntoVpcLambda.class );


    AWSLambdaClient lambdaClient

    @Input
    public String lambdaFunctionName
    @Input
    public String lambdaFunctionSubnetIds
    @Input
    public String lambdaFunctionSecurityGroupIds

    @Input
    public String awsRegion = Regions.US_EAST_1.name


    @TaskAction
    public void doTheTask() throws IOException{
        if( lambdaFunctionName == null || lambdaFunctionName.length() == 0 ){
            throw new Error( "Empty lambda function name!" )
        }
        if( lambdaClient == null ){
            lambdaClient = new AWSLambdaClient( new DefaultAWSCredentialsProviderChain() )
            lambdaClient.setRegion( RegionUtils.getRegion( awsRegion ) )
        }


        String[] subnetIds = lambdaFunctionSubnetIds.split( "," )
        String[] securityGroupIds = lambdaFunctionSecurityGroupIds.split( "," )

        logger.info( "adding {} into subnetIds:{}, securityGroupIds:{}>>>", lambdaFunctionName, subnetIds,
                securityGroupIds )
        def configurationRequest = new UpdateFunctionConfigurationRequest()
        lambdaClient.
                updateFunctionConfiguration( configurationRequest
                        .withFunctionName( lambdaFunctionName )
                        .withVpcConfig( new VpcConfig()
                        .withSubnetIds( subnetIds )
                        .withSecurityGroupIds( securityGroupIds ) ) )

        logger.info( "adding {} into subnetIds:{}, securityGroupIds:{}<<<", lambdaFunctionName, subnetIds,
                securityGroupIds )
    }

}
