package org.pubanatomy.resAuthUrl.dto;

import lombok.Data;

/**
 * Created by greg on 6/13/16.
 */
@Data
public class GetResourceUrlsResponse {


    /**
     * same resourceContentId value as provided by request
     */
    private Integer resourceContentId;

    /**
     * same resourceContentVersion value as provided by request
     */
    private Integer resourceContentVersion;

    /**
     * same resourceType value as provided by request
     */
    private String resourceType;

    /**
     * This could be a unix-style timestamp indicating when any secure URLs in this response shall expire.
     *
     * We will need to consider time-zones and the fact that the client may have a completely inaccurate clock.
     *
     * We could instead send back the ttl as a duration in milliseconds, and ignore the effect of network latency...
     */
    private Long expirationTimestamp;

    /**
     * For LimeLight resources, this is the MediaVault URL for the primary content file
     *
     * For Cloudfront resources, this is the Cloudfront URL for the primary content file
     */
    private String cdnMediaDownloadUrl;

    /**
     * For resources that have not been migrated to S3, this is the application server URL for the primary content file
     *
     * For resources that have been migrated to S3, this is the pre-signed S3 URL for the primary content file
     */
    private String fallbackMediaDownloadUrl;

    /**
     * For video resources, this is an array of FlashMediaServerUrlItems.
     */
    private FlashMediaServerUrlItem[] streamingUrls;

    /**
     * For LimeLight video resources, this is the unsigned LimeLight (not MediaVault!) URL for the defaultPosterFrame
     *
     * For Cloudfront video resources, this is the Cloudfront URL for the defaultPosterFrame
     */
    private String cdnDefaultPosterframeUrl;

    /**
     * For video resources that have not been migrated to S3, this is the application server URL for the defaultPosterFrame
     *
     * For video resources that have been migrated to S3, this is the pre-signed S3 URL for the defaultPosterFrame
     */
    private String fallbackDefaultPosterframeUrl;

    /**
     * For LimeLight video resources, this is the unsigned LimeLight (not MediaVault!) URL for the firstFramePosterFrame
     *
     * For Cloudfront video resources, this is the unsigned Cloudfront URL for the firstFramePosterFrame
     */
    private String cdnFirstFramePosterframeUrl;

    /**
     * For video resources that have not been migrated to S3, this is the application server URL for the firstFramePosterFrame
     *
     * For video resources that have been migrated to S3, this is the pre-signed S3 URL for the firstFramePosterFrame
     */
    private String fallbackFirstFramePosterframeUrl;

    /**
     * For LimeLight resources, this is the unsigned LimeLight (not MediaVault!) URL for the resource thumbnail
     *
     * For Cloudfront resources, this is the unsigned Cloudfront URL for the resource thumbnail
     */
    private String cdnThumbnailUrl;

    /**
     * For resources that have not been migrated to S3, this is the application server URL for the resource thumbnail
     *
     * For resources that have been migrated to S3, this is the pre-signed S3 URL for the resource thumbnail
     */
    private String fallbackThumbnailUrl;



}
