package org.pubanatomy.videotranscoding.api;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Data
public class FetchJobsPerDayRequest{

    private Date startDate;
    private Date endDate;

}
