package org.pubanatomy.awsUtils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.AddPermissionRequest
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest
import com.amazonaws.services.lambda.model.GetFunctionConfigurationResult
import com.amazonaws.services.lambda.model.ResourceConflictException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.BucketNotificationConfiguration
import com.amazonaws.services.s3.model.LambdaConfiguration
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
public class AddS3TriggerToLambda extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( AddS3TriggerToLambda.class );


    AWSLambdaClient lambdaClient;
    AmazonS3Client s3Client;

    @Input
    public String lambdaFunctionName;

    @Input
    public String awsRegion = Regions.US_EAST_1.name

    @Input
    public Map<String, String> s3bucketname2Event = new HashMap<>();

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

        if( s3bucketname2Event.size() > 0 ){

            if( s3Client == null ){
                s3Client = new AmazonS3Client( new DefaultAWSCredentialsProviderChain() )
                s3Client.setRegion( RegionUtils.getRegion( awsRegion ) )
            }

            s3bucketname2Event.each {
                logger.info( "adding trigger to bucket {}, with {}", it.key, it.value )
                String[] eventNameArr = it.value.split( "\\|" );
                if( eventNameArr.length == 1 && eventNameArr[ 0 ] == "" ){
                    logger.debug( "got empty events for bucket{}, will add all ...", it.key )
                    eventNameArr = ["s3:ReducedRedundancyLostObject", "s3:ObjectCreated:*", "s3:ObjectRemoved:*"]
                }
                else{
                    logger.debug( "got events arr:{}", eventNameArr )
                }

                BucketNotificationConfiguration bucketNotificationConfiguration = s3Client.
                        getBucketNotificationConfiguration( it.key )

                LambdaConfiguration s3LambdaConf = null;
                if( bucketNotificationConfiguration == null ){
                    bucketNotificationConfiguration = new BucketNotificationConfiguration()
                }
                else{
                    def tmp = bucketNotificationConfiguration.configurations.find {
                        it.value instanceof LambdaConfiguration && it.value.functionARN == lambdaFunConf.functionArn
                    }
                    if( tmp != null ){
                        s3LambdaConf = tmp.value as LambdaConfiguration;
                        logger.warn( "Found function {} already listen to bucket {}, config:{} ", lambdaFunctionName,
                                it.key,
                                s3LambdaConf.events )
                    }
                }
                if( s3LambdaConf == null ){
                    s3LambdaConf = new LambdaConfiguration( lambdaFunConf.functionArn, eventNameArr )
                }

                def addPermissionRequest = new AddPermissionRequest().withFunctionName( lambdaFunctionName ).
                        withStatementId( it.key + "-" + lambdaFunctionName ).withAction( "lambda:InvokeFunction" ).
                        withPrincipal( "s3.amazonaws.com" ).withSourceArn( "arn:aws:s3:::" + it.key )
                logger.debug( "addPermissionRequest:{}", addPermissionRequest.toString() )
                try{
                    lambdaClient.addPermission( addPermissionRequest )
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

                bucketNotificationConfiguration.addConfiguration( lambdaFunctionName + "-" + it.key, s3LambdaConf )

                logger.info( "adding trigger events:{} ", s3LambdaConf.events )
                s3Client.setBucketNotificationConfiguration( it.key, bucketNotificationConfiguration )
                logger.info( "function {} has been set to listen to {}, config:{}", lambdaFunctionName, it.key,
                        bucketNotificationConfiguration )
            }
        }
        else{
            logger.warn( "No S3 bucket trigger configurations found" )
        }

    }

}
