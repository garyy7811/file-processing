package org.pubanatomy.processImgFla;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import org.pubanatomy.awsS3Download.AwsS3DownloadDAO;
import org.pubanatomy.awsS3Download.DynaTableNVResource;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.validation.ValidationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class ProcessImg{

    private static final Logger logger = LogManager.getLogger( ProcessImg.class );

    public ProcessImg( String awsS3UploadBucket, String awsS3DownloadBucket, String resourceStatusQueueUrl,
                       int maxThumbnailSize ){
        this.awsS3UploadBucket = awsS3UploadBucket;
        this.awsS3DownloadBucket = awsS3DownloadBucket;
        this.maxThumbnailSize = maxThumbnailSize;
        this.resourceStatusQueueUrl = resourceStatusQueueUrl;
    }

    private String resourceStatusQueueUrl;

    private String awsS3UploadBucket;
    private String awsS3DownloadBucket;
    private int    maxThumbnailSize;

    @Autowired
    private AwsS3DownloadDAO downloadDAO;

    @Autowired
    private AmazonS3 amazonS3;


    @Autowired
    private AmazonSQS sqs;

    public String getAwsS3UploadBucket(){
        return awsS3UploadBucket;
    }

    public String getAwsS3DownloadBucket(){
        return awsS3DownloadBucket;
    }

    public DynaTableNVResource onUploadFlaConfirmed( DynaTableAwsS3Upload uploaded ) throws IOException{
        logger.debug( "{}>>>>>>>", uploaded.getS3BucketKey() );

        final String downloadKey = "flash/" + uploaded.getS3BucketKey() + "/fla.swf";
        amazonS3.copyObject( awsS3UploadBucket, uploaded.getS3BucketKey(), awsS3DownloadBucket, downloadKey );

        DynaTableNVResource resFlash =
                downloadDAO.load( uploaded.getS3BucketKey(), DynaTableNVResource.SLIDE_RES_TYPE_flash );

        if( resFlash == null ){
            resFlash = new DynaTableNVResource();
            resFlash.setSourceKey( uploaded.getS3BucketKey() );
            resFlash.setProcessId( DynaTableNVResource.SLIDE_RES_TYPE_flash );
            resFlash.setType( DynaTableNVResource.SLIDE_RES_TYPE_flash );
        }

        resFlash.setDownloadKey( downloadKey );
        resFlash.setFileSize(
                amazonS3.getObjectMetadata( awsS3UploadBucket, uploaded.getS3BucketKey() ).getContentLength() );
        resFlash.setOriginalFileName( uploaded.getFileRefName() );

        downloadDAO.save( resFlash );

        sqs.sendMessage( resourceStatusQueueUrl,
                resFlash.getType() + "|" + resFlash.getSourceKey() + "|" + resFlash.getProcessId() + "|" +
                        ( resFlash.getThumbnailKey() == null ? "copied" : "Finished" ) );
        logger.debug( "{}<<<<<<< download:{}, ", uploaded.getS3BucketKey(), downloadKey );
        return resFlash;
    }

    public DynaTableNVResource onUploadImgConfirmed( DynaTableAwsS3Upload uploaded ) throws IOException{
        logger.debug( "{}>>>>>>>", uploaded.getS3BucketKey() );

        // set up filenames
        final String downloadKey = "image/" + uploaded.getS3BucketKey() + "/image" + uploaded.getFileRefType();
        final String thumbKey = "thumbnail/" + uploaded.getS3BucketKey() + "/thumb.jpg";

        final S3Object uploadedS3Obj = amazonS3.getObject( awsS3UploadBucket, uploaded.getS3BucketKey() );

        BufferedImage srcBufferedImage = null;

        // initialize the resource record
        final DynaTableNVResource imgRes = new DynaTableNVResource();
        imgRes.setType( DynaTableNVResource.SLIDE_RES_TYPE_image );
        imgRes.setSourceKey( uploaded.getS3BucketKey() );
        imgRes.setProcessId( "thumbnailCreatedOn" + System.currentTimeMillis() );
        imgRes.setDownloadKey( downloadKey );
        imgRes.setFileSize( uploadedS3Obj.getObjectMetadata().getContentLength() );
        imgRes.setOriginalFileName( uploaded.getFileRefName() );
        imgRes.setThumbnailKey( thumbKey );

        // Create imgInputStream stream
        ImageInputStream imgInputStream = null;
        try{
            // read source bytes into a BufferedImage - this will validate the image file
            logger.debug( "reading images bytes into BufferedImage" );
            imgInputStream = ImageIO.createImageInputStream( uploadedS3Obj.getObjectContent() );
            // Get the reader
            Iterator<ImageReader> readers = ImageIO.getImageReaders( imgInputStream );

            while( readers.hasNext() ){
                ImageReader reader = readers.next();
                try{
                    reader.setInput( imgInputStream );

                    ImageReadParam param = reader.getDefaultReadParam();

                    // Finally read the image, using settings from param
                    srcBufferedImage = reader.read( 0, param );
                    break;
                }
                catch( Throwable t ){
                    logger.info( "Image reader:{}, failed:{}", reader, t  );
                }
                finally{
                    // Dispose reader in finally block to avoid memory leaks
                    reader.dispose();
                }
            }

            // if we could not parse the image, report invalid image
            if( srcBufferedImage == null ){
                logger.error( "Could not parse image file!" );
                throw new IllegalArgumentException( "Invalid image file" );
            }

            logger.debug( "read image bytes into BufferedImage of type= " + srcBufferedImage.getType() );

            // update resource record using metadata from bufferedImage
            imgRes.setWidth( srcBufferedImage.getWidth() );
            imgRes.setHeight( srcBufferedImage.getHeight() );


            // generate the thumbnail bytes to stream
            logger.debug( "generating thumbnail bytes" );
            final byte[] thumbBytes = getThumbnailBytes( imgRes, srcBufferedImage );

            // update resource record
            imgRes.setThumbnailFileSize( new Integer( thumbBytes.length ).longValue() );

            // now we have validated the image and created the thumbnail bytes - copy the files

            // piece together contentType from the upload record, stripping off the leading period
            String srcContentType = "image/" + uploaded.getFileRefType().substring( 1 );

            // clone source metadata so we can update content-type
            ObjectMetadata copyObjectMetadata = uploadedS3Obj.getObjectMetadata().clone();
            logger.debug( "got copyObjectMetadata:{}", copyObjectMetadata );

            // set the content type from the upload type
            copyObjectMetadata.setContentType( srcContentType );

            // copy the source file to the output bucket
            CopyObjectRequest copyObjectRequest =
                    new CopyObjectRequest( awsS3UploadBucket, uploaded.getS3BucketKey(), awsS3DownloadBucket,
                            downloadKey ).withNewObjectMetadata( copyObjectMetadata );
            logger.info( "copying src image to download bucket" );
            amazonS3.copyObject( copyObjectRequest );

            // put thumbnail file into download bucket
            final ObjectMetadata thumbnailMetadata = new ObjectMetadata();
            thumbnailMetadata.setContentType( "image/png" );
            thumbnailMetadata.setContentLength( thumbBytes.length );
            logger.info( "saving thumbnail to download bucket" );
            amazonS3.putObject( awsS3DownloadBucket, thumbKey, new ByteArrayInputStream( thumbBytes ),
                    thumbnailMetadata );

            // save the resource record
            logger.debug( "saving resource record" );
            downloadDAO.save( imgRes );
        }
        catch( IllegalArgumentException e ){
            final String stackTrace = ExceptionUtils.getStackTrace( e );
            logger.error( " error generating {}'s thumbnail:{}", uploaded.getS3BucketKey(), stackTrace );
            imgRes.setErrorMsg( stackTrace );
            downloadDAO.save( imgRes );
            // in this case, re-throw
            throw e;
        }
        catch( Exception e ){
            final String stackTrace = ExceptionUtils.getStackTrace( e );
            logger.error( " error generating {}'s thumbnail:{}", uploaded.getS3BucketKey(), stackTrace );
            imgRes.setErrorMsg( stackTrace );
            downloadDAO.save( imgRes );
            // in this case, wrap as ValidationException
            throw new ValidationException( "Could not process image file", e );
        }
        finally{
            if( imgInputStream != null ){
                imgInputStream.close();
            }

            // cleanup buffered image and s3 imgInputStream stream
            if( uploadedS3Obj != null ){
                uploadedS3Obj.getObjectContent().close();
                ;
            }

            if( srcBufferedImage != null ){
                srcBufferedImage.flush();
            }

            // always send the sqs message - for both success and error results
            sqs.sendMessage( resourceStatusQueueUrl,
                    imgRes.getType() + "|" + imgRes.getSourceKey() + "|" + "" + imgRes.getProcessId() + "|" +
                            ( imgRes.getErrorMsg() == null ? "Finished" : "Error" ) );

        }

        logger.debug( "{}<<<<<<< download:{}, ", uploaded.getS3BucketKey(), downloadKey );
        return imgRes;
    }

    private byte[] getThumbnailBytes( DynaTableNVResource imgRes, BufferedImage orgInputImg ) throws Exception{

        int l = Math.max( orgInputImg.getHeight(), orgInputImg.getWidth() );

        final int thumbWidth = maxThumbnailSize * orgInputImg.getWidth() / l;
        final int thumbHeight = maxThumbnailSize * orgInputImg.getHeight() / l;
        imgRes.setThumbnailWidth( thumbWidth );
        imgRes.setThumbnailHeight( thumbHeight );

        final Image scaledThumbImg =
                orgInputImg.getScaledInstance( thumbWidth, thumbHeight, BufferedImage.SCALE_SMOOTH );


        BufferedImage thumbImage = new BufferedImage( thumbWidth, thumbHeight, BufferedImage.TYPE_INT_ARGB );
        final Graphics2D graphics2D = thumbImage.createGraphics();
        graphics2D.drawImage( scaledThumbImg, 0, 0, null );
        graphics2D.dispose();

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream cacheImageOutputStream = new MemoryCacheImageOutputStream( outputStream );

        ImageIO.write( thumbImage, "png", cacheImageOutputStream );

        logger.info( "generated thumbnail  with width:{}, height:{} ", thumbImage.getWidth(), thumbImage.getHeight() );
        cacheImageOutputStream.flush();
        cacheImageOutputStream.close();
        thumbImage.flush();
        scaledThumbImg.flush();

        final byte[] thumbBytes = outputStream.toByteArray();

        outputStream.close();

        return thumbBytes;
    }

}
