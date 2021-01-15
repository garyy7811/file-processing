package org.pubanatomy.awsutils;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.Map;

@Data
@DynamoDBTable( tableName = "!!overrid me plz!!" )
@Document( indexName = "!!overrid me plz!!", type = S3ObjectDetails.ES_TYPE )
public class S3ObjectDetails{

    public static final String ES_TYPE = "s3-obj-details";

    @Id
    @DynamoDBIgnore
    private String esId;

    @DynamoDBHashKey
    private String s3ObjectETag;

    @DynamoDBRangeKey
    private String s3FullPath;

    private String s3BucketName;

    private String s3ObjectKey;

    private Long s3ObjectSizeBytes;

    @Field( type = FieldType.Date )
    private Date s3ObjectLastModified;

    @Field( type = FieldType.Nested )
    private Map<String, String> s3ObjectUserMetadata;

}
