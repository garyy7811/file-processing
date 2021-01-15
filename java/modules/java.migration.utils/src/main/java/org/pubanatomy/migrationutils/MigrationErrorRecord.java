package org.pubanatomy.migrationutils;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by greg on 10/5/16.
 */

@Data
@DynamoDBTable( tableName = "!!overrid me plz!!" )
@Document( indexName = "!!overrid me plz!!", type = MigrationErrorRecord.ES_TYPE)
public class MigrationErrorRecord {


    public static final String ES_TYPE = "migration-error-record";

    public MigrationErrorRecord(){

    }

    public MigrationErrorRecord(String errorType, String itemId, String error ){
        this(errorType, itemId, error, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    public MigrationErrorRecord(String errorType, String itemId, String error, String timestamp){
        this.errorType = errorType;
        this.itemId = itemId;
        this.error = error;
        this.timestamp = timestamp;
    }


    @Id
    @Field(type = FieldType.String, store = true )
    @DynamoDBIgnore
    private String esId;

    @DynamoDBHashKey
    private String errorType;

    @DynamoDBRangeKey
    private String itemId;

    private String error;

    private String timestamp;
}
