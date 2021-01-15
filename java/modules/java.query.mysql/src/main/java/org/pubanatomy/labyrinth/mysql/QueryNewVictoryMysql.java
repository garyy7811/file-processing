package org.pubanatomy.labyrinth.mysql;

import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 3/11/2015
 * Time: 1:14 PM
 */
public interface QueryNewVictoryMysql{


    UserGroupClientByCsSessionId loadUserGroupClientIDsByCsSessionId( String args );


    List<Map<String, Object>> loadMissingFonts( boolean b ) throws IOException;

    List<Map<String, Object>> loadAllFonts( boolean b ) throws IOException;


    Long loadResourceMaxRecordId( boolean dummy );

    Object[] selectResourceItemRange( Long[] range );

    Long loadThumbnailMaxRecordId( boolean dummy );

    Object[] selectThumbnailItemRange( Long[] range );

    Map<String, Object> selectThumbnailRecord( Integer thumbnailId );

    Map<String, Object> selectPosterFrameRecord( Integer posterFrameId );


    Object[] selectUpdatedThumbnailItemRange( Object[] args );


    Object[] selectUpdatedResourceItemRange( Object[] args );

    @Data
    public static class UserGroupClientByCsSessionId implements Serializable{

        private static final long serialVersionUID = 1L;


        private String csSessionId;
        private long   userId;
        private String userName;
        private long   groupId;
        private long   clientId;

        public UserGroupClientByCsSessionId( String csSessionId ){
            this.csSessionId = csSessionId;
        }
    }


}
