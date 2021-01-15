package org.pubanatomy.batchpartition;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.Server;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * User: GaryY
 * Date: 7/26/2016
 */
public class StartStopHsql implements ApplicationListener<ContextClosedEvent>{

    private static final Logger logger = LogManager.getLogger( StartStopHsql.class );


    public StartStopHsql( String data0FilePath, String serverurl ){
        logger.info( "hsqlFilePath:{}, serverUrl:{}", data0FilePath, serverurl );

        HsqlProperties p = new HsqlProperties();

        //        'jdbc:hsqldb:hsql://localhost/db0'
        //        'jdbc:hsqldb:hsql://localhost:8988/db0'

        //localhost:8988/db0
        String afterSS = serverurl.split( "://" )[ 1 ];

        final String[] hostAndPort = afterSS.split( "/" );

        //localhost:8988
        if( hostAndPort[ 0 ].contains( ":" ) ){
            p.setProperty( "server.port", hostAndPort[ 0 ].split( ":" )[ 1 ] );
        }

        p.setProperty( "server.database.0", data0FilePath );
        p.setProperty( "server.dbname.0", hostAndPort[ 1 ] );

        server = new Server();
        try{
            server.setProperties( p );
            server.start();
            logger.info( "server {} started", server.getAddress() );
        }
        catch( Exception e ){
            logger.fatal( e );
            try{
                server.shutdown();
            }
            catch( Exception e1 ){
            }
            server = null;
            throw new Error( e );
        }
    }

    private Server server;

    @Override
    public void onApplicationEvent( ContextClosedEvent event ){
        if( server != null ){
            logger.info( "shuting down hsql:{}", server.getAddress() );
            try{
                server.shutdown();
            }
            catch( Exception e ){
                logger.warn( "error shut down:{}", ExceptionUtils.getStackTrace( e ) );
            }
        }

    }
}
