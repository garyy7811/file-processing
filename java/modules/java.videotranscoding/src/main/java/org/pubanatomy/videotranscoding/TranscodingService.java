package org.pubanatomy.videotranscoding;


import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.pubanatomy.awsS3Upload.AwsS3UploadDAO;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.pubanatomy.loginverify.DynaLogInSessionInfo;
import org.pubanatomy.loginverify.DynamoLoginInfoDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.support.QueryInnerHitBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TranscodingService{

    private static final Logger logger = LogManager.getLogger( TranscodingService.class );
    private final String elasticsearchIndexNameFromDynamoDB;

    public TranscodingService( String rootUserId, String awsS3DownloadBucket,
                               String elasticsearchIndexNameFromDynamoDB ){
        this.rootUserId = rootUserId;
        this.awsS3DownloadBucket = awsS3DownloadBucket;
        this.elasticsearchIndexNameFromDynamoDB = elasticsearchIndexNameFromDynamoDB;
    }

    private String rootUserId;
    private String awsS3DownloadBucket;

    @Autowired
    private TranscodingFunctions transcodingFunctions;

    @Autowired
    private TranscodingDAO transcodingDAO;

    @Autowired
    private AwsS3UploadDAO uploadDAO;

    @Autowired
    private AmazonS3 amazonS3;


    @Autowired
    private DynamoLoginInfoDAO loginInfoDAO;

    public List<DynaTableVideoTranscoding> loadByBucketkeyCreateTime( @NotEmpty String csSessionId,
                                                                      @NotEmpty String bucketKey,
                                                                      @NotNull Date createTimeTo,
                                                                      @NotNull Integer maxResult,
                                                                      @NotNull Boolean desc )
            throws IllegalAccessException{

        logger.debug( "{},{},{},{}, {}", csSessionId, bucketKey, createTimeTo, maxResult );

        if( maxResult > 2000 ){
            maxResult = 2000;
        }

        String[] bucketKeyArr = bucketKey.split( "/" );
        if( bucketKeyArr.length != 4 ){
            throw new IllegalArgumentException( "illegalKey" );
        }
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );

        if( rootUserId.equals( logInSessionInfo.getUserId() ) ||
                bucketKeyArr[ 1 ].equals( logInSessionInfo.getUserId() + "" ) ){
            return transcodingDAO.loadByBucketkeyCreateTime( bucketKey, createTimeTo.getTime(), maxResult, desc );
        }
        throw new IllegalAccessException( "requireAuth" );
    }


    public List<DynaTableVideoTranscoding> loadByStatusLastUpdateTime( @NotEmpty String csSessionId,
                                                                       @NotEmpty String status, Date lastUpdateTimeTo,
                                                                       Integer maxResult, Boolean desc )
            throws IllegalAccessException{
        logger.debug( "{},{},{},{}", csSessionId, status, lastUpdateTimeTo, maxResult );

        if( maxResult > 2000 ){
            maxResult = 2000;
        }
        if( lastUpdateTimeTo == null || maxResult < 1 ){
            throw new IllegalArgumentException( "illegalKey" );
        }

        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            throw new IllegalAccessException( "requireAuth" );
        }
        return transcodingDAO.loadByStatusLastUpdateTime( status, lastUpdateTimeTo.getTime(), maxResult, desc );
    }

    public String loadDestPreSignUrl( @NotEmpty String csSessionId, String mediaId, String destination )
            throws IllegalAccessException{
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );

        DynaTableVideoTranscoding record = transcodingDAO.loadByMediaId( mediaId );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) &&
                ! ( logInSessionInfo.getUserId() + "" ).equals( record.getUploadBucketKey().split( "/" )[ 1 ] ) ){

            logger.info( "illegal sessionId:{}", logInSessionInfo );
            throw new IllegalAccessException( "requireAuth" );
        }
        //http://cs-cloud-dev-gary--download.s3.amazonaws.com/video/-1/3/42D8E2C4-621F-1418-FE1B-D6D8394842DB/1461345527030/200x112_5p.jpeg
        String t = ".s3.amazonaws.com/";
        String destKey = destination.substring( destination.indexOf( t ) + t.length() );
        GeneratePresignedUrlRequest destUrlRequest =
                new GeneratePresignedUrlRequest( awsS3DownloadBucket, destKey, HttpMethod.GET );
        destUrlRequest.setExpiration( new Date( System.currentTimeMillis() + 300 * 1000 ) );
        return amazonS3.generatePresignedUrl( destUrlRequest ).toString();
    }

    /**
     * start new encoding job with an uploaded file and new formats
     *
     * @param csSessionId
     * @param uploadedFileCsSessionId
     * @param uploadedFileUploadApplyTime
     * @param formats                     could be null if you want to restart a encoding job already in
     * @return
     */
    public DynaTableVideoTranscoding addTranscoding( @NotEmpty String csSessionId,
                                                     @NotNull String uploadedFileCsSessionId,
                                                     @NotNull Long uploadedFileUploadApplyTime, List formats )
            throws IllegalAccessException, FileNotFoundException{
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            throw new IllegalAccessException( "requireSuper" );
        }

        if( formats != null ){
            if( formats.size() == 0 ){
                formats = null;
            }
            else{
                //todo: validate formats
            }
        }
        DynaTableAwsS3Upload uploadedFile =
                uploadDAO.loadUpload( uploadedFileCsSessionId, uploadedFileUploadApplyTime );
        if( uploadedFile == null ){
            throw new FileNotFoundException(
                    "sessionId:" + uploadedFileCsSessionId + "time:" + uploadedFileUploadApplyTime );
        }

        return transcodingFunctions.addMediaBenchMark( uploadedFile, formats );
    }

    public DynaTableVideoTranscoding reEncodingJob( @NotEmpty String csSessionId, @NotNull String mediaId )
            throws IllegalAccessException{
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            throw new IllegalAccessException( "requireSuper" );
        }

        return transcodingFunctions.reRunJob( mediaId );
    }


    @Autowired
    private ElasticsearchTemplate esTemplate;

    /**
     * called when sorting by Upload record field
     *
     * @param csSessionId
     * @param from
     * @param size
     * @param sortBy
     * @param desc
     * @return
     * @throws IllegalAccessException
     */
    public List<Object[]> esLstByLastupdated( @NotEmpty String csSessionId, int from, int size, String sortBy,
                                              boolean desc ) throws IllegalAccessException{
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            throw new IllegalAccessException( "requireRoot" );
        }

        QueryBuilder queryBuilder = new HasChildQueryBuilder(
                esTemplate.getPersistentEntityFor( DynaTableVideoTranscoding.class ).getIndexType(),
                QueryBuilders.matchAllQuery() ).innerHit( new QueryInnerHitBuilder() );


        SearchResponse response =
                esTemplate.getClient().prepareSearch( elasticsearchIndexNameFromDynamoDB ).setQuery( queryBuilder )
                        .setFrom( from ).setSize( size )
                        .addSort( new FieldSortBuilder( sortBy ).order( desc ? SortOrder.DESC : SortOrder.ASC ) ).get();

        final ObjectMapper objectMapper = new ObjectMapper();

        final List<Object[]> rt = Arrays.stream( response.getHits().getHits() ).map( uploadHit -> {

            DynaTableAwsS3Upload upload = null;
            try{
                upload = objectMapper.readValue( uploadHit.getSourceAsString(), DynaTableAwsS3Upload.class );
            }
            catch( IOException e ){
                throw new RuntimeException( e );
            }

            List<DynaTableVideoTranscoding> transcodingHitLst = uploadHit.getInnerHits().values().stream().map( i -> {
                return Arrays.asList( i.getHits() );
            } ).flatMap( l -> l.stream() ).map( transcoHit -> {
                try{
                    return objectMapper.readValue( transcoHit.getSourceAsString(), DynaTableVideoTranscoding.class );
                }
                catch( IOException e ){
                    throw new RuntimeException( e );
                }
            } ).collect( Collectors.toList() );
            return new Object[]{ upload, transcodingHitLst };
        } ).collect( Collectors.toList() );
        return rt;
    }

    /**
     * called when sorting by Transcoding record field
     *
     * @param csSessionId
     * @param from
     * @param size
     * @param sortBy
     * @param desc
     * @return
     * @throws IllegalAccessException
     */
    public List<Object[]> esLstByLastupdated11( @NotEmpty String csSessionId, int from, int size, String sortBy,
                                                boolean desc ) throws IllegalAccessException{
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            throw new IllegalAccessException( "requireRoot" );
        }


        final ObjectMapper objectMapper = new ObjectMapper();

        final HasParentQueryBuilder hasParentQueryBuilder = new HasParentQueryBuilder(
                esTemplate.getPersistentEntityFor( DynaTableAwsS3Upload.class ).getIndexType(),
                QueryBuilders.matchAllQuery() ).innerHit( new QueryInnerHitBuilder() );

        final SearchResponse response = esTemplate.getClient().prepareSearch( elasticsearchIndexNameFromDynamoDB )
                .setQuery( hasParentQueryBuilder ).setFrom( from ).setSize( size )
                .addSort( new FieldSortBuilder( sortBy ).order( desc ? SortOrder.DESC : SortOrder.ASC ) ).get();

        final List<Object[]> rt = Arrays.stream( response.getHits().getHits() ).map( transcHit -> {

            DynaTableVideoTranscoding transco = null;
            try{
                transco = objectMapper.readValue( transcHit.getSourceAsString(), DynaTableVideoTranscoding.class );
            }
            catch( IOException e ){
                throw new RuntimeException( e );
            }
            DynaTableAwsS3Upload upload = transcHit.getInnerHits().values().stream().map( i -> {
                return Arrays.asList( i.getHits() );
            } ).flatMap( l -> l.stream() ).map( uploadHit -> {
                try{
                    return objectMapper.readValue( uploadHit.getSourceAsString(), DynaTableAwsS3Upload.class );
                }
                catch( IOException e ){
                    throw new RuntimeException( e );
                }
            } ).collect( Collectors.toList() ).get( 0 );

            return new Object[]{ upload, Arrays.asList( transco ) };
        } ).collect( Collectors.toList() );

        return rt;
    }
}
