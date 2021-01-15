package org.pubanatomy.copyResAndThumbToS3;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.pubanatomy.batchpartition.RangePartitionService;
import com.llnw.mediavault.MediaVault;
import com.llnw.mediavault.MediaVaultRequest;
import lombok.Data;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.util.Asserts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.annotation.PostConstruct;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: GaryY
 * Date: 7/15/2016
 */
public class CpResAndThumbToS3{

    private static final Logger logger = LogManager.getLogger( CpResAndThumbToS3.class );


    public static final String SELECT_RESOURCE =
            "SELECT      src.id                AS contentId,\n" + "    src.slide_resource_id AS resourceId,\n" +
                    "    sr.resource_type      AS resourceType,\n" + "    src.version           AS resourceVersion,\n" +
                    "    src.filename          AS resource_fileName,\n" +
                    "    src.filesize          AS resource_fileSize,\n" + "    src.width             AS width,\n" +
                    "    src.height            AS height,\n" + "    src.metadata          AS metadata,\n" +
                    "    src.original_filename AS resource_org_name,\n" +

                    "    thum.id               AS thumb_id,\n" + "    thum.version          AS thumb_version,\n" +
                    "    dframe.id             AS postFrame_id,\n" +
                    "    dframe.version        AS postFrame_version,\n" +
                    "    fframe.id             AS firstFrame_id,\n" +
                    "    fframe.version        AS firstFrame_version\n" +
                    "FROM SlideResource sr INNER JOIN SlideResourceContent src ON sr.id = src.slide_resource_id\n" +
                    "    INNER JOIN Thumbnail thum ON src.thumbnail_id = thum.id\n" +
                    "    LEFT JOIN PosterFrame dframe ON src.default_poster_frame_id = dframe.id\n" +
                    "    LEFT JOIN PosterFrame fframe ON src.first_frame_id = fframe.id\n";


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
    private String labyrinthWebCacheRoot;
    private String labyrinthImageCache;
    private String labyrinthFlashCache;
    private String labyrinthVideoCache;
    private String labyrinthThumbnailCache;
    private String labyrinthPosterFrameCache;
    private String labyrinthS3bucket;


    public CpResAndThumbToS3( String limelightThumbnailContext, String limelightPosterFrameContext,
                              String limelightFlashContext, String limelightImageContext, String limelightVideoContext,
                              String limelightEnvDirectory, String limelightUnsecurePath, String limelightSecurePath,
                              String limelightMediaVaultSecret, String limelightHttpUrlBase,
                              String limelightMultibitratePath, String labyrinthWebCacheRoot,
                              String labyrinthImageCache, String labyrinthFlashCache, String labyrinthVideoCache,
                              String labyrinthThumbnailCache, String labyrinthPosterFrameCache,
                              String labyrinthS3bucket ){
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
        Asserts.notEmpty( labyrinthWebCacheRoot, "labyrinthWebCacheRoot" );
        Asserts.notEmpty( labyrinthImageCache, "labyrinthImageCache" );
        Asserts.notEmpty( labyrinthFlashCache, "labyrinthFlashCache" );
        Asserts.notEmpty( labyrinthVideoCache, "labyrinthVideoCache" );
        Asserts.notEmpty( labyrinthThumbnailCache, "labyrinthThumbnailCache" );
        Asserts.notEmpty( labyrinthPosterFrameCache, "labyrinthPosterFrameCache" );
        Asserts.notEmpty( labyrinthS3bucket, "labyrinthS3bucket" );
    }


    @Autowired
    @Qualifier( "newVictoryMySQL" )
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DynamoDBMapperConfig dynamoDBMapperConfig;

    @Autowired
    private AmazonDynamoDBClient dynamoDBClient;


    private DynamoDBMapper dynamoDBMapper;

    @PostConstruct
    private void postConstruct(){
        dynamoDBMapper = new DynamoDBMapper( dynamoDBClient, dynamoDBMapperConfig );
    }


    public Long loadResMax(){
        return jdbcTemplate.queryForObject( "SELECT MAX(id) FROM SlideResourceContent", Long.class ) + 1;
    }


    public Long loadThumbMax(){
        return jdbcTemplate.queryForObject( "SELECT MAX(id) FROM Slide", Long.class ) + 1;
    }


    public Object[] selectResRange( Long[] range ){
        final List<Map<String, Object>> rt = jdbcTemplate
                .queryForList( SELECT_RESOURCE + " WHERE src.id >= ? AND src.id<? ORDER BY src.id ASC", range[ 0 ],
                        range[ 1 ] );
        return new Object[]{ range, rt };
    }

    public Object[] selectSlideThumbRange( Long[] range ){
        final String sql = "SELECT S.id AS slideId, T.id AS thumbnailId, T.version AS latestThumbnailVersion " +
                "FROM `Slide` S " + " INNER JOIN `Thumbnail` T ON S.thumbnail_id = T.id " +
                "WHERE S.id >= ? AND S.id<?" + " ORDER BY S.id ASC ";
        final List<Map<String, Object>> rt = jdbcTemplate.queryForList( sql, range[ 0 ], range[ 1 ] );
        return new Object[]{ range, rt };
    }

    @Autowired
    private AmazonS3Client s3client;

    public Long[] processEachResRange( Object[] rangeAndLstOfMso ){
        List<Map<String, Object>> lstOfMso = ( List<Map<String, Object>> )rangeAndLstOfMso[ 1 ];

        lstOfMso.stream().forEachOrdered( mso -> {

            Long contentId = Long.parseLong( mso.get( "contentId" ).toString() );
            logger.info( "contentId ID:{}", contentId );


            final ObjectMetadata objMeta = new ObjectMetadata();
            objMeta.setUserMetadata( new HashMap<>() );

            mso.entrySet().forEach( e -> {
                if( e.getValue() != null && ! e.getKey().equals( "metadata" ) ){
                    objMeta.getUserMetadata().put( e.getKey(), e.getValue().toString() );
                }
            } );

            String resourceType = ( String )mso.get( "resourceType" );


            try{
                String cacheDir = null;
                String contextDir = null;
                if( "image".equalsIgnoreCase( resourceType ) ){
                    cacheDir = labyrinthImageCache;
                    contextDir = limelightImageContext;
                }
                else if( "swf".equalsIgnoreCase( resourceType ) ){
                    cacheDir = labyrinthFlashCache;
                    contextDir = limelightFlashContext;
                }
                else if( "video".equalsIgnoreCase( resourceType ) ){
                    cacheDir = labyrinthVideoCache;
                    contextDir = limelightVideoContext;

                    try{
                        copyFrame( objMeta, Long.parseLong( mso.get( "firstFrame_id" ).toString() ),
                                Integer.parseInt( mso.get( "firstFrame_version" ).toString() ) );
                    }
                    catch( Exception e ){
                        dynamoDBMapper.save( new ErrorFile( "firstFrameId-error", contentId.toString(),
                                ExceptionUtils.getStackTrace( e ) ) );
                    }
                    try{
                        copyFrame( objMeta, Long.parseLong( mso.get( "postFrame_id" ).toString() ),
                                Integer.parseInt( mso.get( "postFrame_version" ).toString() ) );
                    }
                    catch( Exception e ){
                        dynamoDBMapper.save( new ErrorFile( "postFrameId-error", contentId.toString(),
                                ExceptionUtils.getStackTrace( e ) ) );
                    }


                    String metadata = ( String )mso.get( "metadata" );

                    NodeList streams = null;
                    try{
                        streams = ( NodeList )xPathFactory.get().newXPath()
                                .evaluate( "/metaData/streams/stream", new InputSource( new StringReader( metadata ) ),
                                        XPathConstants.NODESET );
                    }
                    catch( Exception e ){
                        dynamoDBMapper.save( new ErrorFile( "multi-bit-stream--xpath", contentId.toString(),
                                metadata + ExceptionUtils.getStackTrace( e ) ) );
                    }

                    if( streams != null && streams.getLength() > 0 ){
                        for( int i = 0; i < streams.getLength(); i++ ){
                            final NamedNodeMap siAttributes = streams.item( i ).getAttributes();
                            if( "false".equals( siAttributes.getNamedItem( "isDefault" ).getTextContent() ) ){
                                String siPath = siAttributes.getNamedItem( "path" ).getTextContent();
                                String siFileName = siAttributes.getNamedItem( "fileName" ).getTextContent();

                                String url = limelightHttpUrlBase + "/" + siPath + "/" + siFileName;
                                final MediaVaultRequest options = new MediaVaultRequest( url );
                                options.setEndTime( System.currentTimeMillis() / 1000 + 6000 );

                                InputStream mbDownloadStream = null;
                                try{
                                    mbDownloadStream =
                                            new URL( new MediaVault( limelightMediaVaultSecret ).compute( options ) )
                                                    .openStream();
                                    s3client.putObject( labyrinthS3bucket,
                                            limelightSecurePath + "/" + limelightEnvDirectory + "/" +
                                                    limelightVideoContext + "/" + limelightMultibitratePath + "/" +
                                                    siFileName, mbDownloadStream, objMeta );
                                }
                                catch( Exception e ){
                                    final String tmp = ExceptionUtils.getStackTrace( e );
                                    logger.info( tmp );
                                    dynamoDBMapper.save( new ErrorFile( "multi-bit-stream", url, tmp ) );
                                }
                                finally{
                                    if( mbDownloadStream != null ){
                                        try{
                                            mbDownloadStream.close();
                                        }
                                        catch( Exception e ){
                                            logger.error( e );
                                        }
                                    }
                                }

                            }

                        }
                    }
                }
                else{
                    throw new RuntimeException( "unknown resource type:" + resourceType );
                }

                final String resMysqlFileName = ( String )mso.get( "resource_fileName" );
                final String resSrcPath = getLabyrinthFilePath( cacheDir, contentId + "_" + resMysqlFileName );
                final String resDestPath =
                        getLimelightPath( limelightSecurePath, limelightEnvDirectory, contextDir, resMysqlFileName );

                logger.debug( "start resource:{}->{}", resSrcPath, resDestPath );
                final File resFile = new File( resSrcPath );
                try{
                    long tmp = System.currentTimeMillis();
                    s3client.putObject( labyrinthS3bucket, resDestPath, new FileInputStream( resFile ), objMeta );
                    logger.info( "done resource:{}->{}, speed:{} ", resSrcPath, resDestPath,
                            resFile.length() / ( System.currentTimeMillis() - tmp ) );
                }
                catch( Exception e ){
                    dynamoDBMapper.save( new ErrorFile( "copy-res-content", resSrcPath + ">>>>" + resDestPath,
                            ExceptionUtils.getStackTrace( e ) ) );
                }
                try{
                    copyThumb( mso.get( "thumb_id" ).toString(),
                            Integer.parseInt( mso.get( "thumb_version" ).toString() ) );
                }
                catch( Exception e ){
                    dynamoDBMapper.save( new ErrorFile( "res-thumb",
                            resSrcPath + "[TID:" + mso.get( "thumb_id" ) + "]TV:" + mso.get( "thumb_version" ),
                            ExceptionUtils.getStackTrace( e ) ) );
                }

            }
            catch( Exception e ){
                Long[] tmpR = ( Long[] )rangeAndLstOfMso[ 0 ];
                resPartitioning.errorRange( tmpR[ 0 ], tmpR[ 1 ], contentId, ExceptionUtils.getStackTrace( e ) );
                throw e;
            }
        } );

        return ( Long[] )rangeAndLstOfMso[ 0 ];
    }

    private void copyFrame( ObjectMetadata objMeta, Long videoFrameId, Integer videoFrameVersion ){
        for( ; videoFrameVersion > 0; videoFrameVersion-- ){

            final String frameFileName = videoFrameId + "_" + videoFrameVersion + ".jpg";
            String firstFramePath = getLabyrinthFilePath( labyrinthPosterFrameCache, frameFileName );

            String firstFrameDest =
                    getLimelightPath( limelightUnsecurePath, limelightEnvDirectory, limelightPosterFrameContext,
                            frameFileName );

            logger.debug( "start firstFramePath:{}->{}", firstFramePath, firstFrameDest );
            try{
                s3client.putObject( labyrinthS3bucket, firstFrameDest, new FileInputStream( firstFramePath ), objMeta );
            }
            catch( Exception e ){
                logger.warn( ExceptionUtils.getStackTrace( e ) );
                dynamoDBMapper.save( new ErrorFile( "videoFrame missing", frameFileName,
                        ExceptionUtils.getStackTrace( e ) ) );
            }
            logger.debug( "end firstFramePath:{}->{}", firstFramePath, firstFrameDest );

        }
    }

    public Long[] processEachThumbRange( Object[] rangeAndLstOfMso ){

        List<Map<String, Object>> lstOfMso = ( List<Map<String, Object>> )rangeAndLstOfMso[ 1 ];

        lstOfMso.stream().forEachOrdered( mso -> {

            copyThumb( mso.get( "thumbnailId" ).toString(),
                    Integer.parseInt( mso.get( "latestThumbnailVersion" ).toString() ) );

        } );

        return ( Long[] )rangeAndLstOfMso[ 0 ];
    }

    private void copyThumb( String thumbnailId, Integer version ){

        for( ; version > 0; version-- ){
            String tmpThumbFileName = thumbnailId + "_" + version + ".jpg";

            String fullThumbPath = getLabyrinthFilePath( labyrinthThumbnailCache, tmpThumbFileName );
            String fullDestPath =
                    getLimelightPath( limelightUnsecurePath, limelightEnvDirectory, limelightThumbnailContext,
                            tmpThumbFileName );

            logger.debug( "start slide thumb:{}->{}", fullThumbPath, fullDestPath );
            File thumbFile = new File( fullThumbPath );

            try{
                s3client.putObject( labyrinthS3bucket, fullDestPath, thumbFile );
                logger.debug( "copy slide thumb:{}->{}", thumbFile.getAbsolutePath(), fullDestPath );
            }
            catch( Exception e ){
                logger.info( ExceptionUtils.getStackTrace( e ) );
            }

        }


    }

    private String getLabyrinthFilePath( String cacheFolder, String fileName ){
        return labyrinthWebCacheRoot + File.separator + cacheFolder + File.separator + fileName;
    }

    private String getLimelightPath( String secure, String envDir, String context, String fileName ){
        return secure + "/" + envDir + "/" + context + "/" + fileName;
    }


    @Autowired
    private RangePartitionService resPartitioning;

    private static final ThreadLocal<XPathFactory> xPathFactory = new ThreadLocal<XPathFactory>(){
        @Override
        protected XPathFactory initialValue(){
            return XPathFactory.newInstance();
        }
    };


    @DynamoDBTable( tableName = "$!overriding me!$" )
    @Data
    public static class ErrorFile{

        private ErrorFile( String type, String id, String error ){
            this.type = type;
            this.id = id;
            this.error = error;
        }

        @DynamoDBHashKey
        private String type;

        @DynamoDBRangeKey
        private String id;

        private String error;

    }

}
