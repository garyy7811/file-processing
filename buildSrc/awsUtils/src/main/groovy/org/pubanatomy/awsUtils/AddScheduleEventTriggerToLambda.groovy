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
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * User: flashflexpro@gmail.com
 * Date: 4/11/2016
 * Time: 3:23 PM*/
public class AddScheduleEventTriggerToLambda extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( AddScheduleEventTriggerToLambda.class );


    @Input
    public String lambdaFunctionName

    @Input
    public String ruleNamePrefix

    @Input
    public String lambdaInput = "{}"


    @Input
    public String awsRegion = Regions.US_EAST_1.name

    @Input
    public String ruleScheduleExpression = "rate(5 minutes)";


    @TaskAction
    public void doTheTask() throws IOException{
        String newRuleName = ruleNamePrefix + "--Schedule--" + ruleScheduleExpression.replaceAll( "[\\(\\) ]", "" )

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
        def find = lstExistRules.ruleNames.find { it == newRuleName }
        if( find ){

            if( lstExistRules.ruleNames.size() > 1 ){
                throw new Error( "More rules found:" + lstExistRules.ruleNames.join( ";" ) );
            }
            ruleARN = eventClient.describeRule( new DescribeRuleRequest().withName( newRuleName ) ).getArn()
            logger.info( "<<<<<existing ECS event trigger:" + ruleARN )
        }
        else{
            PutRuleResult putRuleRslt = eventClient.putRule( new PutRuleRequest()
                    .withName( newRuleName )
                    .withScheduleExpression( ruleScheduleExpression )
                    .withDescription( newRuleName ) )
            ruleARN = putRuleRslt.ruleArn;

        }

        ListTargetsByRuleResult lsTgts = eventClient.
                listTargetsByRule( new ListTargetsByRuleRequest().withRule( newRuleName ).withLimit( 30 ) )

        Target t = lsTgts.targets.find { it.arn == lambdaFuncArn }
        if( t == null ){
            def putTargetsRequest = new PutTargetsRequest()
                    .withRule( newRuleName )
                    .withTargets( new Target()
                    .withId( lambdaFunctionName )
                    .withArn( lambdaFuncArn ).withInput( lambdaInput ) )

            PutTargetsResult putTargetsResult = eventClient.putTargets( putTargetsRequest )
            logger.info( "<<<<<adding ECS event trigger:" + putTargetsResult )

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
        else{
            logger.warn( "existing uncheck target:" + new ObjectMapper().writeValueAsString( t ) )
        }
    }

}
