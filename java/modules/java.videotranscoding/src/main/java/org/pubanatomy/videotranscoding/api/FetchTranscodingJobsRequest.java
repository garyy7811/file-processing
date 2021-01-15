package org.pubanatomy.videotranscoding.api;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg on 2/21/17.
 */
@Data
public class FetchTranscodingJobsRequest {

    public enum STATUS_FILTER_TYPE { STATUS_TYPE_ALL, STATUS_TYPE_PENDING, STATUS_TYPE_COMPLETE, STATUS_TYPE_ERROR };

    public static final String SORT_BY_createTime = "createTime";
    public static final String SORT_BY_lastUpdateTime = "lastUpdateTime";
    public static final String SORT_BY_mediaId = "mediaId";
    public static final String SORT_BY_status = "status";


    private Integer from;
    private Integer size;

    private String sortBy = SORT_BY_createTime;
    private Boolean descending;

    private List<Integer> clientIdFilter = new ArrayList<>();
    private Integer mediaIdFilter = null;
    private String originalFileNameMatch = null;
    private STATUS_FILTER_TYPE statusFilterType = STATUS_FILTER_TYPE.STATUS_TYPE_ALL;

}
