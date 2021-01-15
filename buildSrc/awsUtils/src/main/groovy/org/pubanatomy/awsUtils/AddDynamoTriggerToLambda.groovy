package org.pubanatomy.awsUtils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.StreamSpecification
import com.amazonaws.services.dynamodbv2.model.StreamViewType
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest
import com.amazonaws.services.kinesisanalytics.model.InputStartingPosition
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.CreateEventSourceMappingRequest
import com.amazonaws.services.lambda.model.EventSourceMappingConfiguration
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest
import com.amazonaws.services.lambda.model.GetFunctionConfigurationResult
import com.amazonaws.services.lambda.model.ListEventSourceMappingsResult
import com.amazonaws.services.lambda.model.UpdateEventSourceMappingRequest
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * User: flashflexpro@gmail.com
 * Date: 4/11/2016
 * Time: 3:23 PM*/
public class AddDynamoTriggerToLambda extends DefaultTask{

    private static final Logger logger = LogManager.getLogger( AddDynamoTriggerToLambda.class )


    AWSLambdaClient lambdaClient;
    AmazonDynamoDBClient dynamoDB;

    @Input
    public String lambdaFunctionName;

    @Input
    public Map<String, String> tableNameToSizeTypePosition = new HashMap<>()

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

        GetFunctionConfigurationRequest funConfReq = new GetFunctionConfigurationRequest()
        funConfReq.functionName = lambdaFunctionName
        logger.debug( "getting lambda function config with: {}", funConfReq )
        GetFunctionConfigurationResult lambdaFunConf = lambdaClient.getFunctionConfiguration( funConfReq )

        if( tableNameToSizeTypePosition.size() > 0 ){
            if( dynamoDB == null ){
                dynamoDB = new AmazonDynamoDBClient( new DefaultAWSCredentialsProviderChain() );
                dynamoDB.setRegion( RegionUtils.getRegion( awsRegion ) )
            }
            tableNameToSizeTypePosition.each {
                String streamStartingPosition = InputStartingPosition.TRIM_HORIZON.toString();
                String streamType = StreamViewType.NEW_AND_OLD_IMAGES.toString();

                Integer streamBatchSize = 100;

                String[] argArr = it.value.split( "\\|" )

                if( argArr[ 0 ].length() > 0 ){
                    streamBatchSize = Integer.parseInt( argArr[ 0 ] )
                }
                if( argArr.length > 1 ){
                    streamType = argArr[ 1 ]
                }
                if( argArr.length > 2 ){
                    streamStartingPosition = argArr[ 2 ]
                }

                def tableResult = dynamoDB.describeTable( it.key )
                def streamSpec = tableResult.getTable().getStreamSpecification();
                def streamSpecNeedUpdate = true;
                if( streamSpec == null ){
                    logger.info( "No stream spec found for table {}, creating it.", it.key )
                    streamSpec = new StreamSpecification();
                    streamSpec.streamViewType = streamType;
                }
                else{
                    logger.info( "Stream spec found for table{}, type:{}", it.key, streamSpec.streamViewType )
                    if( streamSpec.streamViewType != StreamViewType.NEW_AND_OLD_IMAGES ){
                        logger.warn( "Make sure StreamType {} is correct, usually we use {}",
                                streamSpec.streamViewType,
                                StreamViewType.NEW_AND_OLD_IMAGES )
                    }
                    if( !streamSpec.streamEnabled ){
                        logger.info( "Will try to enabled {}'s stream( was was disabled before build )", it.key )
                    }
                    else{
                        logger.info( "{}'s stream is already enabled with type:{}, no update needed", it.key,
                                streamSpec.streamViewType )
                        streamSpecNeedUpdate = false;
                    }
                }
                if( streamSpecNeedUpdate ){
                    streamSpec.streamEnabled = true;
                    dynamoDB.updateTable(
                            new UpdateTableRequest().withTableName( it.key ).withStreamSpecification( streamSpec ) )
                    tableResult = dynamoDB.describeTable( it.key )
                }

                ListEventSourceMappingsResult existEventMapLst
                existEventMapLst = lambdaClient.listEventSourceMappings()
                EventSourceMappingConfiguration existEventMapping = null;
                if( existEventMapLst.eventSourceMappings.size() > 0 ){
                    existEventMapping = existEventMapLst.eventSourceMappings.find {
                        it.functionArn == lambdaFunConf.functionArn && it.eventSourceArn ==
                                tableResult.getTable().latestStreamArn
                    }
                }
                if( existEventMapping == null ){
                    CreateEventSourceMappingRequest request = new CreateEventSourceMappingRequest()
                    request.setFunctionName( lambdaFunctionName );
                    request.setStartingPosition( streamStartingPosition );
                    request.setBatchSize( streamBatchSize )
                    request.eventSourceArn = tableResult.getTable().latestStreamArn
                    lambdaClient.createEventSourceMapping( request );
                    logger.info( "event mapping created:{}", request )
                }
                else{
                    def stateToLowercase = existEventMapping.state.toLowerCase()
                    if( stateToLowercase == "disabling" || stateToLowercase.indexOf( "delet" ) == 0 ){
                        throw new Error( "Unexpected Event Mapping state:" + existEventMapping.state )
                    }
                    else if( stateToLowercase == "disabled" ){
                        lambdaClient.
                                updateEventSourceMapping(
                                        new UpdateEventSourceMappingRequest().withUUID( existEventMapping.getUUID() ).
                                                withEnabled( true ).withBatchSize( streamBatchSize ) )
                        logger.info( "event mapping found between lambda {} and dynamoDB table {} disabled enabling it",
                                lambdaFunctionName,
                                it.key )
                    }
                    else{
                        logger.info(
                                "event mapping found between lambda {} and dynamoDB table {} nothing to change: {}",
                                lambdaFunctionName,
                                it.key,
                                existEventMapping )
                    }
                }
            }
        }
        else{
            logger.warn( "No dynamoDb table trigger config found" )
        }
    }

}
