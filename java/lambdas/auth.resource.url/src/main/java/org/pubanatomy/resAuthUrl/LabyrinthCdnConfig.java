package org.pubanatomy.resAuthUrl;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/8/2016
 * Time: 3:08 PM
 */
@Data
public class LabyrinthCdnConfig{

    private String thumbnailContext;
    private String posterFrameContext;
    private String flashContext;
    private String imageContext;
    private String videoContext;
    private String envDirectory;
    private String rtmpURL;
    private String cdnHttpUrl;
    private String baseDirectory;
    private String flashApplicationName;
    private String securePath;
    private String unsecurePath;
    private String multibitratePath;
    private String secret;
    private Integer cdnRequestTTL;

    private String dynamoLoginVerification;

    private String flashMediaServerConnectionUrl;


}
