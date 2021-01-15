package org.pubanatomy.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.pubanatomy.awsutils.LambdaBase;
import org.pubanatomy.videotranscoding.TranscodingReportingService;
import com.customshow.videotranscoding.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.ThreadContext;
import org.pubanatomy.videotranscoding.api.*;

/**
 * Lambda function intended to be invoked directly by other java applications, targeting methods
 * on the TranscodingService and the TranscodingReportingService.
 */
public class JavaTranscodingService extends LambdaBase<String>
        implements RequestHandler<TranscodingServiceRequest, TranscodingServiceResponse>{

    @Override
    public TranscodingServiceResponse handleRequest( TranscodingServiceRequest input, Context context ){


        // cache AWSRequestId for use in log4j
        ThreadContext.put("AWSRequestId", context.getAwsRequestId());

        // diagnostic logging
        reportHandlerInvocation();

        logger.info( "got input: {}", input );

        ObjectMapper objectMapper = new ObjectMapper();

        TranscodingServiceResponse output = new TranscodingServiceResponse();
        Object responsePayloadInstance = null;

        try{

            // deserialize the request...
            logger.debug( "attempting to load class: {}", input.getRequestPayloadClass() );
            Class requestClass = Class.forName( input.getRequestPayloadClass() );
            logger.debug( "attempting to deserialize request:\n{}", input.getRequestPayloadJson() );
            Object requestPayload = objectMapper.readValue( input.getRequestPayloadJson(), requestClass );

            if( requestPayload instanceof HelloWorldRequest ){
                responsePayloadInstance = handleHelloWorld( ( HelloWorldRequest )requestPayload );
            }
            else if( requestPayload instanceof FetchTranscodingJobsRequest ){
                responsePayloadInstance = handleFetchTranscodingJobs( ( FetchTranscodingJobsRequest )requestPayload );
            }
            else if( requestPayload instanceof FetchJobsPerDayRequest ){
                responsePayloadInstance = handleFetchJobsPerDayRequest( ( FetchJobsPerDayRequest )requestPayload );
            }
            else{
                logger.warn( "could not identify request subclass!" );
                throw new Exception( "Unknown request type: " + input.getClass() );
            }

            // serialize the result
            if( responsePayloadInstance != null ){
                logger.debug( "successulf processing, serializing responsePayloadInstance: {}",
                        responsePayloadInstance );
                output.setStatus( TranscodingServiceResponse.STATUS_SUCCESS );
                output.setResponsePayloadClass( responsePayloadInstance.getClass().getName() );
                output.setResponsePayloadJson( objectMapper.writeValueAsString( responsePayloadInstance ) );
            }
            else{
                throw new Exception( "responsePayloadInstance is null for request: " + requestPayload );
            }


        }
        catch( Exception e ){
            logger.error( "Error processing request", e );
            output.setStatus( TranscodingServiceResponse.STATUS_ERROR );
            output.setErrorMessage( e.toString() );
        }

        logger.info( "returning output: {}", output );

        return output;
    }

    private FetchJobsPerDayResponse handleFetchJobsPerDayRequest( FetchJobsPerDayRequest requestPayload ){

        logger.info( "handleFetchJobsPerDayRequest got {}", requestPayload );
        TranscodingReportingService transcodingReportingService =
                ( TranscodingReportingService )getAppContext().getBean( "transcodingReportingService" );
        final FetchJobsPerDayResponse fetchJobsPerDayResponse =
                transcodingReportingService.fetchTranscodingJobsPerDay( requestPayload );
        logger.info( "handleFetchJobsPerDayRequest return {}", fetchJobsPerDayResponse );
        return fetchJobsPerDayResponse;

    }

    private HelloWorldResponse handleHelloWorld( HelloWorldRequest request ){

        logger.info( "handleHelloWorld got: {}", request );
        TranscodingReportingService transcodingReportingService =
                ( TranscodingReportingService )getAppContext().getBean( "transcodingReportingService" );
        HelloWorldResponse response = transcodingReportingService.doHelloWorld( request );
        logger.info( "handleHelloWorld returning: {}", request );

        return response;
    }

    private FetchTranscodingJobsResponse handleFetchTranscodingJobs( FetchTranscodingJobsRequest request )
            throws Exception{

        logger.debug( "processing FetchTranscodingJobs: {}", request );

        TranscodingReportingService transcodingReportingService =
                ( TranscodingReportingService )getAppContext().getBean( "transcodingReportingService" );

        FetchTranscodingJobsResponse response = transcodingReportingService.fetchTranscodingJobs( request );

        logger.debug( "returning {} items", response.getItems().size() );

        return response;
    }


    public static void main( String[] args ){
        long tmp = System.currentTimeMillis();
        System.out.println( "main entered at: " + tmp );

        JavaTranscodingService jts = new JavaTranscodingService();
        jts.getAppContext();
        long t = System.currentTimeMillis() - tmp;
        System.out.println( "completed context initiailzation in " + t + " ms" );

    }

}
