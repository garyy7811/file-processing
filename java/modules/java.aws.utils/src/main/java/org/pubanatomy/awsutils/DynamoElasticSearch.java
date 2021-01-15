package org.pubanatomy.awsutils;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.mapping.PersistentProperty;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 9/19/2016
 */
public class DynamoElasticSearch{

    public DynamoElasticSearch( Map<String, Class> dynamoDbTableSet, String indexName ){
        this.dynamoDbTableSet = dynamoDbTableSet;
        this.indexName = indexName;
    }

    private static final Logger logger = LogManager.getLogger( DynamoElasticSearch.class );

    private Map<String, Class> dynamoDbTableSet;
    private String             indexName;

    public Map<String, Class> getDynamoDbTableSet(){
        return dynamoDbTableSet;
    }

    public String getIndexName(){
        return indexName;
    }

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private AmazonDynamoDB dynamoDB;


    public String handleRequest( DynamodbEvent input ){
        try{
            logger.debug( new ObjectMapper().writeValueAsString( input ) );
        }
        catch( JsonProcessingException e ){
            logger.error( e );
        }

        //todo: look into how to use bulk API to make this more efficient
        logger.info( "{} records to process >>>>>", input.getRecords().size() );
        input.getRecords().forEach( eachRecord -> {
            logger.info( "event name:{}", eachRecord.getEventName() );
            try{
                handleChange( dynamoDbTableSet.keySet().stream().filter( a -> {
                    return eachRecord.getEventSourceARN().indexOf( "table/" + a ) > 0;
                } ).findFirst().get(), eachRecord.getDynamodb() );
            }
            catch( Exception e ){
                logger.error( e );
                throw new Error( e );
            }

        } );
        logger.info( "{} records processed <<<<< ", input.getRecords().size() );

        return "0";
    }

    private void handleChange( String tableName, StreamRecord eachRecordChange )
            throws InvocationTargetException, IllegalAccessException, IOException{

        Class dynamoClass = dynamoDbTableSet.get( tableName );
        ElasticsearchPersistentEntity persistentEntity = elasticsearchTemplate.getPersistentEntityFor( dynamoClass );

        PersistentProperty idProperty = persistentEntity.getIdProperty();

        final DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride( DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement( tableName ) )
                .build();

        DynamoDBMapper dynamoMapper = new DynamoDBMapper( dynamoDB, mapperConfig );

        String esId;
        if( eachRecordChange.getKeys().size() == 1 ){
            esId = eachRecordChange.getKeys().values().iterator().next().getS();
        }
        else{
            final List<KeySchemaElement> keySchema = getKeySchemaLst( tableName );
            esId = eachRecordChange.getKeys().get( keySchema.get( 0 ).getAttributeName() ).getS() + "∈@∋" +
                    eachRecordChange.getKeys().get( keySchema.get( 1 ).getAttributeName() ).getS();
        }

        if( eachRecordChange.getNewImage() != null ){
            final Object newImgInst = dynamoMapper.marshallIntoObject( dynamoClass, eachRecordChange.getNewImage() );
            BeanUtils.getPropertyDescriptor( dynamoClass, idProperty.getName() ).getWriteMethod()
                    .invoke( newImgInst, esId );
            PersistentProperty parentIdProperty = persistentEntity.getParentIdProperty();


            logger.debug( ">>>index:{}", esId );
            final IndexQuery idx = new IndexQuery();
            idx.setObject( newImgInst );
            idx.setId( esId );
            idx.setIndexName( indexName );

            if( parentIdProperty != null ){
                idx.setParentId( ( String )BeanUtils.getPropertyDescriptor( dynamoClass, parentIdProperty.getName() )
                        .getReadMethod().invoke( newImgInst ) );
            }
            elasticsearchTemplate.index( idx );
            logger.debug( "<<<index:{}", esId );
        }
        //delete
        else{
            logger.debug( ">>>deleting:{}", esId );

            DeleteRequestBuilder delReq =
                    elasticsearchTemplate.getClient().prepareDelete( indexName, persistentEntity.getIndexType(), esId );
            if( persistentEntity.getParentIdProperty() != null ){
                final Object oldImgInst =
                        dynamoMapper.marshallIntoObject( dynamoClass, eachRecordChange.getOldImage() );

                delReq.setParent( ( String )BeanUtils
                        .getPropertyDescriptor( dynamoClass, persistentEntity.getParentIdProperty().getName() )
                        .getReadMethod().invoke( oldImgInst ) );
            }

            if( delReq.get().isFound() ){
                logger.debug( "<<<deleted:{}", esId );
            }
            else{
                logger.warn( "IndexNotFoundException when deleting:{}", esId );
            }

        }
    }

    private static volatile HashMap<String, List<KeySchemaElement>> tablename2keyschemalstcache = new HashMap<>();

    private List<KeySchemaElement> getKeySchemaLst( String tableName ){
        List<KeySchemaElement> rt = tablename2keyschemalstcache.computeIfAbsent( tableName,
                k -> dynamoDB.describeTable( tableName ).getTable().getKeySchema().stream().sorted( ( a, b ) -> {
                    return a.getKeyType().equals( "HASH" ) ? 1 : - 1;
                } ).collect( Collectors.toList() ) );
        return rt;
    }

}
