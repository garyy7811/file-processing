package org.pubanatomy.siutils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.granite.config.GraniteConfig;
import org.granite.messaging.amf.io.convert.NoConverterFoundException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import javax.validation.*;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User: flashflexpro@gmail.com
 * Date: 3/1/2016
 * Time: 3:30 PM
 */
public class RPCServiceInvoker implements ApplicationContextAware{

    public static final ValidatorFactory defaultValidatorFactory = Validation.buildDefaultValidatorFactory();

    public RPCServiceInvoker( Map<String, Object> serviceBeanMap ){
        this.serviceBeanMap = serviceBeanMap;
    }

    private Map<String, Object> serviceBeanMap;

    protected static Logger logger = LogManager.getLogger( RPCServiceInvoker.class );

    public void dumpServiceBeanMap(){
        logger.info( "{} services registered", serviceBeanMap.size() );
        for( String serviceName : serviceBeanMap.keySet() ){
            logger.info( "{}={}", serviceName, serviceBeanMap.get( serviceName ) );
        }
    }

    //todo: remove unnecessary spring integration dependency
    public Message callServiceMethod( Message<Object[]> inputMsg ) throws Throwable{
        String serviceName = ( String )inputMsg.getHeaders().get( "serviceName" );
        String methodName = ( String )inputMsg.getHeaders().get( "methodName" );

        logger.debug( "{},{}", serviceName, methodName );

        Object csSessionId = inputMsg.getHeaders().get( "csSessionId" );
        if( csSessionId == null || ! ( csSessionId instanceof String ) ){
            logger.warn( " no csSessionId " );
            return MessageBuilder.withPayload( new RpcError( 401, "HeaderHasNoSessionId" ) ).build();
        }

        Object serviceBean = serviceBeanMap.get( serviceName );

        if( serviceBean == null ){
            logger.error( "service bean not found for {}" + serviceName );
            return MessageBuilder.withPayload( new RpcError( 405, "service=null" ) ).build();
        }

        Object[] payLoad = inputMsg.getPayload();
        if( payLoad == null ){
            payLoad = new Object[ 1 ];
            payLoad[ 0 ] = csSessionId;
        }
        else{
            payLoad = ArrayUtils.add( payLoad, 0, csSessionId );
        }


        Method method = null;
        Object rt = null;
        try{
            method = findMethod( serviceBean.getClass(), methodName, payLoad );
            if( method == null ){
                logger.error( "failed to locate method:{},{},{},", serviceBean.getClass().getCanonicalName(),
                        methodName, payLoad );
                return MessageBuilder.withPayload( new RpcError( 405, "method=null" ) ).build();
            }
            Validator validator = defaultValidatorFactory.getValidator();

            Set<ConstraintViolation<Object>> validateResults =
                    validator.forExecutables().validateParameters( serviceBean, method, payLoad );
            if( validateResults.size() > 0 ){
                String s = validateResults.stream().map( c -> c.getMessage() ).collect( Collectors.joining( ";\r\n" ) );
                return MessageBuilder.withPayload( new RpcError( 415, "invalid:" + s ) ).build();
            }

            rt = method.invoke( serviceBean, payLoad );
        }
        catch( InvocationTargetException ie ){
            Throwable e;
            e = ie.getCause();
            logger.warn( ExceptionUtils.getStackTrace( e ) );
            int co = 500;
            if( e instanceof IllegalAccessException ){
                co = 403;
            }
            else if( e instanceof ValidationException || e instanceof IllegalArgumentException ||
                    e instanceof IllegalStateException ){
                co = 400;
            }
            else if( e instanceof UnknownServiceException || e instanceof FileNotFoundException ){
                co = 404;
            }
            rt = new RpcError( co, e.getMessage() == null ? e.toString() : e.getMessage() );
        }
        return MessageBuilder.withPayload( rt == null ? "" : rt ).copyHeaders( inputMsg.getHeaders() ).build();
    }


    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException{
        this.applicationContext = applicationContext;
    }


    public static final HashMap<Class, HashMap<String, HashMap<Class[], Method>>> class2Method2paramsMap =
            new HashMap<>();

    @Autowired
    private GraniteConfig graniteConfig;

    private Method findMethod( Class clazz, String methodName, Object[] params ){
        HashMap<String, HashMap<Class[], Method>> method2paramsMap = class2Method2paramsMap.get( clazz );
        if( method2paramsMap == null ){
            method2paramsMap = new HashMap<>();
            class2Method2paramsMap.put( clazz, method2paramsMap );
            for( int i = 0; i < clazz.getMethods().length; i++ ){
                Method method = clazz.getMethods()[ i ];
                HashMap<Class[], Method> paramsMap =
                        method2paramsMap.computeIfAbsent( method.getName(), k -> new HashMap<>() );

                method.getParameterAnnotations();
                paramsMap.put( method.getParameterTypes(), method );
            }
        }

        HashMap<Class[], Method> param2Method = method2paramsMap.get( methodName );
        if( param2Method == null ){
            logger.debug( "can't find method:{}::{}", clazz.getCanonicalName(), methodName );
            return null;
        }

        nextParamsToMethod:
        for( Map.Entry<Class[], Method> p2mEntry : param2Method.entrySet() ){
            if( p2mEntry.getKey().length == params.length ){
                for( int i = 0; i < p2mEntry.getKey().length; i++ ){
                    if( params[ i ] != null && ! params[ i ].getClass().isAssignableFrom( p2mEntry.getKey()[ i ] ) ){
                        try{
                            //                            p2mEntry.getValue().getAnnotations() todo: authentication with Spring Security before convert!
                            params[ i ] = graniteConfig.getConverters().convert( params[ i ], p2mEntry.getKey()[ i ] );
                        }
                        catch( NoConverterFoundException e ){
                            logger.warn( "method:{}::{}, with params[{}] type mismatch p:{}<-!->m:{}",
                                    clazz.getCanonicalName(), methodName, i, params[ i ].getClass(),
                                    p2mEntry.getKey().getClass() );
                            continue nextParamsToMethod;
                        }
                    }
                }
                return p2mEntry.getValue();
            }
        }
        logger.warn( "{} method:{}::{} have not matched", param2Method.size(), clazz.getCanonicalName(), methodName );
        return null;
    }

}
