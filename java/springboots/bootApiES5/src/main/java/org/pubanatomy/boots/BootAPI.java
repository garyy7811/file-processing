package org.pubanatomy.boots;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@ImportResource( "root-context.xml" )
@Import( { EmbeddedServletContainerAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
        ServerPropertiesAutoConfiguration.class } )
public class BootAPI{

    public static void main( String[] args ){
        SpringApplication.run( BootAPI.class, args );
    }

}
