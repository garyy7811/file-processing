package org.pubanatomy.awsS3Upload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.util.Base64;
import org.pubanatomy.awsS3Download.AwsS3DownloadDAO;
import org.pubanatomy.awsS3Download.DynaTableNVResource;
import org.pubanatomy.configPerClient.ConfigPerClientDAO;
import org.pubanatomy.configPerClient.DynaTableClientConfig;
import org.pubanatomy.loginverify.DynaLogInSessionInfo;
import org.pubanatomy.loginverify.DynamoLoginInfoDAO;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AwsS3UploadService{

    private static final Logger logger = LogManager.getLogger( AwsS3UploadService.class );

    private String awsS3UploadBucket;
    private String awsS3DownloadBucket;
    private String awsS3UploadUrl;
    private String awsS3DownloadUrl;
    private String resourceStatusQueueUrl;
    private String rootUserId;

    public String getAwsS3UploadBucket(){
        return awsS3UploadBucket;
    }

    public String getAwsS3UploadUrl(){
        return awsS3UploadUrl;
    }

    public AwsS3UploadService( String awsS3UploadBucket, String awsS3DownloadBucket, String awsS3UploadUrl,
                               String awsS3DownloadUrl, String queueUrl, String rootUserId ){
        if( awsS3UploadBucket == null || awsS3UploadUrl == null ){
            throw new IllegalArgumentException( "uploadBucket can't be null" );
        }
        this.awsS3UploadBucket = awsS3UploadBucket;
        this.awsS3DownloadBucket = awsS3DownloadBucket;
        this.awsS3UploadUrl = awsS3UploadUrl;
        this.awsS3DownloadUrl = awsS3DownloadUrl;
        this.resourceStatusQueueUrl = queueUrl;
        this.rootUserId = rootUserId;
    }

    @Autowired
    protected AWSCredentialsProvider credentialsProvider;

    @Autowired
    protected AwsS3UploadDAO awsS3UploadDAO;

    @Autowired
    protected AwsS3DownloadDAO awsS3DownloadDAO;

    private static SimpleDateFormat format;

    public static SimpleDateFormat getFormat(){
        return format;
    }

    @Autowired
    private DynamoLoginInfoDAO loginInfoDAO;

    @Autowired
    private ConfigPerClientDAO configPerClientDAO;

    @Autowired
    private AmazonSQS sqs;

    public AwsS3UploadDAO getAwsS3UploadDAO(){
        return awsS3UploadDAO;
    }

    public AwsS3DownloadDAO getAwsS3DownloadDAO(){
        return awsS3DownloadDAO;
    }

    public DynamoLoginInfoDAO getLoginInfoDAO(){
        return loginInfoDAO;
    }

    public ConfigPerClientDAO getConfigPerClientDAO(){
        return configPerClientDAO;
    }

    public static final List<String> SUPPORTED_EXTS =
            Arrays.asList( ".swf", ".jpg", ".jpeg", ".gif", ".png", ".flv", ".f4v", ".mp4", ".mov", ".wmv", ".avi",
                    ".mpg", ".mpeg", ".m4v", ".wma" );

    /**
     * private Long   fileRefCreationDate;
     * private Long   fileRefModificationDate;
     * private String fileRefCreator;
     * private String fileRefName;
     * private Long fileRefSizeBytes;
     * private String fileRefType;
     * private String extraMsg;
     */

    public Object[] authenticateUploading( String csSessionId, Date fileRefCreationDate, Date fileRefModificationDate,
                                           String fileRefCreator, String fileRefName, Long fileRefSizeBytes,
                                           String fileRefType, String extraMsg ) throws IllegalAccessException{
        logger.debug( "csSessionId = [" + csSessionId + "], fileRefCreationDate = [" + fileRefCreationDate +
                "], fileRefModificationDate = [" + fileRefModificationDate + "], fileRefCreator = [" + fileRefCreator +
                "], fileRefName = [" + fileRefName + "], fileRefSizeBytes = [" + fileRefSizeBytes +
                "], fileRefType = [" + fileRefType + "], extraMsg = [" + extraMsg + "]" );

        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        DynaTableClientConfig clientConfig = configPerClientDAO.loadConfig( logInSessionInfo.getClientId() );

        if( ! clientConfig.getUpload().isEnabled() ){
            logger.info( "upload disabled by{}, time:{}", clientConfig.getUpload().getEnabledChangedBy(),
                    new Date( clientConfig.getUpload().getEnabledChangedTime() ) );
            throw new IllegalArgumentException( "clientConfigUploadDisabled" );
        }

        Long maxSizeBytes = clientConfig.getUpload().getUploadSizeLimitPerFileInM() * 1024 * 1024;

        if( fileRefSizeBytes > maxSizeBytes ){
            logger.info( "File size:{}, exceeded limit:{}", fileRefSizeBytes,
                    clientConfig.getUpload().getUploadSizeLimitPerFileInM() );
            throw new IllegalArgumentException(
                    "uploadFileSizeOverLimit:" + clientConfig.getUpload().getUploadSizeLimitPerFileInM() );
        }

        if( format == null ){
            format = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.S'Z'" );
            format.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        }
        if( fileRefType == null || fileRefType.length() == 0 ){
            int lastPeriod = fileRefName.lastIndexOf( "." );
            if( lastPeriod > 0 ){
                fileRefType = fileRefName.substring( lastPeriod );
            }
            else{
                throw new IllegalArgumentException( "FileExtensionNotFound" );
            }
        }
        fileRefType = fileRefType.toLowerCase();
        if( SUPPORTED_EXTS.indexOf( fileRefType ) < 0 ){
            logger.info( "InvalidFileType:{}", fileRefType );
            throw new IllegalArgumentException( "InvalidFileType" );
        }

        long uploadApplyTimeStamp = System.currentTimeMillis();
        String uploadS3BucketKey =
                logInSessionInfo.getClientId() + "/" + logInSessionInfo.getUserId() + "/" + csSessionId + "/" +
                        uploadApplyTimeStamp;

        AWSCredentials credentials = null;
        String policyB64 = null;
        String signature = null;
        String policy_document = null;
        try{
            credentials = credentialsProvider.getCredentials();

            logger.info( "Now generating policy ..." );

            Date expireDate = new Date( uploadApplyTimeStamp + 120 * 1000 );
            policy_document = "{\n" + "    \"expiration\": \"" + format.format( expireDate ) + "\",\n" +
                    "    \"conditions\": [\n" + "        {\"bucket\": \"" + awsS3UploadBucket + "\"},\n" +
                    "        {\"key\": \"" + uploadS3BucketKey + "\"},\n" + "        {\"acl\": \"private\"},\n" +
                    "        [\"starts-with\", \"$Filename\", \"\"],\n" +
                    "        [\"eq\", \"$success_action_status\", \"201\"]\n" + "    ]\n" + "}";

            Mac hmac = Mac.getInstance( "HmacSHA1" );
            hmac.init( new SecretKeySpec( credentials.getAWSSecretKey().getBytes( "UTF-8" ), "HmacSHA1" ) );
            policyB64 = new String( Base64.encode( policy_document.getBytes( "UTF-8" ) ), "UTF-8" );
            signature = new String( Base64.encode( hmac.doFinal( policyB64.getBytes( "UTF-8" ) ) ), "UTF-8" );
        }
        catch( Exception e ){
            logger.error( ExceptionUtils.getStackTrace( e ) );
            throw new IllegalArgumentException( "internal.error" );
        }

        DynaTableAwsS3Upload upload = new DynaTableAwsS3Upload();

        upload.setCsSessionId( csSessionId );
        upload.setFileRefSizeBytes( fileRefSizeBytes );
        upload.setFileRefCreationDate( fileRefCreationDate.getTime() );
        upload.setFileRefModificationDate( fileRefModificationDate.getTime() );
        upload.setFileRefCreator( fileRefCreator );
        upload.setFileRefName( fileRefName );
        upload.setFileRefType( fileRefType );
        upload.setExtraMsg( extraMsg );
        upload.setUserId( logInSessionInfo.getUserId() );
        //        upload.setUserName( logInSessionInfo.getUserName() );
        //        upload.setGroupId( logInSessionInfo.getGroupId() );
        upload.setClientId( logInSessionInfo.getClientId() );

        upload.setS3Bucket( awsS3UploadBucket );
        upload.setS3BucketKey( uploadS3BucketKey );


        upload.setAwSAccessKeyId( credentials.getAWSAccessKeyId() );
        upload.setApplyTimeStamp( uploadApplyTimeStamp );
        awsS3UploadDAO.saveUpload( upload );

        String uploadBucketOrUrl = awsS3UploadBucket;
        if( awsS3UploadUrl.startsWith( "http" ) ){
            uploadBucketOrUrl = awsS3UploadUrl;
        }

        logger.info( "returning: {}", policy_document );

        return new Object[]{ uploadBucketOrUrl, //urlRequest.url [0]
                credentials.getAWSAccessKeyId(), //vars.AWSAccessKeyId [1]
                upload.getS3BucketKey(), //vars.key [2]
                policyB64, //vars.policy [3]
                signature, //vars.signature [4]
                upload.getApplyTimeStamp(), //[5]
                upload.getFileRefType() //[6]
        };
    }


    public void uploadResult( String csSessionId, Long applyTimeStamp, String errorId, String error, String errorTxt )
            throws IllegalAccessException{

        logger.info( "csSessionId:{}, applyTimeStamp:{}", csSessionId, applyTimeStamp );

        String errorMsg = null;
        if( errorId != null ){
            errorMsg = errorId + ":|:" + error + ":|:" + errorTxt;
        }

        for( int i = 0; i < 3; i++ ){
            try{
                DynaTableAwsS3Upload asu = awsS3UploadDAO.loadUpload( csSessionId, applyTimeStamp );
                if( asu == null ){
                    logger.info( "no record found with csSessionId: {}", csSessionId );
                    return;
                }
                asu.setUploadedByClientTime( System.currentTimeMillis() );
                if( errorMsg != null ){
                    asu.setErrorMsg( errorMsg );
                }

                awsS3UploadDAO.saveUpload( asu );
                return;
            }
            catch( AmazonServiceException e ){
                logger.warn( ExceptionUtils.getStackTrace( e ) );
                if( "413".equals( e.getErrorCode() ) && errorMsg != null ){
                    errorMsg = errorMsg.substring( 0, errorMsg.length() / 2 );
                    i--;
                }
            }
        }
    }

    public Object[] authenticateResourceThumbnailUpload( String csSessionId, Long slideResourceId, String thumbnailType,
                                                         String s3KeyPattern ) throws IllegalAccessException{
        logger.debug( "csSessionId:{}, slideResourceId:{}, thumbnailType:{},s3KeyPattern:{}", csSessionId,
                slideResourceId, thumbnailType, s3KeyPattern );
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        return authS3Post( s3KeyPattern );
    }

    public Object[] authenticateSlideThumbnailUpload( String csSessionId, Long slideId, String s3KeyPattern )
            throws IllegalAccessException{
        logger.debug( "csSessionId:{}, slideId:{}, s3KeyPattern:{}", csSessionId, slideId, s3KeyPattern );

        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );

        return authS3Post( s3KeyPattern );
    }

    private Object[] authS3Post( String s3KeyPattern ){
        if( format == null ){
            format = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.S'Z'" );
            format.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        }

        String policy = "{\n  \"expiration\": \"$expirationDate$\",\n  \"conditions\": [\n    {\n" +
                "      \"bucket\": \"$bucketName$\"\n    },\n    {\n      \"acl\": \"$acl$\"\n    },\n" +
                "    [\n      \"starts-with\",\n      \"$key\",\n      \"$keyPattern$\"\n    ],\n" +
                "    [\n      \"eq\",\n      \"$success_action_status\",\n      \"201\"\n    ]\n  ]\n}";


        final long ttl = 4 * 3600 * 1000;
        //AWS S3 access control list for policy
        String acl = "private";
        policy = policy.replaceAll( "\\$expirationDate\\$",
                format.format( new Date( System.currentTimeMillis() + ttl ) ) );
        policy = policy.replaceAll( "\\$bucketName\\$", awsS3DownloadBucket );
        policy = policy.replaceAll( "\\$keyPattern\\$", s3KeyPattern );
        policy = policy.replaceAll( "\\$acl\\$", acl );

        //access key used for policy signature
        String awsAccessKeyId = credentialsProvider.getCredentials().getAWSAccessKeyId();

        String encodedPolicy = null;
        String signature = null;
        try{
            Mac hmac = Mac.getInstance( "HmacSHA1" );
            hmac.init( new SecretKeySpec( credentialsProvider.getCredentials().getAWSSecretKey().getBytes( "UTF-8" ),
                    "HmacSHA1" ) );

            //base64 encoded policy document
            encodedPolicy = new String( Base64.encode( policy.getBytes( "UTF-8" ) ), "UTF-8" );
            //base64 encoded policy signature
            signature = new String( Base64.encode( hmac.doFinal( encodedPolicy.getBytes( "UTF-8" ) ) ), "UTF-8" );
        }
        catch( Throwable e ){
            logger.error( e );
            throw new RuntimeException( "internal.error" );
        }

        return new Object[]{ awsS3DownloadUrl, awsAccessKeyId, acl, encodedPolicy, signature, ttl };
    }


    public List<DynaTableAwsS3Upload> loadByUserId( @NotEmpty String csSessionId, @NotNull String userId,
                                                    @NotNull Date uploadTimeTo, Integer maxResult, Boolean desc )
            throws IllegalAccessException{

        logger.debug( "{},{},{},{}, {}", csSessionId, userId, uploadTimeTo, maxResult, desc );

        if( maxResult > 2000 ){
            maxResult = 2000;
        }
        if( uploadTimeTo == null || maxResult < 1 ){
            throw new IllegalArgumentException( "illegalTimeOrMaxRslt" );
        }
        DynaLogInSessionInfo logInSessionInfo = loginInfoDAO.loadCsSessionInfo( csSessionId, true );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            if( userId == null ){
                userId = logInSessionInfo.getUserId();
            }
            else if( ! logInSessionInfo.getUserId().equals( userId ) ){
                logger.debug( " userId mismatch:{}<=>{}", userId, logInSessionInfo.getUserId() );
                throw new IllegalAccessException( "requireAuth" );
            }
        }

        return awsS3UploadDAO.loadByUserId( userId, uploadTimeTo.getTime(), maxResult, desc );
    }


    @Autowired
    private AmazonS3 s3Client;

    /**
     * @param csSessionId
     * @param uploadApplyTime
     * @param thumbBytes
     * @param width
     * @param height
     * @param thumbWidth
     * @param thumbHeight
     * @return
     */
    public String postFlashThumbnail( @NotEmpty String csSessionId, @NotNull Long uploadApplyTime,
                                      @NotNull byte[] thumbBytes, @NotNull Integer width, @NotNull Integer height,
                                      @NotNull Integer thumbWidth, @NotNull Integer thumbHeight )
            throws IllegalAccessException{
        logger.debug( "{},{},{},{},{},{},", csSessionId, uploadApplyTime, thumbBytes.length, width, height, thumbWidth,
                thumbHeight );
        DynaTableAwsS3Upload uploadedSWF = awsS3UploadDAO
                .loadUpload( awsS3UploadDAO.getUploadKeyWithSessionId( csSessionId, uploadApplyTime.toString() ) );
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType( "image/png" );
        final String swfThumbDownloadKey = "thumbnail/" + uploadedSWF.getS3BucketKey() + "/thumb.png";
        PutObjectResult rslt =
                s3Client.putObject( awsS3DownloadBucket, swfThumbDownloadKey, new ByteArrayInputStream( thumbBytes ),
                        metadata );

        DynaTableNVResource resFlash =
                awsS3DownloadDAO.load( uploadedSWF.getS3BucketKey(), DynaTableNVResource.SLIDE_RES_TYPE_flash );
        if( resFlash == null ){
            resFlash = new DynaTableNVResource();
            resFlash.setType( DynaTableNVResource.SLIDE_RES_TYPE_flash );
            resFlash.setSourceKey( uploadedSWF.getS3BucketKey() );
            resFlash.setProcessId( DynaTableNVResource.SLIDE_RES_TYPE_flash );
        }

        resFlash.setWidth( width );
        resFlash.setHeight( height );
        resFlash.setThumbnailKey( swfThumbDownloadKey );
        resFlash.setThumbnailWidth( thumbWidth );
        resFlash.setThumbnailHeight( thumbHeight );
        resFlash.setThumbnailFileSize( new Integer( thumbBytes.length ).longValue() );
        awsS3DownloadDAO.save( resFlash );
        final String msg = resFlash.getType() + "|" + resFlash.getSourceKey() + "|" + resFlash.getProcessId() + "|" +
                ( resFlash.getDownloadKey() == null ? "snapshot" : "Finished" );
        sqs.sendMessage( resourceStatusQueueUrl, msg );

        logger.debug( "SQS, url:{}, msg{}", resourceStatusQueueUrl, msg );

        return rslt.getContentMd5();
    }


}
