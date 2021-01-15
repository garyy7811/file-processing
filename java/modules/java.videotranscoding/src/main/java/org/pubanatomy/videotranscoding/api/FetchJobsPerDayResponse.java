package org.pubanatomy.videotranscoding.api;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class FetchJobsPerDayResponse{

    private List<DateJobsPair> items;
    private Long               totalResults;

    @Data
    public static class DateJobsPair implements Serializable{
        private Date date;
        private long jobsCount = 0L;

        private Integer pendingJobs   = 0;
        private Integer completedJobs = 0;
        private Integer failedJobs    = 0;

        private Map<String, Long> statusToCount = new HashMap<>();
    }
}
