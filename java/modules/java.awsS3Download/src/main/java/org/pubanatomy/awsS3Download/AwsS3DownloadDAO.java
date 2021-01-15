package org.pubanatomy.awsS3Download;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class AwsS3DownloadDAO{

    private static final Logger logger = LogManager.getLogger( AwsS3DownloadDAO.class );

    public AwsS3DownloadDAO( String awsS3DownloadDynamoTablename ){
        this.awsS3DownloadDynamoTablename = awsS3DownloadDynamoTablename;
    }

    protected String awsS3DownloadDynamoTablename;

    protected AmazonDynamoDB dynamoDB;

    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private void setDynamoDB( AmazonDynamoDB dynamoDB ){
        this.dynamoDB = dynamoDB;
        final DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder().withTableNameOverride(
                DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement( awsS3DownloadDynamoTablename ) )
                .build();

        dynamoDBMapper = new DynamoDBMapper( dynamoDB, mapperConfig );
    }

    public DynamoDBMapper getDynamoDBMapper(){
        return dynamoDBMapper;
    }

    public void save( DynaTableNVResource res ){
        dynamoDBMapper.save( res );
    }

    public DynaTableNVResource load( String sourceKey, String processId ){
        return dynamoDBMapper.load( DynaTableNVResource.class, sourceKey, processId );
    }
}
