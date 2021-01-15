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
public class SlideStats {

    private final LocalDateTime startTime = LocalDateTime.now();

    @Setter
    private Long slideStartId;
    @Setter
    private Long slideEndId = 0L;
    @Setter
    private Long slideBatchSize;
    @Setter
    private Long slideThreads;


    private LocalDateTime lastSlideProccessedTime = LocalDateTime.MIN;
    private Long slideBatchCount = 0L;
    private Long slidesDurationTotalMillis = 0L;
    private Long slidesMaxProcessedId = 0L;
    private Long slidesProcessedCount = 0L;
    private Long slideThumbsProcessedCount = 0L;
    private Long slideThumbsMissingCount = 0L;
    private Long slideThumbsSkippedCount = 0L;


    @Synchronized
    public void slideBatchProcessed(Long batchSize, Long maxSlideId, Long batchDuration) {

        lastSlideProccessedTime = LocalDateTime.now();
        slideBatchCount++;
        slidesProcessedCount+=batchSize;
        slidesDurationTotalMillis +=batchDuration;
        slidesMaxProcessedId = Math.max(slidesMaxProcessedId, maxSlideId);
    }

    @Synchronized
    public void incrementSlideThumbsProcessedCount() {
        slideThumbsProcessedCount++;
    }

    @Synchronized
    public void incrementSlideThumbsMissingCount() {
        slideThumbsMissingCount++;
    }

    @Synchronized
    public void incrementSlideThumbsSkippedCount() {
        slideThumbsSkippedCount++;
    }


    public SlideMigrationReport generateReport() {

        SlideMigrationReport report = new SlideMigrationReport();

        report.setSlideStartId(slideStartId);
        report.setSlideEndId(slideEndId);
        report.setSlideBatchSize(slideBatchSize);
        report.setSlideThreads(slideThreads);

        report.setStartTime(startTime);
        report.setLastSlideProccessedTime(lastSlideProccessedTime);
        report.setSlideBatchCount(slideBatchCount);
        report.setSlidesMaxProcessedId(slidesMaxProcessedId);
        report.setSlidesProcessedCount(slidesProcessedCount);
        report.setSlideThumbsUploadedCount(slideThumbsProcessedCount);
        report.setSlideThumbsMissingCount(slideThumbsMissingCount);
        report.setSlideThumbsSkippedCount(slideThumbsSkippedCount);


        long estimatedSlidesTotal = slideEndId - slideStartId;
        report.setEstimatedSlidesTotal(estimatedSlidesTotal);
        long estimatedBatchesTotal = estimatedSlidesTotal / slideBatchSize;
        report.setEstimatedSlideBatchesRemaining(estimatedBatchesTotal);

        if (slideBatchCount > 0 && slidesProcessedCount > 0) {

            long estimatedSlidesRemaining = estimatedSlidesTotal - slidesProcessedCount;
            long estimatedBatchesRemaining = estimatedSlidesRemaining / slideBatchSize;

            long millisPerSlideBatch = slidesDurationTotalMillis / slideBatchCount;
            long millisPerSlide = slidesDurationTotalMillis / slidesProcessedCount;

            long estimatedMillisRemaining = 0;
            if (estimatedSlidesRemaining > slideBatchCount*slideThreads) {
                estimatedMillisRemaining = (estimatedBatchesRemaining * millisPerSlideBatch) / slideThreads;
            } else {
                estimatedMillisRemaining = estimatedSlidesRemaining * millisPerSlide;
            }

            long estimatedHoursRemaining = TimeUnit.MILLISECONDS.toHours(estimatedMillisRemaining);
            long estimatedMinutesRemaining = TimeUnit.MILLISECONDS.toMinutes(estimatedMillisRemaining) % 60;
            long estimatedSecondsRemaining = TimeUnit.MILLISECONDS.toSeconds(estimatedMillisRemaining) % 60;

            report.setEstimatedSlidesRemaining(estimatedSlidesRemaining);
            report.setEstimatedSlideBatchesRemaining(estimatedBatchesRemaining);
            report.setMillisPerSlideBatch(millisPerSlideBatch);
            report.setMillisPerSlide(millisPerSlide);
            report.setSlidesEstimatedMillisRemaining(estimatedMillisRemaining);
            report.setSlidesEstimatedHoursRemaining(estimatedHoursRemaining);
            report.setSlidesEstimatedMinutesRemaining(estimatedMinutesRemaining);
            report.setSlidesEstimatedSecondsRemaining(estimatedSecondsRemaining);



            long actualProcessingSeconds =
                    lastSlideProccessedTime.toEpochSecond(ZoneOffset.UTC) -
                            startTime.toEpochSecond(ZoneOffset.UTC);

            String actualProcessingTime = String.format("%02d:%02d:%02d",
                    TimeUnit.SECONDS.toHours(actualProcessingSeconds),
                    TimeUnit.SECONDS.toMinutes(actualProcessingSeconds) % 60,
                    TimeUnit.SECONDS.toSeconds(actualProcessingSeconds) % 60
            );



            long avgMillisPerSlide = (actualProcessingSeconds*1000) / slidesProcessedCount;
            long avgMillisperslideBatch = (actualProcessingSeconds*1000) / slideBatchCount;

            double resourcePercentComplete = ((double)slidesProcessedCount / (double)estimatedSlidesTotal);

            long percentBasedSecondsRemaining = (long)((double)actualProcessingSeconds / resourcePercentComplete) - actualProcessingSeconds;
            String percentBasedTimeRemaining = String.format("%02d:%02d:%02d",
                    TimeUnit.SECONDS.toHours(percentBasedSecondsRemaining),
                    TimeUnit.SECONDS.toMinutes(percentBasedSecondsRemaining) % 60,
                    TimeUnit.SECONDS.toSeconds(percentBasedSecondsRemaining) % 60
            );


            report.setSlideProcessedTimeSeconds(actualProcessingSeconds);
            report.setSlideProcessedTime(actualProcessingTime);
            report.setAvgMillisPerSlide(avgMillisPerSlide);
            report.setAvgMillisPerSlideBatch(avgMillisperslideBatch);
            report.setSlidesPercentComplete(resourcePercentComplete);
            report.setSlidesEstimatedTimeRemaining(percentBasedTimeRemaining);

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


