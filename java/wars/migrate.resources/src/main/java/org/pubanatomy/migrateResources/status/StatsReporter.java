package org.pubanatomy.migrateResources.status;

import com.amazonaws.util.EC2MetadataUtils;
import org.pubanatomy.migrationutils.StatusManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 11/2/16.
 */
@Log4j2
public class StatsReporter {


    @Autowired
    StatusManager statusManager;

    @Autowired
    ResourceStats resourceStats;

    @Autowired
    SlideStats slideStats;

    @Autowired
    ErrorStats errorStats;

    private String instanceId;

    public void initializeInstanceId() {

        try {
            instanceId = EC2MetadataUtils.getInstanceId();
            if (instanceId == null) {
                instanceId = "un-known";
            }

            log.info("got instanceId: "+  instanceId);

        } catch (Exception e) {
            log.error("Error fetching instanceId");
            instanceId = "error";
        }

    }

    public synchronized Boolean reportCurrentStats() {

        Map<String, Long> aggregateMap = new HashMap<>();

        // add the resource stats
        ResourceMigrationReport resourceReport = resourceStats.generateReport();

        Long resourceFileNamesAdjusted = resourceReport.getResourceFileNamesAdjusted();
        aggregateMap.put("count.resourceFileNamesAdjusted", resourceFileNamesAdjusted);
        Long resourceFilesUploadedCount = resourceReport.getResourceFilesUploadedCount();
        aggregateMap.put("count.resourceFilesUploadedCount", resourceFilesUploadedCount);
        Long resourceThumbsUploadedCount = resourceReport.getResourceThumbsUploadedCount();
        aggregateMap.put("count.resourceThumbsUploadedCount", resourceThumbsUploadedCount);
        Long resourcePosterFramesUploadedCount = resourceReport.getResourcePosterFramesUploadedCount();
        aggregateMap.put("count.resourcePosterFramesUploadedCount", resourcePosterFramesUploadedCount);
        Long resourceMultiBitrateStreamsUploadedCount = resourceReport.getResourceMultiBitrateStreamsUploadedCount();
        aggregateMap.put("count.resourceMultiBitrateStreamsUploadedCount", resourceMultiBitrateStreamsUploadedCount);
        Long resourcesTotalFilesUploaded = resourceReport.totalFilesUploaded();
        aggregateMap.put("count.resourcesTotalFilesUploaded", resourcesTotalFilesUploaded);


        Long resourceFilesSkippedCount = resourceReport.getResourceFilesSkippedCount();
        aggregateMap.put("count.resourceFilesSkippedCount", resourceFilesSkippedCount);
        Long resourceThumbsSkippedCount = resourceReport.getResourceThumbsSkippedCount();
        aggregateMap.put("count.resourceThumbsSkippedCount", resourceThumbsSkippedCount);
        Long resourcePosterFramesSkippedCount = resourceReport.getResourcePosterFramesSkippedCount();
        aggregateMap.put("count.resourcePosterFramesSkippedCount", resourcePosterFramesSkippedCount);
        Long resourceMultiBitrateStreamsSkippedCount = resourceReport.getResourceStreamsSkippedCount();
        aggregateMap.put("count.resourceMultiBitrateStreamsSkippedCount", resourceMultiBitrateStreamsSkippedCount);

        // add the slide stats
        SlideMigrationReport slideReport = slideStats.generateReport();
        Long slideThumbsUploadedCount = slideReport.getSlideThumbsUploadedCount();
        aggregateMap.put("count.slideThumbsUploadedCount", slideThumbsUploadedCount);
        Long slidesProcessedCount = slideReport.getSlidesProcessedCount();
        aggregateMap.put("count.slidesProcessedCount", slidesProcessedCount);
        Long slideThumbsMissingCount = slideReport.getSlideThumbsMissingCount();
        aggregateMap.put("count.slideThumbsMissingCount", slideThumbsMissingCount);

        Long slideThumbsSkippedCount = slideReport.getSlideThumbsSkippedCount();
        aggregateMap.put("count.slideThumbsSkippedCount", slideThumbsSkippedCount);

        // add total uploads
        Long totalFilesUploaded = resourcesTotalFilesUploaded + slideThumbsUploadedCount;
        aggregateMap.put("count.totalFilesUploaded", totalFilesUploaded);

        // add all of the errors
        Map<String, Long> errorMap = errorStats.getErrorCounts();
        errorMap.entrySet().stream().forEach( entry -> {
            aggregateMap.put("error."+entry.getKey(), entry.getValue());
        });

        if (instanceId == null) {
            initializeInstanceId();
        }

        log.info("invoking statusManager.reportWorkerStats");
        Boolean result = statusManager.reportWorkerStats(instanceId,  aggregateMap);
        log.info("statusManager.reportWorkerStats returned: {}", result);
        return  result;
    }
}
