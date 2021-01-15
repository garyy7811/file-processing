package org.pubanatomy.videotranscoding.api;

import lombok.Data;

/**
 * Created by greg on 2/17/17.
 */
@Data
public class TranscodingServiceRequest {

    private String requestPayloadClass;
    private String requestPayloadJson;

}
