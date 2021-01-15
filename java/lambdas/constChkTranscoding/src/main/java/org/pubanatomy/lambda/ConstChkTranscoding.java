package org.pubanatomy.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.pubanatomy.awsutils.LambdaBase;
import org.pubanatomy.videotranscoding.TranscodingFunctions;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class ConstChkTranscoding extends LambdaBase<String> implements RequestStreamHandler{

    @Override
    public void handleRequest( InputStream input, OutputStream output, Context context ) throws IOException{

        // cache AWSRequestId for use in log4j
        ThreadContext.put("AWSRequestId", context.getAwsRequestId());

        reportHandlerInvocation();

        try{
            sendMessage( "inputUpdateStatusChannel",
                    MessageBuilder.withPayload( TranscodingFunctions.Result.STATUS_retry_421 ).build() );
            sendMessage( "inputUpdateStatusChannel",
                    MessageBuilder.withPayload( TranscodingFunctions.Result.STATUS_processing ).build() );
            sendMessage( "inputUpdateStatusChannel",
                    MessageBuilder.withPayload( TranscodingFunctions.Result.STATUS_readToProcess ).build() );
            sendMessage( "inputUpdateStatusChannel",
                    MessageBuilder.withPayload( TranscodingFunctions.Result.Status_new ).build() );
        }
        catch( Throwable e ){
            logger.error( ExceptionUtils.getStackTrace( e ) );
            throw e;
        }

    }

    public static void main( String[] args ){
        Message<String> msg = MessageBuilder.withPayload( TranscodingFunctions.Result.STATUS_error ).build();
        new ConstChkTranscoding().sendMessage( "inputUpdateStatusChannel", msg );
    }
}
