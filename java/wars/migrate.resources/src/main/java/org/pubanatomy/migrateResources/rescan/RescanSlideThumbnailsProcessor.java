package org.pubanatomy.migrateResources.rescan;

import org.pubanatomy.migrateResources.CopySlideThumbnailsProcessor;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by greg on 11/30/16.
 */
public class RescanSlideThumbnailsProcessor extends CopySlideThumbnailsProcessor {


    @Autowired
    private SourcePollingChannelAdapter updatedSlideThumbnailInputAdapter;

    @Value("${slideThumbnail.batchSize}")
    protected Long slideThumbnailMaxResults;

    @Value("${slideThumbnail.minimumModifiedDate}")
    protected String slideThumbnailMinModifiedDate;

    protected Long lastMaxSlideIdProcessed = 0L;

    @Override
    public Long[] requestNextRange() {

        // minSlideId starts at 0 or is set to last processed id
        Long minSlideId = lastMaxSlideIdProcessed;

        // max slideId is either configured or obtained from server
        Long maxSlideId = 0L;

        if (slideThumbnailEndId >= 0) {
            maxSlideId = slideThumbnailEndId;
            logger.info("requestNextRange using configured maxSlideId:{}", maxSlideId);
        } else {
            logger.info("requestNextRange loading max resource id");
            maxSlideId = loadMaxRecordId();
        }

        slideStats.setSlideEndId(maxSlideId);

        // just return these values
        logger.info("next range: [{},{}]", minSlideId, maxSlideId);

        return new Long[] {minSlideId, maxSlideId};
    }

    @Override
    public Object[] selectItemRange(Long[] range) {

        Split split = SimonManager.getStopwatch("selectItemRange."+getFileType()).start();

        Assert.notNull(range, "range must not be null!");
        Assert.isTrue(range.length == 2, "range should be length 2, got: " + range.length);

        logger.info("invoking remote selectUpdatedThumbnailItemRange({},{},{},{})",range[0],range[1],slideThumbnailMinModifiedDate ,slideThumbnailMaxResults);

        Object[] methodInput = new Object[] {
                range[0],
                range[1],
                slideThumbnailMinModifiedDate,
                slideThumbnailMaxResults
        };

        Object[] methodResult = null;

        try {
            methodResult = queryNewVictoryMysql.selectUpdatedThumbnailItemRange(methodInput);
        } catch (Exception e) {
            handleFatalError("Error invoking remote selectUpdatedThumbnailItemRange", e);
        }

        Object[] rangeAndResults = new Object[] {range, methodResult[0]};

        logger.debug("remote selectUpdatedThumbnailItemRange returned: " + rangeAndResults);

        split.stop();

        return rangeAndResults;
    }

    @Override
    public void markRangeComplete(Object rangeObj) {

        // in this implementation, we'll simply adjust our lastMaxSlideIdProcessed using the rangeEnd value
        Long[] range = (Long[])rangeObj;
        lastMaxSlideIdProcessed = range[1];
        logger.info("Set lastMaxSlideIdProcessed={}", lastMaxSlideIdProcessed);
    }

    @Override
    public Long[] processRange( Object[] rangeAndLstOfMso ){

        // only use single thread, and adjust the range[] struct to maintain
        // the rangeStart, but set rangeEnd to the highest value in the results

        long startMillis = System.currentTimeMillis();

        Long[] range = (Long[])rangeAndLstOfMso[0];
        Long rangeStart = range[0];

        List<Map<String, Object>> lstOfMso = ( List<Map<String, Object>> )rangeAndLstOfMso[ 1 ];

        // IF we are given an empty list, then this is a halt condition - we will simply report stats and
        // trigger a fatal error which will stop processor from running
        if (lstOfMso.isEmpty()) {

            logger.info("got empty results - halting processor!");
            Long emptyRangeEnd = range[1];
            Long emptyRangeSize = emptyRangeEnd - rangeStart;
            Long emptyDurationMillis = 100L;
            reportStats(emptyRangeSize, emptyRangeEnd, emptyDurationMillis);

            updatedSlideThumbnailInputAdapter.stop();

            throw new RuntimeException("No more updatedSlideThumbs to process");
        }

        List<Integer> processedSlideIds = new LinkedList<>();

        logger.info("processing {} items using single thread", lstOfMso.size());

        lstOfMso.stream().forEachOrdered( mso -> {
            processItem(mso);
            Integer slideId = Integer.parseInt(mso.get("slideId").toString());
            processedSlideIds.add(slideId);
        } );

        // locate the maximum slideId and convert to long
        Long rangeEnd = processedSlideIds.stream().max(Comparator.naturalOrder()).get().longValue();
        // set the rangeSize - which no longer represents number of slides processed, but rather just scope of slideId's covered
        Long rangeSize = rangeEnd - rangeStart;

        long durationMillis = System.currentTimeMillis() - startMillis;

        reportStats(rangeSize, rangeEnd, durationMillis);

        return ( Long[] )rangeAndLstOfMso[ 0 ];
    }

    @Override
    protected void processThumbnailVersion(Integer thumbnailId, Integer latestThumbnailVersion, Integer tmpVersion, String fileExtension, Map<String, Object> metaMso) {


        // in this implementation we first derive the target S3 object id and check if it already exists,
        // in which case we'll skip it...
        String tmpThumbFileName = thumbnailId + "_" + tmpVersion + fileExtension;
        String tmpS3ObjectId = getS3Path(envConfig.getLimelightThumbnailContext(), tmpThumbFileName);

        try {
            if (doesS3ObjectExist(tmpS3ObjectId)) {
                logger.info("found thumbnailVersion already exists for s3ObjectId={}", tmpS3ObjectId);
                slideStats.incrementSlideThumbsSkippedCount();
                return;
            }
        } catch (Exception e) {
            reportError("error-checking-thumbnail-s3-exists", tmpS3ObjectId, e);
            return;
        }

        // FIXME - faking processing for the moment!
        slideStats.incrementSlideThumbsProcessedCount();

        // ok - this version is not yet in S3, so process it now
        //super.processThumbnailVersion(thumbnailId, latestThumbnailVersion, tmpVersion, fileExtension, metaMso);
    }

    @Override
    protected void handleFatalError(String message, Throwable cause) {


        try {
            updatedSlideThumbnailInputAdapter.stop();
        } catch (Exception e) {
            logger.error("Failed to stop updatedSlideThumbnailInputAdapter", e);
        }

        super.handleFatalError(message, cause);
    }
}
