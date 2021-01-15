package org.pubanatomy.copyResThumbToS3;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import lombok.Data;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class CopyResThumbToS3 implements RequestHandler<S3Event, String>{
    static{
        System.setProperty( "Log4jContextSelector ", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector" );
    }

    private static AmazonS3Client                     s3Client;
    private static DynamoDBMapper                     dynamoDBMapper;
    private static DefaultAWSCredentialsProviderChain credentialsProvider;

    public CopyResThumbToS3() throws IOException{
        if( credentialsProvider == null ){
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        }
        if( s3Client == null ){
            s3Client = new AmazonS3Client( credentialsProvider );
        }
        if( dynamoDBMapper == null ){

            final String tableName = System.getenv( "copyResThumbS3recordTablename" );

            AmazonDynamoDB dynamoDB = new AmazonDynamoDBClient( credentialsProvider );
            if( dynamoDBMapper == null ){
                dynamoDBMapper = new DynamoDBMapper( dynamoDB, DynamoDBMapperConfig.builder()
                        .withTableNameOverride( new DynamoDBMapperConfig.TableNameOverride( tableName ) ).build() );
            }
            CreateTableRequest createTableRequest =
                    dynamoDBMapper.generateCreateTableRequest( DynamoCopyResThumbToS3.class );
            try{
                DescribeTableResult descRslt = dynamoDB.describeTable( createTableRequest.getTableName() );
                return;
            }
            catch( Exception e ){
                if( ! ( e instanceof ResourceNotFoundException ) ){
                    throw e;
                }
            }
            try{
                if( createTableRequest.getLocalSecondaryIndexes() != null ){
                    createTableRequest.getLocalSecondaryIndexes().stream().forEach(
                            l -> l.setProjection( new Projection().withProjectionType( ProjectionType.ALL ) ) );
                }

                if( createTableRequest.getGlobalSecondaryIndexes() != null ){
                    createTableRequest.getGlobalSecondaryIndexes().stream().forEach( g -> {
                        g.setProvisionedThroughput( new ProvisionedThroughput( 9L, 9L ) );
                        g.setProjection( new Projection().withProjectionType( ProjectionType.ALL ) );
                    } );
                }

                createTableRequest.setProvisionedThroughput( new ProvisionedThroughput( 9L, 9L ) );

                dynamoDB.createTable( createTableRequest );
            }
            catch( Exception e ){
                if( e instanceof ResourceInUseException &&
                        "ResourceInUseException".equals( ( ( ResourceInUseException )e ).getErrorCode() ) ){
                    return;
                }
                throw e;
            }

        }
    }

    @Override
    public String handleRequest( S3Event input, Context context ){

        context.getLogger().log( ">>>>>>>>>>>>>>>>>>>>>>>>>" );
        List<DynamoCopyResThumbToS3> lst = input.getRecords().stream().map( s -> {

            final S3EventNotification.S3ObjectEntity s3ObjectEntity = s.getS3().getObject();
            context.getLogger().log( "s.getS3().getObject().getKey()--->>" + s3ObjectEntity.getKey() );
            final S3Object s3Object = s3Client.getObject( s.getS3().getBucket().getName(), s3ObjectEntity.getKey() );
            ObjectMetadata meta = s3Object.getObjectMetadata();
            try{
                s3Object.close();
            }
            catch( IOException e ){
                e.printStackTrace();
            }
            final DynamoCopyResThumbToS3 rt = new DynamoCopyResThumbToS3();

            rt.setS3bucketKey( s3ObjectEntity.getKey() );
            rt.setContentId( meta.getUserMetadata().get( "contentId" ) );
            rt.setResourceId( meta.getUserMetadata().get( "resourceId" ) );
            rt.setResourceType( meta.getUserMetadata().get( "resourceType" ) );
            rt.setResourceVersion( meta.getUserMetadata().get( "resourceVersion" ) );
            rt.setResource_fileName( meta.getUserMetadata().get( "resource_fileName" ) );
            rt.setResource_fileSize( meta.getUserMetadata().get( "resource_fileSize" ) );
            rt.setWidth( meta.getUserMetadata().get( "width" ) );
            rt.setHeight( meta.getUserMetadata().get( "height" ) );
            rt.setResource_org_name( meta.getUserMetadata().get( "resource_org_name" ) );
            rt.setThumb_fileName( meta.getUserMetadata().get( "thumb_fileName" ) );
            rt.setPostFrame_fileName( meta.getUserMetadata().get( "postFrame_fileName" ) );
            rt.setFirstFrame_fileName( meta.getUserMetadata().get( "firstFrame_fileName" ) );

            context.getLogger().log( "s.getS3().getObject().getKey()--->rt>" + rt.toString() );

            return rt;
        } ).collect( Collectors.toList() );

        dynamoDBMapper.batchSave( lst );

        context.getLogger().log( "::::::::::::batchSave:" + lst.size() );

        return "0";
    }

    @DynamoDBTable( tableName = "$!overriding me!$" )
    @Data
    public static class DynamoCopyResThumbToS3{

        @DynamoDBHashKey
        private String s3bucketKey;

        @DynamoDBRangeKey
        private String contentId;

        private String resourceId;
        private String resourceType;
        private String resourceVersion;
        private String resource_fileName;
        private String resource_fileSize;
        private String width;
        private String height;
        private String resource_org_name;
        private String thumb_fileName;
        private String postFrame_fileName;
        private String firstFrame_fileName;

    }

    public static void main( String[] args ) throws IOException{

    }
}
