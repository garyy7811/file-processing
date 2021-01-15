package org.pubanatomy.amfspringmvc.utils; /**
 * User: flashflexpro@gmail.com
 * Date: 1/3/14
 * Time: 3:21 PM
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FlashSocketPolicyServer{

    private static final Logger log = LogManager.getLogger( FlashSocketPolicyServer.class );

    private static ServerSocket serverSock;
    private static boolean listening = true;
    private static Thread serverThread;
    private static String socketPolicy = "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy" +
            ".dtd\">\n" +
            "<cross-domain-policy> \n" +
            "   <site-control permitted-cross-domain-policies=\"master-only\"/>\n" +
            "   <allow-access-from domain=\"*\" to-ports=\"*\" />\n" +
            "</cross-domain-policy>\n";

    private int mapping843PortTo = 843;

    public int getMapping843PortTo(){
        return mapping843PortTo;
    }

    public void setMapping843PortTo( int mapping843PortTo ){
        this.mapping843PortTo = mapping843PortTo;
    }

    @PostConstruct
    public void contextInitialized(){
        serverThread = new Thread( new Runnable(){
            public void run(){
                try{
                    log.debug( "FlashSocketPolicyServer: Start listening:" + mapping843PortTo );
                    serverSock = new ServerSocket( mapping843PortTo );

                    while( listening ){
                        log.debug( "FlashSocketPolicyServer: Listening:" + mapping843PortTo );
                        final Socket sock = serverSock.accept();

                        Thread t = new Thread( new Runnable(){
                            public void run(){
                                try{
                                    log.debug( "FlashSocketPolicyServer: Handling Request..." );

                                    sock.setSoTimeout( 10000 );

                                    InputStream in = sock.getInputStream();

                                    byte[] buffer = new byte[ 23 ];

                                    if( in.read( buffer ) != -1 && ( new String( buffer ) ).startsWith(
                                            "<policy-file-request/>" ) ){
                                        log.debug( "FlashSocketPolicyServer: Serving Policy File..." );

                                        OutputStream out = sock.getOutputStream();

                                        out.write( socketPolicy.getBytes() );

                                        out.write( 0x00 );

                                        out.flush();
                                        out.close();
                                    }
                                    else{
                                        log.debug( "FlashSocketPolicyServer: Ignoring Invalid Request" );
                                        log.debug( "  " + ( new String( buffer ) ) );
                                    }

                                }
                                catch( Exception ex ){
                                    log.debug( "FlashSocketPolicyServer: Error: " + ex.toString() );
                                }
                                finally{
                                    try{ sock.close(); } catch( Exception ex2 ){}
                                }
                            }
                        } );
                        t.start();
                    }
                }
                catch( Exception ex ){
                    log.debug( "FlashSocketPolicyServer: Error: " + ex.toString() );
                }
            }
        } );
        serverThread.start();
    }

    @PreDestroy
    public void contextDestroyed(){
        log.debug( "FlashSocketPolicyServer: Shutting Down..." );

        if( listening ){
            listening = false;
        }

        if( !serverSock.isClosed() ){
            try{ serverSock.close(); } catch( Exception ex ){}
        }

    }
}
