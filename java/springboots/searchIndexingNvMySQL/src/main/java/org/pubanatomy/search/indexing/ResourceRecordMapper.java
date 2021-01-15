package org.pubanatomy.search.indexing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 7/26/2017
 */
public class ResourceRecordMapper implements org.springframework.jdbc.core.RowMapper<ResourceLibraryItem>, ReleaseStrategy, CorrelationStrategy{

    protected Logger logger = LogManager.getLogger( this.getClass() );

    @Override
    public ResourceLibraryItem mapRow( ResultSet rs, int rowNum ) throws SQLException{
        final ResourceLibraryItem rt = new ResourceLibraryItem();
        rt.setTargetUuid( rs.getString( "targetUuid" ) );
        rt.setTargetId( rs.getInt( "targetId" ) );
        rt.setTargetName( rs.getString( "targetName" ) );
        rt.setFolderId( rs.getInt( "folderId" ) );
        rt.setIsLib( rs.getBoolean( "isLib" ) );
        rt.setTargetType( rs.getString( "targetType" ) );
        rt.setOwnerId( rs.getInt( "ownerId" ) );
        rt.setOwnerFirstName( rs.getString( "ownerFirstName" ) );
        rt.setOwnerLastName( rs.getString( "ownerLastName" ) );
        rt.setOwnerName( rt.getOwnerFirstName() + " " + rt.getOwnerLastName() );
        rt.setOwnerUsername( rs.getString( "ownerUsername" ) );
        rt.setCreatedTime( rs.getTimestamp( "createdTime" ) );
        rt.setModifiedTime( rs.getTimestamp( "modifiedTime" ) );
        rt.setResourceWidth( rs.getInt( "resourceWidth" ) );
        rt.setResourceHeight( rs.getInt( "resourceHeight" ) );
        rt.setFilesize( rs.getLong( "filesize" ) );
        rt.setOrgFilename( rs.getString( "orgFilename" ) );
        rt.setResourceDuration( metadataToDuration( rs.getString( "metadata" ) ) );
        rt.setThumbnailFileName( rs.getString( "thumbnailFileName" ) );
        rt.setThumbnailWidth( rs.getInt( "thumbnailWidth" ) );
        rt.setThumbnailHeight( rs.getInt( "thumbnailHeight" ) );
        return rt;
    }

    private Integer metadataToDuration( String meta ){
        Matcher m = Pattern.compile( "duration=\"\\d+?\"" ).matcher( meta );
        if( m.find() ){
            final String found = m.group();
            return Integer.parseInt( found.substring( 10, found.length() - 1 ) );
        }
        return 0;
    }

    @Override
    public boolean canRelease( MessageGroup group ){
        final int size = group.getMessages().size();

        Map<Boolean, List<Message<?>>> isItemLstMap = group.getMessages().stream().collect( Collectors.groupingBy( it -> ( it.getPayload() instanceof List ) ) );

        if( isItemLstMap.size() > 1 ){
            List<ResourceLibraryItem> resItemLst =
                    isItemLstMap.get( true ).stream().map( it -> ( List<ResourceLibraryItem> )it.getPayload() ).flatMap( Collection::stream ).collect( Collectors.toList() );
            List<PathMapper.Paths> pathsLst = isItemLstMap.get( false ).stream().map( it -> ( PathMapper.Paths )it.getPayload() ).collect( Collectors.toList() );

            if( pathsLst.size() <= resItemLst.size() ){
                resItemLst.forEach( r -> {
                    Optional<PathMapper.Paths> rsp = pathsLst.stream().filter( p -> p.getFolderId().equals( r.getFolderId() ) ).findAny();

                    if( rsp.isPresent() ){
                        r.setNamePath( StringUtils.collectionToDelimitedString( rsp.get().getNamePath(), "/" ) );
                        r.setUuidPath( StringUtils.collectionToDelimitedString( rsp.get().getUuidPath(), "/" ) );
                    }

                } );
                final boolean rt = resItemLst.stream().noneMatch( it -> it.getNamePath() == null );
                if( rt ){
                    logger.debug( "releasing, group {}->{}", group.getGroupId(), size );
                }
                else{
                    logger.debug( "waiting, group {}->{}, p{}, i{}", group.getGroupId(), size, pathsLst.size(), resItemLst.size() );
                }
                return rt;
            }
        }
        logger.debug( "waiting, group {}->{}", group.getGroupId(), size );
        return false;
    }

    @Override
    public Object getCorrelationKey( Message<?> message ){
        if( message.getPayload() instanceof List ){
            ResourceLibraryItem ele = ( ResourceLibraryItem )( ( List )message.getPayload() ).get( 0 );
            return ele.getFolderId();
        }

        if( message.getPayload() instanceof PathMapper.Paths ){
            return ( ( PathMapper.Paths )message.getPayload() ).getFolderId();
        }

        throw new Error( "WTF!" );
    }

    public Collection<List<ResourceLibraryItem>> groupByFolderId( List<ResourceLibraryItem> lst ){
        return lst.stream().collect( Collectors.groupingBy( ResourceLibraryItem::getFolderId ) ).values();
    }
}
