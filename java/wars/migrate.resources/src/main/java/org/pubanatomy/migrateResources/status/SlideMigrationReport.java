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
public class SlideMigrationReport {

    private LocalDateTime startTime;
    private LocalDateTime lastSlideProccessedTime;

    private Long slideStartId;
    private Long slideEndId;
    private Long slideBatchSize;
    private Long slideThreads;

    private Long slideBatchCount;
    private Long slidesMaxProcessedId;
    private Long slidesProcessedCount;
    private Long slideThumbsUploadedCount;
    private Long slideThumbsMissingCount;
    private Long slideThumbsSkippedCount;

    private long millisPerSlideBatch;
    private long millisPerSlide;

    private long estimatedSlidesTotal;
    private long estimatedSlideBatchesTotal;

    private long estimatedSlidesRemaining;
    private long estimatedSlideBatchesRemaining;

    private long slidesEstimatedMillisRemaining;
    private long slidesEstimatedHoursRemaining;
    private long slidesEstimatedMinutesRemaining;
    private long slidesEstimatedSecondsRemaining;

    private long slideProcessedTimeSeconds;
    private String slideProcessedTime;
    private long avgMillisPerSlideBatch;
    private long avgMillisPerSlide;
    private String slidesEstimatedTimeRemaining;
    private double slidesPercentComplete;

    public Map<String, String> toMap() {

        Map<String, String> reportMap = new LinkedHashMap<>();

        if (startTime != null) {
            reportMap.put("startTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startTime));
        } else {
            reportMap.put("startTime", "N/A");
        }

        if (lastSlideProccessedTime != null) {
            reportMap.put("lastSlideProccessedTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(lastSlideProccessedTime));
        } else {
            reportMap.put("lastSlideProccessedTime", "N/A");
        }

        reportMap.put("slideStartId", String.valueOf(slideStartId));
        reportMap.put("slideEndId", String.valueOf(slideEndId));
        reportMap.put("slideBatchSize", String.valueOf(slideBatchSize));
        reportMap.put("slideThreads", String.valueOf(slideThreads));
        reportMap.put("slideBatchCount", String.valueOf(slideBatchCount));
        reportMap.put("slidesMaxProcessedId", String.valueOf(slidesMaxProcessedId));
        reportMap.put("slidesProcessedCount", String.valueOf(slidesProcessedCount));
        reportMap.put("slideThumbsProcessedCount", String.valueOf(slideThumbsUploadedCount));
        reportMap.put("slideThumbsMissingCount", String.valueOf(slideThumbsMissingCount));
        reportMap.put("slideThumbsSkippedCount", String.valueOf(slideThumbsSkippedCount));
        reportMap.put("millisPerSlideBatch", String.valueOf(millisPerSlideBatch));
        reportMap.put("millisPerSlide", String.valueOf(millisPerSlide));
        reportMap.put("estimatedSlidesTotal", String.valueOf(estimatedSlidesTotal));
        reportMap.put("estimatedSlideBatchesTotal", String.valueOf(estimatedSlideBatchesTotal));
        reportMap.put("estimatedSlidesRemaining", String.valueOf(estimatedSlidesRemaining));
        reportMap.put("estimatedSlideBatchesRemaining", String.valueOf(estimatedSlideBatchesRemaining));

        reportMap.put("slideProcessedTimeSeconds", String.valueOf(slideProcessedTimeSeconds));
        reportMap.put("slideProcessedTime", String.valueOf(slideProcessedTime));
        reportMap.put("avgMillisPerSlideBatch", String.valueOf(avgMillisPerSlideBatch));
        reportMap.put("avgMillisPerSlide", String.valueOf(avgMillisPerSlide));
        reportMap.put("slidesEstimatedTimeRemaining", String.valueOf(slidesEstimatedTimeRemaining));
        reportMap.put("slidesPercentComplete", String.valueOf(slidesPercentComplete));

        return reportMap;
    }
}
