package org.pubanatomy.awsutils;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.util.Base64;
import org.pubanatomy.siutils.RPCServiceInvoker;
import com.flashflexpro.graniteamf.SimpleGraniteConfig;
import lombok.Data;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.io.File;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 3/1/2016
 * Time: 3:30 PM
 */
public class LambdaBase<T>{

    // logger used during static initialization
    private static Logger init_logger;

    // diagnostic counters and timestamps

    private static Integer s_constructorCount = 0;
    private static Integer s_initCount = 0;
    private static Date s_initTimestamp;
    private static Integer s_handlerCount = 0;
    private static Date s_firstHandlerTimestamp;


    private static ApplicationContext applicationContext;
    private static Throwable          initializeSpringError;
    private static AmazonEC2          ec2Client;

    // instance logger used during processing - mapped to sub-class instance
    protected Logger logger = LogManager.getLogger( this.getClass() );

    public LambdaBase() {
        s_constructorCount++;
        logger.info("s_constructorCount="+s_constructorCount);
    }

    public ApplicationContext getAppContext(){
        return applicationContext;
    }

    public static Throwable getInitializeSpringError(){
        return initializeSpringError;
    }

    public static AmazonEC2 getEc2Client(){
        return ec2Client;
    }

    public static String getContextXmlPath(){
        return contextXmlPath;
    }

    protected static String contextXmlPath = "/root-context.xml";


    static{

        s_initCount++;
        s_initTimestamp = new Date();
        System.out.println("**** LambdaBase - static initializer entered with s_initCount="+s_initCount);

        initLog();

        if( init_logger.isDebugEnabled() ){
            reportInstanceInfo();
        }

        initContext();

    }

    protected static void initLog() {

        System.setProperty( "log4j2.disable.jmx", "true");
        System.setProperty( "Log4jContextSelector ", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector" );

        init_logger = LogManager.getLogger( LambdaBase.class );

        Long loggerLoadedMs = (new Date()).getTime() - s_initTimestamp.getTime();
        init_logger.info("loggerLoadedMs={}", loggerLoadedMs);
    }

    /**
     * utility used during initialization to report information about the EC2 Instance we are running on
     */
    protected static void reportInstanceInfo() {

        try{
            ec2Client = AmazonEC2ClientBuilder.defaultClient();
            init_logger.debug( "********START*********\n{}", ec2Client.describeInstances().toString() );
        }
        catch( Throwable e ){
            init_logger.debug( "getting ec2 inst info failed:{}", e.getMessage() );
        }

        final Runtime runtime = Runtime.getRuntime();
        init_logger.info( "JVM:{}, availble cpu:{},mem free:{},max:{},total:{}",
                ManagementFactory.getRuntimeMXBean().getName(), runtime.availableProcessors(), runtime.freeMemory(),
                runtime.maxMemory(), runtime.totalMemory() );

        for( File root : File.listRoots() ){
            init_logger.info( ">>>>" + root.getName() + ">>>>" + "AbsolutePath:" + root.getAbsolutePath() + "; TotalSpace:" +
                    root.getTotalSpace() + "; FreeSpace:" + root.getFreeSpace() + "; UsableSpace:" +
                    root.getUsableSpace() + " <<<<" + root.getName() );
        }

    }

    protected static void initContext(){
        if( applicationContext != null ){
            init_logger.fatal( "context already initialized??-->>{}", contextXmlPath );
            throw new Error( "context already initialized??" );
        }
        try{
            long t = System.currentTimeMillis();
            init_logger.info( "initializing spring context start on {}>>>>>", t );
            applicationContext = new ClassPathXmlApplicationContext( contextXmlPath );
            init_logger.info( "spring context initialized <<<<< started on {}, cost:{}", t, System.currentTimeMillis() - t );
        }
        catch( Throwable e ){
            initializeSpringError = e;
            init_logger.error( "error initializing Spring context e:{} ", e );
        }
    }

    /**
     * optional utility to log handler metrics:
     *  - time from initialization to first handler invocation
     *  - number of invocations
     */
    protected void reportHandlerInvocation() {

        s_handlerCount++;

        if (s_handlerCount == 1) {
            s_firstHandlerTimestamp = new Date();
            long timeToFirstHandler = s_firstHandlerTimestamp.getTime() -  s_initTimestamp.getTime();
            logger.debug("timeToFirstHandler={}", timeToFirstHandler);
        }

        logger.debug("s_initCount={}, s_handlerCount={}", s_initCount, s_handlerCount);
    }


    /**
     * allows for consistent use of RPC-via-spring implementation in subclasses that
     * implement a handleRequest taking a ProxyInput
     * @param input
     * @return
     */
    public Map<String, Object> handleRpcInvokerRequest(ProxyInput input ){

        final HashMap<String, Object> rt = new HashMap<>();
        if( getInitializeSpringError() != null ){
            rt.put( "body", "CtxInitError" );
            rt.put( "statusCode", 500 );
            return rt;
        }

        if (input.isWarmup()) {
            logger.info("handling warmup!");
            rt.put("warmup", "ok");
            return rt;
        }

        try{
            String[] pathArr = input.getPath().split( "/" );

            final String csSessionId = pathArr[ pathArr.length - 1 ];
            final String methodName = pathArr[ pathArr.length - 2 ];
            final String serviceName = pathArr[ pathArr.length - 3 ];

            ThreadContext.put( "CsSessionId", csSessionId );

            rt.put( "body", rpcInvoker( csSessionId, serviceName, methodName, input.getBody() ) );
            rt.put( "statusCode", 200 );
        }
        catch( Throwable e ){
            logger.error( ExceptionUtils.getStackTrace( e ) );

            rt.put( "body", e.getMessage() );
            rt.put( "statusCode", 500 );
        }

        return rt;
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


    public String rpcInvoker( String csSessionId, String serviceName, String methodName, String input64 )
            throws Throwable{
        //todo: verify sessionId

        long rpcStart = System.currentTimeMillis();
        SimpleGraniteConfig graniteConfig = getAppContext().getBean( SimpleGraniteConfig.class );

        final byte[] decode = Base64.decode( input64.getBytes( "UTF-8" ) );
        final Object[] args = ( Object[] )graniteConfig.decode( ByteBuffer.wrap( decode ) );

        RPCServiceInvoker rpcServiceInvoker = getAppContext().getBean( RPCServiceInvoker.class );

        Map<String, Object> headers = new HashMap<>();
        headers.put( "csSessionId", csSessionId );
        headers.put( "serviceName", serviceName );
        headers.put( "methodName", methodName );

        final Message<Object[]> inputMsg = MessageBuilder.createMessage( args, new MessageHeaders( headers ) );
        Message rtMsg = rpcServiceInvoker.callServiceMethod( inputMsg );

        String rt;
        if( rtMsg == null ){
            rt = null;
        }
        else{
            rt = new String( Base64.encode( graniteConfig.encode( rtMsg.getPayload() ).array() ) );
        }

        init_logger.debug( "RPC exe cost:{}", System.currentTimeMillis() - rpcStart );
        return rt;
    }


    @Data
    public static class ProxyInput implements Serializable{


        public static final String WARMUP_RESOURCE = "warmup!";

        private String                  resource;
        private String                  path;
        private String                  httpMethod;
        private HashMap<String, String> headers;
        private HashMap<String, String> queryStringParameters;
        private HashMap<String, String> pathParameters;
        private HashMap<String, String> stageVariables;
        private HashMap<String, Object> requestContext;
        private String                  body;

        public Boolean isWarmup() {
            return WARMUP_RESOURCE.equals(resource);
        }

    }

}
