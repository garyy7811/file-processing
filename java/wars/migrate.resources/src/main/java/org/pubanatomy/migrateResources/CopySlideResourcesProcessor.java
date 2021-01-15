package org.pubanatomy.migrateResources;

import com.amazonaws.services.s3.model.ObjectMetadata;
import org.pubanatomy.batchpartition.RangePartitionService;
import org.pubanatomy.migrateResources.status.ResourceStats;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Map;

/**
 * Created by greg on 10/5/16.
 */
public class CopySlideResourcesProcessor extends CopyMediaProcessor {

    @Autowired
    private ResourceStats resourceStats;

    @Autowired
    private CopyPosterFramesProcessor posterFramesProcessor;

    @Autowired
    private CopyResourceThumbnailsProcessor resourceThumbnailsProcessor;


    @Autowired
    private RangePartitionService slideResourceContentPartitioning;



    public Long[] requestNextRange() {

        Long maxResId = 0L;

        if (slideResourceEndId >= 0) {
            maxResId = slideResourceEndId;
            logger.info("requestNextRange using configured slideResourceEndId:{}", slideResourceEndId);
        } else {
            logger.info("requestNextRange loading max resource id");
            maxResId = loadMaxRecordId();
        }

        resourceStats.setResourceEndId(maxResId);

        logger.info("making remote call to allocateRange({})", maxResId);

        Object allocateResult = null;

        try {
            allocateResult = slideResourceContentPartitioning.allocateRange( maxResId );
        } catch (Exception e) {
            // NOTE: this will halt further processing!
            handleFatalError("Unexpected error in allocateRange", e);
        }

        if (allocateResult instanceof Long[]) {

            Long[] nextRange = (Long[])allocateResult;

            if (nextRange.length == 0) {
                logger.warn("allocateRange returned empty list - returning null!");
            } else if (nextRange.length == 2) {
                logger.info("returning nextRange: {} => {}]", nextRange[0], nextRange[1]);
                return nextRange;
            } else {
                logger.warn("unexpected nextRange size: " + nextRange.length);
            }

        }  else {
            logger.warn("allocateRange did NOT return a Long[]: {}" + allocateResult);
        }

        return null;
    }

    public void markRangeComplete(Object rangeObj) {

        logger.info("got rangeObj: " + rangeObj);

        try {
            if (rangeObj instanceof Long[]) {
                Long[] range = (Long[])rangeObj;
                logger.info("invoking remote doneRange({},{})", range[0], range[1]);
                int response = slideResourceContentPartitioning.doneRange( range[ 0 ], range[ 1 ], ".");
                logger.info("remote doneRange({},{}) returned: {}", range[0], range[1], response);
            } else {
                logger.error("got unexpected rangObj: " + rangeObj);
            }
        } catch (Exception e) {
            // NOTE: this will halt further processing!
            handleFatalError("Unexpected error in markRangeComplete", e);
        }

    }

    @Override
    public String getFileType() {
        return "slideResource";
    }


    @Override
    public Long loadMaxRecordId(){

//        String maxRecordSql = "SELECT MAX(id) FROM SlideResourceContent";
//        Long maxRecordId = jdbcTemplate.queryForObject(maxRecordSql, Long.class ) + 1;
//        logger.info("loadMaxRecordId returning:  " + maxRecordId);
//        return maxRecordId;

        Split split = SimonManager.getStopwatch("loadMaxRecordId."+getFileType()).start();

        logger.debug("invoking remote loadResourceMaxRecordId");

        Long maxRecordId = null;

        try {
            maxRecordId = queryNewVictoryMysql.loadResourceMaxRecordId(true);
        } catch (Exception e) {
            // NOTE: this will halt further processing!
            handleFatalError("Unexpected error in loadResourceMaxRecordId", e);
        }

        logger.info("remote loadResourceMaxRecordId returned: " + maxRecordId);

        split.stop();

        return maxRecordId;

    }


    @Override
    public Object[] selectItemRange( Long[] range ){
//        String itemRangeSQL =
//                "SELECT SR.id as slide_resource_id, \n" +
//                "  SR.resource_type as slide_resource_type, \n" +
//                "  SR.name as slide_resource_name, \n" +
//                "  SRC.id as slide_resource_content_id, \n" +
//                "  SRC.version as slide_resource_content_version, \n" +
//                "  SRC.filename as slide_resource_content_filename, \n" +
//                "  SRC.original_filename as slide_resource_content_original_filename, \n" +
//                "  SRC.filesize as slide_resource_content_filesize, \n" +
//                "  SRC.width as slide_resource_content_width, \n" +
//                "  SRC.height as slide_resource_content_height, \n" +
//                "  SRC.thumbnail_id as thumbnail_id, \n" +
//                "  SRC.default_poster_frame_id as default_poster_frame_id, \n" +
//                "  SRC.first_frame_id as first_frame_id, \n" +
//                "  SRC.metadata as metadata \n" +
//                "FROM Magnet.SlideResourceContent SRC \n" +
//                "  INNER JOIN Magnet.SlideResource SR ON SRC.slide_resource_id = SR.id \n" +
//                "WHERE SRC.id >= ? AND SRC.id < ? \n" +
//                "ORDER BY SRC.id ASC";
//
//        final List<Map<String, Object>> rt = jdbcTemplate.queryForList( itemRangeSQL, range[ 0 ], range[ 1 ] );
//        logger.info("selectItemRange["+range[0]+", " + range[1]+"] returning " + rt.size() + " items");
//        return new Object[]{ range, rt };


        Split split = SimonManager.getStopwatch("selectItemRange."+getFileType()).start();


        Assert.notNull(range, "range must not be null!");
        Assert.isTrue(range.length == 2, "range should be length 2, got: " + range.length);

        logger.info("invoking remote selectResourceItemRange("+range[0]+", "+range[1]+")");

        Object[] rangeAndResults = queryNewVictoryMysql.selectResourceItemRange(range);

        logger.debug("remote selectResourceItemRange returned: " + rangeAndResults);

        split.stop();

        return rangeAndResults;
    }

    @Override
    public void reportStats(long rangeSize, long rangeEnd, long durationMillis) {
        resourceStats.resourceBatchProcessed(rangeSize, rangeEnd, durationMillis);
        statsReporter.reportCurrentStats();
    }


    @Override
    public void processItem(Map<String, Object> mso) {

        processResourceFile(mso);

        resourceThumbnailsProcessor.processItem(mso);

        String resourceType = mso.get("slide_resource_type").toString();
        if (resourceType.equals("video")) {

            posterFramesProcessor.processItem(mso);
        }

    }

    public void processResourceFile(Map<String, Object> mso) {


        Integer slideResourceContentId = Integer.parseInt(mso.get("slide_resource_content_id").toString());

        Integer slideResourceContentVersion = Integer.parseInt(mso.get("slide_resource_content_version").toString());

        String resourceType = mso.get("slide_resource_type").toString();

        logger.info("Processing {} slideResourceContentId={}", resourceType, slideResourceContentId);


        String slideResourceContentFilename = mso.get("slide_resource_content_filename").toString();

        // if the filename from the database contains a "/" character, then we know it is already in cs-cloud, so we can
        // skip those resources!
        if (slideResourceContentFilename.indexOf("/") >= 0) {
            logger.warn("found cs-cloud resource file - skipping!");
            resourceStats.incrementResourceFilesSkippedCount();
            return;
        }

        String fileExtension = slideResourceContentFilename.substring(slideResourceContentFilename.lastIndexOf("."));

        // set up src and dst filenames
        String legacyResourceFileName = slideResourceContentId + "_" + slideResourceContentFilename;

        String localReosurcePath = getLocalFilePath( getLocalCacheForType(resourceType), legacyResourceFileName );

        String appServerUrl =  getAppServerUrl(
                getRemoteContextForType(resourceType),
                legacyResourceFileName );


        // by default assume we'll use the actual slideResourceContentFilename in the S3 Object Id
        String s3ObjectFileName = slideResourceContentFilename;


        // NOTE: we will check if the slideResourceContentFilename matches the
        // form "${slideResourceContentId}_${slideResourceContentVersion}.${extension}",
        // and if it does not, we'll use that value and make a note in the metadata

//        String keyVersionFilename = slideResourceContentId + "_" + slideResourceContentVersion + fileExtension;
//
//        if (keyVersionFilename.equals(slideResourceContentFilename)) {
//            // found a match - this is what we expect
//            mso.put("adjusted_filename", "false");
//        } else {
//            s3ObjectFileName = keyVersionFilename;
//            mso.put("adjusted_filename", "true");
//            resourceStats.incrementResourceFileNamesAdjusted();
//            logger.warn("SlideResourceContent.fileName adjusted legacyFilename=" + slideResourceContentFilename + " s3FileName=" + keyVersionFilename);
//        }

        String fullDestPath = getS3Path(resourceType, s3ObjectFileName);

        File localFile = null;


        if (checkS3Exists) {

            try {
                if (doesS3ObjectExist(fullDestPath)) {
                    logger.info("found resource already exists for s3ObjectId={}", fullDestPath);
                    resourceStats.incrementResourceFilesSkippedCount();
                    return;
                }
            } catch (Exception e) {
                reportError("error-checking-resource-s3-exists", fullDestPath, e);
                return;
            }
        }


        try{

            if (useLocalFileSystem) {
                localFile = new File(localReosurcePath);
                if (!localFile.exists()) {
                    logger.warn("missing local file - skipping!");
                    resourceStats.incrementResourceFilesSkippedCount();
                    return;
                }
            } else {

                // attempt from appServer
                localFile = downloadRemoteFile(appServerUrl);
            }


            if (localFile == null) {
                reportError("missing-slide-resource-content", legacyResourceFileName, fullDestPath);
                return;
            }

            // add in a couple extra metadata fields
            mso.put("legacy_filename", legacyResourceFileName);
            mso.put("cs_cloud_media_type", "slide_resource_content");
            mso.put("slide_resource_content_file_extension", fileExtension);

            ObjectMetadata tmpObjectMetadata = buildObjectMetadata(mso);

            copyLocalFileToS3(localFile, fullDestPath, tmpObjectMetadata);

            resourceStats.incrementResourceFilesUploadedCount();

        }
        catch( Exception e ){
            logger.error("Unexpected error copying thumbnail '"+fullDestPath+"'", e);
            reportError("processSlideThumbnail", "slide-thumbnail-"+legacyResourceFileName, e);
        } finally {

            if (useLocalFileSystem) {
                logger.debug("NOT cleaning up local resource file: " + localFile);
            } else if (localFile != null) {
                logger.debug("deleting tmp resource file: " + localFile.getAbsolutePath());
                localFile.delete();
            }
        }

    }

    protected String getLocalCacheForType(String resourceType) {

        if (resourceType.equals("image")) {
            return envConfig.getLabyrinthImageCache();
        } else if (resourceType.equals("flash")) {
            return envConfig.getLabyrinthFlashCache();
        } else if (resourceType.equals("video")) {
            return envConfig.getLabyrinthVideoCache();
        } else {
            throw new IllegalArgumentException("unknown resourceType: " +resourceType);
        }
    }
    protected String getRemoteContextForType(String resourceType) {

        if (resourceType.equals("image")) {
            return envConfig.getLimelightImageContext();
        } else if (resourceType.equals("flash")) {
            return envConfig.getLimelightFlashContext();
        } else if (resourceType.equals("video")) {
            return envConfig.getLimelightVideoContext();
        } else {
            throw new IllegalArgumentException("unknown resourceType: " +resourceType);
        }
    }
}
