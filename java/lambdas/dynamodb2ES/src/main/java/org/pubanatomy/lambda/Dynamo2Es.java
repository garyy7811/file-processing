package org.pubanatomy.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import org.pubanatomy.awsutils.DynamoElasticSearch;
import org.pubanatomy.awsutils.LambdaBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.ThreadContext;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class Dynamo2Es extends LambdaBase<Object> implements RequestHandler<DynamodbEvent, String>{

    @Override
    public String handleRequest( DynamodbEvent input, Context context ){
        try{

            // cache AWSRequestId for use in log4j
            ThreadContext.put("AWSRequestId", context.getAwsRequestId());

            // diagnostic logging
            reportHandlerInvocation();

            logger.info( "{} records>>>>>", input.getRecords().size() );
            final String rt = getAppContext().getBean( DynamoElasticSearch.class ).handleRequest( input );
            logger.info( "{} records<<<<<", input.getRecords().size() );
            return rt;
        }
        catch( Throwable e ){
            logger.error( ExceptionUtils.getStackTrace( e ) );
            throw e;
        }
    }


    public static void main( String[] args ){
        new Dynamo2Es().getAppContext();
        System.out.println( "done" );
    }

}
