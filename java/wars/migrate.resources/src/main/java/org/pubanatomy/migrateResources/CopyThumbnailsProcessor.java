package org.pubanatomy.migrateResources;

import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 10/5/16.
 */
public abstract class CopyThumbnailsProcessor extends CopyMediaProcessor {



    protected abstract void incrementThumbsMissingCount();
    protected abstract void incrementThumbsProcessedCountCount();
    protected abstract void incrementThumbsSkippedCount();

    public Map<String, Object> buildThumbnailMetadata(Map<String, Object> sqlMso) {

        Map<String, Object> metaMso = new HashMap<>();

        metaMso.put("cs_cloud_media_type", "thumbnail");

        metaMso.put("thumbnail_id", sqlMso.get("thumbnailId"));
        metaMso.put("thumbnail_version", sqlMso.get("version"));
        metaMso.put("thumbnail_filename", sqlMso.get("fileName"));
        metaMso.put("thumbnail_filesize", sqlMso.get("fileSize"));
        metaMso.put("thumbnail_width", sqlMso.get("width"));
        metaMso.put("thumbnail_height", sqlMso.get("height"));

        metaMso.put("legacy_cdn_enabled", sqlMso.get("cdnEnabled"));

        return metaMso;
    }


    protected void processThumbnailVersion(Integer thumbnailId, Integer latestThumbnailVersion, Integer tmpVersion, String fileExtension, Map<String, Object> metaMso ) {

        // we may need to adjust the file extension for updated posterframes
        String altFileExtension = fileExtension.equalsIgnoreCase(".jpg") ? ".png" : ".jpg";


        String tmpThumbFileName = thumbnailId + "_" + tmpVersion + fileExtension;

        String tmpS3ObjectId = getS3Path(envConfig.getLimelightThumbnailContext(), tmpThumbFileName);

        if (checkS3Exists) {

            try {
                if (doesS3ObjectExist(tmpS3ObjectId)) {
                    logger.info("found thumbnailVersion already exists for s3ObjectId={}", tmpS3ObjectId);
                    incrementThumbsSkippedCount();
                    return;
                } else {
                    String altS3ObjectId = tmpS3ObjectId.replace(fileExtension, altFileExtension);
                    if (doesS3ObjectExist(altS3ObjectId)) {
                        logger.info("found thumbnailVersion already exists for alt s3ObjectId={}", altS3ObjectId);
                        incrementThumbsSkippedCount();
                        return;
                    }
                }
            } catch (Exception e) {
                reportError("error-checking-thumbnail-s3-exists", tmpS3ObjectId, e);
                return;
            }
        }


        String localThumbPath = getLocalFilePath( envConfig.getLabyrinthThumbnailCache(), tmpThumbFileName );

        String appServerThumbUrl =  getAppServerUrl(
                envConfig.getLimelightThumbnailContext(),
                tmpThumbFileName );


        File thumbFile = null;

        try{

            if (useLocalFileSystem) {
                thumbFile = new File(localThumbPath);
                if (!thumbFile.exists()) {
                    logger.warn("missing local file - skipping!");
                    incrementThumbsSkippedCount();
                    return;
                }
            } else {

                // attempt from appServer
                thumbFile = downloadRemoteFile(appServerThumbUrl);

                if (thumbFile == null) {
                    // try the alternate file extension
                    String altAppServerUrl = appServerThumbUrl.replace(fileExtension, altFileExtension);
                    thumbFile = downloadRemoteFile(altAppServerUrl);

                    if (thumbFile != null) {

                        // alternate file extension worked
                        logger.info("found thumbnail using altFileExtension");

                        // set tmpPosterFrameFileName to use altFileExtension
                        tmpThumbFileName = tmpThumbFileName.replace(fileExtension, altFileExtension);
                        // adjust s3 filename to also use altFileExtension
                        tmpS3ObjectId = tmpS3ObjectId.replace(fileExtension, altFileExtension);

                        metaMso.put("adjusted_file_extension", "true");
                    }
                }

            }


            // now check if we got a file at all
            if (thumbFile == null) {

                incrementThumbsMissingCount();

                // if the latestThumbnailVersion is missing, this is unexpected, so report the error
                if (tmpVersion == latestThumbnailVersion) {
                    reportError("missing-latest-thumbnail", "slide-thumbnail-" + tmpThumbFileName, tmpS3ObjectId);
                }

                // always log warning then continue loop
                logger.warn("Thumbnail file not found: " + tmpS3ObjectId);

            } else {

                // update metaMso for tmpVersion
                metaMso.put("thumbnail_version", String.valueOf(tmpVersion));
                metaMso.put("thumbnail_filename", tmpThumbFileName);
                metaMso.put("thumbnail_filesize", String.valueOf(thumbFile.length()));

                // build a separate ObjectMetaData for each file
                ObjectMetadata tmpObjectMetadata = buildObjectMetadata(metaMso);

                copyLocalFileToS3(thumbFile, tmpS3ObjectId, tmpObjectMetadata);

                incrementThumbsProcessedCountCount();
            }
        }
        catch( Exception e ){
            logger.error("Unexpected error copying thumbnail '"+tmpS3ObjectId+"'", e);
            reportError("processThumbnail", "thumbnail-"+tmpThumbFileName, e);
        } finally {
            // only cleanup if we are NOT using local filesystem!
            if (useLocalFileSystem) {
                logger.debug("NOT cleaning up local thumbnail: "+ thumbFile);
            } else if (thumbFile != null) {
                logger.debug("deleting tmp thumbnail: " + thumbFile.getAbsolutePath());
                thumbFile.delete();
            }
        }
    }
}
