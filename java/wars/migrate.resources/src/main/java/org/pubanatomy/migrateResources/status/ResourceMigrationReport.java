package org.pubanatomy.migrateResources.status;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by greg on 10/7/16.
 */
@Data
public class ResourceMigrationReport {

    private LocalDateTime startTime;
    private LocalDateTime lastResourceProccessedTime;

    private Long resourceStartId;
    private Long resourceEndId;
    private Long resourceBatchSize;
    private Long resourceThreads;

    private Long resourceBatchCount;
    private Long resourcesMaxProcessedId;
    private Long resourcesProcessedCount;
    private Long resourceFileNamesAdjusted;
    private Long resourceFilesUploadedCount;
    private Long resourceThumbsUploadedCount;
    private Long resourcePosterFramesUploadedCount;
    private Long resourceMultiBitrateStreamsUploadedCount;

    private Long resourceFilesSkippedCount;
    private Long resourceThumbsSkippedCount;
    private Long resourcePosterFramesSkippedCount;
    private Long resourceStreamsSkippedCount;

    private long millisPerResourceBatch;
    private long millisPerResource;

    private long estimatedResourcesTotal;
    private long estimatedResourceBatchesTotal;

    private long estimatedResourcesRemaining;
    private long estimatedResourceBatchesRemaining;

    private long resourceEstimatedMillisRemaining;
    private long resourceEstimatedHoursRemaining;
    private long resourceEstimatedMinutesRemaining;
    private long resourceEstimatedSecondsRemaining;


    private long resourceProcessedTimeSeconds;
    private String resourceProcessedTime;
    private long avgMillisPerResourceBatch;
    private long avgMillisPerResource;
    private String resourceEstimatedTimeRemaining;
    private double resourcePercentComplete;


    public long totalFilesUploaded() {

        return resourceFilesUploadedCount
                + resourceThumbsUploadedCount
                + resourcePosterFramesUploadedCount
                + resourceMultiBitrateStreamsUploadedCount;
    }

    public Map<String, String> toMap() {

        Map<String, String> reportMap = new LinkedHashMap<>();

        if (startTime != null) {
            reportMap.put("startTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startTime));
        } else {
            reportMap.put("startTime", "N/A");
        }

        if (lastResourceProccessedTime != null) {
            reportMap.put("lastResourceProccessedTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(lastResourceProccessedTime));
        } else {
            reportMap.put("lastResourceProccessedTime", "N/A");
        }

        reportMap.put("resourceStartId", String.valueOf(resourceStartId));
        reportMap.put("resourceEndId", String.valueOf(resourceEndId));
        reportMap.put("resourceBatchSize", String.valueOf(resourceBatchSize));
        reportMap.put("resourceThreads", String.valueOf(resourceThreads));
        reportMap.put("resourceBatchCount", String.valueOf(resourceBatchCount));
        reportMap.put("resourcesMaxProcessedId", String.valueOf(resourcesMaxProcessedId));
        reportMap.put("resourcesProcessedCount", String.valueOf(resourcesProcessedCount));
        reportMap.put("resourceFileNamesAdjusted", String.valueOf(resourceFileNamesAdjusted));
        reportMap.put("resourceFilesUploadedCount", String.valueOf(resourceFilesUploadedCount));
        reportMap.put("resourceThumbsUploadedCount", String.valueOf(resourceThumbsUploadedCount));
        reportMap.put("resourcePosterFramesUploadedCount", String.valueOf(resourcePosterFramesUploadedCount));
        reportMap.put("resourceMultiBitrateStreamsUploadedCount", String.valueOf(resourceMultiBitrateStreamsUploadedCount));
        reportMap.put("resourcesTotalFilesUploaded", String.valueOf(totalFilesUploaded()));

        reportMap.put("resourceFilesSkippedCount", String.valueOf(resourceFilesSkippedCount));
        reportMap.put("resourceThumbsSkippedCount", String.valueOf(resourceThumbsSkippedCount));
        reportMap.put("resourcePosterFramesSkippedCount", String.valueOf(resourcePosterFramesSkippedCount));
        reportMap.put("resourceStreamsSkippedCount", String.valueOf(resourceStreamsSkippedCount));

        reportMap.put("millisPerResourceBatch", String.valueOf(millisPerResourceBatch));
        reportMap.put("millisPerResource", String.valueOf(millisPerResource));
        reportMap.put("estimatedResourcesTotal", String.valueOf(estimatedResourcesTotal));
        reportMap.put("estimatedResourceBatchesTotal", String.valueOf(estimatedResourceBatchesTotal));
        reportMap.put("estimatedResourcesRemaining", String.valueOf(estimatedResourcesRemaining));
        reportMap.put("estimatedResourceBatchesRemaining", String.valueOf(estimatedResourceBatchesRemaining));

        reportMap.put("resourceProcessedTimeSeconds", String.valueOf(resourceProcessedTimeSeconds));
        reportMap.put("resourceProcessedTime", String.valueOf(resourceProcessedTime));
        reportMap.put("avgMillisPerResourceBatch", String.valueOf(avgMillisPerResourceBatch));
        reportMap.put("avgMillisPerResource", String.valueOf(avgMillisPerResource));
        reportMap.put("resourceEstimatedTimeRemaining", String.valueOf(resourceEstimatedTimeRemaining));
        reportMap.put("resourcePercentComplete", String.valueOf(resourcePercentComplete));

        return reportMap;
    }
}
