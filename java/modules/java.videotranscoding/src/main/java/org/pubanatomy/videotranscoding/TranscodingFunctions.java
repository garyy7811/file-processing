package org.pubanatomy.videotranscoding;


import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.pubanatomy.configPerClient.ConfigPerClientDAO;
import org.pubanatomy.configPerClient.DynaTableClientConfig;
import org.pubanatomy.configPerClient.DynaTableClientConfigTranscode;
import org.pubanatomy.configPerClient.DynaTableClientConfigTranscodeFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.web.client.HttpClientErrorException;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class TranscodingFunctions{

    private static final Logger logger = LogManager.getLogger( TranscodingFunctions.class );

    public static final String SUPPORT_TYPES_STR = ";.flv;.f4v;.mp4;.mov;.wmv;.avi;.mpg;.mpeg;.m4v;.wma;";

    @Autowired
    private ConfigPerClientDAO clientConfigFuncs;

    @Autowired
    private TranscodingDAO transcodingDAO;

    @Autowired
    private AmazonS3 amazonS3;

    private String urlCalledByEncodingCom;
    private String userIdForEncodingCom;
    private String userkeyForEncodingCom;
    private String awsS3DownloadBucket;
    private int backToUpdateStatusInMin = 5;
    private String awsS3DownloadBucketAccessKeyAndEncodedSecret;

    public String getUrlCalledByEncodingCom(){
        return urlCalledByEncodingCom;
    }

    public String getUserIdForEncodingCom(){
        return userIdForEncodingCom;
    }

    public String getUserkeyForEncodingCom(){
        return userkeyForEncodingCom;
    }

    public String getAwsS3DownloadBucket(){
        return awsS3DownloadBucket;
    }

    public int getBackToUpdateStatusInMin(){
        return backToUpdateStatusInMin;
    }

    public TranscodingFunctions( String urlCalledByEncodingCom, String userIdForEncodingCom,
                                 String userkeyForEncodingCom, String awsS3DownloadBucket,
                                 String awsS3DownloadBucketAccessKeyAndEncodedSecret, int backToUpdateStatusInMin ){
        this.urlCalledByEncodingCom = urlCalledByEncodingCom;
        this.userIdForEncodingCom = userIdForEncodingCom;
        this.userkeyForEncodingCom = userkeyForEncodingCom;
        this.awsS3DownloadBucket = awsS3DownloadBucket;
        this.awsS3DownloadBucketAccessKeyAndEncodedSecret = awsS3DownloadBucketAccessKeyAndEncodedSecret;
        this.backToUpdateStatusInMin = backToUpdateStatusInMin;
    }


    public Object onFileUploadReady( DynaTableAwsS3Upload uploadedFile ){

        logger.debug( uploadedFile.toString() );


        DynaTableClientConfig c = clientConfigFuncs.loadConfig( uploadedFile.getClientId() );
        DynaTableClientConfigTranscode config = c.getTranscode();
        if( ! config.isEnabled() ){
            logger.info( " NOT enabled:[{}], {},: {}", c.getClientId(), c.getClientName(),
                    config.getEnabledChangedBy() );
            return uploadedFile;
        }

        if( ! config.getAutoStartOnUploaded() ){
            logger.info( " NOT auto Start:[{}], {}", c.getClientId(), c.getClientName() );
            return uploadedFile;
        }
        return addMediaBenchMark( uploadedFile, null );
    }

    public DynaTableVideoTranscoding addMediaBenchMark( DynaTableAwsS3Upload uploadedFile,
                                                        List<DynaTableClientConfigTranscodeFormat> formats ){
        String fileExt = uploadedFile.getFileRefType();
        if( fileExt.length() < 3 || ! SUPPORT_TYPES_STR.contains( ";" + fileExt.toLowerCase() + ";" ) ){
            logger.info( " NOT supported: {}" + uploadedFile.getS3BucketKey() );
            throw new IllegalArgumentException( "transcodeFileFormatNotSupported" );
        }
        DynaTableClientConfig c = clientConfigFuncs.loadConfig( uploadedFile.getClientId() );
        DynaTableClientConfigTranscode config = c.getTranscode();
        if( ! config.isEnabled() ){
            logger.info( " NOT enabled:[{}], {},: {}", c.getClientId(), c.getClientName(),
                    config.getEnabledChangedBy() );
            throw new IllegalStateException( "transcodeConfigNotEnabled" );
        }
        URL srcFileUrl = amazonS3.generatePresignedUrl( uploadedFile.getS3Bucket(), uploadedFile.getS3BucketKey(),
                new Date( System.currentTimeMillis() + config.getS3UploadReadAuthExpireDelayInSecs() * 1000 ),
                HttpMethod.GET );

        DynaTableVideoTranscoding transcoding = new DynaTableVideoTranscoding();
        transcoding.setUploadBucketKey( uploadedFile.getS3BucketKey() );
        transcoding.setCreateTime( System.currentTimeMillis() );

        try{
            QueryBody query = getNewQueryBody( Query.AddMediaBenchmark );
            query.setSource( srcFileUrl.toString() );

            logger.debug( "calling encoding with query:{}", query );
            Map addMediaRslt = ( Map )sendInJson( new Query( query ), Map.class );
            logger.debug( addMediaRslt );
            transcoding.setMediaId( ( String )( ( Map )addMediaRslt.get( "response" ) ).get( "MediaID" ) );
            transcoding.setStatus( Result.Status_new );
            transcoding.setFormats( formats );
        }
        catch( Exception e ){
            transcoding.setMediaId( System.currentTimeMillis() + "" );
            handleEncodingComError( transcoding, e );
        }

        transcoding.setLastUpdateTime( System.currentTimeMillis() );
        transcodingDAO.save( transcoding );
        return transcoding;
    }

    public DynaTableVideoTranscoding reRunJob( String mediaId ){
        DynaTableVideoTranscoding rt = transcodingDAO.loadByMediaId( mediaId );
        if( rt == null ){
            throw new IllegalArgumentException( "MediaNotFound" );
        }
        try{
            QueryBody queryBody = getNewQueryBody( Query.ProcessMedia );
            queryBody.setMediaid( mediaId );
            Map response = ( Map )sendInJson( new Query( queryBody ), Map.class );

            Map errors = ( Map )( ( Map )response.get( "response" ) ).get( "errors" );
            if( errors != null ){
                rt.setErrorMsg( getObjJsonMapper().toJson( errors ) );
                logger.warn( rt.getErrorMsg() );
            }
            else{
                rt.setErrorMsg( null );
            }
        }
        catch( Exception e ){
            handleEncodingComError( rt, e );
        }
        transcodingDAO.save( rt );
        return rt;
    }


    public DynaTableVideoTranscoding onCallFromEncodingComServer( Result rslt ) throws Exception{

        logger.debug( rslt );
        ResultBody resultBody = rslt.getResult();
        DynaTableVideoTranscoding encodingRecord = transcodingDAO.loadByMediaId( resultBody.getMediaid() );

        if( encodingRecord == null ){
            logger.error( "unabled to locate transcoding record with {}" + resultBody );
            return null;
        }

        if( resultBody.getTaskid() == null ){
            encodingRecord.setStatus( resultBody.getStatus() );
        }

        if( Result.STATUS_saved.equals( resultBody.getStatus() ) ){
            syncFormatsStatusAndDescription( resultBody, encodingRecord );
            if( resultBody.getTaskid() != null ){
                encodingRecord.setStatus( Result.STATUS_processing );
            }
        }
        else if( Result.STATUS_error.equals( resultBody.getStatus() ) ){
            encodingRecord
                    .setErrorMsg( resultBody.getDescription() == null ? "" : resultBody.getDescription().toString() );
            syncFormatsStatusAndDescription( resultBody, encodingRecord );
        }
        else if( Result.STATUS_processing.equals( resultBody.getStatus() ) ){
            if( resultBody.getFormat() != null && encodingRecord.getFormats() != null ){
                int size = encodingRecord.getFormats().size();
                if( size == resultBody.getFormat().size() ){
                    for( int i = 0; i < size; i++ ){
                        DynaTableClientConfigTranscodeFormat rf = encodingRecord.getFormats().get( i );
                        rf.setTaskid( resultBody.getFormat().get( i ).getTaskid() );
                        rf.setStatus( resultBody.getFormat().get( i ).getStatus() );
                    }
                }
                else{
                    Jackson2JsonObjectMapper objJsonMapper = getObjJsonMapper();
                    logger.error( "size not marching: resultBody:{}, record{}",
                            objJsonMapper.toJson( resultBody.getFormat() ),
                            objJsonMapper.toJson( encodingRecord.getFormats() ) );
                }
            }
        }
        else if( Result.STATUS_readToProcess.equals( resultBody.getStatus() ) ){
            try{
                QueryBody queryBody = getNewQueryBody( Query.GetMediaInfo );
                queryBody.setMediaid( resultBody.getMediaid() );
                GetMediaInfoResponse response =
                        ( GetMediaInfoResponse )sendInJson( new Query( queryBody ), GetMediaInfoResponse.class );
                encodingRecord.setMediaInfo( response.getResponse() );
                encodingRecord.setErrorMsg( null );
            }
            catch( Exception e ){
                handleEncodingComError( encodingRecord, e );
            }

            //this is a video then
            if( encodingRecord.getMediaInfo() != null && encodingRecord.getMediaInfo().getBitrate() != null ){
                try{
                    QueryBody updateMedia = getNewQueryBody( Query.UpdateMedia );
                    updateMedia.setMediaid( encodingRecord.getMediaId() );

                    if( encodingRecord.getFormats() == null ){
                        logger.info( "Use default formats for Media:{},file:{}", encodingRecord.getMediaId(),
                                encodingRecord.getUploadBucketKey() );
                        DynaTableClientConfig clientConfig =
                                clientConfigFuncs.loadConfig( encodingRecord.getUploadBucketKey().split( "/" )[ 0 ] );
                        encodingRecord.setFormats( genFormatsByConfigAndMediaInfo( encodingRecord.getMediaInfo(),
                                clientConfig.getTranscode() ) );
                    }
                    else{
                        logger.info( "User Existing formats" );
                        DynaTableVideoTranscodingMediaInfo info = encodingRecord.getMediaInfo();
                        encodingRecord.getFormats().forEach( f -> {
                            if( "mp4".equals( f.getOutput() ) ){
                                if( f.getBitrate() == null ){
                                    Integer orgBitrate =
                                            DynaTableClientConfigTranscodeFormat.getBitRateInK( info.getBitrate() );
                                    Integer fmtBitrate =
                                            DynaTableClientConfigTranscodeFormat.getBitRateInK( f.getBitrate() );
                                    f.setBitrate( Math.min( orgBitrate, fmtBitrate ) + "k" );
                                }
                                if( f.getFramerate() == null ){
                                    f.setFramerate( info.getFrame_rate() );
                                }
                                if( f.getSize() == null ){
                                    f.setSize( info.getSize() );
                                }
                            }
                            else{
                                if( f.getWidth() == null || f.getHeight() == null || f.getSize() == null ){
                                    String[] tmp = info.getSize().split( "x" );
                                    f.setWidth( tmp[ 0 ] );
                                    f.setHeight( tmp[ 1 ] );
                                    f.setSize( info.getSize() );
                                }
                            }
                        } );
                    }

                    if( encodingRecord.getFormats().size() == 1 ){
                        handlePresignedDestUrl( encodingRecord.getUploadBucketKey(),
                                encodingRecord.getFormats().get( 0 ), true );
                    }
                    else{

                        DynaTableClientConfigTranscodeFormat pf =
                                encodingRecord.getFormats().stream().filter( f -> "mp4".equals( f.getOutput() ) )
                                        .max( ( o1, o2 ) -> {
                                            Integer b1 = DynaTableClientConfigTranscodeFormat
                                                    .getBitRateInK( o1.getBitrate() );
                                            Integer b2 = DynaTableClientConfigTranscodeFormat
                                                    .getBitRateInK( o2.getBitrate() );
                                            return b1 > b2 ? 1 : ( b2 > b1 ? - 1 : 0 );
                                        } ).get();

                        encodingRecord.getFormats().forEach(
                                f -> handlePresignedDestUrl( encodingRecord.getUploadBucketKey(), f, f == pf ) );
                    }

                    updateMedia.setFormat( encodingRecord.getFormats() );
                    Map rt = ( Map )sendInJson( new Query( updateMedia ), Map.class );
                    Map errors = ( Map )( ( Map )rt.get( "response" ) ).get( "errors" );
                    if( errors != null ){
                        encodingRecord.setErrorMsg( getObjJsonMapper().toJson( errors ) );
                        logger.warn( encodingRecord.getErrorMsg() );
                    }
                    else{
                        QueryBody getStatusBody = getNewQueryBody( Query.GetStatus );
                        getStatusBody.setMediaid( resultBody.getMediaid() );
                        GetStatusResponse statusResponse =
                                ( GetStatusResponse )sendInJson( new Query( getStatusBody ), GetStatusResponse.class );
                        //                        Encoding.com returns new here messing up!
                        //                        encodingRecord.setStatus( statusResponse.getResponse().getStatus() );
                        Arrays.stream( statusResponse.getResponse().getFormat() ).forEach( rsltFmt -> {

                            final DynaTableClientConfigTranscodeFormat found =
                                    encodingRecord.getFormats().stream().filter( recdFmt -> {
                                        return recdFmt.getDestination().equalsIgnoreCase( rsltFmt.getDestination() );
                                    } ).findAny().get();
                            found.setTaskid( rsltFmt.getId() );
                            found.setStatus( rsltFmt.getStatus() );
                            found.setDescription( rsltFmt.getDescription() );
                        } );
                    }
                }
                catch( Exception e ){
                    handleEncodingComError( encodingRecord, e );
                }
            }
            else if( encodingRecord.getErrorMsg() == null ){
                //something not a video came to transcoding table !
                encodingRecord.setStatus( Result.STATUS_wrong_input );
            }
        }
        else if( Result.STATUS_finished.equals( resultBody.getStatus() ) ){
            syncFormatsStatusAndDescription( resultBody, encodingRecord );
            logger.info( " !!!yeah!!! finished:{}", resultBody.mediaid );
        }
        else{
            logger.warn( "Unknown status:{}", resultBody.getStatus() );
            encodingRecord.setErrorMsg( "Unknown status:" + resultBody.getStatus() );
        }
        encodingRecord.setLastUpdateTime( System.currentTimeMillis() );
        transcodingDAO.save( encodingRecord );
        return encodingRecord;
    }

    private void syncFormatsStatusAndDescription( ResultBody resultBody, DynaTableVideoTranscoding encodingRecord ){
        logger.debug( " resultBody:{}", resultBody );
        if( encodingRecord.getFormats() == null || encodingRecord.getFormats().size() == 0 ){
            logger.error( "no format found for record:{}, resultBody:{}", encodingRecord, resultBody );
            return;
        }
        if( resultBody.getFormat() != null ){
            resultBody.getFormat().forEach( rsltFormat -> {
                if( rsltFormat.getTaskid() == null && rsltFormat.getDestination() == null ){
                    logger.error( "no format no task in rsltFormat:{}", rsltFormat );
                    return;
                }
                final Optional<DynaTableClientConfigTranscodeFormat> any =
                        encodingRecord.getFormats().stream().filter( recordFormat -> {
                            if( rsltFormat.getTaskid() != null ){
                                return rsltFormat.getTaskid().equals( recordFormat.getTaskid() );
                            }

                            if( rsltFormat.getDestination() != null ){
                                return rsltFormat.getDestination().equals( recordFormat.getDestination() );
                            }
                            return false;
                        } ).findAny();
                if( any.isPresent() ){
                    final DynaTableClientConfigTranscodeFormat fmt = any.get();
                    fmt.setStatus( rsltFormat.getStatus() );
                    fmt.setDescription( rsltFormat.getDescription() );
                    if( Result.STATUS_error.equals( fmt.getStatus() ) ){
                        encodingRecord.setStatus( Result.STATUS_error );
                        encodingRecord.setErrorMsg( rsltFormat.getTaskid() + ": " + rsltFormat.getDescription() );
                    }
                }
                else{
                    final String joined =
                            resultBody.getFormat().stream().map( i -> i.getTaskid() + "@" + i.getDestination() )
                                    .collect( Collectors.joining( ", " ) );
                    logger.warn( "Not found from records:{}", joined );
                }
            } );
        }
        else{
            if( resultBody.getTaskid() == null && resultBody.getDestination() == null ){
                logger.warn( "no format no task in resultBody:{}", resultBody );
                return;
            }

            final Optional<DynaTableClientConfigTranscodeFormat> any =
                    encodingRecord.getFormats().stream().filter( i -> {
                        return ( resultBody.getTaskid().equals( i.getTaskid() ) ||
                                resultBody.getDestination().equals( i.getDestination() ) );
                    } ).findAny();

            if( any.isPresent() ){
                final DynaTableClientConfigTranscodeFormat fmt = any.get();
                fmt.setStatus( resultBody.getStatus() );
                fmt.setDescription(
                        resultBody.getDescription() == null ? null : resultBody.getDescription().toString() );
                if( Result.STATUS_error.equals( fmt.getStatus() ) ){
                    encodingRecord.setStatus( Result.STATUS_error );
                    encodingRecord.setErrorMsg( resultBody.getTaskid() + ": " + resultBody.getDescription() );
                }
            }
            else{
                logger.warn( "Not found from records: with taskid:{}, dest:{}", resultBody.getTaskid(),
                        resultBody.getDestination() );
            }
        }
    }


    public List<DynaTableVideoTranscoding> updateEncodingComStatusByMediaIdArray( String[] mediaIdArr ){
        final List<DynaTableVideoTranscoding> recordLst = transcodingDAO.loadByMediaIdArr( mediaIdArr );
        updateAndProcessAgain( recordLst );
        return recordLst;
    }


    /**
     * @param status
     */
    public List<DynaTableVideoTranscoding> updateEncodingComStatusByStatus( String status ){
        List<DynaTableVideoTranscoding> transcodingRecordLst = transcodingDAO
                .loadByStatusLastUpdateTime( status, System.currentTimeMillis() - backToUpdateStatusInMin * 60000, 2000,
                        true );

        logger.info( "found {} for status {}", transcodingRecordLst.size(), status );

        if( transcodingRecordLst.size() == 0 ){
            return transcodingRecordLst;
        }

        updateAndProcessAgain( transcodingRecordLst );
        return transcodingRecordLst;
    }

    private void updateAndProcessAgain( List<DynaTableVideoTranscoding> transcodingRecordLst ){
        String mediaidLst =
                transcodingRecordLst.stream().map( r -> r.getMediaId() ).collect( Collectors.joining( "," ) );

        QueryBody qb = getNewQueryBody( Query.GetStatus );
        qb.setExtended( "yes" );
        qb.setMediaid( mediaidLst );
        try{
            GetStatusResponseExtended resp =
                    ( GetStatusResponseExtended )sendInJson( new Query( qb ), GetStatusResponseExtended.class );

            List<DynaTableVideoTranscoding> delLst = new ArrayList<>();
            List<GetMediaStatusResponse> mediaJobLst = Arrays.asList( resp.getResponse().getJob() );
            mediaJobLst.stream().forEach( s -> {//GetMediaStatusResponse
                if( ! "yes".equals( s.getDeleted() ) ){
                    Result fakeRslt = new Result();
                    ResultBody fakeResultBody = new ResultBody();
                    fakeRslt.setResult( fakeResultBody );
                    fakeResultBody.setMediaid( s.getId() );
                    fakeResultBody.setStatus( s.getStatus() );
                    fakeResultBody.setDescription( s.getDescription() );

                    if( s.getFormat() != null ){
                        List<DynaTableClientConfigTranscodeFormat> fakeFormat =
                                new ArrayList<DynaTableClientConfigTranscodeFormat>( s.getFormat().length );
                        fakeResultBody.setFormat( fakeFormat );
                        Arrays.stream( s.getFormat() ).forEach( f -> {//GetMediaStatusResponseTask

                            DynaTableClientConfigTranscodeFormat gstsfmt = new DynaTableClientConfigTranscodeFormat();
                            gstsfmt.setTaskid( f.getId() );
                            gstsfmt.setStatus( f.getStatus() );
                            gstsfmt.setDescription( f.getDescription() );
                            fakeFormat.add( gstsfmt );

                        } );
                    }

                    try{
                        onCallFromEncodingComServer( fakeRslt );
                    }
                    catch( Exception e ){
                        logger.error( ExceptionUtils.getStackTrace( e ) );
                        transcodingDAO
                                .save( transcodingRecordLst.stream().filter( a -> a.getMediaId().equals( s.getId() ) )
                                        .findFirst().get() );
                    }
                }
                else{
                    final DynaTableVideoTranscoding vtrscd =
                            transcodingRecordLst.stream().filter( a -> a.getMediaId().equals( s.getId() ) ).findAny()
                                    .get();
                    vtrscd.setStatus( Result.STATUS_deleted );
                    vtrscd.setErrorMsg( "deleted by encoding.com" );
                    delLst.add( vtrscd );
                }
            } );

            if( delLst.size() > 0 ){
                transcodingDAO.save( delLst );
            }
        }
        catch( Exception e ){
            logger.error( ExceptionUtils.getStackTrace( e ) );
            transcodingRecordLst.stream().forEach( t -> {
                handleEncodingComError( t, e );
            } );
            transcodingDAO.save( transcodingRecordLst );
        }
    }


    private QueryBody getNewQueryBody( String action ){
        QueryBody queryBody = new QueryBody( userIdForEncodingCom, userkeyForEncodingCom, action );
        queryBody.setNotify( urlCalledByEncodingCom );
        queryBody.setNotify_upload( urlCalledByEncodingCom );
        queryBody.setNotify_encoding_errors( urlCalledByEncodingCom );
        return queryBody;
    }

    public List<DynaTableClientConfigTranscodeFormat> genFormatsByConfigAndMediaInfo(
            DynaTableVideoTranscodingMediaInfo info, DynaTableClientConfigTranscode config ){

        Integer orgBitrate = DynaTableClientConfigTranscodeFormat.getBitRateInK( info.getBitrate() );

        //all the bitrate lower then original
        final List<DynaTableClientConfigTranscodeFormat> configFormatCopies = config.getFormatCopies();
        List<DynaTableClientConfigTranscodeFormat> mp4Fmts = configFormatCopies.stream().filter(
                i -> i.isEnabled() && i.getOutput().equals( "mp4" ) &&
                        DynaTableClientConfigTranscodeFormat.getBitRateInK( i.getBitrate() ) < orgBitrate ).map( i -> {
            i.setSize( info.getSize() );
            i.setFramerate( info.getFrame_rate() );
            return i;
        } ).collect( Collectors.toList() );

        //any configuration bitrate ge than original bit rate, add the original rate
        if( configFormatCopies.stream().anyMatch( i -> i.getBitrate() != null &&
                DynaTableClientConfigTranscodeFormat.getBitRateInK( i.getBitrate() ) >= orgBitrate ) ){
            DynaTableClientConfigTranscodeFormat cpOrg =
                    config.getACopyOfFormat( DynaTableClientConfigTranscodeFormat.DEFAULT_VIDEO );
            cpOrg.setSize( info.getSize() );
            cpOrg.setFramerate( info.getFrame_rate() );
            cpOrg.setBitrate( info.getBitrate() );
            cpOrg.setIdentification( "Original Bitrate" );
            mp4Fmts.add( cpOrg );
        }


        List<DynaTableClientConfigTranscodeFormat> thmbFmts =
                configFormatCopies.stream().filter( i -> i.isEnabled() && i.getOutput().equals( "thumbnail" ) )
                        .map( aCopyOfFormat -> {


                            String[] tmp = info.getSize().split( "x" );
                            Integer srcWidth = Integer.valueOf( tmp[ 0 ] );
                            Integer srcHeight = Integer.valueOf( tmp[ 1 ] );

                            switch( aCopyOfFormat.getIdentification() ){
                                case DynaTableClientConfigTranscodeFormat.DEFAULT_POSTER_FRAME:
                                case DynaTableClientConfigTranscodeFormat.DEFAULT_FIRST_FRAME:
                                    // nothing to do here, actually - we don't need to set the width or height, since that will
                                    // be automatically set to match the source video - but - we will set it anyway to match the source
                                    // size for possible use later in the pipeline
                                    aCopyOfFormat.setWidth( srcWidth.toString() );
                                    aCopyOfFormat.setHeight( srcHeight.toString() );
                                    logger.debug( "set {} w,h to {},{}", aCopyOfFormat.getIdentification(), srcWidth,
                                            srcHeight );
                                    break;
                                case DynaTableClientConfigTranscodeFormat.DEFAULT_THUMB:
                                    // for the thumbnail, we are standardizing on width=200, and technically we don't need to set the
                                    // height because encoding.com will set it to match the source aspect ratio - but - we do need to
                                    // store the height in dynamodb for use later in the pipeline
                                    Integer thumbWidth = 200;
                                    Integer thumbHeight = ( int )( ( ( double )srcHeight / ( double )srcWidth ) *
                                            ( double )thumbWidth );
                                    aCopyOfFormat.setHeight( thumbHeight.toString() );
                                    logger.debug( "set thumb w,h to {},{} from source {},{}", thumbWidth, thumbHeight,
                                            srcWidth, srcHeight );
                                    break;
                                default:
                                    logger.error( "NOT MP4 OR THUMBNAIL, what is this format:{}",
                                            aCopyOfFormat.getIdentification() );
                                    break;
                            }
                            return aCopyOfFormat;
                        } ).collect( Collectors.toList() );


        mp4Fmts.addAll( thmbFmts );
        return mp4Fmts;
    }

    private void handlePresignedDestUrl( String uploadBucketKey, DynaTableClientConfigTranscodeFormat format,
                                         Boolean isPrime ){

        logger.debug( "generating destination for output={}, identification={}", format.getOutput(),
                format.getIdentification() );

        final String destinationBase =
                "https://" + awsS3DownloadBucketAccessKeyAndEncodedSecret + "@" + awsS3DownloadBucket +
                        ".s3.amazonaws.com/";

        final String destinationUrl;

        if( format.getOutput().equals( "mp4" ) ){

            logger.debug( "found mp4 output - checking for default or multibitrate video" );

            if( isPrime ){
                destinationUrl = destinationBase + "video/" + uploadBucketKey + "/" + format.getBitrate().trim() + ".mp4";
                format.setIsDefault( Boolean.TRUE.toString() );
            }
            else{
                destinationUrl = destinationBase + "video/mb/" + uploadBucketKey + "/" + format.getBitrate().trim() + ".mp4";
                format.setIsDefault( Boolean.FALSE.toString() );
            }

        }
        else{

            switch( format.getIdentification() ){
                case DynaTableClientConfigTranscodeFormat.DEFAULT_THUMB:
                    // resource thumbnail
                    destinationUrl = destinationBase + "thumbnail/" + uploadBucketKey + "/thumb.jpg";
                    break;
                case DynaTableClientConfigTranscodeFormat.DEFAULT_POSTER_FRAME:
                    // default posterframe
                    destinationUrl = destinationBase + "posterframe/" + uploadBucketKey + "/default_frame.jpg";
                    break;
                case DynaTableClientConfigTranscodeFormat.DEFAULT_FIRST_FRAME:
                    // first-frame posterframe
                    destinationUrl = destinationBase + "posterframe/" + uploadBucketKey + "/first_frame.jpg";
                    break;
                default:
                    logger.warn( "Found unexpected format identification:{}, full format:{}",
                            format.getIdentification(), format );
                    throw new RuntimeException( "unexpected format:" + format );
            }

        }

        logger.debug( "setting destination={}", destinationUrl );
        format.setDestination( destinationUrl );

    }

    //>>>>>>
    public Jackson2JsonObjectMapper getObjJsonMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        objectMapper.configure( DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true );
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        objectMapper.configure( DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false );
        return new Jackson2JsonObjectMapper( objectMapper );
    }

    @Autowired
    private EncodingComAPI encodingCom;

    public Object sendInJson( Serializable query, Class rtType ) throws Exception{
        Jackson2JsonObjectMapper jsonObjectMapper = getObjJsonMapper();
        String json = jsonObjectMapper.toJson( query );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType( MediaType.APPLICATION_FORM_URLENCODED );
        String respStr = encodingCom.sendInJson( new HttpEntity<Object>( encodeEncodingCom( json ), headers ) );
        if( respStr.indexOf( "Wrong user id or key!" ) > 0 && respStr.indexOf( "error" ) > 0 ){
            logger.fatal(
                    "\n*******************************************fatal********************************************\n" +
                            respStr + "\n" + userIdForEncodingCom );
            throw new Error( respStr );
        }
        logger.debug( "return:{}", respStr );
        if( rtType == null ){
            return respStr;
        }
        return jsonObjectMapper.fromJson( respStr, rtType );
    }

    public String encodeEncodingCom( String json ) throws UnsupportedEncodingException{
        logger.debug( "{}", json );
        return "json=" + URLEncoder.encode( json, "utf-8" );
    }

    public String decodeEncodingCom( String str ) throws UnsupportedEncodingException{
        final String rt = URLDecoder.decode( str.substring( 5 ), "utf-8" );
        logger.debug( "{}", rt );
        return rt;
    }

    private void handleEncodingComError( DynaTableVideoTranscoding rt, Exception e ){
        if( e instanceof HttpClientErrorException && ( ( HttpClientErrorException )e ).getStatusCode().value() == 421 ){
            rt.setStatus( Result.STATUS_retry_421 );
        }
        else{
            rt.setStatus( Result.STATUS_error );
        }
        rt.setErrorMsg( ExceptionUtils.getStackTrace( e ) );
        logger.warn( rt.getErrorMsg() );
    }
    //<<<<<<


    @Data
    private static abstract class EncodingComJson implements Serializable{

        protected String mediaid;
        protected String source;

        protected List<DynaTableClientConfigTranscodeFormat> format;

    }

    @Data
    public static class Query implements Serializable{
        public static final String AddMediaBenchmark = "AddMediaBenchmark";
        public static final String GetMediaInfo      = "GetMediaInfo";
        public static final String UpdateMedia       = "UpdateMedia";
        public static final String ProcessMedia      = "ProcessMedia";
        public static final String GetStatus         = "GetStatus";

        protected QueryBody query;

        public Query(){
        }

        public Query( QueryBody query ){
            this.query = query;
        }
    }

    @Data
    @EqualsAndHashCode( callSuper = true )
    public static class QueryBody extends EncodingComJson implements Serializable{
        public QueryBody(){
        }

        public QueryBody( String userid, String userkey, String action ){
            this.userid = userid;
            this.userkey = userkey;
            this.action = action;
        }

        protected String userid;
        protected String userkey;
        protected String action;

        protected String extended;

        protected String notify_format = "json";
        protected String notify;
        protected String notify_upload;
        protected String notify_encoding_errors;
    }

    @Data
    public static class GetMediaInfoResponse implements Serializable{
        protected DynaTableVideoTranscodingMediaInfo response;
    }

    @Data
    public static class GetStatusResponse implements Serializable{
        protected GetMediaStatusResponse response;
    }

    @Data
    public static class GetStatusResponseExtended implements Serializable{
        protected GetStatusResponseBodyExtended response;
    }


    @Data
    public static class GetStatusResponseBodyExtended implements Serializable{
        protected GetMediaStatusResponse[] job;
    }

    @Data
    public static class GetMediaStatusResponse implements Serializable{

        private String                       id;
        private String                       userid;
        private String                       sourcefile;
        private String                       status;
        private String                       description;
        private String                       notifyurl;
        private String                       created;
        private String                       started;
        private String                       finished;
        private String                       prevstatus;
        private String                       downloaded;
        private String                       filesize;
        private String                       processor;
        private String                       region;
        private String                       time_left;
        private String                       progress;
        private String                       time_left_current;
        private String                       progress_current;
        private String                       queue_time;
        private String                       deleted;
        private GetMediaStatusResponseTask[] format;

    }

    @Data
    public static class GetMediaStatusResponseTask implements Serializable{

        private String id;
        private String status;
        private String created;
        private String started;
        private String finished;
        private String description;
        private String destination;
        private String destination_status;
        private String convertedsize;
        private String errorcode;
        private String errorsuggestion;
        private String queued;
        private String converttime;
        private String time_left;
        private String progress;
        private String time_left_current;
        private String progress_current;
        private String file_size;

    }


    @Data
    @EqualsAndHashCode( callSuper = true )
    public static class ResultBody extends EncodingComJson implements Serializable{

        protected String taskid;
        protected String destination;
        protected String encodinghost;
        protected Object description;
        protected String status;
    }

    @Data
    public static class Result implements Serializable{
        public static final String Status_new           = "New";
        public static final String STATUS_readToProcess = "Ready to process";
        public static final String STATUS_processing    = "Processing";
        public static final String STATUS_error         = "Error";
        public static final String STATUS_deleted       = "Deleted";
        public static final String STATUS_wrong_input   = "wrong_input";
        public static final String STATUS_retry_421     = "RETRY_421";
        public static final String STATUS_saved         = "Saved";
        public static final String STATUS_finished      = "Finished";
        protected ResultBody result;

    }

}
