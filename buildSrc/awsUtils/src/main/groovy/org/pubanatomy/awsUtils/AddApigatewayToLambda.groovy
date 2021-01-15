package org.pubanatomy.awsUtils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.AmazonApiGatewayClient
import com.amazonaws.services.apigateway.model.*
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.AddPermissionRequest
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest
import com.amazonaws.services.lambda.model.GetFunctionConfigurationResult
import com.amazonaws.services.lambda.model.ResourceConflictException
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * User: flashflexpro@gmail.com
 * Date: 4/11/2016
 * Time: 3:23 PM*/
public class AddApigatewayToLambda extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( AddApigatewayToLambda.class );


    AWSLambdaClient        lambdaClient
    AmazonApiGatewayClient gatewayClient

    @Input
    public String lambdaFunctionName

    @Input
    public String apiGatewayApiId

    @Input
    public String apiGatewayResourceId

    @Input
    public String apiGatewayStagename

    @Input
    public String awsRegion = Regions.US_EAST_1.name

    public String apiGatewayInvokeUrl


    @TaskAction
    public void doTheTask() throws IOException{
        if( lambdaFunctionName == null || lambdaFunctionName.length() == 0 ){
            throw new Error( "Empty lambda function name!" )
        }

        if( lambdaClient == null ){
            lambdaClient = new AWSLambdaClient( new DefaultAWSCredentialsProviderChain() )
            lambdaClient.setRegion( RegionUtils.getRegion( awsRegion ) )
        }

        GetFunctionConfigurationRequest funConfReq = new GetFunctionConfigurationRequest()
        funConfReq.functionName = lambdaFunctionName
        logger.debug( "getting lambda function config with: {}", funConfReq )
        GetFunctionConfigurationResult lambdaFunConf = lambdaClient.getFunctionConfiguration( funConfReq )

        if( gatewayClient == null ){
            gatewayClient = new AmazonApiGatewayClient( new DefaultAWSCredentialsProviderChain() )
        }

        String uri = "arn:aws:apigateway:" + awsRegion + ":lambda:path/2015-03-31/functions/" +
                lambdaFunConf.functionArn + "/invocations"

        def putIntegrationRequest = new PutIntegrationRequest().withRestApiId( apiGatewayApiId )
                .withResourceId( apiGatewayResourceId ).withType( IntegrationType.AWS_PROXY )
                .withHttpMethod( "ANY" ).withIntegrationHttpMethod( "ANY" ).withUri( uri )
        logger.info( "put integration:{} >>>", putIntegrationRequest )
        PutIntegrationResult putRslt = gatewayClient.putIntegration( putIntegrationRequest );
        logger.info( "put integration:{} <<<", putRslt )

        try{
            lambdaClient.addPermission( new AddPermissionRequest().withFunctionName( lambdaFunctionName )
                    .withStatementId( apiGatewayApiId + "-" + lambdaFunctionName ).
                    withAction( "lambda:InvokeFunction" ).
                    withPrincipal( "apigateway.amazonaws.com" ) )
        }
        catch( e ){
            if( e instanceof ResourceConflictException && e.statusCode == 409 ){
                logger.info( "Permission already exist:{}", e.message )
            }
            else{
                logger.error( ExceptionUtils.getStackTrace( e ) )
                throw e
            }
        }

        if( apiGatewayStagename != null ){
            gatewayClient.createDeployment( new CreateDeploymentRequest().withRestApiId( apiGatewayApiId ).
                    withStageName( apiGatewayStagename ) );

            apiGatewayInvokeUrl =
                    "https://" + apiGatewayApiId + ".execute-api." + awsRegion + ".amazonaws.com/" + apiGatewayStagename

            logger.info( "deployed --> apiGatewayInvokeUrl:{}", apiGatewayInvokeUrl )
        }

    }

}
