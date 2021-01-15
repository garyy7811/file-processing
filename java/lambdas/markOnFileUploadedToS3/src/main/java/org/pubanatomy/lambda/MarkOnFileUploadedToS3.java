package org.pubanatomy.lambda;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class MarkOnFileUploadedToS3 implements RequestHandler<S3Event, String>{

    private static Logger logger = LogManager.getLogger( MarkOnFileUploadedToS3.class );

    private static DynamoDBMapper                     dynamoDBMapper;
    private static DefaultAWSCredentialsProviderChain credentialsProvider;


    public MarkOnFileUploadedToS3(){
        if( credentialsProvider == null ){
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        }
        if( dynamoDBMapper == null ){

            final String tableName = System.getenv( "awsS3UploadDynamoTablename" );

            AmazonDynamoDB dynamoDB = new AmazonDynamoDBClient( credentialsProvider );
            if( dynamoDBMapper == null ){
                dynamoDBMapper = new DynamoDBMapper( dynamoDB, DynamoDBMapperConfig.builder()
                        .withTableNameOverride( new DynamoDBMapperConfig.TableNameOverride( tableName ) ).build() );
            }
        }
    }

    @Override
    public String handleRequest( S3Event input, Context context ){

        try{
            logger.info( "got {} records>>>>>", input.getRecords().size() );
            input.getRecords().forEach( s -> {
                DynaTableAwsS3Upload asu =
                        dynamoDBMapper.load( DynaTableAwsS3Upload.class, s.getS3().getObject().getKey() );
                asu.setUploadedConfirmTimeStamp( s.getEventTime().getMillis() );
                dynamoDBMapper.save( asu );
            } );
            logger.info( "got {} records<<<<<", input.getRecords().size() );
        }
        catch( Throwable e ){
            logger.error( ExceptionUtils.getStackTrace( e ) );
            throw e;
        }

        return "0";
    }
}
