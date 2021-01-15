package org.pubanatomy.migrateResources;

import org.pubanatomy.batchpartition.RangePartitionService;
import org.pubanatomy.migrateResources.status.SlideStats;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by greg on 10/5/16.
 */
public class CopySlideThumbnailsProcessor extends CopyThumbnailsProcessor {


    @Autowired
    protected SlideStats slideStats;


    @Autowired
    private RangePartitionService slideThumbnailPartitioning;

    public Long[] requestNextRange() {

        Long maxSlideId = 0L;

        if (slideThumbnailEndId >= 0) {
            maxSlideId = slideThumbnailEndId;
            logger.info("requestNextRange using configured maxSlideId:{}", maxSlideId);
        } else {
            logger.info("requestNextRange loading max resource id");
            maxSlideId = loadMaxRecordId();
        }

        slideStats.setSlideEndId(maxSlideId);


        logger.info("making remote call to allocateRange({})", maxSlideId);
        Object allocateResult = null;

        try {
            allocateResult = slideThumbnailPartitioning.allocateRange(maxSlideId);
        } catch (Exception e) {
            // NOTE: this will halt further processing!
            handleFatalError("Unexpected error in allocateRange", e);
        }

        if (allocateResult instanceof Long[]) {

            Long[] nextRange = (Long[])allocateResult;
            if (nextRange.length == 0) {
                logger.warn("allocateRange returned empty list - returning null!");
            } else {
                return nextRange;
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
                int response = slideThumbnailPartitioning.doneRange( range[ 0 ], range[ 1 ], ".");
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
        return "slideThumbnail";
    }

    @Override
    public Long loadMaxRecordId(){

        Split split = SimonManager.getStopwatch("loadMaxRecordId."+getFileType()).start();

        logger.debug("invoking remote loadThumbnailMaxRecordId");

        Long maxRecordId = null;

        try {
            maxRecordId = queryNewVictoryMysql.loadThumbnailMaxRecordId(true);
        } catch (Exception e) {
            // NOTE: this will halt further processing!
            handleFatalError("Unexpected error in loadResourceMaxRecordId", e);
        }

        logger.info("remote loadThumbnailMaxRecordId returned: " + maxRecordId);

        split.stop();

        return maxRecordId;

//        String maxRecordSql = "SELECT MAX(id) FROM Slide";
//        Long maxRecordId = jdbcTemplate.queryForObject(maxRecordSql, Long.class ) + 1;
//        logger.info("loadMaxRecordId returning:  " + maxRecordId);
//        return maxRecordId;
    }

    @Override
    public Object[] selectItemRange( Long[] range ){

        Split split = SimonManager.getStopwatch("selectItemRange."+getFileType()).start();

        Assert.notNull(range, "range must not be null!");
        Assert.isTrue(range.length == 2, "range should be length 2, got: " + range.length);

        logger.info("invoking remote selectThumbnailItemRange("+range[0]+", "+range[1]+")");

        Object[] rangeAndResults = queryNewVictoryMysql.selectThumbnailItemRange(range);

        logger.debug("remote selectThumbnailItemRange returned: " + rangeAndResults);

        split.stop();

        return rangeAndResults;

//        String itemRangeSQL =
//                "SELECT T.id AS thumbnailId, T.version, T.fileName, T.fileSize, T.width, T.height, T.cdnEnabled, S.id AS slideId " +
//                "FROM Magnet.Thumbnail T " +
//                "   INNER JOIN Magnet.Slide S   ON T.id = S.thumbnail_id " +
//                "WHERE S.id >= ? AND S.id<? " +
//                "ORDER BY T.id ASC ";
//        final List<Map<String, Object>> rt = jdbcTemplate.queryForList( itemRangeSQL, range[ 0 ], range[ 1 ] );
//        logger.info("selectItemRange["+range[0]+", " + range[1]+"] returning " + rt.size() + " items");
//        return new Object[]{ range, rt };
    }

    @Override
    public void reportStats(long rangeSize, long rangeEnd, long durationMillis) {
        slideStats.slideBatchProcessed(rangeSize, rangeEnd, durationMillis);
        statsReporter.reportCurrentStats();
    }


    @Override
    protected void incrementThumbsMissingCount() {
        slideStats.incrementSlideThumbsMissingCount();
    }

    @Override
    protected void incrementThumbsSkippedCount() {
        slideStats.incrementSlideThumbsSkippedCount();
    }

    @Override
    protected void incrementThumbsProcessedCountCount() {
        slideStats.incrementSlideThumbsProcessedCount();
    }

    @Override
    public Map<String, Object> buildThumbnailMetadata(Map<String, Object> sqlMso) {

        Map<String, Object> metaMso = super.buildThumbnailMetadata(sqlMso);

        metaMso.put("slide_id", sqlMso.get("slideId"));
        metaMso.put("thumbnail_type", "slide");

        return metaMso;
    }

    @Override
    public void processItem(Map<String, Object> mso) {

        if (versionThreads > 1) {
            processItemMultiThreads(mso);
        } else {
            processItemSingleThread(mso);
        }
    }

    public void processItemSingleThread(Map<String, Object> mso) {

        Map<String, Object> metaMso = buildThumbnailMetadata(mso);

        // we never expect thumbnailId to be null
        Integer thumbnailId = Integer.parseInt(mso.get("thumbnailId").toString());

        // thumbnail version could possibly be null
        if (mso.get("version") == null) {
            reportError("slide-thumbnail-null-version", thumbnailId.toString(), "found null version for slide thumbnail");
            return;
        }

        // value not null, so assume it is valid number
        Integer latestThumbnailVersion = Integer.parseInt(mso.get("version").toString());


        // fileName could also be null
        if (mso.get("fileName") == null) {
            reportError("slide-thumbnail-null-fileName", thumbnailId.toString(), "found null fileName for slide thumbnail");
            return;
        }

        String thumbnailFileName = mso.get("fileName").toString();


        String fileExtension = thumbnailFileName.substring(thumbnailFileName.indexOf("."));

        // add file extension to metadata map
        metaMso.put("thumbnail_file_extension", fileExtension);

        logger.info("Processing thumbnailId={}, latestThumbnailVersion={} using a single thread", thumbnailId, latestThumbnailVersion);

        for(Integer tmpVersion = latestThumbnailVersion ; tmpVersion > 0; tmpVersion-- ){

            // copy tmpVersion to a final variable to be used in lambda
            final Integer versionToProcess = tmpVersion;

            // copy metadata to separate object to make it thread-safe
            final Map<String, Object> msoToProcess = new HashMap<>();
            msoToProcess.putAll(msoToProcess);

            try {
                processThumbnailVersion(thumbnailId, latestThumbnailVersion, versionToProcess, fileExtension, metaMso);
            } catch (Exception e) {
                logger.error("Unexpected error processing thumbnailId="+thumbnailId+", version="+versionToProcess, e);
                reportError("processThumbnailVersion", "thumb-"+thumbnailId+"-"+versionToProcess, e);
            }

        }

    }

    public void processItemMultiThreads(Map<String, Object> mso) {

        Map<String, Object> metaMso = buildThumbnailMetadata(mso);

        // we never expect thumbnailId to be null
        Integer thumbnailId = Integer.parseInt(mso.get("thumbnailId").toString());

        // thumbnail version could possibly be null
        if (mso.get("version") == null) {
            reportError("slide-thumbnail-null-version", thumbnailId.toString(), "found null version for slide thumbnail");
            return;
        }

        // value not null, so assume it is valid number
        Integer latestThumbnailVersion = Integer.parseInt(mso.get("version").toString());


        // fileName could also be null
        if (mso.get("fileName") == null) {
            reportError("slide-thumbnail-null-fileName", thumbnailId.toString(), "found null fileName for slide thumbnail");
            return;
        }
        String thumbnailFileName = mso.get("fileName").toString();


        // if the filename from the database contains a "/" character, then we know it is already in cs-cloud, so we can
        // skip those resources!
        if (thumbnailFileName.indexOf("/") >= 0) {
            logger.warn("found cs-cloud thumbnail - skipping!");
            slideStats.incrementSlideThumbsSkippedCount();
            return;
        }


        String fileExtension = thumbnailFileName.substring(thumbnailFileName.indexOf("."));

        // add file extension to metadata map
        metaMso.put("thumbnail_file_extension", fileExtension);

        logger.info("Processing thumbnailId={}, latestThumbnailVersion={} using {} threads", thumbnailId, latestThumbnailVersion, versionThreads);

        ExecutorService versionExecutor = Executors.newFixedThreadPool(versionThreads);

        for(Integer tmpVersion = latestThumbnailVersion ; tmpVersion > 0; tmpVersion-- ){

            // copy tmpVersion to a final variable to be used in lambda
            final Integer versionToProcess = tmpVersion;

            // copy metadata to separate object to make it thread-safe
            final Map<String, Object> msoToProcess = new HashMap<>();
            msoToProcess.putAll(msoToProcess);

            versionExecutor.submit(() -> {

                logger.debug("threaded invocation of processItem");
                try {
                    processThumbnailVersion(thumbnailId, latestThumbnailVersion, versionToProcess, fileExtension, metaMso);
                } catch (Exception e) {
                    logger.error("Unexpected error processing thumbnailId="+thumbnailId+", version="+versionToProcess, e);
                    reportError("processThumbnailVersion", "thumb-"+thumbnailId+"-"+versionToProcess, e);
                }
            });

        }

        logger.info("requested all items processed - waiting for completion");

        try {
            versionExecutor.shutdown();
            boolean shutdownComplete = false;
            int timeoutCount = 0;
            while (!shutdownComplete) {
                shutdownComplete = versionExecutor.awaitTermination(10, TimeUnit.MINUTES);
                if (!shutdownComplete) {
                    timeoutCount++;
                    reportError("version-executor-awaitTerminationTimeout-"+timeoutCount, "thumbnailId-"+thumbnailId, "timeoutCount="+timeoutCount);
                }
            }

            logger.info("executor terminated");
        } catch (InterruptedException ie) {
            reportError("version-executor-interrupted", "thumbnailId-"+thumbnailId, ie);
        }

    }




}
