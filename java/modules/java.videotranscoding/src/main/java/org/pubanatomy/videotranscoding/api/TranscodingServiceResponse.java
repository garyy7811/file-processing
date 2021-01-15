package org.pubanatomy.videotranscoding.api;

import lombok.Data;

/**
 * Created by greg on 2/17/17.
 */
@Data
public class TranscodingServiceResponse {


    public static final Integer STATUS_SUCCESS = 0;
    public static final Integer STATUS_ERROR = 1;

    private int status;
    private String errorMessage;


    private String responsePayloadClass;
    private String responsePayloadJson;

    public boolean isSuccess() {
        return status == STATUS_SUCCESS;
    }

}
