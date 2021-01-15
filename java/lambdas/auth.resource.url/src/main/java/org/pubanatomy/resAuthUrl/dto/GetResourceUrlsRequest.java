package org.pubanatomy.resAuthUrl.dto;

import lombok.Data;

/**
 * Created by greg on 6/13/16.
 */
@Data
public class GetResourceUrlsRequest {

    /**
     * resourceContentId maps to SlideResourceContent.id - this is really the only primary key necessary
     */
    private Integer resourceContentId;
    /**
     * resourceContentVersion maps to SlideResourceContent.version, but this could be removed from the API if desired
     */
    private Integer resourceContentVersion;

    /**
     * resourceType maps to SlideResourceContent.slideResource.type - it may not be necessary depending on server-side
     * implementation, but it might be useful to make the server code simpler, since the client will always know the
     * type of resources when requesting the URL.
     */
    private String resourceType;

    /**
     * flag to indicate whether to return the "mediaDownloadUrl", which is the HTTP/HTTPS url used for a (non-streaming)
     * download of the primary media asset file over HTTP/S.
     *
     * default value is true.
     */
    private Boolean includeMediaDownloadUrl;

    /**
     * flag to indicate whether to return the streamingUrls for a 'video' type resource.
     *
     * default value is true.
     */
    private Boolean includeStreamingUrls;


    /**
     * flag to indicate whether to return the defaultPosterframeUrl and firstFramePosterframeUrl for
     * a 'video' type resource.
     *
     * default value is true.
     */
    private Boolean includePosterFrameUrls;

    /**
     * flag to indicate whether to return the thumbnailUrl.
     *
     * default value is false
     */
    private Boolean includeThumbnailUrls;
}
