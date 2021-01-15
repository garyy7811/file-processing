package org.pubanatomy.search.indexing;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 7/26/2017
 */
public class PathMapper implements org.springframework.jdbc.core.RowMapper<PathMapper.PathElement>{


    @Override
    public PathElement mapRow( ResultSet rs, int rowNum ) throws SQLException{
        final PathElement rt = new PathElement();
        rt.setUuid( rs.getString( "uuid" ) );
        rt.setId( rs.getInt( "id" ) );
        final String n = rs.getString( "name" );
        rt.setName( n == null ? "" : n.replaceAll( "/", "-" ) );
        rt.setDepth( rs.getInt( "depth" ) );
        return rt;
    }

    @Data
    public static class PathElement{

        private String uuid;
        private int    id;

        private String name;
        private int    depth;

    }


    @Data
    public static class Paths{

        private Integer folderId;

        private List<String> uuidPath;

        private List<Integer> idPath;

        private List<String> namePath;

    }

    public Paths folderArrToPath( List<PathElement> elementList ){

        final Paths rt = new Paths();

        rt.setUuidPath( elementList.stream().map( PathElement::getUuid ).collect( Collectors.toList() ) );

        rt.setIdPath( elementList.stream().map( PathElement::getId ).collect( Collectors.toList() ) );

        rt.setNamePath( elementList.stream().map( PathElement::getName ).collect( Collectors.toList() ) );

        rt.setFolderId( elementList.get( elementList.size() - 1 ).getId() );


        return rt;

    }


}
