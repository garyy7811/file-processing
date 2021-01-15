package org.pubanatomy.awsS3Upload;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import org.pubanatomy.loginverify.DynaLogInSessionInfo;
import org.pubanatomy.loginverify.DynamoLoginInfoDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class AwsS3UploadDAO{

    private static final Logger logger = LogManager.getLogger( AwsS3UploadDAO.class );

    public AwsS3UploadDAO( String awsS3UploadDynamoTablename ){
        this.awsS3UploadDynamoTablename = awsS3UploadDynamoTablename;
    }

    private String awsS3UploadDynamoTablename;

    public String getAwsS3UploadDynamoTablename(){
        return awsS3UploadDynamoTablename;
    }

    protected AmazonDynamoDB dynamoDB;
    protected DynamoDBMapper dynamoDBMapper;

    public DynamoDBMapper getDynamoDBMapper(){
        return dynamoDBMapper;
    }

    @Autowired
    private void setDynamoDB( AmazonDynamoDB dynamoDB ){
        this.dynamoDB = dynamoDB;
        final DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder().withTableNameOverride(
                DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement( awsS3UploadDynamoTablename ) ).build();

        dynamoDBMapper = new DynamoDBMapper( dynamoDB, mapperConfig );
    }

    public DynaTableAwsS3Upload loadUpload( String uploadS3Key ){
        logger.debug( "key:{}", uploadS3Key );
        return dynamoDBMapper.load( DynaTableAwsS3Upload.class, uploadS3Key );
    }

    public void saveUpload( DynaTableAwsS3Upload upload ){
        dynamoDBMapper.save( upload );

    }

    public List<DynaTableAwsS3Upload> loadByUserId( String userId, long uploadTimeTo, int maxResult, boolean desc ){
        logger.debug( "userId:{}, uploadTimeTo:{}, max:{}, desc:{}", userId, uploadTimeTo, maxResult, desc );
        DynaTableAwsS3Upload hashKObject = new DynaTableAwsS3Upload();
        hashKObject.setUserId( userId );
        Condition rangeKeyCondition =
                new Condition().withComparisonOperator( desc ? ComparisonOperator.LT : ComparisonOperator.GT )
                        .withAttributeValueList( new AttributeValue().withN( uploadTimeTo + "" ) );
        PaginatedQueryList<DynaTableAwsS3Upload> lst = dynamoDBMapper.query( DynaTableAwsS3Upload.class,
                new DynamoDBQueryExpression<DynaTableAwsS3Upload>().withIndexName( "userId_applyTime" )
                        .withHashKeyValues( hashKObject ).withRangeKeyCondition( "applyTimeStamp", rangeKeyCondition )
                        .withScanIndexForward( ! desc ).withConsistentRead( false ).withLimit( maxResult ) );
        return lst;
    }

    @Autowired
    private DynamoLoginInfoDAO loginInfoDAO;

    public DynaTableAwsS3Upload loadUpload( String csSessionId, Long uploadedFileUploadApplyTime )
            throws IllegalAccessException{
        return loadUpload( getUploadKeyWithSessionId( csSessionId, uploadedFileUploadApplyTime.toString() ) );
    }

    public String getUploadKeyWithSessionId( String sessionId, String uploadApplyTime )
            throws IllegalAccessException{
        if( uploadApplyTime == null ){
            throw new IllegalAccessException( "requireUploadApplyTime" );
        }
        DynaLogInSessionInfo si = loginInfoDAO.loadCsSessionInfo( sessionId, true );
        final String rt = si.getClientId() + "/" + si.getUserId() + "/" + si.getCsSessionId() + "/" + uploadApplyTime;
        logger.debug( "returning:{}", rt );
        return rt;
    }


}
