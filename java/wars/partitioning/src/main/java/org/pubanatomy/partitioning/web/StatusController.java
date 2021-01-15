package org.pubanatomy.partitioning.web;

import org.pubanatomy.batchpartition.RangePartitionService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by greg on 10/7/16.
 */
@Controller
@Log4j2
public class StatusController {




    @Autowired
    private RangePartitionService slideResourceContentPartitioning;

    @Autowired
    private RangePartitionService slideThumbnailPartitioning;

    @RequestMapping("/")
    public ModelAndView getDefaultPage() {
        return getConfig();
    }

    @RequestMapping("/config")
    public ModelAndView getConfig() {

        String statusMsg = "Config as of " + DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());


        Map<String, String> configMap = new HashMap<String, String>();

        try {
            Properties configProperties = new Properties();


            InputStream configInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");

            configProperties.load(configInputStream);

            configInputStream.close();;

            for (final String name: configProperties.stringPropertyNames())
                configMap.put(name, configProperties.getProperty(name));

        } catch (IOException e) {

            log.error("Error loading config file", e);
            statusMsg = e.getMessage();

        }


        ModelAndView mav = new ModelAndView("config");
        mav.addObject("statusMsg", statusMsg);
        mav.addObject("configMap", configMap);

        return mav;
    }

    @RequestMapping("/status")
    public ModelAndView getStatus() {

        String statusMessage = "Status as of " + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        return doGetStatus(statusMessage);
    }

    @RequestMapping("/reset")
    public ModelAndView doReset(@RequestParam("tableName") String tableName) {

        log.info("doReset with tableName={}", tableName);

        String statusMsg = "";

        if ("resource".equals(tableName)) {
            log.info("Resetting resource table!");
            slideResourceContentPartitioning.resetAll();;
            statusMsg = "Reset resource table";
        } else if ("thumbs".equals(tableName)) {
            log.info("Resetting slide thumbs table!");
            slideThumbnailPartitioning.resetAll();
            statusMsg = "Reset thumbs table";
        } else {
            statusMsg = "unknown input: '"+ tableName + "'";
        }

        return doGetStatus(statusMsg);
    }


    protected ModelAndView doGetStatus(String statusMessage) {

        Map<String, String> resourceReportMap = generateReportForPartitioner(slideResourceContentPartitioning);
        Map<String, String> slideThumbsReportMap = generateReportForPartitioner(slideThumbnailPartitioning);

        ModelAndView mav = new ModelAndView("status");
        mav.addObject("statusMessage", statusMessage);
        mav.addObject("resourceReportMap", resourceReportMap);
        mav.addObject("slideThumbsReportMap", slideThumbsReportMap);

        return mav;
    }

    protected Map<String, String> generateReportForPartitioner(RangePartitionService rps) {

        Map<String, String> reportMap = new LinkedHashMap<>();

        // gather the config values
        Long rootFrom = rps.getConfig().getRootFrom();
        reportMap.put("rootFrom", String.valueOf(rootFrom));

        Long rootTo = rps.getConfig().getRootTo();
        reportMap.put("rootTo", String.valueOf(rootTo));

        Long batchSize = rps.getConfig().getStep();
        reportMap.put("batchSize", String.valueOf(batchSize));

        // check if anything has been done yet
        Long countAll = rps.sumForStatus(null);

        if (countAll > 0) {

            Long workingBatches = rps.countWorking();
            Long doneBatches = rps.countDone();
            Long errorBatches = rps.countError();
            Long totalRecords = workingBatches+doneBatches+errorBatches;

            // totalRecords
            reportMap.put("totalRecords", String.valueOf(totalRecords));
            // workingBatches
            reportMap.put("workingBatches", String.valueOf(workingBatches));

            Long workingItems = rps.sumWorking();
            Long doneItems = rps.sumDone();
            Long errorItems = rps.sumError();
            Long totalProcessedItems = workingItems+doneItems+errorItems;

            // workingItems
            reportMap.put("workingItems", String.valueOf(workingItems));
            // doneItems
            reportMap.put("doneItems", String.valueOf(doneItems));
            // errorItems
            reportMap.put("errorItems", String.valueOf(errorItems));
            // totalProcessedItems
            reportMap.put("totalProcessedItems", String.valueOf(totalProcessedItems));


            // estimatedTotalItems
            Long estimatedTotalItems = rootTo - rootFrom;
            reportMap.put("estimatedTotalItems", String.valueOf(estimatedTotalItems));

            // estimatedRemainingItems
            Long estimatedRemainingItems = estimatedTotalItems - totalProcessedItems;
            reportMap.put("estimatedRemainingItems", String.valueOf(estimatedRemainingItems));

            // percentComplete
            Double percentComplete = (totalProcessedItems.doubleValue() / estimatedTotalItems.doubleValue());
            reportMap.put("percentComplete", String.format("%.4f",percentComplete));

            // firstBatchTime
            Long firstBatchTimeMillis = rps.getOldestCreatedTime();
            LocalDateTime firstBatchDateTime = Instant.ofEpochMilli(firstBatchTimeMillis).atZone(ZoneOffset.systemDefault()).toLocalDateTime();
            reportMap.put("firstBatchTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(firstBatchDateTime));

            // lastBatchTime
            Long lastBatchTimeMillis = rps.getLatestUpdateTime();
            LocalDateTime lastBatchDateTime = Instant.ofEpochMilli(lastBatchTimeMillis).atZone(ZoneOffset.systemDefault()).toLocalDateTime();
            reportMap.put("lastBatchTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(lastBatchDateTime));


            // totalDurationTime
            Long totalDurationTimeMillis = lastBatchTimeMillis - firstBatchTimeMillis;
            String totalDurationTime = String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(totalDurationTimeMillis),
                    TimeUnit.MILLISECONDS.toMinutes(totalDurationTimeMillis) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(totalDurationTimeMillis) % 60
            );

            reportMap.put("totalDurationTime", totalDurationTime);

            // estimatedRemainingTime
            Long estimatedRemainingTimeMillis = (long)(totalDurationTimeMillis.doubleValue() / percentComplete) - totalDurationTimeMillis;
            String estimatedRemainingTime = String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(estimatedRemainingTimeMillis),
                    TimeUnit.MILLISECONDS.toMinutes(estimatedRemainingTimeMillis) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(estimatedRemainingTimeMillis) % 60
            );

            reportMap.put("estimatedRemainingTime", estimatedRemainingTime);

            Long millisPerItem = totalDurationTimeMillis / totalProcessedItems;
            reportMap.put("millisPerItem", String.valueOf(millisPerItem));

        }

        return reportMap;
    }



}
