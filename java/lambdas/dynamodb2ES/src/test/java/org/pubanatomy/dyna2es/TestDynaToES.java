package org.pubanatomy.dyna2es;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.OperationType;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.pubanatomy.awsutils.DynamoElasticSearch;
import org.pubanatomy.awsutils.S3ObjectDetails;
import org.pubanatomy.configPerClient.ConfigPerClientDAO;
import org.pubanatomy.configPerClient.DynaTableClientConfig;
import org.pubanatomy.configPerClient.DynaTableClientConfigTranscodeFormat;
import org.pubanatomy.videotranscoding.DynaTableVideoTranscoding;
import org.pubanatomy.videotranscoding.TranscodingFunctions;
import org.pubanatomy.videotranscoding.TranscodingReportingService;
import org.pubanatomy.videotranscoding.api.FetchJobsPerDayRequest;
import org.pubanatomy.videotranscoding.api.FetchJobsPerDayResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.HasChildQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.support.QueryInnerHitBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.MappingBA;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 11/9/2016
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-test.xml" )
public class TestDynaToES{

    private static final Logger logger = LogManager.getLogger( TestDynaToES.class );


    @Autowired
    private ElasticsearchTemplate esTemplate;

    @Autowired
    private DynamoElasticSearch de;

    @Autowired
    private ConfigPerClientDAO clientDAO;

    @Before
    public void setup() throws IOException{
        esTemplate.deleteIndex( de.getIndexName() );
        esTemplate.createIndex( de.getIndexName() );

        esTemplate.refresh( de.getIndexName() );
    }


    @Autowired
    private TranscodingReportingService transcodingReportingService;


    @Value( "es-transc-record.json" )
    private Resource esTranscodeRecord;

    @Test
    public void testTranscLambdas() throws IOException, ParseException{
        addMapping( DynaTableVideoTranscoding.class );

        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ssZ" );

        System.out.println( sdf.format( new Date() ) );

        //2017-03-01 17:58:40-0500

        final String[] soureArr = { "2017-02-01 01:58:40-0500@" + TranscodingFunctions.Result.Status_new,
                "2017-02-01 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-01 05:08:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-01 05:18:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-01 11:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-01 17:58:40-0500@" + TranscodingFunctions.Result.STATUS_processing,
                "2017-02-01 19:58:40-0500@" + TranscodingFunctions.Result.STATUS_processing,
                "2017-02-01 21:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-01 07:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,

                "2017-02-02 01:58:40-0500@" + TranscodingFunctions.Result.Status_new,
                "2017-02-02 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-02 05:08:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-02 07:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-02 17:18:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-02 17:52:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-02 10:38:40-0500@" + TranscodingFunctions.Result.STATUS_readToProcess,
                "2017-02-02 17:51:40-0500@" + TranscodingFunctions.Result.STATUS_deleted,


                "2017-02-03 01:58:40-0500@" + TranscodingFunctions.Result.Status_new,
                "2017-02-03 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-03 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-03 05:08:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-03 05:18:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-03 11:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-03 17:58:40-0500@" + TranscodingFunctions.Result.STATUS_processing,
                "2017-02-03 19:58:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-03 19:58:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-03 19:58:40-0500@" + TranscodingFunctions.Result.STATUS_saved,
                "2017-02-03 21:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-03 07:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-03 07:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-03 17:18:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-03 17:18:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-03 17:18:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-03 17:52:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-03 10:38:40-0500@" + TranscodingFunctions.Result.STATUS_readToProcess,


                "2017-02-04 01:58:40-0500@" + TranscodingFunctions.Result.Status_new,
                "2017-02-04 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-04 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-04 05:08:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-04 05:18:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-04 11:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-04 07:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-04 17:18:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-04 17:18:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-04 17:18:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-04 17:52:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-04 10:38:40-0500@" + TranscodingFunctions.Result.STATUS_readToProcess,


                "2017-02-05 01:58:40-0500@" + TranscodingFunctions.Result.Status_new,
                "2017-02-05 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-05 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-05 07:58:40-0500@" + TranscodingFunctions.Result.STATUS_error,
                "2017-02-05 17:52:40-0500@" + TranscodingFunctions.Result.STATUS_retry_421,
                "2017-02-05 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-05 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-05 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-05 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-05 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_finished,

                "2017-02-06 01:58:40-0500@" + TranscodingFunctions.Result.Status_new,
                "2017-02-06 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-06 10:38:40-0500@" + TranscodingFunctions.Result.STATUS_readToProcess,
                "2017-02-06 01:58:40-0500@" + TranscodingFunctions.Result.Status_new,
                "2017-02-06 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_wrong_input,
                "2017-02-06 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_saved,
                "2017-02-06 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-06 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_finished,
                "2017-02-06 04:58:40-0500@" + TranscodingFunctions.Result.STATUS_finished


        };

        final ObjectMapper objectMapper = new ObjectMapper();
        Arrays.stream( soureArr ).forEach( s -> {
            String[] dats = s.split( "@" );
            Date d;
            try{
                d = sdf.parse( dats[ 0 ] );
            }
            catch( ParseException e ){
                throw new Error( e );
            }
            final IndexQuery indexQuery = new IndexQuery();
            DynaTableVideoTranscoding transcoding = null;
            try{
                transcoding =
                        objectMapper.readValue( esTranscodeRecord.getInputStream(), DynaTableVideoTranscoding.class );
            }
            catch( IOException e ){
                throw new Error( e );
            }
            transcoding.setMediaId( "mediaId:" + d.getTime() );
            transcoding.setLastUpdateTime( d.getTime() );
            transcoding.setUploadBucketKey( "parent:" + d.getTime() );
            transcoding.setStatus( dats[ 1 ] );
            indexQuery.setObject( transcoding );
            indexQuery.setIndexName( de.getIndexName() );
            indexQuery.setParentId( transcoding.getUploadBucketKey() );
            esTemplate.index( indexQuery );
        } );

        esTemplate.refresh( de.getIndexName() );

        final FetchJobsPerDayRequest jobsPerDayRequest = new FetchJobsPerDayRequest();

        jobsPerDayRequest.setStartDate( sdf.parse( "2017-02-06 09:03:08-0500" ) );
        jobsPerDayRequest.setEndDate( jobsPerDayRequest.getStartDate() );
        FetchJobsPerDayResponse resp = transcodingReportingService.fetchTranscodingJobsPerDay( jobsPerDayRequest );


        Assert.assertEquals( 1486357200000L, resp.getItems().get( 0 ).getDate().getTime() );
        Assert.assertEquals( 9, resp.getItems().get( 0 ).getJobsCount() );
        Assert.assertEquals( 2, resp.getItems().get( 0 ).getStatusToCount().get( TranscodingFunctions.Result.Status_new.toLowerCase() ).intValue() );
        Assert.assertEquals( 3, resp.getItems().get( 0 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_finished.toLowerCase() ).intValue() );
        Assert.assertEquals( 2, resp.getItems().get( 0 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_wrong_input.toLowerCase() ).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 0 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_readToProcess.toLowerCase() ).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 0 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_saved.toLowerCase() ).intValue() );

        jobsPerDayRequest.setStartDate( sdf.parse( "2017-01-31 09:03:08-0500" ) );
        jobsPerDayRequest.setEndDate( sdf.parse( "2017-02-05 09:03:08-0500" ) );
        resp = transcodingReportingService.fetchTranscodingJobsPerDay( jobsPerDayRequest );

        Assert.assertEquals( 57L, resp.getTotalResults().longValue() );
        Assert.assertEquals( 6, resp.getItems().size() );

        Assert.assertEquals( 1485838800000L, resp.getItems().get( 0 ).getDate().getTime() );
        Assert.assertEquals( 0, resp.getItems().get( 0 ).getJobsCount() );
//        Assert.assertEquals( 0, resp.getItems().get( 0 ).getStatusToCount() );

        Assert.assertEquals( 1485925200000L, resp.getItems().get( 1 ).getDate().getTime() );
        Assert.assertEquals( 9, resp.getItems().get( 1 ).getJobsCount() );
        Assert.assertEquals( 1, resp.getItems().get( 1 ).getStatusToCount().get( TranscodingFunctions.Result.Status_new.toLowerCase() ).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 1 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_wrong_input.toLowerCase()).intValue() );
        Assert.assertEquals( 2, resp.getItems().get( 1 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_processing.toLowerCase() ).intValue() );
        Assert.assertEquals( 2, resp.getItems().get( 1 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_finished.toLowerCase() ).intValue() );
        Assert.assertEquals( 3, resp.getItems().get( 1 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_error.toLowerCase() ).intValue() );

        Assert.assertEquals( 1486011600000L, resp.getItems().get( 2 ).getDate().getTime() );
        Assert.assertEquals( 8, resp.getItems().get( 2 ).getJobsCount() );
        Assert.assertEquals( 1, resp.getItems().get( 2 ).getStatusToCount().get( TranscodingFunctions.Result.Status_new.toLowerCase() ).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 2 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_finished.toLowerCase() ).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 2 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_readToProcess.toLowerCase() ).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 2 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_wrong_input.toLowerCase() ).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 2 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_error.toLowerCase() ).intValue() );
        Assert.assertEquals( 2, resp.getItems().get( 2 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_retry_421.toLowerCase() ).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 2 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_deleted.toLowerCase() ).intValue() );

        Assert.assertEquals( 1486098000000L, resp.getItems().get( 3 ).getDate().getTime() );
        Assert.assertEquals( 18, resp.getItems().get( 3 ).getJobsCount() );
        Assert.assertEquals( 4, resp.getItems().get( 3 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_finished.toLowerCase()).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 3 ).getStatusToCount().get( TranscodingFunctions.Result.Status_new.toLowerCase() ).intValue() );
        Assert.assertEquals( 2, resp.getItems().get( 3 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_wrong_input.toLowerCase()).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 3 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_saved.toLowerCase() ).intValue() );
        Assert.assertEquals( 4, resp.getItems().get( 3 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_error.toLowerCase() ).intValue() );
        Assert.assertEquals( 4, resp.getItems().get( 3 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_retry_421.toLowerCase()).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 3 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_processing.toLowerCase()).intValue() );

        Assert.assertEquals( 1486184400000L, resp.getItems().get( 4 ).getDate().getTime() );
        Assert.assertEquals( 12, resp.getItems().get( 4 ).getJobsCount() );
        Assert.assertEquals( 2, resp.getItems().get( 4 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_finished.toLowerCase()).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 4 ).getStatusToCount().get( TranscodingFunctions.Result.Status_new.toLowerCase() ).intValue() );
        Assert.assertEquals( 2, resp.getItems().get( 4 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_wrong_input.toLowerCase()).intValue() );
        Assert.assertEquals( 1, resp.getItems().get( 4 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_readToProcess.toLowerCase() ).intValue() );
        Assert.assertEquals( 2, resp.getItems().get( 4 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_error.toLowerCase() ).intValue() );
        Assert.assertEquals( 4, resp.getItems().get( 4 ).getStatusToCount().get( TranscodingFunctions.Result.STATUS_retry_421.toLowerCase()).intValue() );

        Assert.assertEquals( 1486270800000L, resp.getItems().get( 5 ).getDate().getTime() );
        Assert.assertEquals( 10, resp.getItems().get( 5 ).getJobsCount() );
    }


    @Test
    public void testObjectDetails() throws IOException{
        addMapping( S3ObjectDetails.class );

        final String indexName = de.getIndexName();

        ElasticsearchPersistentEntity pe = esTemplate.getPersistentEntityFor( S3ObjectDetails.class );

        final S3ObjectDetails s3ObjectDetails = new S3ObjectDetails();

        s3ObjectDetails.setS3FullPath( "a/bc/d" );
        s3ObjectDetails.setS3ObjectETag( "setS3ObjectETag" );
        s3ObjectDetails.setS3BucketName( "testabdc" );
        s3ObjectDetails.setS3ObjectLastModified( new Date() );
        final HashMap<String, String> metadata = new HashMap<>();
        metadata.put( "content-type", "xml/text" );
        metadata.put( "length", "13892382" );
        s3ObjectDetails.setS3ObjectUserMetadata( metadata );

        final String tmpId = "TmpID" + System.currentTimeMillis();


        final IndexQuery idx = new IndexQuery();
        idx.setId( tmpId );
        idx.setIndexName( indexName );
        //        idx.setType( pe.getIndexType() );

        idx.setObject( s3ObjectDetails );

        esTemplate.index( idx );
        esTemplate.refresh( indexName );

        final NativeSearchQuery query = new NativeSearchQuery( QueryBuilders.matchAllQuery() );
        query.setIds( Arrays.asList( tmpId ) );
        query.addIndices( indexName );
        query.addTypes( pe.getIndexType() );
        esTemplate.query( query, ( ResultsExtractor<S3ObjectDetails> )response -> {
            try{
                S3ObjectDetails rslt =
                        new ObjectMapper().readValue( response.getHits().getAt( 0 ).source(), S3ObjectDetails.class );
                s3ObjectDetails.setEsId( rslt.getEsId() );
                Assert.assertEquals( s3ObjectDetails, rslt );
            }
            catch( IOException e ){
                e.printStackTrace();
            }
            return null;
        } );
    }

    @Test
    public void testSaveClientConfig() throws IOException{
        addMapping( DynaTableClientConfig.class );

        final String indexName = de.getIndexName();

        ElasticsearchPersistentEntity pe = esTemplate.getPersistentEntityFor( DynaTableClientConfig.class );

        final DynaTableClientConfig clientConfig = ConfigPerClientDAO.getDefaultDynaTableClientConfig( "1" );

        final String tmpId = "TmpID" + System.currentTimeMillis();


        final IndexQuery idx = new IndexQuery();
        idx.setId( tmpId );
        idx.setIndexName( indexName );
        idx.setType( pe.getIndexType() );

        idx.setObject( clientConfig );

        esTemplate.index( idx );
        esTemplate.refresh( indexName );

        clientConfig.getTranscode().getFormats().get( DynaTableClientConfigTranscodeFormat.DEFAULT_VIDEO )
                .setDestination( "abcddee" );

        esTemplate.index( idx );
        esTemplate.refresh( indexName );

        final NativeSearchQuery query = new NativeSearchQuery( QueryBuilders.matchAllQuery() );
        query.setIds( Arrays.asList( tmpId ) );
        query.addIndices( indexName );
        query.addTypes( pe.getIndexType() );
        DynaTableClientConfig r = esTemplate.query( query, new ResultsExtractor<DynaTableClientConfig>(){
            @Override
            public DynaTableClientConfig extract( SearchResponse response ){
                try{
                    DynaTableClientConfig rslt = new ObjectMapper()
                            .readValue( response.getHits().getAt( 0 ).source(), DynaTableClientConfig.class );
                    Assert.assertEquals( clientConfig, rslt );
                }
                catch( IOException e ){
                    e.printStackTrace();
                }
                return null;
            }
        } );

    }

    @Autowired
    private AmazonDynamoDBClient dynamoDBClient;


    @Value( "dyna-transc-change.json" )
    private Resource dynaTranscChange;

    @Value( "dyna-transc-record.json" )
    private Resource dynaTranscRecord;

    @Value( "dyna-upload-record.json" )
    private Resource dynaResUploadRecord;

    @Test
    public void testHandleNewTranscodeRecord() throws IOException{

        final String transcodingType =
                esTemplate.getPersistentEntityFor( DynaTableVideoTranscoding.class ).getIndexType();
        final String uploadType = esTemplate.getPersistentEntityFor( DynaTableAwsS3Upload.class ).getIndexType();

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn( com.amazonaws.services.dynamodbv2.model.Record.class, IgnoreSetEvNameMixIn.class );
        objectMapper
                .addMixIn( com.amazonaws.services.dynamodbv2.model.StreamRecord.class, IgnoreSetStrNameMixIn.class );

        DynamoDBMapper uploadDynaMapper = getDynamoDBMapper( DynaTableAwsS3Upload.class );
        DynamoDBMapper trancDynaMapper = getDynamoDBMapper( DynaTableVideoTranscoding.class );

        addMapping( DynaTableVideoTranscoding.class );
        addMapping( DynaTableAwsS3Upload.class );


        final DynamodbEvent uploadRecordEv =
                objectMapper.readValue( this.dynaResUploadRecord.getInputStream(), DynamodbEvent.class );
        replaceTableNameWithEnv( uploadRecordEv, "cs-cloud-dev-gary--upload", DynaTableAwsS3Upload.class );


        final DynaTableAwsS3Upload uploadRecordFromNewImg = uploadDynaMapper
                .marshallIntoObject( DynaTableAwsS3Upload.class,
                        uploadRecordEv.getRecords().get( 0 ).getDynamodb().getNewImage() );

        de.handleRequest( uploadRecordEv );


        final DynamodbEvent trancodeRecordEv =
                objectMapper.readValue( dynaTranscRecord.getInputStream(), DynamodbEvent.class );
        replaceTableNameWithEnv( trancodeRecordEv, "cs-cloud-dev-gary--transcoding", DynaTableVideoTranscoding.class );

        DynaTableVideoTranscoding transcRecordFromImg = trancDynaMapper
                .marshallIntoObject( DynaTableVideoTranscoding.class,
                        trancodeRecordEv.getRecords().get( 0 ).getDynamodb().getNewImage() );

        DynamodbEvent transcodeUpdateEv =
                objectMapper.readValue( dynaTranscChange.getInputStream(), DynamodbEvent.class );
        replaceTableNameWithEnv( transcodeUpdateEv, "cs-cloud-dev-gary--transcoding", DynaTableVideoTranscoding.class );

        //make sure the changeEvent's old image is the same as in ES
        transcodeUpdateEv.getRecords().get( 0 ).getDynamodb()
                .setOldImage( trancodeRecordEv.getRecords().get( 0 ).getDynamodb().getNewImage() );

        de.handleRequest( trancodeRecordEv );//inserted

        de.handleRequest( transcodeUpdateEv );//updated


        List<DynaTableVideoTranscoding> newTranscImg = transcodeUpdateEv.getRecords().stream().map( i -> {
            return trancDynaMapper.marshallIntoObject( DynaTableVideoTranscoding.class, i.getDynamodb().getNewImage() );
        } ).collect( Collectors.toList() );


        esTemplate.refresh( de.getIndexName() );

        NativeSearchQuery ns = new NativeSearchQuery( QueryBuilders.matchAllQuery() );
        ns.addIndices( de.getIndexName() );
        ns.setIds( Arrays.asList( newTranscImg.get( 0 ).getUploadBucketKey() ) );
        List<DynaTableVideoTranscoding> rslt = esTemplate.queryForList( ns, DynaTableVideoTranscoding.class );

        Assert.assertEquals( newTranscImg.get( 0 ), rslt.get( 0 ) );

        ns = new NativeSearchQuery( QueryBuilders.matchAllQuery() );
        ns.addIndices( de.getIndexName() );
        ns.setIds( Arrays.asList( newTranscImg.get( 0 ).getUploadBucketKey() ) );


        Assert.assertEquals( uploadRecordFromNewImg,
                esTemplate.queryForList( ns, DynaTableAwsS3Upload.class ).get( 0 ) );

        final NativeSearchQuery hasChildNs = new NativeSearchQuery( QueryBuilders.hasChildQuery( transcodingType,
                QueryBuilders.termQuery( "mediaId", transcRecordFromImg.getMediaId() ) ) );
        hasChildNs.addIndices( de.getIndexName() );
        List<DynaTableAwsS3Upload> hasChildRslt = esTemplate.queryForList( hasChildNs, DynaTableAwsS3Upload.class );


        final HasChildQueryBuilder hasChildQueryBuilder =
                new HasChildQueryBuilder( transcodingType, QueryBuilders.matchAllQuery() )
                        .innerHit( new QueryInnerHitBuilder() );
        final NativeSearchQuery hasChiBldNv = new NativeSearchQuery( hasChildQueryBuilder );
        hasChiBldNv.addIndices( de.getIndexName() );

        List<DynaTableAwsS3Upload> hasChBldrRslt =
                esTemplate.query( hasChiBldNv, new ResultsExtractor<List<DynaTableAwsS3Upload>>(){
                    @Override
                    public List<DynaTableAwsS3Upload> extract( SearchResponse response ){

                        try{
                            DynaTableVideoTranscoding dynaTableVideoTranscoding = objectMapper.readValue(
                                    response.getHits().getHits()[ 0 ].getInnerHits().values().stream().findFirst().get()
                                            .getHits()[ 0 ].source(), DynaTableVideoTranscoding.class );

                            Assert.assertEquals( newTranscImg.get( 0 ), dynaTableVideoTranscoding );
                            return null;
                        }
                        catch( IOException e ){
                            e.printStackTrace();
                        }
                        return null;
                    }
                } );

        Assert.assertEquals( hasChildRslt.get( 0 ), uploadRecordFromNewImg );


        transcodeUpdateEv.getRecords().get( 0 ).getDynamodb().setNewImage( null );

        de.handleRequest( transcodeUpdateEv );
        esTemplate.refresh( de.getIndexName() );

        ns = new NativeSearchQuery( QueryBuilders.matchAllQuery() );
        ns.addIndices( de.getIndexName() );
        ns.setIds( Arrays.asList( newTranscImg.get( 0 ).getMediaId() ) );


        List<DynaTableVideoTranscoding> delRslt = esTemplate.queryForList( ns, DynaTableVideoTranscoding.class );
        Assert.assertEquals( 0, delRslt.size() );
    }

    private void replaceTableNameWithEnv( DynamodbEvent uploadRecordEv, String tableName,
                                          Class<? extends Serializable> clzz ){
        final DynamodbEvent.DynamodbStreamRecord record = uploadRecordEv.getRecords().get( 0 );
        final String envTableName =
                de.getDynamoDbTableSet().entrySet().stream().filter( i -> i.getValue().equals( clzz ) ).findFirst()
                        .get().getKey();
        final String replaced = record.getEventSourceARN().replaceAll( tableName, envTableName );
        record.setEventSourceARN( replaced );
    }

    private DynamoDBMapper getDynamoDBMapper( Class<? extends Serializable> beanClass ){
        return new DynamoDBMapper( dynamoDBClient, DynamoDBMapperConfig.builder().withTableNameOverride(
                DynamoDBMapperConfig.TableNameOverride
                        .withTableNameReplacement( de.getDynamoDbTableSet().entrySet().stream().filter( e -> {
                            return e.getValue().equals( beanClass );
                        } ).findFirst().get().getKey() ) ).build() );
    }

    private void addMapping( Class clazz ) throws IOException{
        ElasticsearchPersistentEntity persisEntity = esTemplate.getPersistentEntityFor( clazz );
        final XContentBuilder mapping = MappingBA.
                buildMapping( clazz, persisEntity.getIndexType(), persisEntity.getIdProperty().getField().getName(),
                        persisEntity.getParentType() );
        esTemplate.putMapping( de.getIndexName(), persisEntity.getIndexType(), mapping );


        System.out.println( persisEntity.getIndexType() + ">>>>>>" + mapping.string() );
    }

    private abstract class IgnoreSetEvNameMixIn{
        @JsonIgnore
        abstract void setEventName( OperationType eventName );
    }

    private abstract class IgnoreSetStrNameMixIn{
        @JsonIgnore
        abstract void setStreamViewType( StreamViewType streamViewType );
    }
}
