package org.pubanatomy.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.pubanatomy.awsutils.LambdaBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.integration.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class OnCallFromEncodingCom extends LambdaBase<String>
        implements RequestHandler<LambdaBase.ProxyInput, Map<String, Object>>{

    @Override
    public Map<String, Object> handleRequest( ProxyInput input, Context context ){


        // cache AWSRequestId for use in log4j
        ThreadContext.put("AWSRequestId", context.getAwsRequestId());

        // diagnostic logging
        reportHandlerInvocation();

        final HashMap<String, Object> rt = new HashMap<>();
        try{

            // special case of checking for warmup event
            if (input.isWarmup()) {
                logger.info("handling warmup!");
                rt.put("warmup", "ok");
                return rt;
            }

            sendMessage( "inputCallFromEncodingComChannel", MessageBuilder.withPayload( input.getBody() ).build() );
            rt.put( "statusCode", 200 );
        }
        catch( Throwable e ){
            logger.error( ExceptionUtils.getStackTrace( e ) );
            rt.put( "statusCode", 500 );
        }

        rt.put( "body", "0" );
        return rt;
    }

    public static void main( String[] args ){
        long tmp = System.currentTimeMillis();
        new OnCallFromEncodingCom().getAppContext();
        long t = System.currentTimeMillis() - tmp;
        System.out.println( t );
    }

}
