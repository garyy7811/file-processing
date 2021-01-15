package org.pubanatomy.migrateResources;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.securitytoken.model.PackedPolicyTooLargeException;
import org.pubanatomy.migrateResources.status.ResourceStats;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Map;

/**
 * Created by greg on 10/5/16.
 */
public class CopyPosterFramesProcessor extends CopyMediaProcessor {


    @Autowired
    private ResourceStats resourceStats;


    @Override
    public String getFileType() {
        return "posterframe";
    }


    /**
     * method is not used by this processor
     * @return
     */
    @Override
    public Long loadMaxRecordId() {
        return null;
    }

    /**
     * method is not used by this processor
     * @return
     */
    @Override
    public Object[] selectItemRange(Long[] range) {
        return new Object[0];
    }

    public Map<String, Object> selectPosterFrameRecord( Integer posterFrameId ){


        Split split = SimonManager.getStopwatch("selectPosterFrameRecord."+getFileType()).start();

        final Map<String, Object> result = queryNewVictoryMysql.selectPosterFrameRecord(posterFrameId);

        split.stop();

        return result;

//        String itemSQL =
//                "SELECT P.id AS poster_frame_id, " +
//                "       P.version as poster_frame_version, " +
//                "       P.fileName as poster_frame_filename, " +
//                "       P.fileSize as poster_frame_filesize, " +
//                "       P.width as poster_frame_width, " +
//                "       P.height as poster_frame_height, " +
//                "       P.cdnEnabled as legacy_cdn_enabled " +
//                "FROM Magnet.PosterFrame P " +
//                "WHERE P.id = ?";
//
//        final Map<String, Object> result = jdbcTemplate.queryForMap( itemSQL, posterFrameId );
//        return result;
    }

    @Override
    public void reportStats(long rangeSize, long rangeEnd, long durationMillis) {
        //noop
    }

    @Override
    public void processItem(Map<String, Object> resourceContentMso) {

        Integer resourceContentId = Integer.parseInt(resourceContentMso.get("slide_resource_content_id").toString());

        Integer defaultFrameId = Integer.parseInt(resourceContentMso.get("default_poster_frame_id").toString());
        processPosterFrame(resourceContentId, defaultFrameId, "default_frame");

        Integer firstFrameId = Integer.parseInt(resourceContentMso.get("first_frame_id").toString());
        processPosterFrame(resourceContentId, firstFrameId, "first_frame");
    }

    public void processPosterFrame(Integer resourceContentId, Integer posterFrameId, String posterFrameType) {


        Map<String, Object> posterFrameMso = selectPosterFrameRecord(posterFrameId);

        // add extra items to the metadata map
        posterFrameMso.put("cs_cloud_media_type", "poster_frame");
        posterFrameMso.put("slide_resource_content_id", resourceContentId);
        posterFrameMso.put("poster_frame_type", posterFrameType);

        //
        Integer latestVersion = Integer.parseInt(posterFrameMso.get("poster_frame_version").toString());

        String posterFrameFileName = posterFrameMso.get("poster_frame_filename").toString();


        // if the filename from the database contains a "/" character, then we know it is already in cs-cloud, so we can
        // skip those resources!
        if (posterFrameFileName.indexOf("/") >= 0) {
            logger.warn("found cs-cloud posterframe - skipping!");
            resourceStats.incrementResourcePosterFramesSkippedCount();
            return;
        }


        String fileExtension = posterFrameFileName.substring(posterFrameFileName.indexOf("."));

        // add file extension to metadata map
        posterFrameMso.put("poster_frame_file_extension", fileExtension);

        logger.info("Processing posterFrameId={}, latestVersion={}", posterFrameId, latestVersion);

        for(Integer tmpVersion = latestVersion ; tmpVersion > 0; tmpVersion-- ){


            try {
                processPosterframeVersion(posterFrameType,posterFrameId, tmpVersion, fileExtension, posterFrameMso);
            } catch (Exception e) {
                logger.error("Unexpected error processing posterframe="+posterFrameId+", version="+tmpVersion, e);
                reportError("processPosterFrameVersion", "posterFrameId-"+posterFrameId+"-"+tmpVersion, e);
            }
        }
    }

    protected void processPosterframeVersion(String posterFrameType, Integer posterFrameId, Integer tmpVersion, String fileExtension, Map<String, Object> metaMso ) {

        // we may need to adjust the file extension for updated posterframes
        String altFileExtension = fileExtension.equalsIgnoreCase(".jpg") ? ".png" : ".jpg";

        String tmpPosterFrameFileName = posterFrameId + "_" + tmpVersion + fileExtension;


        String localPosterFramePath = getLocalFilePath( envConfig.getLabyrinthPosterFrameCache(), tmpPosterFrameFileName );

        String appServerUrl =  getAppServerUrl(
                envConfig.getLimelightPosterFrameContext(),
                tmpPosterFrameFileName );

        String fullDestPath = getS3Path(envConfig.getLimelightPosterFrameContext(), tmpPosterFrameFileName);


        if (checkS3Exists) {

            try {
                if (doesS3ObjectExist(fullDestPath)) {
                    logger.info("found posterframe already exists for s3ObjectId={}", fullDestPath);
                    resourceStats.incrementResourcePosterFramesSkippedCount();
                    return;
                } else {
                    // try the alternate file extension
                    String altS3Path = fullDestPath.replace(fileExtension, altFileExtension);
                    if (doesS3ObjectExist(altS3Path)) {
                        logger.info("found posterframe already exists for alt s3ObjectId={}", altS3Path);
                        resourceStats.incrementResourcePosterFramesSkippedCount();
                        return;
                    }
                }
            } catch (Exception e) {
                reportError("error-checking-posterframe-s3-exists", fullDestPath, e);
                return;
            }
        }

        File localFile = null;

        try{

            if (useLocalFileSystem) {
                localFile = new File(localPosterFramePath);
                if (!localFile.exists()) {
                    logger.warn("missing local file - skipping!");
                    resourceStats.incrementResourcePosterFramesSkippedCount();
                    return;
                }
            } else {

                // attempt from appServer
                localFile = downloadRemoteFile(appServerUrl);

                if (localFile == null) {

                    // try the alternate file extension
                    String altAppServerUrl = appServerUrl.replace(fileExtension, altFileExtension);
                    localFile = downloadRemoteFile(altAppServerUrl);

                    if (localFile != null) {

                        // alternate file extension worked
                        logger.info("found posterframe using altFileExtension");

                        // set tmpPosterFrameFileName to use altFileExtension
                        tmpPosterFrameFileName = tmpPosterFrameFileName.replace(fileExtension, altFileExtension);
                        // adjust s3 filename to also use altFileExtension
                        fullDestPath = fullDestPath.replace(fileExtension, altFileExtension);

                        metaMso.put("adjusted_file_extension", "true");
                    }
                }

            }

            // now check if we got a file at all
            if (localFile == null) {
                reportError("missing-"+posterFrameType, "posterframe-" + tmpPosterFrameFileName, appServerUrl);
            } else {

                // update metaMso for tmpVersion
                metaMso.put("poster_frame_version", String.valueOf(tmpVersion));
                metaMso.put("poster_frame_filename", tmpPosterFrameFileName);
                metaMso.put("poster_frame_filesize", String.valueOf(localFile.length()));

                // build a separate ObjectMetaData for each file
                ObjectMetadata tmpObjectMetadata = buildObjectMetadata(metaMso);

                copyLocalFileToS3(localFile, fullDestPath, tmpObjectMetadata);

                resourceStats.incrementResourcePosterFramesUploadedCount();
            }

        } catch( Exception e ){
            logger.error("Unexpected error copying "+posterFrameType+" '"+fullDestPath+"'", e);
            reportError("process-"+posterFrameType, posterFrameType+"-"+tmpPosterFrameFileName, e);
        } finally {

            if (useLocalFileSystem) {
                logger.debug("NOT cleaning up local posterframe: " + localFile);
            } else if (localFile != null) {
                logger.debug("deleting tmp posterFrame: " + localFile.getAbsolutePath());
                localFile.delete();
            }
        }
    }
}
