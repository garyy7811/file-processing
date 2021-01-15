package org.pubanatomy.search.indexing;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * User: GaryY
 * Date: 7/26/2017
 */
public class FolderRecordMapper extends ResourceRecordMapper{

    @Override
    public ResourceLibraryItem mapRow( ResultSet rs, int rowNum ) throws SQLException{
        final ResourceLibraryItem rt = new ResourceLibraryItem();
        rt.setTargetUuid( rs.getString( "targetUuid" ) );
        rt.setFolderId( rs.getInt( "folderId" ) );
        rt.setTargetType( "folder" );
        rt.setIsLib( rs.getBoolean( "isLib" ) );
        rt.setOwnerId( rs.getInt( "ownerId" ) );
        rt.setOwnerUsername( rs.getString( "ownerUsername" ) );
        rt.setOwnerFirstName( rs.getString( "ownerFirstName" ) );
        rt.setOwnerLastName( rs.getString( "ownerLastName" ) );
        rt.setOwnerName( rt.getOwnerFirstName() + " " + rt.getOwnerLastName() );
        rt.setCreatedTime( rs.getTimestamp( "createdTime" ) );
        rt.setModifiedTime( rs.getTimestamp( "modifiedTime" ) );
        return rt;
    }

}
