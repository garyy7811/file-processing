package org.pubanatomy.migrateResources.status;

import lombok.Setter;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by greg on 5/5/16.
 */
@Log4j2
public class ResourceStats {

    private final LocalDateTime startTime = LocalDateTime.now();

    @Setter
    private Long resourceStartId;
    @Setter
    private Long resourceEndId = 0L;
    @Setter
    private Long resourceBatchSize;
    @Setter
    private Long resourceThreads;


    private LocalDateTime lastResourceProccessedTime = LocalDateTime.MIN;
    private Long resourceBatchCount = 0L;
    private Long resourcesDurationTotalMillis = 0L;
    private Long resourcesMaxProcessedId = 0L;

    private Long resourcesProcessedCount = 0L;
    private Long resourceFileNamesAdjusted = 0L;

    private Long resourceFilesUploadedCount = 0L;
    private Long resourceThumbsUploadedCount = 0L;
    private Long resourcePosterFramesUploadedCount = 0L;
    private Long resourceMultiBitrateStreamsUploadedCount = 0L;

    private Long resourceFilesSkippedCount = 0L;
    private Long resourceThumbsSkippedCount = 0L;
    private Long resourcePosterFramesSkippedCount = 0L;
    private Long resourceMultiBitrateStreamsSkippedCount = 0L;


    @Synchronized
    public void resourceBatchProcessed(Long batchSize, Long maxResourceId, Long batchDuration) {

        lastResourceProccessedTime = LocalDateTime.now();
        resourceBatchCount++;
        resourcesProcessedCount+=batchSize;
        resourcesDurationTotalMillis +=batchDuration;
        resourcesMaxProcessedId = Math.max(resourcesMaxProcessedId, maxResourceId);
    }

    @Synchronized
    public void incrementResourceFileNamesAdjusted() {
        resourceFileNamesAdjusted++;
    }

    @Synchronized
    public void incrementResourceFilesUploadedCount() {
        resourceFilesUploadedCount++;
    }

    @Synchronized
    public void incrementResourceThumbsUploadedCount() {
        resourceThumbsUploadedCount++;
    }

    @Synchronized
    public void incrementResourcePosterFramesUploadedCount() {
        resourcePosterFramesUploadedCount++;
    }

    @Synchronized
    public void incrementResourceMultiBitrateStreamsUploadedCount() {
        resourceMultiBitrateStreamsUploadedCount++;
    }



    @Synchronized
    public void incrementResourceFilesSkippedCount() {
        resourceFilesSkippedCount++;
    }

    @Synchronized
    public void incrementResourceThumbsSkippedCount() {
        resourceThumbsSkippedCount++;
    }

    @Synchronized
    public void incrementResourcePosterFramesSkippedCount() {
        resourcePosterFramesSkippedCount++;
    }

    @Synchronized
    public void incrementResourceMultiBitrateStreamsSkippedCount() {
        resourceMultiBitrateStreamsSkippedCount++;
    }



    public ResourceMigrationReport generateReport() {

        ResourceMigrationReport report = new ResourceMigrationReport();

        report.setResourceStartId(resourceStartId);
        report.setResourceEndId(resourceEndId);
        report.setResourceBatchSize(resourceBatchSize);
        report.setResourceThreads(resourceThreads);

        report.setStartTime(startTime);
        report.setLastResourceProccessedTime(lastResourceProccessedTime);
        report.setResourceBatchCount(resourceBatchCount);
        report.setResourcesMaxProcessedId(resourcesMaxProcessedId);
        report.setResourcesProcessedCount(resourcesProcessedCount);

        report.setResourceFileNamesAdjusted(resourceFileNamesAdjusted);
        report.setResourceFilesUploadedCount(resourceFilesUploadedCount);
        report.setResourceThumbsUploadedCount(resourceThumbsUploadedCount);
        report.setResourcePosterFramesUploadedCount(resourcePosterFramesUploadedCount);
        report.setResourceMultiBitrateStreamsUploadedCount(resourceMultiBitrateStreamsUploadedCount);

        report.setResourceFilesSkippedCount(resourceFilesSkippedCount);
        report.setResourceThumbsSkippedCount(resourceThumbsSkippedCount);
        report.setResourcePosterFramesSkippedCount(resourcePosterFramesSkippedCount);
        report.setResourceStreamsSkippedCount(resourceMultiBitrateStreamsSkippedCount);

        long estimatedResourcesTotal = resourceEndId - resourceStartId;
        report.setEstimatedResourcesTotal(estimatedResourcesTotal);
        long estimatedBatchesTotal = estimatedResourcesTotal / resourceBatchSize;
        report.setEstimatedResourceBatchesTotal(estimatedBatchesTotal);

        if (resourceBatchCount > 0 && resourcesProcessedCount > 0) {

            long estimatedResourcesRemaining = estimatedResourcesTotal - resourcesProcessedCount;
            long estimatedBatchesRemaining = estimatedResourcesRemaining / resourceBatchSize;

            long millisPerResourceBatch = resourcesDurationTotalMillis / resourceBatchCount;
            long millisPerResource = resourcesDurationTotalMillis / resourcesProcessedCount;

            long estimatedMillisRemaining = 0;
            if (estimatedResourcesRemaining > resourceBatchCount*resourceThreads) {
                estimatedMillisRemaining = (estimatedBatchesRemaining * millisPerResourceBatch);
            } else {
                estimatedMillisRemaining = estimatedResourcesRemaining * millisPerResource;
            }

            long estimatedHoursRemaining = TimeUnit.MILLISECONDS.toHours(estimatedMillisRemaining);
            long estimatedMinutesRemaining = TimeUnit.MILLISECONDS.toMinutes(estimatedMillisRemaining) % 60;
            long estimatedSecondsRemaining = TimeUnit.MILLISECONDS.toSeconds(estimatedMillisRemaining) % 60;

            report.setEstimatedResourcesRemaining(estimatedResourcesRemaining);
            report.setEstimatedResourceBatchesRemaining(estimatedBatchesRemaining);
            report.setMillisPerResourceBatch(millisPerResourceBatch);
            report.setMillisPerResource(millisPerResource);
            report.setResourceEstimatedMillisRemaining(estimatedMillisRemaining);
            report.setResourceEstimatedHoursRemaining(estimatedHoursRemaining);
            report.setResourceEstimatedMinutesRemaining(estimatedMinutesRemaining);
            report.setResourceEstimatedSecondsRemaining(estimatedSecondsRemaining);


            long actualProcessingSeconds =
                    lastResourceProccessedTime.toEpochSecond(ZoneOffset.UTC) -
                    startTime.toEpochSecond(ZoneOffset.UTC);

            String actualProcessingTime = String.format("%02d:%02d:%02d",
                    TimeUnit.SECONDS.toHours(actualProcessingSeconds),
                    TimeUnit.SECONDS.toMinutes(actualProcessingSeconds) % 60,
                    TimeUnit.SECONDS.toSeconds(actualProcessingSeconds) % 60
            );



            long avgMillisPerResource = (actualProcessingSeconds*1000) / resourcesProcessedCount;
            long avgMillisperResourceBatch = (actualProcessingSeconds*1000) / resourceBatchCount;

            double resourcePercentComplete = ((double)resourcesProcessedCount / (double)estimatedResourcesTotal);

            long percentBasedSecondsRemaining = (long)((double)actualProcessingSeconds / resourcePercentComplete) - actualProcessingSeconds;
            String percentBasedTimeRemaining = String.format("%02d:%02d:%02d",
                    TimeUnit.SECONDS.toHours(percentBasedSecondsRemaining),
                    TimeUnit.SECONDS.toMinutes(percentBasedSecondsRemaining) % 60,
                    TimeUnit.SECONDS.toSeconds(percentBasedSecondsRemaining) % 60
                    );


            report.setResourceProcessedTimeSeconds(actualProcessingSeconds);
            report.setResourceProcessedTime(actualProcessingTime);
            report.setAvgMillisPerResource(avgMillisPerResource);
            report.setAvgMillisPerResourceBatch(avgMillisperResourceBatch);
            report.setResourcePercentComplete(resourcePercentComplete);
            report.setResourceEstimatedTimeRemaining(percentBasedTimeRemaining);

        }



        return report;
    }

    public void dumpReport() {

        log.info("***** dumpReport start *****");

        Map<String, String> reportMap = generateReport().toMap();

        reportMap.entrySet().forEach( e -> {
            log.info("{}={}", e.getKey(), e.getValue());
        });

        log.info("***** dumpReport complete *****");

    }
}


