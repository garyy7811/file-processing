package org.pubanatomy.migrateResources;

import com.amazonaws.services.s3.model.ObjectMetadata;
import org.pubanatomy.migrateResources.status.ResourceStats;
import com.llnw.mediavault.MediaVault;
import com.llnw.mediavault.MediaVaultRequest;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPathConstants;
import java.io.File;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 10/5/16.
 */
public class CopyMultiBitrateVideosProcessor extends CopyMediaProcessor {


    @Autowired
    private ResourceStats resourceStats;


    @Override
    public String getFileType() {
        return "mbstream";
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


    @Override
    public void reportStats(long rangeSize, long rangeEnd, long durationMillis) {
        //noop
    }

    @Override
    public void processItem(Map<String, Object> resourceContentMso) {


        Integer resourceContentId = Integer.parseInt(resourceContentMso.get("slide_resource_content_id").toString());

        String metadata = ( String )resourceContentMso.get( "metadata" );

        NodeList streams = null;

        try{
            streams = ( NodeList )xPathFactory.get().newXPath()
                    .evaluate( "/metaData/streams/stream", new InputSource( new StringReader( metadata ) ),
                            XPathConstants.NODESET );
        }
        catch( Exception e ){
            reportError("multi-bitrate-stream-xpath",
                        resourceContentId.toString(),
                        metadata + "\n Error:" + ExceptionUtils.getStackTrace( e )
                        );
            return;
        }


        if( streams != null && streams.getLength() > 0 ){

            Map<String, Object> metadataMso = new HashMap<>();
            metadataMso.putAll(resourceContentMso);
            metadataMso.put("cs_cloud_media_type", "multi_bitrate_stream");

            for( int i = 0; i < streams.getLength(); i++ ){

                final NamedNodeMap siAttributes = streams.item( i ).getAttributes();

                try {
                    processStream(metadataMso, siAttributes);
                } catch (Exception e) {
                    reportError("process-stream-loop", resourceContentId + "-stream-" + i, e);
                }

            }
        }


    }

    /**
     * <stream  id="2"
     *          isDefault="false"
     *          bitRate="1000"
     *          contentSize="351954"
     *          fileName="12916_1_1000.mp4"
     *          path="s/demo/video/mb"
     *          height="360"
     *          width="640"
     *          videoType="mp4"
     *          />
     *
     */
    public void processStream(Map<String, Object> metadataMso, NamedNodeMap siAttributes ) {

        Boolean isDefaultStream = Boolean.valueOf(siAttributes.getNamedItem("isDefault").getTextContent());

        // we don't process the default stream here
        if (isDefaultStream) {
            logger.debug("Found default stream - aborting");
            return;
        }


        String tmpStreamId = siAttributes.getNamedItem("id").getTextContent();
        String tmpFileName = siAttributes.getNamedItem( "fileName" ).getTextContent();
        String tmpBitrate = siAttributes.getNamedItem("bitRate").getTextContent();

        String remoteStreamPath = siAttributes.getNamedItem( "path" ).getTextContent();

        logger.info("processing multibitrate stream: " + tmpFileName);

        metadataMso.put("multi_bitrate_stream_id", tmpStreamId);
        metadataMso.put("multi_bitrate_stream_filename", tmpFileName);
        metadataMso.put("multi_bitrate_stream_bitrate", tmpBitrate);



        String rawUrl = envConfig.getLimelightHttpUrlBase() + "/" + remoteStreamPath + "/" + tmpFileName;

        final MediaVaultRequest options = new MediaVaultRequest(rawUrl);
        options.setEndTime(System.currentTimeMillis() / 1000 + 6000);

        String secureUrl = new MediaVault(envConfig.getLimelightMediaVaultSecret())
                .compute(options);


        String s3Path = getS3Path(envConfig.getLimelightVideoContext() + "/" + envConfig.getLimelightMultibitratePath(), tmpFileName);

        if (checkS3Exists) {

            try {
                if (doesS3ObjectExist(s3Path)) {
                    logger.info("found stream already exists for s3ObjectId={}", s3Path);
                    resourceStats.incrementResourceMultiBitrateStreamsSkippedCount();
                    return;
                }
            } catch (Exception e) {
                reportError("error-checking-stream-s3-exists", s3Path, e);
                return;
            }
        }



        File localFile = null;

        try {

            // first attempt to load file from cdn
            localFile = downloadRemoteFile(secureUrl);


            if (localFile == null) {
                reportError("missing-stream", tmpStreamId, secureUrl);
                return;
            }

            ObjectMetadata objectMetadata = buildObjectMetadata(metadataMso);

            copyLocalFileToS3(localFile, s3Path, objectMetadata);

            resourceStats.incrementResourceMultiBitrateStreamsUploadedCount();

        } catch( Exception e ){
            logger.error("Unexpected error copying multibitrate stream '"+tmpFileName+"'", e);
            reportError("process-multibitrate-stream", tmpFileName, e);
        } finally {

            if (localFile != null) {
                logger.debug("deleting tmp stream: " + localFile.getAbsolutePath());
                localFile.delete();
            }
        }

    }
}
