package org.pubanatomy.boots;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ImportResource;


@ImportResource( "app-search-indexing.xml" )
public class SearchIndexingNvMySQL {

    public static void main( String[] args ){
        SpringApplication.run( SearchIndexingNvMySQL.class, args );
    }

}
