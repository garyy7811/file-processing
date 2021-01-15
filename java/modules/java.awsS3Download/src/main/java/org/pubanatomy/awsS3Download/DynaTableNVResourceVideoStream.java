package org.pubanatomy.awsS3Download;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/11/2016
 * Time: 2:56 PM
 */
@Data
@DynamoDBDocument
public class DynaTableNVResourceVideoStream implements Serializable{


    private Long fileSize;
    private Long bitRate;
    private String relativePath;
    private String isDefault;


}
