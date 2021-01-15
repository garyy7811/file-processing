package org.pubanatomy.labyrinth.mysql;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * User: flashflexpro@gmail.com
 * Date: 3/12/2015
 * Time: 11:26 AM
 */
public class QueryNewVictoryMysqlImpl implements QueryNewVictoryMysql{

    private static final Logger logger = LogManager.getLogger( QueryNewVictoryMysqlImpl.class );

    @Autowired
    @Qualifier( "newVictoryMySQL" )
    private JdbcTemplate jdbcTemplate;

    @Override
    @Cacheable( value = "loadUserGroupClientIDsByCsSessionId" )
    public UserGroupClientByCsSessionId loadUserGroupClientIDsByCsSessionId( String csSessionId ){
        logger.debug( "csSessionId:{}", csSessionId );
        UserGroupClientByCsSessionId rt = new UserGroupClientByCsSessionId( csSessionId );
        try{
            String sql =
                    "SELECT u.id AS userId, u.username, u.group_id, g.client_id \n" + "FROM\n" + "  Magnet.User u\n" +
                            "  INNER JOIN Magnet.ApplicationSession a ON u.id = a.user_id\n" +
                            "  INNER JOIN Magnet.`Group` g ON g.id = u.group_id\n" +
                            "  INNER JOIN Session s ON s.id = a.id\n" +
                            "WHERE s.date_ended IS NULL AND a.flex_client_id=?";
            List<Map<String, Object>> lst = jdbcTemplate.queryForList( sql, csSessionId );
            if( lst.size() == 0 ){
                logger.info( "Nothing found with csSessionId:{}", csSessionId );
                return rt;
            }
            if( lst.size() > 1 ){
                logger.warn( "With csSessionId:{}, multi usernames:{}", csSessionId,
                        lst.stream().map( r -> r.get( "username" ).toString() ).collect( joining( ";" ) ) );
            }
            Map<String, Object> rslt = lst.get( 0 );
            Integer clientId = ( Integer )rslt.get( "client_id" );
            rt.setClientId( clientId == null ? - 1 : clientId );
            rt.setUserId( ( Integer )rslt.get( "userId" ) );
            rt.setUserName( ( String )rslt.get( "username" ) );
            rt.setGroupId( ( Integer )rslt.get( "group_id" ) );
        }
        catch( Exception e ){
            logger.error( "e:{}", ExceptionUtils.getStackTrace( e ) );
        }
        return rt;
    }

    private Resource findMissingFonts = new ClassPathResource( "/fonts.sql/FIND_FIX_MissingFonts.sql" );

    private Resource selectAllFonts = new ClassPathResource( "/fonts.sql/SELECT_All_CustomFonts_Summary.sql" );

    public List<Map<String, Object>> loadMissingFonts( boolean b ) throws IOException{
        return jdbcTemplate
                .queryForList( IOUtils.toString( new InputStreamReader( findMissingFonts.getInputStream() ) ) ).stream()
                .map( el -> {
                    return new HashMap<String, Object>( el );
                } ).collect( Collectors.toList() );
    }

    public List<Map<String, Object>> loadAllFonts( boolean b ) throws IOException{
        return jdbcTemplate.queryForList( IOUtils.toString( new InputStreamReader( selectAllFonts.getInputStream() ) ) )
                .stream().map( el -> {
                    return new HashMap<String, Object>( el );
                } ).collect( Collectors.toList() );
    }

    @Override
    public Long loadResourceMaxRecordId( boolean dummy ){
        String maxRecordSql = "SELECT MAX(id) FROM SlideResourceContent";
        Long maxRecordId = jdbcTemplate.queryForObject( maxRecordSql, Long.class ) + 1;
        logger.info( "loadMaxRecordId returning:  " + maxRecordId );
        return maxRecordId;
    }

    @Override
    public Object[] selectResourceItemRange( Long[] range ){
        String itemRangeSQL =
                "SELECT SR.id as slide_resource_id, \n" + "  SR.resource_type as slide_resource_type, \n" +
                        "  SR.name as slide_resource_name, \n" + "  SRC.id as slide_resource_content_id, \n" +
                        "  SRC.version as slide_resource_content_version, \n" +
                        "  SRC.filename as slide_resource_content_filename, \n" +
                        "  SRC.original_filename as slide_resource_content_original_filename, \n" +
                        "  SRC.filesize as slide_resource_content_filesize, \n" +
                        "  SRC.width as slide_resource_content_width, \n" +
                        "  SRC.height as slide_resource_content_height, \n" + "  SRC.thumbnail_id as thumbnail_id, \n" +
                        "  SRC.default_poster_frame_id as default_poster_frame_id, \n" +
                        "  SRC.first_frame_id as first_frame_id, \n" + "  SRC.metadata as metadata \n" +
                        "FROM Magnet.SlideResourceContent SRC \n" +
                        "  INNER JOIN Magnet.SlideResource SR ON SRC.slide_resource_id = SR.id \n" +
                        "WHERE SRC.id >= ? AND SRC.id < ? \n" + "ORDER BY SRC.id ASC";

        final List<Map<String, Object>> rt =
                jdbcTemplate.queryForList( itemRangeSQL, range[ 0 ], range[ 1 ] ).stream().map( o -> {
                    return new HashMap<>( o );
                } ).collect( Collectors.toList() );
        logger.info( "selectItemRange[" + range[ 0 ] + ", " + range[ 1 ] + "] returning " + rt.size() + " items" );
        return new Object[]{ range, rt };
    }

    @Override
    public Long loadThumbnailMaxRecordId( boolean dummy ){
        String maxRecordSql = "SELECT MAX(id) FROM Slide";
        Long maxRecordId = jdbcTemplate.queryForObject( maxRecordSql, Long.class ) + 1;
        logger.info( "loadMaxRecordId returning:  " + maxRecordId );
        return maxRecordId;
    }

    @Override
    public Object[] selectThumbnailItemRange( Long[] range ){
        String itemRangeSQL =
                "SELECT T.id AS thumbnailId, T.version, T.fileName, T.fileSize, T.width, T.height, T.cdnEnabled, S.id AS slideId " +
                        "FROM Magnet.Thumbnail T " + "   INNER JOIN Magnet.Slide S   ON T.id = S.thumbnail_id " +
                        "WHERE S.id >= ? AND S.id<? " + "ORDER BY T.id ASC ";
        final List<Map<String, Object>> rt =
                jdbcTemplate.queryForList( itemRangeSQL, range[ 0 ], range[ 1 ] ).stream().map( o -> {
                    return new HashMap<>( o );
                } ).collect( Collectors.toList() );
        logger.info( "selectItemRange[" + range[ 0 ] + ", " + range[ 1 ] + "] returning " + rt.size() + " items" );
        return new Object[]{ range, rt };
    }


    public Map<String, Object> selectThumbnailRecord( Integer thumbnailId ){

        String itemSQL =
                "SELECT T.id AS thumbnailId, " + "       T.version, " + "       T.fileName, " + "       T.fileSize, " +
                        "       T.width, " + "       T.height, " + "       T.cdnEnabled " + "FROM Magnet.Thumbnail T " +
                        "WHERE T.id = ?";

        final Map<String, Object> result = jdbcTemplate.queryForMap( itemSQL, thumbnailId );
        return new HashMap<>( result );
    }

    public Map<String, Object> selectPosterFrameRecord( Integer posterFrameId ){

        String itemSQL = "SELECT P.id AS poster_frame_id, " + "       P.version as poster_frame_version, " +
                "       P.fileName as poster_frame_filename, " + "       P.fileSize as poster_frame_filesize, " +
                "       P.width as poster_frame_width, " + "       P.height as poster_frame_height, " +
                "       P.cdnEnabled as legacy_cdn_enabled " + "FROM Magnet.PosterFrame P " + "WHERE P.id = ?";

        final Map<String, Object> result = jdbcTemplate.queryForMap( itemSQL, posterFrameId );
        return new HashMap<>( result );
    }

    public Object[] selectUpdatedThumbnailItemRange( Object[] args ){
        Long minSlideId = ( Long )args[ 0 ];
        Long maxSlideId = ( Long )args[ 1 ];
        String minModifiedDate = ( String )args[ 2 ];
        Long maxResults = ( Long )args[ 3 ];

        String sql =
                "SELECT T.id AS thumbnailId, T.version, T.fileName, T.fileSize, T.width, T.height, T.cdnEnabled, S.id AS slideId\n" +
                        "FROM Magnet.Thumbnail T\n" + "   INNER JOIN Magnet.Slide S   ON T.id = S.thumbnail_id\n" +
                        "   INNER JOIN Magnet.Access A on S.uuid = A.uuid\n" + "WHERE S.id >= ? AND S.id < ?\n" +
                        "      AND T.version > 1\n" + "      AND A.modifiedTime > DATE(?)\n" + "ORDER BY T.id ASC\n" +
                        "LIMIT ?";
        final List<Map<String, Object>> rt =
                jdbcTemplate.queryForList( sql, minSlideId, maxSlideId, minModifiedDate, maxResults ).stream()
                        .map( i -> new HashMap<>( i ) ).collect( toList() );
        return new Object[]{ rt };
    }


    public Object[] selectUpdatedResourceItemRange( Object[] args ){

        Long minSrcId = ( Long )args[ 0 ];
        Long maxSrcId = ( Long )args[ 1 ];
        String minModifiedDate = ( String )args[ 2 ];
        Long maxResults = ( Long )args[ 3 ];

        String sql = "SELECT SR.id as slide_resource_id, \n" + "  SR.resource_type as slide_resource_type, \n" +
                "  SR.name as slide_resource_name, \n" + "  SRC.id as slide_resource_content_id, \n" +
                "  SRC.version as slide_resource_content_version, \n" +
                "  SRC.filename as slide_resource_content_filename, \n" +
                "  SRC.original_filename as slide_resource_content_original_filename, \n" +
                "  SRC.filesize as slide_resource_content_filesize, \n" +
                "  SRC.width as slide_resource_content_width, \n" +
                "  SRC.height as slide_resource_content_height, \n" + "  SRC.thumbnail_id as thumbnail_id, \n" +
                "  SRC.default_poster_frame_id as default_poster_frame_id, \n" +
                "  SRC.first_frame_id as first_frame_id, \n" + "  SRC.metadata as metadata \n" +
                "FROM Magnet.SlideResourceContent SRC \n" +
                "  INNER JOIN Magnet.SlideResource SR ON SRC.slide_resource_id = SR.id\n" +
                "  INNER JOIN Magnet.Access A ON SR.uuid = A.uuid\n" +
                "  INNER JOIN Magnet.PosterFrame PF ON SRC.default_poster_frame_id = PF.id\n" +
                "WHERE SRC.id >= ? AND SRC.id < ?\n" + "  AND PF.version > 1\n" + "  AND A.modifiedTime > DATE(?)\n" +
                "ORDER BY SRC.id ASC\n" + "LIMIT ?";
        final List<Map<String, Object>> rt =
                jdbcTemplate.queryForList( sql, minSrcId, maxSrcId, minModifiedDate, maxResults ).stream()
                        .map( i -> new HashMap<>( i ) ).collect( toList() );
        return new Object[]{ rt };
    }
}
