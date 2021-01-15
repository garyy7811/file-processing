package org.pubanatomy.configPerClient;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * encoding.com
 * User: flashflexpro@gmail.com
 * Date: 2/11/2016
 * Time: 2:56 PM
 */
@DynamoDBTable( tableName = "$!overriding me!$" )
@Data
@Document( indexName = "!!overrid me plz!!", type = DynaTableClientConfig.ES_TYPE )
public class DynaTableClientConfig{

    public static final String ES_TYPE = "client-config";

    @Id
    @Field( type = FieldType.String, store = true )
    @DynamoDBHashKey
    private String clientId;


    private String clientName;

    private Long initialTime;

    private Long lastModifyTime;


    @DynamoDBVersionAttribute
    private Long version = null;


    @Field( type = FieldType.Object )
    private DynaTableClientConfigTranscode transcode;

    @Field( type = FieldType.Object )
    private DynaTableClientConfigUpload upload;


}
