package org.pubanatomy.indexS3BucketInDDB;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.customshow.awsutils.S3ObjectDetails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Lambda function listening for S3 Events, which inserts new DyamoDB records representing
 * new S3 Objects for the purpose of providing a searchable index of the S3 Bucket.
 */
public class InsertS3ObjectToDdbIndex implements RequestHandler<S3Event, String>{

    private static Logger logger;
    static {
        System.setProperty( "Log4jContextSelector ", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector" );
        logger = LogManager.getLogger(InsertS3ObjectToDdbIndex.class);
        logger.info(">>>>>>>>>>>>>-logger loaded->>>>>>>>>>>>>");
    }

    private static AmazonS3Client                     s3Client;
    private static DynamoDBMapper                     dynamoDBMapper;
    private static DefaultAWSCredentialsProviderChain credentialsProvider;

    private long constructorStartdMillis;
    private String millisSinceConstructor() {
        return " (" + (System.currentTimeMillis()-constructorStartdMillis) + ")";
    }

    public InsertS3ObjectToDdbIndex() throws IOException{

        constructorStartdMillis = System.currentTimeMillis();

        logger.info(">>>>>>>>>>>>>-constructing new InsertS3ObjectToDdbIndex->>>>>>>>>>>>>");

        if( credentialsProvider == null ){
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
            logger.info("loaded DefaultAWSCredentialsProviderChain ");
        }

        if( s3Client == null ){
            s3Client = new AmazonS3Client( credentialsProvider );
            logger.info("loaded AmazonS3Client" + millisSinceConstructor());
        }

        if( dynamoDBMapper == null ){

            Properties properties = new Properties();
            final InputStream inputStream = getClass().getResourceAsStream( "/config.properties" );
            properties.load( inputStream );
            inputStream.close();

            logger.info("loaded config.properties" + millisSinceConstructor());

            final String tableName = properties.getProperty( "indexS3Bucket.ddb.table" );

            logger.info("configuring dynamoDB table: " + tableName + millisSinceConstructor());


            AmazonDynamoDB dynamoDB = new AmazonDynamoDBClient( credentialsProvider );
            if( dynamoDBMapper == null ){
                dynamoDBMapper = new DynamoDBMapper( dynamoDB, DynamoDBMapperConfig.builder()
                        .withTableNameOverride( new DynamoDBMapperConfig.TableNameOverride( tableName ) ).build() );
            }

            logger.info("dynamoDBMapper initialized" + millisSinceConstructor());
        }

        logger.info("<<<<<<<<<<-constructor-complete-<<<<<<<<<<" + millisSinceConstructor());
    }

    @Override
    public String handleRequest( S3Event input, Context context ){

        logger.info( ">>>>>>>>>>>>>-handleRequest-entered->>>>>>>>>>>>"  + millisSinceConstructor());

        List<S3ObjectDetails> lst = input.getRecords().stream().map( s -> {

            final String s3BucketName = s.getS3().getBucket().getName();
            final S3EventNotification.S3ObjectEntity s3ObjectEntity = s.getS3().getObject();
            final String s3ObjectKey = s3ObjectEntity.getKey();

            logger.info("processing {} - {}", s3BucketName, s3ObjectKey);

            final ObjectMetadata objectMetadata = s3Client.getObjectMetadata( s3BucketName, s3ObjectKey );

            logger.info("fetched objectMetadata from S3");

            final S3ObjectDetails s3ObjectDetails = new S3ObjectDetails();

            // We're using ETag as the partition key since it is reasonably randomly distributed
            s3ObjectDetails.setS3ObjectETag(objectMetadata.getETag());

            // full path is the new sort/range key since it will remain fully unique across all
            // S3 Buckets (we index multiple buckets in a single DDB table)
            String s3FullPath = s3BucketName + "/" + s3ObjectKey;
            s3ObjectDetails.setS3FullPath(s3FullPath);

            s3ObjectDetails.setS3BucketName(s3BucketName);
            s3ObjectDetails.setS3ObjectKey(s3ObjectKey);

            s3ObjectDetails.setS3ObjectLastModified(objectMetadata.getLastModified());
            s3ObjectDetails.setS3ObjectSizeBytes(objectMetadata.getContentLength());

            Map<String, String> s3UserMetadata = new HashMap<>();
            s3UserMetadata.putAll(objectMetadata.getUserMetadata());
            s3ObjectDetails.setS3ObjectUserMetadata(s3UserMetadata);

            logger.info("returning S3ObjectDetails: {}", s3ObjectDetails);

            return s3ObjectDetails;

        } ).collect( Collectors.toList() );

        logger.info("starting batchSave on " + lst.size() + " item(s)");

        dynamoDBMapper.batchSave( lst );

        logger.info("batchSave complete");

        logger.info( "<<<<<<<<<<-handleRequest-complete-<<<<<<<<<<" );

        return "0";
    }


    public static void main( String[] args ) throws IOException{

    }
}
