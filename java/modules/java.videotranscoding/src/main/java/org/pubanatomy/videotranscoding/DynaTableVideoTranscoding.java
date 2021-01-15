package org.pubanatomy.videotranscoding;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.pubanatomy.configPerClient.DynaTableClientConfigTranscodeFormat;
import org.pubanatomy.siutils.ClientInfo;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;


import java.io.Serializable;
import java.util.List;

/**
 * encoding.com
 * User: flashflexpro@gmail.com
 * Date: 2/11/2016
 * Time: 2:56 PM
 */
@DynamoDBTable( tableName = "$!overriding me!$" )
@Data
@Document( indexName = "!!overrid me plz!!", type = DynaTableVideoTranscoding.ES_TYPE )
public class DynaTableVideoTranscoding implements Serializable{

    public static final String ES_TYPE = "video-transcoding";

    @DynamoDBVersionAttribute
    private Long version = null;

    @Parent( type = DynaTableAwsS3Upload.ES_TYPE )
    @Field( type = FieldType.String, store = true )
    @DynamoDBIndexHashKey( globalSecondaryIndexName = "uploadBucketKey_createTime" )
    private String uploadBucketKey;


    @Id
    @Field( type = FieldType.String, store = true )
    @DynamoDBHashKey
    private String mediaId;

    @DynamoDBIndexRangeKey( globalSecondaryIndexName = "uploadBucketKey_createTime" )
    private Long createTime;

    @DynamoDBIndexRangeKey( globalSecondaryIndexName = "status_lastUpdateTime" )
    private Long lastUpdateTime;

    @ClientInfo( enumStrings = { "Ready to process", "Processing", "Error", "wrong_input", "RETRY_421", "Saved",
            "Finished" } )
    @DynamoDBIndexHashKey( globalSecondaryIndexName = "status_lastUpdateTime" )
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String status;

    private String errorMsg;

    @Field( type = FieldType.Object )
    private DynaTableVideoTranscodingMediaInfo mediaInfo;

    @Field( type = FieldType.Nested )
    private List<DynaTableClientConfigTranscodeFormat> formats;

}
