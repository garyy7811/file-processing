package org.pubanatomy.migrateResources;

import org.pubanatomy.migrateResources.status.ResourceStats;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Created by greg on 10/5/16.
 */
public class CopyResourceThumbnailsProcessor extends CopyThumbnailsProcessor {

    @Autowired
    private ResourceStats resourceStats;


    @Override
    public String getFileType() {
        return "resourceThumbnail";
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

    public Map<String, Object> selectThumbnailRecord( Integer thumbnailId ){


        Split split = SimonManager.getStopwatch("selectThumbnailRecord."+getFileType()).start();

        final Map<String, Object> result = queryNewVictoryMysql.selectThumbnailRecord(thumbnailId);

        split.stop();

        return result;

//        String itemSQL =
//                "SELECT T.id AS thumbnailId, " +
//                "       T.version, " +
//                "       T.fileName, " +
//                "       T.fileSize, " +
//                "       T.width, " +
//                "       T.height, " +
//                "       T.cdnEnabled " +
//                "FROM Magnet.Thumbnail T " +
//                "WHERE T.id = ?";
//
//        final Map<String, Object> result = jdbcTemplate.queryForMap( itemSQL, thumbnailId );
//        return result;
    }

    @Override
    protected void incrementThumbsMissingCount() {
        // noop
    }

    @Override
    protected void incrementThumbsProcessedCountCount() {
        resourceStats.incrementResourceThumbsUploadedCount();
    }

    @Override
    protected void incrementThumbsSkippedCount() {
        resourceStats.incrementResourceThumbsSkippedCount();
    }

    @Override
    public Map<String, Object> buildThumbnailMetadata(Map<String, Object> sqlMso) {

        Map<String, Object> metaMso = super.buildThumbnailMetadata(sqlMso);

        metaMso.put("thumbnail_type", "resource");

        return metaMso;
    }

    @Override
    public void reportStats(long itemsProcessed, long endId, long durationMillis) {
        //noop
    }


    @Override
    public void processItem(Map<String, Object> resourceContentMso) {

        Integer resourceContentId = Integer.parseInt(resourceContentMso.get("slide_resource_content_id").toString());
        Integer thumbnailId = Integer.parseInt(resourceContentMso.get("thumbnail_id").toString());

        Map<String, Object> thumbnailMso = selectThumbnailRecord(thumbnailId);

        Map<String, Object> metaMso = buildThumbnailMetadata(thumbnailMso);

        // add the resourceContentId to the metadata map
        metaMso.put("slide_resource_content_id", resourceContentId);

        //
        Integer latestThumbnailVersion = Integer.parseInt(thumbnailMso.get("version").toString());

        String thumbnailFileName = thumbnailMso.get("fileName").toString();

        // if the filename from the database contains a "/" character, then we know it is already in cs-cloud, so we can
        // skip those resources!
        if (thumbnailFileName.indexOf("/") >= 0) {
            logger.warn("found cs-cloud thumbnail - skipping!");
            resourceStats.incrementResourceThumbsSkippedCount();
            return;
        }

        String fileExtension = thumbnailFileName.substring(thumbnailFileName.indexOf("."));

        // add file extension to metadata map
        metaMso.put("thumbnail_file_extension", fileExtension);

        logger.info("Processing thumbnailId={}, latestThumbnailVersion={}", thumbnailId, latestThumbnailVersion);

        for(Integer tmpVersion = latestThumbnailVersion ; tmpVersion > 0; tmpVersion-- ){

            try {
                processThumbnailVersion(thumbnailId, latestThumbnailVersion, tmpVersion, fileExtension, metaMso);
            } catch (Exception e) {
                logger.error("Unexpected error processing resource thumbnailId="+thumbnailId+", version="+tmpVersion, e);
                reportError("processResourceThumbnailVersion", "thumb-"+thumbnailId+"-"+tmpVersion, e);
            }

        }

    }

}
