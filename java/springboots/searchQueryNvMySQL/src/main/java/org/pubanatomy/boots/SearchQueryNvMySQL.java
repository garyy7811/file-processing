package org.pubanatomy.boots;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ImportResource;


@ImportResource( "app-search-query.xml" )
public class SearchQueryNvMySQL{

    public static void main( String[] args ){
        SpringApplication.run( SearchQueryNvMySQL.class, args );
    }

}
