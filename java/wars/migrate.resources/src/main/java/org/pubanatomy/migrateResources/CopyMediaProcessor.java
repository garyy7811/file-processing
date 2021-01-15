package org.pubanatomy.migrateResources;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.pubanatomy.labyrinth.mysql.QueryNewVictoryMysql;
import org.pubanatomy.migrateResources.status.ErrorStats;
import org.pubanatomy.migrateResources.status.StatsReporter;
import org.pubanatomy.migrationutils.MigrationErrorRecord;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * User: Greg
 * Date: 10/5/2016
 */
public abstract class CopyMediaProcessor {

    protected final Logger logger = LogManager.getLogger( this.getClass() );

    protected Stopwatch downloadDurationStopwatch;
    protected Stopwatch downloadBytesStopwatch;
    protected Stopwatch downloadKbpsStopwatch;
    protected Stopwatch downloadActiveStopwatch;
    protected Stopwatch uploadDurationStopwatch;
    protected Stopwatch uploadBytesStopwatch;
    protected Stopwatch uploadKbpsStopwatch;
    protected Stopwatch uploadActiveStopwatch;

    public CopyMediaProcessor(){

        logger.info("Constructed");

        downloadDurationStopwatch = SimonManager.getStopwatch("download.duration." + getFileType());
        downloadBytesStopwatch = SimonManager.getStopwatch("download.bytes." + getFileType());
        downloadKbpsStopwatch = SimonManager.getStopwatch("download.kbps." + getFileType());
        downloadActiveStopwatch = SimonManager.getStopwatch("download.active." + getFileType());
        uploadDurationStopwatch = SimonManager.getStopwatch("upload.duration." + getFileType());
        uploadBytesStopwatch = SimonManager.getStopwatch("upload.bytes." + getFileType());
        uploadKbpsStopwatch = SimonManager.getStopwatch("upload.kbps." + getFileType());
        uploadActiveStopwatch = SimonManager.getStopwatch("upload.active." + getFileType());
    }

    @Autowired
    protected QueryNewVictoryMysql queryNewVictoryMysql;


    @Autowired
    protected ErrorStats errorStats;


    @Autowired
    StatsReporter statsReporter;


    @Autowired
    protected EnvironmentConfig envConfig;


    @Autowired
    private SourcePollingChannelAdapter resourceInputAdapter;

    @Autowired
    private SourcePollingChannelAdapter slideThumbnailInputAdapter;


    @Autowired
    @Qualifier( "newVictoryMySQL" )
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected DynamoDBMapperConfig dynamoDBMapperConfig;

    @Autowired
    protected AmazonDynamoDBClient dynamoDBClient;

    @Autowired
    protected AmazonS3Client s3client;

    protected DynamoDBMapper dynamoDBMapper;

    @Value("${executor.item.threads}")
    protected Integer itemThreads;

    @Value("${executor.version.threads}")
    protected Integer versionThreads;

    @Value("${slideResource.endId}")
    protected Long slideResourceEndId;

    @Value("${slideThumbnail.endId}")
    protected Long slideThumbnailEndId;


    @Value("${processor.checkS3Exists}")
    protected Boolean checkS3Exists;

    @Value("${processor.useLocalFileSystem}")
    protected Boolean useLocalFileSystem;

    @Value("${processor.dryRunOnly}")
    protected Boolean dryRunOnly;



    @PostConstruct
    protected void postConstruct(){
        dynamoDBMapper = new DynamoDBMapper( dynamoDBClient, dynamoDBMapperConfig );
    }



    protected static final ThreadLocal<XPathFactory> xPathFactory = new ThreadLocal<XPathFactory>(){
        @Override
        protected XPathFactory initialValue(){
            return XPathFactory.newInstance();
        }
    };


    public abstract String getFileType();

    public abstract Long loadMaxRecordId();

    public abstract Object[] selectItemRange( Long[] range );

    public abstract void processItem(Map<String, Object> mso);

    public abstract void reportStats(long rangeSize, long rangeEnd, long durationMillis);


    public ObjectMetadata buildObjectMetadata(Map<String, Object> mso) {

        ObjectMetadata objMeta = new ObjectMetadata();
        objMeta.setUserMetadata( new HashMap<>() );

        mso.entrySet().forEach( e -> {
            if( e.getValue() != null && ! e.getKey().equals( "metadata" ) ){
                objMeta.getUserMetadata().put( e.getKey(), e.getValue().toString() );
            }
        } );

        return objMeta;
    }

    protected Boolean doesS3ObjectExist(String s3ObjectId) throws Exception {

        Boolean objectExists = false;

        try {
            logger.debug("checking existence of s3ObjectId={}", s3ObjectId);
            objectExists = s3client.doesObjectExist(envConfig.getLabyrinthS3bucket(), s3ObjectId);
            logger.debug("got objectExists={} for s3ObjectId={}", objectExists, s3ObjectId);
        } catch (Exception e) {
            reportError("doesS3ObjectExist", s3ObjectId, e);
        }

        return objectExists;
    }

    protected void copyLocalFileToS3(File localFile, String s3ObjectId, ObjectMetadata objectMetadata) throws Exception {

        String localPath = localFile.getAbsolutePath();

        logger.debug( "copyLocalFileToS3:{}->{}", localPath, s3ObjectId );

        // start recording transfer metrics
        Split durationSplit = uploadDurationStopwatch.start();
        uploadActiveStopwatch.addSplit(Split.create(uploadDurationStopwatch.getActive() * 1000));
        long uploadBytes = 0;

        try{
            if (!localFile.exists()) {
                reportError("local-file-not-found", s3ObjectId, localPath);
                return;
            }

            String activeThreadName = Thread.currentThread().getName();
            objectMetadata.getUserMetadata().put("cs-upload-thread", activeThreadName);

            // set the contentLength using the File.length to avoid the S3Client from
            // buffering the entire file, as contentLength is a required attribute when
            // invoking the S3 Put operation.
            uploadBytes = localFile.length();
            objectMetadata.setContentLength(uploadBytes);

            // now update the contentType based upon file extension
            String contentType = getContentTypeForFileExtension(s3ObjectId);
            objectMetadata.setContentType(contentType);

            // also add the contentType to the userMetaData
            objectMetadata.getUserMetadata().put("cs-content-type", contentType);

            objectMetadata.getUserMetadata().put("cs-checkS3Exists", checkS3Exists.toString());

            final FileInputStream inputStream = new FileInputStream(localFile);

            putInputStreamToS3(inputStream, s3ObjectId, objectMetadata);

            // finish recording transfer metrics
            durationSplit.stop();
            long durationMillis = TimeUnit.MILLISECONDS.convert(durationSplit.runningFor(), TimeUnit.NANOSECONDS);
            long kbps = uploadBytes / durationMillis;

            uploadBytesStopwatch.addSplit(Split.create(uploadBytes));
            uploadKbpsStopwatch.addSplit(Split.create(kbps));
        }
        catch( Exception e ){
            reportError("copyLocalFileToS3", localPath, e);
            throw e;
        } finally {
            // just in case the split hadn't been stopped...
            durationSplit.stop();
        }
    }

    protected File downloadRemoteFile(String remoteUrl) {

        logger.debug("downloadRemoteFile({})", remoteUrl);

        URL url = null;
        File localTmpFile = null;
        ReadableByteChannel rbc = null;
        FileOutputStream fos = null;

        // start recording transfer metrics
        Split durationSplit = downloadDurationStopwatch.start();
        downloadActiveStopwatch.addSplit(Split.create(downloadDurationStopwatch.getActive() * 1000));
        long downloadBytes = 0;

        try {

            // open remote stream for reading
            url = new URL(remoteUrl);
            rbc = Channels.newChannel(url.openStream());

            // open local stream for writing
            localTmpFile = Files.createTempFile("tmp_", ".tmp").toFile();
            fos = new FileOutputStream(localTmpFile);

            // transfer from remote to local
            downloadBytes = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

//            logger.info("NOT downlaoding file!");
//            byte[] tmpBytes = "test".getBytes();
//            fos.write(tmpBytes);
//            downloadBytes = tmpBytes.length;

            logger.debug("bytesTransferred={} from remoteUrl={}, to localFile={} ", downloadBytes, remoteUrl, localTmpFile.getName());


            // finish recording transfer metrics
            durationSplit.stop();
            long durationMillis = TimeUnit.MILLISECONDS.convert(durationSplit.runningFor(), TimeUnit.NANOSECONDS);
            long kbps = downloadBytes / durationMillis;

            downloadBytesStopwatch.addSplit(Split.create(downloadBytes));
            downloadKbpsStopwatch.addSplit(Split.create(kbps));


        } catch (Exception e) {

            logger.error("Error downloading remote file '"+ remoteUrl +"'", e);

            if (localTmpFile != null) {
                logger.debug("deleting tmp file for failed download: " + localTmpFile.getAbsolutePath());

                localTmpFile.delete();
                localTmpFile = null;
            }

        } finally {

            try {
                if (rbc != null) {
                    rbc.close();
                }
            } catch (Exception e) {

            }

            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {

            }

            // just in case the split hadn't been stopped...
            durationSplit.stop();
        }

        return localTmpFile;
    }


    protected void putInputStreamToS3(InputStream inputStream, String s3ObjectId, ObjectMetadata objectMetadata) {

        logger.debug( "putS3 start:{}", s3ObjectId );
        long startTimeMs = System.currentTimeMillis();

        try{

            if (dryRunOnly) {

                logger.info("dryRunOnly - NOT putting file to s3!");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                    // noop
                }

            } else {

                PutObjectRequest poRequest = new PutObjectRequest(envConfig.getLabyrinthS3bucket(), s3ObjectId, inputStream, objectMetadata );
                PutObjectResult poResult =  s3client.putObject(poRequest);
                logger.info("completed putting object with key=" + poRequest.getKey());
            }

        }
        catch( Exception e ){
            reportError("putInputStreamToS3", s3ObjectId, e);
            throw e;
        } finally {
            try {
                inputStream.close();
            } catch (Exception e2) {
                logger.error("Error closing inputStream for file:"+s3ObjectId, e2);
            }
        }


        long deltaMs = System.currentTimeMillis() - startTimeMs;

        long contentLength = objectMetadata.getContentLength();
        String dataRateKbps = String.format("%.2f", ((double)contentLength / (double)deltaMs));

        logger.debug( "putS3 complete for s3_id={}, duration_ms={}, content_length={}, Kbps={}", s3ObjectId, deltaMs, contentLength, dataRateKbps);
    }



    protected void reportError(String errorType, String fileId, Exception e) {
        logger.error(errorType + " for " + fileId, e);
        dynamoDBMapper.save( new MigrationErrorRecord( errorType, fileId, ExceptionUtils.getStackTrace( e )));
        errorStats.recordError(errorType);
    }

    protected void reportError(String errorType, String fileId, String msg) {
        logger.error(errorType + " for " + fileId + ": " + msg);
        dynamoDBMapper.save( new MigrationErrorRecord( errorType, fileId, msg));
        errorStats.recordError(errorType);
    }


    public Long[] processRange( Object[] rangeAndLstOfMso ){

        Long[] result;

        if (itemThreads > 1) {
            result = processRangeMultiThreads(rangeAndLstOfMso);
        } else {
            result = processRangeSingleThread(rangeAndLstOfMso);
        }

        return result;
    }

    private Long[] processRangeSingleThread( Object[] rangeAndLstOfMso ){

        long startMillis = System.currentTimeMillis();


        Long[] range = (Long[])rangeAndLstOfMso[0];
        Long rangeStart = range[0];
        Long rangeEnd = range[1];
        Long rangeSize = rangeEnd - rangeStart;

        List<Map<String, Object>> lstOfMso = ( List<Map<String, Object>> )rangeAndLstOfMso[ 1 ];

        logger.info("processing {} items using single thread", lstOfMso.size());

        lstOfMso.stream().forEachOrdered( mso -> {
                processItem(mso);
        } );

        long durationMillis = System.currentTimeMillis() - startMillis;

        reportStats(rangeSize, rangeEnd, durationMillis);

        return ( Long[] )rangeAndLstOfMso[ 0 ];
    }

    private Long[] processRangeMultiThreads( Object[] rangeAndLstOfMso ){

        long startMillis = System.currentTimeMillis();


        Long[] range = (Long[])rangeAndLstOfMso[0];
        Long rangeStart = range[0];
        Long rangeEnd = range[1];
        Long rangeSize = rangeEnd - rangeStart;

        List<Map<String, Object>> lstOfMso = ( List<Map<String, Object>> )rangeAndLstOfMso[ 1 ];

        logger.info("processing {} items using {} threads", lstOfMso.size(), itemThreads);

        ExecutorService itemExecutor = Executors.newFixedThreadPool(itemThreads);

        lstOfMso.stream().forEachOrdered( mso -> {

            itemExecutor.submit(() -> {
                logger.debug("threaded invocation of processItem");
                processItem(mso);
            });


        } );

        logger.info("requested all items processed - waiting for completion");

        try {
            itemExecutor.shutdown();
            boolean shutdownComplete = false;
            int timeoutCount = 0;
            while (!shutdownComplete) {
                shutdownComplete = itemExecutor.awaitTermination(10, TimeUnit.MINUTES);
                if (!shutdownComplete){
                    timeoutCount++;
                    reportError("item-executor-awaitTerminationTimeout-"+timeoutCount, "range-"+rangeStart+"-"+rangeEnd, "timeoutCount="+timeoutCount);
                }
            }

            logger.info("executor terminated");
        } catch (InterruptedException ie) {
            reportError("item-executor-interrupted", "range-"+rangeStart+"-"+rangeEnd, ie);
        }

        long durationMillis = System.currentTimeMillis() - startMillis;


        reportStats(rangeSize, rangeEnd, durationMillis);

        return ( Long[] )rangeAndLstOfMso[ 0 ];
    }


    protected String getContentTypeForFileExtension(String filename) {

        String fileExtension = filename.substring(filename.lastIndexOf(".")+1).toLowerCase();

        String contentType;

        switch (fileExtension) {
            case "swf":
                contentType = "application/x-shockwave-flash";
                break;
            case "mp4":
                contentType = "video/mp4";
                break;
            case "jpg":
            case "jpeg":
                contentType = "image/jpeg";
                break;
            case "png":
                contentType = "image/png";
                break;
            case "gif":
                contentType = "image/gif";
                break;
            case "bmp":
                contentType = "image/bmp";
                break;
            default:
                logger.warn("unexpected file extension:{}", fileExtension);
                contentType = "application/octet-stream";
                break;
        }

        return contentType;
    }


    protected String getLocalFilePath( String cacheFolder, String fileName ){
        // replace any spaces in fileName with underscores
        fileName = fileName.replace(" ", "_");
        return envConfig.getLabyrinthWebCacheRoot() + File.separator + cacheFolder + File.separator + fileName;
    }

    protected String getAppServerUrl( String context, String fileName ){
        // replace any spaces in fileName with underscores
        fileName = fileName.replace(" ", "_");
        return envConfig.getLabyrinthHttpUrlBase() +  "/" + context + "/" + fileName;
    }

    protected String getS3Path( String context, String fileName ){
        return context + "/" + fileName;
    }

    /**
     * utility method that will log a message to the fatal level, which should result in an email
     * being sent, as well as shutting down the inputAdapaters to stop further processing.
     *
     * @param message
     * @param cause
     */
    protected void handleFatalError(String message, Throwable cause) {

        logger.fatal("Halting processing due to fatal error: " + message, cause);

        try {
            resourceInputAdapter.stop();
        } catch (Exception e) {
            logger.error("Failed to stop resourceInputAdapter", e);
        }

        try {
            slideThumbnailInputAdapter.stop();
        } catch (Exception e) {
            logger.error("Failed to stop slideThumbnailInputAdapter", e);
        }


        throw new RuntimeException(message, cause);
    }
}
