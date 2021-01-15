package org.pubanatomy.migrateResources;

import lombok.Data;
import org.apache.http.util.Asserts;

/**
 * Created by greg on 10/5/16.
 */
@Data
public class EnvironmentConfig {

    private String limelightThumbnailContext;
    private String limelightPosterFrameContext;
    private String limelightFlashContext;
    private String limelightImageContext;
    private String limelightVideoContext;
    private String limelightEnvDirectory;
    private String limelightUnsecurePath;
    private String limelightSecurePath;
    private String limelightMediaVaultSecret;
    private String limelightHttpUrlBase;
    private String limelightMultibitratePath;
    private String labyrinthHttpUrlBase;
    private String labyrinthWebCacheRoot;
    private String labyrinthImageCache;
    private String labyrinthFlashCache;
    private String labyrinthVideoCache;
    private String labyrinthThumbnailCache;
    private String labyrinthPosterFrameCache;
    private String labyrinthS3bucket;


    public EnvironmentConfig(String limelightThumbnailContext, String limelightPosterFrameContext,
                             String limelightFlashContext, String limelightImageContext, String limelightVideoContext,
                             String limelightEnvDirectory, String limelightUnsecurePath, String limelightSecurePath,
                             String limelightMediaVaultSecret, String limelightHttpUrlBase,
                             String limelightMultibitratePath,
                             String labyrinthHttpUrlBase, String labyrinthWebCacheRoot,
                             String labyrinthImageCache, String labyrinthFlashCache,
                             String labyrinthVideoCache, String labyrinthThumbnailCache,
                             String labyrinthPosterFrameCache, String labyrinthS3bucket ) {

        this.limelightThumbnailContext = limelightThumbnailContext;
        this.limelightPosterFrameContext = limelightPosterFrameContext;
        this.limelightFlashContext = limelightFlashContext;
        this.limelightImageContext = limelightImageContext;
        this.limelightVideoContext = limelightVideoContext;
        this.limelightEnvDirectory = limelightEnvDirectory;
        this.limelightUnsecurePath = limelightUnsecurePath;
        this.limelightSecurePath = limelightSecurePath;
        this.limelightMediaVaultSecret = limelightMediaVaultSecret;
        this.limelightHttpUrlBase = limelightHttpUrlBase;
        this.limelightMultibitratePath = limelightMultibitratePath;
        this.labyrinthHttpUrlBase = labyrinthHttpUrlBase;
        this.labyrinthWebCacheRoot = labyrinthWebCacheRoot;
        this.labyrinthImageCache = labyrinthImageCache;
        this.labyrinthFlashCache = labyrinthFlashCache;
        this.labyrinthVideoCache = labyrinthVideoCache;
        this.labyrinthThumbnailCache = labyrinthThumbnailCache;
        this.labyrinthPosterFrameCache = labyrinthPosterFrameCache;
        this.labyrinthS3bucket = labyrinthS3bucket;


        Asserts.notEmpty( limelightThumbnailContext, "limelightThumbnailContext" );
        Asserts.notEmpty( limelightPosterFrameContext, "limelightPosterFrameContext" );
        Asserts.notEmpty( limelightFlashContext, "limelightFlashContext" );
        Asserts.notEmpty( limelightImageContext, "limelightImageContext" );
        Asserts.notEmpty( limelightVideoContext, "limelightVideoContext" );
        Asserts.notEmpty( limelightEnvDirectory, "limelightEnvDirectory" );
        Asserts.notEmpty( limelightUnsecurePath, "limelightUnsecurePath" );
        Asserts.notEmpty( limelightSecurePath, "limelightSecurePath" );
        Asserts.notEmpty( labyrinthHttpUrlBase, "labyrinthHttpUrlBase" );
        Asserts.notEmpty( labyrinthWebCacheRoot, "labyrinthWebCacheRoot" );
        Asserts.notEmpty( labyrinthImageCache, "labyrinthImageCache" );
        Asserts.notEmpty( labyrinthFlashCache, "labyrinthFlashCache" );
        Asserts.notEmpty( labyrinthVideoCache, "labyrinthVideoCache" );
        Asserts.notEmpty( labyrinthThumbnailCache, "labyrinthThumbnailCache" );
        Asserts.notEmpty( labyrinthPosterFrameCache, "labyrinthPosterFrameCache" );
        Asserts.notEmpty( labyrinthS3bucket, "labyrinthS3bucket" );
    }



}
