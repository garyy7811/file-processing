package org.pubanatomy.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpRequestExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class OnApiGatewayCall implements RequestStreamHandler{

    private static Logger logger = LogManager.getLogger( OnApiGatewayCall.class );


    public OnApiGatewayCall() throws IOException{
        if( host == null ){
            final String httpHostname = System.getenv( "httpHostname" );
            final String httpHostPort = System.getenv( "httpHostPort" );
            logger.info( "host>{}:{}", httpHostname, httpHostPort );
            host = new HttpHost( httpHostname, Integer.parseInt( httpHostPort ) );
        }
        if( conn == null ){
            final String httpClientBufferSize = System.getenv( "httpClientBufferSize" );
            logger.info( "httpClientBufferSize :{}", httpClientBufferSize );
            conn = new DefaultBHttpClientConnection( Integer.parseInt( httpClientBufferSize ) );
            if( ! conn.isOpen() ){
                Socket socket = new Socket( host.getHostName(), host.getPort() );
                conn.bind( socket );
            }
        }

        if( coreContext == null ){
            coreContext = HttpCoreContext.create();
            coreContext.setTargetHost( host );
        }
        if( serverPath == null ){
            serverPath = System.getenv( "httpServerPath" );
            logger.info( "serverPath:{}", serverPath );
        }
    }

    static HttpRequestExecutor          httpexecutor = new HttpRequestExecutor();
    static DefaultBHttpClientConnection conn         = null;
    static HttpCoreContext              coreContext  = null;
    static HttpHost                     host         = null;
    static String                       serverPath   = null;


    @Override
    public void handleRequest( InputStream input, OutputStream output, Context context ) throws IOException{

        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest( "POST", serverPath );
        request.setEntity( new InputStreamEntity( input ) );
        try{
            httpexecutor.execute( request, conn, coreContext ).getEntity().writeTo( output );
        }
        catch( HttpException e ){
            throw new Error( e );
        }
        finally{
            conn.close();
        }

    }

    @Override
    protected void finalize() throws Throwable{
        super.finalize();
        conn.close();
    }
}
