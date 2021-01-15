package org.pubanatomy.videotranscoding;


import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import com.customshow.videotranscoding.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.support.QueryInnerHitBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.range.RangeBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.pubanatomy.videotranscoding.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

public class TranscodingReportingService{

    private static final Logger logger = LogManager.getLogger( TranscodingReportingService.class );
    private final String elasticsearchIndexNameFromDynamoDB;

    public TranscodingReportingService( String elasticsearchIndexNameFromDynamoDB ){
        this.elasticsearchIndexNameFromDynamoDB = elasticsearchIndexNameFromDynamoDB;
    }


    @Autowired
    private ElasticsearchTemplate esTemplate;


    /**
     * This is a simple test method to be used as a proof of concept
     *
     * @param request
     * @return
     */
    public HelloWorldResponse doHelloWorld( HelloWorldRequest request ){
        logger.info( "processing HelloWorldRequest: " + request );
        HelloWorldResponse response = new HelloWorldResponse();
        response.setMessage( "Hello, " + request.getName() );
        return response;
    }

    /**
     * returns a list of UploadTranscodings objects which represent video transcoding jobs matching
     * the given request parameters.
     *
     * @param request
     * @return
     * @throws IllegalAccessException
     */
    public FetchTranscodingJobsResponse fetchTranscodingJobs( FetchTranscodingJobsRequest request )
            throws IllegalAccessException{

        // build the search parameters

        String awsS3UploadType = esTemplate.getPersistentEntityFor( DynaTableAwsS3Upload.class ).getIndexType();

        // transcode record filter
        BoolQueryBuilder transcodeFilter = boolQuery();

        if( request.getMediaIdFilter() != null ){
            transcodeFilter.must( termQuery( "mediaId", request.getMediaIdFilter() ) );
        }

        switch( request.getStatusFilterType() ){
            case STATUS_TYPE_COMPLETE:
                transcodeFilter.must( matchQuery( "status", "Finished" ) );
                break;
            case STATUS_TYPE_ERROR:
                transcodeFilter.must( matchQuery( "status", "Error" ) );
                break;
            case STATUS_TYPE_PENDING:
                transcodeFilter.mustNot( matchQuery( "status", "Finished" ) )
                        .mustNot( matchQuery( "status", "Error" ) );
                break;
            case STATUS_TYPE_ALL:
            default:
                // noop
                break;
        }

        BoolQueryBuilder uploadQuery = boolQuery();
        if( request.getClientIdFilter().size() > 0 ){
            uploadQuery.must( termsQuery( "clientId", request.getClientIdFilter() ) );
        }
        if( request.getOriginalFileNameMatch() != null ){
            String wilcardPattern = "*" + request.getOriginalFileNameMatch() + "*";
            uploadQuery.must( wildcardQuery( "fileRefName", wilcardPattern ) );
        }

        BoolQueryBuilder compoundQuery = boolQuery()
                .must( hasParentQuery( awsS3UploadType, uploadQuery ).innerHit( new QueryInnerHitBuilder() ) )
                .filter( transcodeFilter );


        // build the sort parameters
        FieldSortBuilder sortParams = new FieldSortBuilder( request.getSortBy() )
                .order( request.getDescending() ? SortOrder.DESC : SortOrder.ASC );

        // build the query based on request parameters
        SearchRequestBuilder requestBuilder =
                esTemplate.getClient().prepareSearch( elasticsearchIndexNameFromDynamoDB ).setQuery( compoundQuery )
                        .setFrom( request.getFrom() ).setSize( request.getSize() ).addSort( sortParams );

        logger.debug("sending request: {}" + requestBuilder);

        // execute the query
        SearchResponse response = requestBuilder.get();


        logger.debug("got response: {}", response);


        final FetchTranscodingJobsResponse responseObj = new FetchTranscodingJobsResponse();

        final ObjectMapper objectMapper = new ObjectMapper();


        List<FetchTranscodingJobsResponse.UploadTranscodings> items =
                Arrays.stream( response.getHits().getHits() ).map( transcodeHit -> {


                    FetchTranscodingJobsResponse.UploadTranscodings tempItem =
                            new FetchTranscodingJobsResponse.UploadTranscodings();

                    DynaTableAwsS3Upload upload = null;
                    DynaTableVideoTranscoding transcodeRecord = null;
                    try{
                        transcodeRecord = objectMapper
                                .readValue( transcodeHit.getSourceAsString(), DynaTableVideoTranscoding.class );

                        tempItem.setSingleTranscodeRecord( transcodeRecord );


                        SearchHit uploadHit = transcodeHit.getInnerHits().values().iterator().next().iterator().next();
                        upload = objectMapper.readValue( uploadHit.getSourceAsString(), DynaTableAwsS3Upload.class );

                        tempItem.setUpload( upload );

                    }
                    catch( IOException e ){
                        throw new RuntimeException( e );
                    }

                    return tempItem;

                } ).collect( Collectors.toList() );

        responseObj.setItems( items );
        responseObj.setTotalResults( response.getHits().getTotalHits() );

        return responseObj;
    }


    /**
     * returns a list of pair objects which represent each day's jobs counts and each status' counts
     */
    public FetchJobsPerDayResponse fetchTranscodingJobsPerDay( FetchJobsPerDayRequest request ){

        if( request.getStartDate().getTime() > request.getEndDate().getTime() ){
            throw new IllegalArgumentException( "StartDate can't be greater than EndDate!" );
        }


        Calendar calStart = Calendar.getInstance( TimeZone.getDefault() );
        Calendar calEnd = Calendar.getInstance( TimeZone.getDefault() );

        calStart.setTime( request.getStartDate() );
        calEnd.setTime( request.getEndDate() );

        calEnd.set( Calendar.HOUR_OF_DAY, 23 );
        calEnd.set( Calendar.MINUTE, 59 );
        calEnd.set( Calendar.SECOND, 59 );

        calStart.set( Calendar.HOUR_OF_DAY, 0 );
        calStart.set( Calendar.MINUTE, 0 );
        calStart.set( Calendar.SECOND, 0 );

        final String rangeAggName = "thisRangeAgg";
        final RangeBuilder rangeByLastUpdate = AggregationBuilders.range( rangeAggName );
        rangeByLastUpdate.field( "lastUpdateTime" );

        long milStart = calStart.getTime().getTime();
        long milEnd = calEnd.getTime().getTime();
        for( long md = calStart.getTime().getTime(); md < milEnd; md += 24 * 3600 * 1000 ){
            rangeByLastUpdate.addRange( md + "", md, md + 24 * 3600 * 1000 );
        }

        final String subTermAggName = "subRangeTermAgg";
        final TermsBuilder termsAgg = AggregationBuilders.terms( subTermAggName );
        termsAgg.field( "status" );
        termsAgg.size( 20 );
        rangeByLastUpdate.subAggregation( termsAgg );


        SearchResponse rawRslt = esTemplate.getClient().prepareSearch( elasticsearchIndexNameFromDynamoDB ).setQuery(
                QueryBuilders.rangeQuery( "lastUpdateTime" ).from( milStart )
                        .to( milEnd ) ).addAggregation( rangeByLastUpdate ).setFrom( 0 )
                .setSize( 0 ).get();

        final FetchJobsPerDayResponse rt = new FetchJobsPerDayResponse();
        rt.setTotalResults( rawRslt.getHits().getTotalHits() );
        rt.setItems(
                ( ( MultiBucketsAggregation )rawRslt.getAggregations().getAsMap().get( rangeAggName ) ).getBuckets()
                        .stream().map( rangeBucket -> {

                    final FetchJobsPerDayResponse.DateJobsPair p = new FetchJobsPerDayResponse.DateJobsPair();
                    p.setJobsCount( rangeBucket.getDocCount() );
                    p.setDate( new Date( Long.parseLong( rangeBucket.getKeyAsString() ) ) );


                    // NOTE: our data in ElasticSearch is currently being "analyzed" using default settings,
                    // which results in the status field values being converted to all lower-case and split into
                    // tokens on whitespace and punctuation - so - for the time being we will always convert the
                    // returned termBucket terms to all lower-case both when putting values into the table
                    // and when pulling them back out, such that this code will work in both cases where the
                    // the files ARE analyized, and are NOT analyzed...


                    final StringTerms termBucket =
                            ( StringTerms )rangeBucket.getAggregations().getAsMap().get( subTermAggName );
                    termBucket.getBuckets().forEach( sbct -> {
                        // NOTE: converting termBucket term to lowercase!
                        p.getStatusToCount().put( sbct.getKeyAsString().toLowerCase(), sbct.getDocCount() );
                    } );

                    // calculate aggregate values


                    // NOTE: converting status constants to lowercase!
                    final Long finished = p.getStatusToCount().get( TranscodingFunctions.Result.STATUS_finished.toLowerCase() );
                    if( finished != null ){
                        p.setCompletedJobs( finished.intValue() );
                    }

                    // NOTE: converting status constants to lowercase!
                    final Long error = p.getStatusToCount().get( TranscodingFunctions.Result.STATUS_error.toLowerCase() );
                    if( error != null ){
                        p.setFailedJobs( error.intValue() );
                    }

                    p.setPendingJobs( new Long( p.getJobsCount() ).intValue() - p.getCompletedJobs() -
                            p.getFailedJobs() );


                    return p;

                } ).collect( Collectors.toList() ) );

        return rt;
    }


}
