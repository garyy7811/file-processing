package org.pubanatomy.batchpartition;


import lombok.Data;

/**
 * User: flashflexpro@gmail.com
 * Date: 4/23/2015
 * Time: 4:40 PM
 * <context:annotation-config/>
 * <tx:annotation-driven transaction-manager="transactionManager"/>
 */

public interface RangePartitionService{


    public Long[] allocateRange( Long rootTo );

    public Long[] allocateRange( Long rootTo, String comment );

    public Long[] allocateRange( Long rootTo, Long[] range, boolean rangeOnForce, String comment );

    public int updateRange( Long rangeFrom, Long rangeTo, String status, String comment );


    public int doneRange( Long rangeFrom, Long rangeTo, String comment );

    public int errorRange( Long rangeFrom, Long rangeTo, Long failedOn, String comment );

    public Long countByStatus( String status );

    public Long countWorking();
    public Long countDone();
    public Long countError();


    public Long sumForStatus(String status);
    public Long sumWorking();
    public Long sumDone();
    public Long sumError();

    public Long getOldestCreatedTime();
    public Long getLatestUpdateTime();

    public int cleanUnfinishedStatuses();

    public int cleanStatuses( String[] status );

    public void resetAll();

    public Config getConfig();


    @Data
    public static class Config{

        public static final String STATUS_WORKING = "working";
        public static final String STATUS_DONE    = "DONE";
        public static final String STATUS_ERROR   = "ERROR";




        private String tableName;
        private Long rootFrom = Long.MIN_VALUE;
        private Long rootTo   = Long.MAX_VALUE;
        private Long step     = 40L;

        private Integer maxWorkingInParallel = 0;

        private Long maxWorkingTimeInSec = 3600L;

    }
}
