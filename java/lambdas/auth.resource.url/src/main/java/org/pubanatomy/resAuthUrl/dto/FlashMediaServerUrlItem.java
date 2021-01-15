package org.pubanatomy.resAuthUrl.dto;

import lombok.Data;

/**
 * Created by greg on 6/13/16.
 */
@Data
public class FlashMediaServerUrlItem {

    /**
     * URL used to establish a connection to the FlashMediaServer used by the FlashMediaServerUrlItems included in
     * this response.
     */
    private String flashMediaServerConnectionUrl;

    /**
     * non-zero value indicating the bitrate in kbps, for the video stream referenced by the streamUrl
     */
    private Integer bitrate;

    /**
     * RTMP stream url to use when connected via the flashMediaServerConnectionUrl
     *
     * As an example, a file with server path "/s/video/12_3_4.mp4" would have a streamUrl of "mp4:s/test/video/12_3_4"
     */
    private String streamUrl;

}
