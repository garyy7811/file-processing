package org.pubanatomy.videotranscoding;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.sqs.AmazonSQS;
import org.pubanatomy.awsS3Download.AwsS3DownloadDAO;
import org.pubanatomy.awsS3Download.DynaTableNVResource;
import org.pubanatomy.awsS3Download.DynaTableNVResourceVideoStream;
import org.pubanatomy.configPerClient.DynaTableClientConfigTranscodeFormat;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Collectors;


/**
 * User: flashflexpro@gmail.com
 * Date: 6/14/2016
 * Time: 4:43 PM
 */
public class OnTranscodeStatusChanged{

    private static final Logger logger = LogManager.getLogger( OnTranscodeStatusChanged.class );

    private String resourceStatusQueueUrl;
    private String downloadBucketName;

    public OnTranscodeStatusChanged( String resourceStatusQueueUrl, String downloadBucketName ){
        this.resourceStatusQueueUrl = resourceStatusQueueUrl;
        this.downloadBucketName = downloadBucketName;
        BUCKET_BASE_URL = downloadBucketName + ".s3.amazonaws.com/";
    }

    @Autowired
    private AwsS3DownloadDAO downloadDAO;

    @Autowired
    private AmazonS3Client s3Client;

    @Autowired
    private AmazonSQS sqs;

    private final String BUCKET_BASE_URL;

    public void onMediaStatusChanged( DynaTableVideoTranscoding transcRecord ){
        logger.debug( "mediaId:{}, status:{}", transcRecord.getMediaId(), transcRecord.getStatus() );

        if( TranscodingFunctions.Result.STATUS_finished.equals( transcRecord.getStatus() ) ){
            DynaTableNVResource res = downloadDAO.load( transcRecord.getUploadBucketKey(), transcRecord.getMediaId() );

            if( res == null ){
                res = new DynaTableNVResource();
            }

            res.setType( DynaTableNVResource.SLIDE_RES_TYPE_video );

            res.setSourceKey( transcRecord.getUploadBucketKey() );
            res.setProcessId( transcRecord.getMediaId() );

            try{
                final DynaTableNVResource finalRes = res;

                res.setFileInfoLst( transcRecord.getFormats().stream().filter( f -> "mp4".equals( f.getOutput() ) )
                        .map( transStream -> {

                            final DynaTableNVResourceVideoStream resStream = new DynaTableNVResourceVideoStream();
                            resStream.setRelativePath( transStream.getDestination().split( BUCKET_BASE_URL )[ 1 ] );
                            resStream.setFileSize(
                                    s3Client.getObjectMetadata( downloadBucketName, resStream.getRelativePath() )
                                            .getContentLength() );

                            // setting the bitrate in kbps - strip off the 'k' we expect to find at the end of the value
                            // from the transcoding record, and parse the result to an integer representing kbps
                            resStream.setBitRate( Long.parseLong(
                                    transStream.getBitrate().trim().substring( 0, transStream.getBitrate().length() - 1 ) ) );

                            resStream.setIsDefault( transStream.getIsDefault() );

                            // we will store the default stream's path as the downloadKey, since it is the file
                            // to be downloaded when streaming is not being used.
                            if( Boolean.valueOf( resStream.getIsDefault() ) ){
                                finalRes.setDownloadKey( resStream.getRelativePath() );
                            }

                            return resStream;
                        } ).collect( Collectors.toList() ) );


                transcRecord.getFormats().stream().filter( f -> "thumbnail".equals( f.getOutput() ) )
                        .forEach( thFmt -> {

                            logger.debug( "post-processing for format:{}", thFmt.getIdentification() );

                            String s3ObjectId = thFmt.getDestination().split( BUCKET_BASE_URL )[ 1 ];

                            logger.debug( "fetching s3 metadata for {} ", s3ObjectId );
                            ObjectMetadata fmtFileMeta = s3Client.getObjectMetadata( downloadBucketName, s3ObjectId );

                            if( DynaTableClientConfigTranscodeFormat.DEFAULT_THUMB
                                    .equals( thFmt.getIdentification() ) ){
                                finalRes.setThumbnailWidth( Integer.parseInt( thFmt.getWidth() ) );
                                finalRes.setThumbnailHeight( Integer.parseInt( thFmt.getHeight() ) );
                                finalRes.setThumbnailKey( s3ObjectId );
                                finalRes.setThumbnailFileSize( fmtFileMeta.getContentLength() );
                            }
                            else if( DynaTableClientConfigTranscodeFormat.DEFAULT_POSTER_FRAME
                                    .equals( thFmt.getIdentification() ) ){
                                finalRes.setDefaultPosterframeFileSize( fmtFileMeta.getContentLength() );
                                finalRes.setDefaultPosterframeKey( s3ObjectId );
                            }
                            else if( DynaTableClientConfigTranscodeFormat.DEFAULT_FIRST_FRAME
                                    .equals( thFmt.getIdentification() ) ){
                                finalRes.setFirstFramePosterframeFileSize( fmtFileMeta.getContentLength() );
                                finalRes.setFirstFramePosterframeKey( s3ObjectId );
                            }
                            else{
                                finalRes.setErrorMsg(
                                        "UNKNOW THUMNAIL ID:" + thFmt.getIdentification() + ", format:\n" + thFmt );
                                logger.error( "UNKNOWN THUMBNAIL identification={}, full format:{}",
                                        thFmt.getIdentification(), thFmt );
                            }

                        } );

                final String[] size = transcRecord.getMediaInfo().getSize().split( "x" );

                res.setWidth( Integer.parseInt( size[ 0 ] ) );
                res.setHeight( Integer.parseInt( size[ 1 ] ) );
                res.setFileSize( Long.parseLong( transcRecord.getMediaInfo().getFilesize() ) );
            }
            catch( Exception e ){
                logger.error( e );
                res.setErrorMsg( ExceptionUtils.getStackTrace( e ) );
            }

            downloadDAO.save( res );
        }

        sqs.sendMessage( resourceStatusQueueUrl,
                DynaTableNVResource.SLIDE_RES_TYPE_video + "|" + transcRecord.getUploadBucketKey() + "|" +
                        transcRecord.getMediaId() + "|" + transcRecord.getStatus() );


    }

}
