package org.pubanatomy.awsS3Upload;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/11/2016
 * Time: 2:56 PM
 */
@Data
@DynamoDBTable( tableName = "$!overriding me!$" )
@Document( indexName = "!!overrid me plz!!", type = DynaTableAwsS3Upload.ES_TYPE )
public class DynaTableAwsS3Upload implements Serializable{

    public static final String ES_TYPE = "aws-s3-upload";

    @DynamoDBVersionAttribute
    private Long version = null;

    //>>> input from client
    //[_fileReference.creationDate, _fileReference.modificationDate, _fileReference.creator, _fileReference.name, _fileReference.size, _fileReference.type]
    private String csSessionId;

    private Long fileRefSizeBytes;

    private Long fileRefCreationDate;
    private Long fileRefModificationDate;

    private String fileRefCreator;
    private String fileRefName;
    private String fileRefType;
    private String extraMsg;
    private String errorMsg;
    //<<<

    //query Mysql with csSessionId
    @DynamoDBIndexHashKey( globalSecondaryIndexName = "userId_applyTime" )
    private String userId;

    private String clientId;

    //config
    private String s3Bucket;

    //clientId/userId/csSessionId/applyTimeStamp
    @Id
    @Field(type = FieldType.String, store = true )
    @DynamoDBHashKey
    private String s3BucketKey;

    private String awSAccessKeyId;

    @DynamoDBIndexRangeKey( globalSecondaryIndexName = "userId_applyTime" )
    private Long applyTimeStamp;
    private Long uploadedByClientTime;

    //Name is used to trigger another logic
    private Long uploadedConfirmTimeStamp;

}
