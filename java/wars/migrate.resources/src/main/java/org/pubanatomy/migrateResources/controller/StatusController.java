package org.pubanatomy.migrateResources.controller;

import com.amazonaws.util.EC2MetadataUtils;
import com.customshow.migrateResources.status.*;
import org.pubanatomy.migrateResources.status.*;
import org.pubanatomy.migrationutils.StatusManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by greg on 10/7/16.
 */
@Controller
@Log4j2
public class StatusController {

    @Autowired
    private ResourceStats resourceStats;

    @Autowired
    private SlideStats slideStats;

    @Autowired
    private ErrorStats errorStats;


    @Autowired
    private SourcePollingChannelAdapter resourceInputAdapter;

    @Autowired
    private SourcePollingChannelAdapter updatedSlideThumbnailInputAdapter;

    @Autowired
    private SourcePollingChannelAdapter slideThumbnailInputAdapter;

    @RequestMapping("/")
    public ModelAndView getIndexPage() {
        return doGetStatus("");
    }

    @RequestMapping("/status")
    public ModelAndView getStatus() {
        return doGetStatus("");
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

    @RequestMapping("control")
    public ModelAndView getControl(@RequestParam("adapterName") String adapterName, @RequestParam("newState") String newState) {

        String statusMsg = "";

        if (adapterName.equals("resources")) {
            if (newState.equals("start")) {
                if (resourceInputAdapter.isRunning()) {
                    statusMsg = "Requested resourceInputAdapter.start, but already running!";
                    log.warn(statusMsg);
                } else {
                    log.info("Starting resourceInputAdapter");
                    resourceInputAdapter.start();
                    log.info("resourceInputAdapter started");

                    statusMsg = "Started resourceInputAdapter";
                }
            } else if (newState.equals("stop")) {
                if (!resourceInputAdapter.isRunning()) {
                    statusMsg = "Requested resourceInputAdapter.stop, but not running!";
                    log.warn(statusMsg);
                } else {
                    log.info("Stopping resourceInputAdapter");
                    resourceInputAdapter.stop();
                    log.info("resourceInputAdapter stopped");

                    statusMsg = "stopped resourceInputAdapter";
                }
            } else {
                statusMsg = "Unknown newState: " + newState;
                log.warn(statusMsg);
            }
        } else if (adapterName.equals("slides")) {

            if (newState.equals("start")) {
                if (slideThumbnailInputAdapter.isRunning()) {
                    statusMsg = "Requested slideThumbnailInputAdapter.start, but already running!";
                    log.warn(statusMsg);
                } else {
                    log.info("Starting slideThumbnailInputAdapter");
                    slideThumbnailInputAdapter.start();
                    log.info("slideThumbnailInputAdapter started");

                    statusMsg = "Started slideThumbnailInputAdapter";
                }
            } else if (newState.equals("stop")) {
                if (!slideThumbnailInputAdapter.isRunning()) {
                    statusMsg = "Requested slideThumbnailInputAdapter.stop, but not running!";
                    log.warn(statusMsg);
                } else {
                    log.info("Stopping slideThumbnailInputAdapter");
                    slideThumbnailInputAdapter.stop();
                    log.info("slideThumbnailInputAdapter stopped");

                    statusMsg = "stopped slideThumbnailInputAdapter";
                }
            } else {
                statusMsg = "Unknown newState: " + newState;
                log.warn(statusMsg);
            }

        } else if (adapterName.equals("updatedSlides")) {

            if (newState.equals("start")) {
                if (updatedSlideThumbnailInputAdapter.isRunning()) {
                    statusMsg = "Requested updatedSlideThumbnailInputAdapter.start, but already running!";
                    log.warn(statusMsg);
                } else {
                    log.info("Starting updatedSlideThumbnailInputAdapter");
                    updatedSlideThumbnailInputAdapter.start();
                    log.info("updatedSlideThumbnailInputAdapter started");

                    statusMsg = "Started updatedSlideThumbnailInputAdapter";
                }
            } else if (newState.equals("stop")) {
                if (!updatedSlideThumbnailInputAdapter.isRunning()) {
                    statusMsg = "Requested updatedSlideThumbnailInputAdapter.stop, but not running!";
                    log.warn(statusMsg);
                } else {
                    log.info("Stopping updatedSlideThumbnailInputAdapter");
                    updatedSlideThumbnailInputAdapter.stop();
                    log.info("updatedSlideThumbnailInputAdapter stopped");

                    statusMsg = "stopped updatedSlideThumbnailInputAdapter";
                }
            } else {
                statusMsg = "Unknown newState: " + newState;
                log.warn(statusMsg);
            }

        } else {
            statusMsg = "Unknown adapterName: " + adapterName;
            log.warn(statusMsg);
        }

        return doGetStatus(statusMsg);


    }



    @Autowired
    StatusManager statusManager;

    protected Long counterCounter = 0L;

    @RequestMapping("statusManager")
    protected ModelAndView testStatusManager(String statusMessage) {

        String msg = "pending";
        String instanceId = "n/a";


        try {
            instanceId = EC2MetadataUtils.getInstanceId();
        } catch (Exception e) {
            log.error("Error getting instanceId", e);
        }

        counterCounter++;

        log.info("sending some stats to the statusManager for hostName: ");
        Map<String, Long> sampleStats = new HashMap<>();
        sampleStats.put("foo.one",   100L + (counterCounter));
        sampleStats.put("foo.two",   1000L + (counterCounter*20));
        sampleStats.put("foo.three", 10000L + (counterCounter*30));

        try {
            statusManager.reportWorkerStats(instanceId, sampleStats);
            msg = "successfully sent sampleStats";
        } catch (Exception e) {
            log.error("Error invoking reportWorkerStats", e);
            msg = e.getMessage();
        }

        return doGetStatus(msg);
    }

    protected ModelAndView doGetStatus(String statusMessage) {

        ResourceMigrationReport resourceMigrationReport = resourceStats.generateReport();
        Map<String, String> resourceMigrationReportMap = resourceMigrationReport.toMap();

        SlideMigrationReport slideMigrationReport = slideStats.generateReport();
        Map<String, String> slideMigrationReportMap = slideMigrationReport.toMap();

        Map<String, Long> errorMap = errorStats.getErrorCounts();

        long totalFilesUploaded = resourceMigrationReport.totalFilesUploaded() + slideMigrationReport.getSlideThumbsUploadedCount();
        String statusSummary = totalFilesUploaded + " files uploaded as of " + DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());

        ModelAndView mav = new ModelAndView("status");
        mav.addObject("statusMessage", statusMessage);
        mav.addObject("statusSummary", statusSummary);
        mav.addObject("resourceInputAdapterStatus", resourceInputAdapter.isRunning() ? "Running" : "Stopped");
        mav.addObject("slideThumbnailInputAdapterStatus", slideThumbnailInputAdapter.isRunning() ? "Running" : "Stopped");
        mav.addObject("updatedSlideThumbnailInputAdapterStatus", updatedSlideThumbnailInputAdapter.isRunning() ? "Running" : "Stopped");
        mav.addObject("resourceMigrationReportMap", resourceMigrationReportMap);
        mav.addObject("slideMigrationReportMap", slideMigrationReportMap);
        mav.addObject("errorMap", errorMap);

        return mav;
    }


}
