package org.pubanatomy.awsUtils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClient
import com.amazonaws.services.cloudwatchevents.model.*
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.AddPermissionRequest
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest
import com.amazonaws.services.lambda.model.ResourceConflictException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction


public class AddEcsEventTriggerToLambda extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( AddEcsEventTriggerToLambda.class );


    private static final String eventPattern = "{\n" + "  \"source\": [\n" + "    \"aws.ecs\"\n" + "  ],\n" +
            "  \"detail-type\": [\n" + "    \"ECS Task State Change\"\n" + "  ],\n" + "  \"detail\": {\n" +
            "    \"clusterArn\": [\n" + "      \"arn:aws:ecs:us-east-1:726469603241:cluster/--CLUSTERNAME--\"\n" +
            "    ]\n" + "  }\n" + "}";


    @Input
    public String lambdaFunctionName;

    @Input
    public String clusterName;


    @Input
    public String awsRegion = Regions.US_EAST_1.name


    @TaskAction
    public void doTheTask() throws IOException{
        if( !clusterName?.trim() ){
            throw new Error( "Empty cluster name" )
        }
        if( !lambdaFunctionName?.trim() ){
            throw new Error( "Empty lambdaFunctionName" )
        }

        def newRuleName = clusterName + "-ECS-State--" + lambdaFunctionName
        logger.info( ">>>>>adding schecule event trigger: ruleName:{}, lambdaFunctionName:{}", newRuleName,
                lambdaFunctionName )

        AWSLambdaClient lambdaClient = new AWSLambdaClient()
        lambdaClient.setRegion( RegionUtils.getRegion( awsRegion ) )

        String lambdaFuncArn = lambdaClient.getFunctionConfiguration(
                new GetFunctionConfigurationRequest().withFunctionName( lambdaFunctionName ) ).functionArn

        AmazonCloudWatchEventsClient eventClient = new AmazonCloudWatchEventsClient(
                new DefaultAWSCredentialsProviderChain() )
        eventClient.setRegion( RegionUtils.getRegion( awsRegion ) )

        logger.info( "lambdaFuncArn:{}", lambdaFuncArn )

        ListRuleNamesByTargetResult lstExistRules = eventClient.
                listRuleNamesByTarget( new ListRuleNamesByTargetRequest().withTargetArn( lambdaFuncArn ) )

        String ruleARN = null;
        if( lstExistRules.ruleNames.size() > 0 ){

            if( lstExistRules.ruleNames.size() > 1 ){
                throw new Error( "More rules found:" + lstExistRules.ruleNames.join( ";" ) );
            }
            ruleARN = eventClient.describeRule( new DescribeRuleRequest().withName( newRuleName ) ).getArn()
            logger.info( "<<<<<existing ECS event trigger:" + ruleARN )
        }
        else{
            PutRuleResult putRuleRslt = eventClient.putRule( new PutRuleRequest()
                    .withName( newRuleName )
                    .withEventPattern( eventPattern.replaceAll( "--CLUSTERNAME--", clusterName ) )
                    .withDescription( newRuleName ) )
            ruleARN = putRuleRslt.ruleArn;

            def putTargetsRequest = new PutTargetsRequest()
                    .withRule( newRuleName )
                    .withTargets( new Target()
                    .withId( lambdaFunctionName )
                    .withArn( lambdaFuncArn ) )

            PutTargetsResult putTargetsResult = eventClient.putTargets( putTargetsRequest )
            logger.info( "<<<<<adding ECS event trigger:" + putTargetsResult )
        }



        try{
            lambdaClient.addPermission( new AddPermissionRequest()
                    .withFunctionName( lambdaFunctionName )
                    .withStatementId( newRuleName )
                    .withAction( "lambda:InvokeFunction" )
                    .withPrincipal( "events.amazonaws.com" ).withSourceArn( ruleARN ) )
        }
        catch( e ){
            if( e instanceof ResourceConflictException && e.statusCode == 409 ){
                logger.info( "Permission already exist:{}", e.message )
            }
            else{
                logger.error( e.toString() )
                throw e;
            }
        }

    }

}
