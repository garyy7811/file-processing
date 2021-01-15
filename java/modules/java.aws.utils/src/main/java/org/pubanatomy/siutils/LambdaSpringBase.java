package org.pubanatomy.siutils;

import org.pubanatomy.awsutils.LambdaBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * User: flashflexpro@gmail.com
 * Date: 3/1/2016
 * Time: 3:30 PM
 */
public abstract class LambdaSpringBase<T> extends LambdaBase{

    protected static Logger init_logger;

    private static ApplicationContext applicationContext;

    // instance logger used during processing - mapped to sub-class instance
    protected Logger logger = LogManager.getLogger( this.getClass() );


    public ApplicationContext getAppContext(){
        return applicationContext;
    }


    public static String getContextXmlPath(){
        return contextXmlPath;
    }

    protected static String contextXmlPath = "/root-context.xml";

    static{
        initContext();
    }

    protected static void initContext(){

        System.setProperty( "log4j2.disable.jmx", "true");
        System.setProperty( "Log4jContextSelector ", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector" );
        init_logger = LogManager.getLogger( LambdaSpringBase.class );

        if( applicationContext != null ){
            init_logger.fatal( "context already initialized??-->>{}", contextXmlPath );
            throw new Error( "context already initialized??" );
        }

        long t = System.currentTimeMillis();
        init_logger.info( "initializing spring context start on {}>>>>>", t );
        applicationContext = new ClassPathXmlApplicationContext( contextXmlPath );

        init_logger.info( "spring context initialized <<<<< started on {}, cost:{}", t, System.currentTimeMillis() - t );

    }


    public void sendMessage( String channelName, Message msg ){
        sendAndReceiveMessage( channelName, msg, false );
    }

    public T sendAndReceiveMessage( String channelName, Message msg ){
        return sendAndReceiveMessage( channelName, msg, true );
    }

    public T sendAndReceiveMessage( String channelName, Message msg, boolean rt ){
        init_logger.debug( "LambdaBase.sendAndReceiveMessage >> channelName:{}, msg:{} ", channelName, msg );

        MessageChannel inCh = getAppContext().getBean( channelName, MessageChannel.class );
        MessagingTemplate msgTemp = new MessagingTemplate();
        Message<?> rtMsg = null;
        if( rt ){
            rtMsg = msgTemp.sendAndReceive( inCh, msg );
            init_logger.debug( "LambdaBase.sendAndReceiveMessage return:{}", rtMsg );
            return ( T )rtMsg;
        }

        msgTemp.send( inCh, msg );
        return null;
    }


}
