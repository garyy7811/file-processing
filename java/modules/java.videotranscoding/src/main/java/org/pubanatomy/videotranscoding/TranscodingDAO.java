package org.pubanatomy.videotranscoding;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;


public class TranscodingDAO{

    private static final Logger logger = LogManager.getLogger( TranscodingDAO.class );

    public TranscodingDAO( String awsTranscodingDynamoTablename ){
        this.awsTranscodingDynamoTablename = awsTranscodingDynamoTablename;
    }

    private String awsTranscodingDynamoTablename;

    public String getAwsTranscodingDynamoTablename(){
        return awsTranscodingDynamoTablename;
    }

    protected AmazonDynamoDB dynamoDB;

    @Autowired
    private void setDynamoDB( AmazonDynamoDB dynamoDB ){
        this.dynamoDB = dynamoDB;
        final DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder().withTableNameOverride(
                DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement( awsTranscodingDynamoTablename ) )
                .build();

        transcodingDynamoMapper = new DynamoDBMapper( dynamoDB, mapperConfig );
    }


    private DynamoDBMapper transcodingDynamoMapper;

    public DynamoDBMapper getTranscodingDynamoMapper(){
        return transcodingDynamoMapper;
    }

    public void save( DynaTableVideoTranscoding transcoding ){
        transcodingDynamoMapper.save( transcoding );
        logger.debug( transcoding );
    }

    public DynaTableVideoTranscoding loadByMediaId( String mediaId ){
        return transcodingDynamoMapper.load( DynaTableVideoTranscoding.class, mediaId );
    }

    public List<DynaTableVideoTranscoding> loadByStatusLastUpdateTime( String status, long lastUpdateTimeTo,
                                                                       int maxResult, boolean desc ){

        DynaTableVideoTranscoding hashKObject = new DynaTableVideoTranscoding();
        hashKObject.setStatus( status );
        Condition rangeKeyCondition =
                new Condition().withComparisonOperator( desc ? ComparisonOperator.LT : ComparisonOperator.GT )
                        .withAttributeValueList( new AttributeValue().withN( lastUpdateTimeTo + "" ) );
        PaginatedQueryList<DynaTableVideoTranscoding> lst = transcodingDynamoMapper
                .query( DynaTableVideoTranscoding.class, new DynamoDBQueryExpression<DynaTableVideoTranscoding>()
                        .withIndexName( "status_lastUpdateTime" ).withHashKeyValues( hashKObject )
                        .withRangeKeyCondition( "lastUpdateTime", rangeKeyCondition ).withScanIndexForward( ! desc )
                        .withConsistentRead( false ).withLimit( maxResult ) );
        return lst;
    }

    public List<DynaTableVideoTranscoding> loadByBucketkeyCreateTime( String bucketKey, long createTimeTo,
                                                                      int maxResult, boolean desc ){


        DynaTableVideoTranscoding hashKObject = new DynaTableVideoTranscoding();
        hashKObject.setUploadBucketKey( bucketKey );
        Condition rangeKeyCondition =
                new Condition().withComparisonOperator( desc ? ComparisonOperator.LT : ComparisonOperator.GT )
                        .withAttributeValueList( new AttributeValue().withN( createTimeTo + "" ) );
        PaginatedQueryList<DynaTableVideoTranscoding> lst = transcodingDynamoMapper
                .query( DynaTableVideoTranscoding.class, new DynamoDBQueryExpression<DynaTableVideoTranscoding>()
                        .withIndexName( "uploadBucketKey_createTime" ).withHashKeyValues( hashKObject )
                        .withRangeKeyCondition( "createTime", rangeKeyCondition ).withScanIndexForward( ! desc )
                        .withConsistentRead( false ).withLimit( maxResult ) );
        return lst;
    }

    public void save( List<DynaTableVideoTranscoding> transcodingRecordLst ){
        transcodingDynamoMapper.batchSave( transcodingRecordLst );
    }

    public List<DynaTableVideoTranscoding> loadByMediaIdArr( String[] mediaIdArr ){
        List<KeyPair> kLst = Arrays.asList( mediaIdArr ).stream().map( m -> new KeyPair().withHashKey( m ) )
                .collect( Collectors.toList() );

        final HashMap<Class<?>, List<KeyPair>> itemsToGetMap = new HashMap<>();
        itemsToGetMap.put( DynaTableVideoTranscoding.class, kLst );
        Map<String, List<Object>> rslt = transcodingDynamoMapper.batchLoad( itemsToGetMap );
        return rslt.values().stream().findAny().get().stream().map( r -> ( DynaTableVideoTranscoding )r )
                .collect( Collectors.toList() );
    }

}
