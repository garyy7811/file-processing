package org.pubanatomy.lambda;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.Base64;
import org.pubanatomy.awsutils.LambdaBase;
import org.pubanatomy.reporting.dto.DtoReportingAbstract;
import org.pubanatomy.reporting.dto.DtoReportingActivity;
import org.pubanatomy.reporting.dto.DtoReportingShowEnd;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashflexpro.graniteamf.SimpleGraniteConfig;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.granite.messaging.amf.io.RegexAMF3DeserializerSecurizer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class OnReportEventFromFlash implements RequestHandler<LambdaBase.ProxyInput, Map<String, Object>>{


    private static final ObjectMapper objectMapper = new ObjectMapper(  );

    private static SimpleGraniteConfig sgc;
    private static AmazonKinesis       amazonKinesis;

    private static String              kinesisStreamName;

    {

        try{
            final RegexAMF3DeserializerSecurizer amf3DeserializerSecurizer = new RegexAMF3DeserializerSecurizer();
            amf3DeserializerSecurizer.setParam( "(^com\\.customshow\\..+|^com\\.flashflexpro\\..+)" );
            sgc = new SimpleGraniteConfig( amf3DeserializerSecurizer );

            kinesisStreamName = System.getenv( "reportingKinesisStreamName" );

            String accessKey = System.getenv( "runtimeAwsAccessKeyId" );
            String secretKey = System.getenv( "runtimeAwsSecretAccessKey" );
            amazonKinesis = new AmazonKinesisClient(
                    new AWSStaticCredentialsProvider( new BasicAWSCredentials( accessKey, secretKey ) ) );
        }
        catch( Throwable e ){
            throw new Error( e );
        }
    }

    public static void onMessage( DtoReportingAbstract dto ) throws IOException{
        if( dto.getFlexClientId() == null || "".equals( dto.getFlexClientId() ) ){
            throw new RuntimeException( " illeagal partitionKey->" + dto.getShowSessionId() );
        }
        PutRecordRequest request = new PutRecordRequest();
        request.setStreamName( kinesisStreamName );
        request.setPartitionKey( dto.getFlexClientId() );
        request.setData( sgc.encode( dto ) );
        PutRecordResult rslt = amazonKinesis.putRecord( request );
    }

    @Override
    public Map<String, Object> handleRequest( LambdaBase.ProxyInput input, Context context ){
        context.getLogger().log( "handleRequest>>>" + input.toString() );

        final HashMap<String, Object> rt = new HashMap<>();

        try{
            //from Labyrinth
            if( input.getPath().toLowerCase().endsWith( "/terminate" ) ){
                DtoReportingShowEnd dtoReportingShowEnd = new DtoReportingShowEnd();
                dtoReportingShowEnd.setRecTimeStamp( System.currentTimeMillis() );
                dtoReportingShowEnd.setFlexClientId( input.getBody() );
                /**
                 * this is the key, we can't let it null, we don't have it because
                 * 1) it's generated on clientside and never send to server, and
                 * 2)the user exiting browser event can't be delivered to server for sure.
                 *
                 * but this is from serverside, and it is sure the termination
                 */
                dtoReportingShowEnd.setShowSessionId( input.getBody() + DtoReportingShowEnd.SESSION_TERMINATER );
                dtoReportingShowEnd.setTermination( true );
                onMessage( dtoReportingShowEnd );
            }
            else{
                String inputBody = input.getBody();

                //handle legacy-->>                _urlRequest.data = "{\"a\":\"" + cdr.toString() + "\"}";
                final TreeMap<String, String> tmpHeaderMap = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
                tmpHeaderMap.putAll( input.getHeaders() );
                if( "application/json".equalsIgnoreCase( tmpHeaderMap.get( "content-type" ) ) ){
                    try{
                        Map<String,String> a= objectMapper.readValue( inputBody, Map.class );
                        String tmp = a.get( "a" );
                        if( tmp != null && tmp.length() > 3 ){
                            inputBody = tmp;
                        }
                    }
                    catch( Throwable e ){
                        context.getLogger().log( "Json type" );
                    }
                }

                if( inputBody.startsWith( "{\"a\":\"" ) ){
                    inputBody = inputBody.substring( 6, inputBody.length() - 2 );
                }
                //handle legacy--<<


                Object decoded = sgc.decode( ByteBuffer.wrap( Base64.decode( inputBody.getBytes( "UTF-8" ) ) ) );
                if( input.getPath().toLowerCase().endsWith( "/useract" ) && decoded instanceof DtoReportingActivity ){
                    final DtoReportingActivity activity = ( DtoReportingActivity )decoded;
                    activity.setRecTimeStamp( System.currentTimeMillis() );
                    onMessage( activity );
                }
                else if( input.getPath().toLowerCase().endsWith( "/offline" ) && decoded instanceof Object[] ){
                    final Object[] offlineMsgs = ( Object[] )decoded;

                    long now = System.currentTimeMillis();
                    long gap = now - Long.parseLong( ( String )offlineMsgs[ 0 ] );

                    List<DtoReportingActivity> offlineMsgLst = new ArrayList<>( offlineMsgs.length - 1 );
                    for( int i = 1; i < offlineMsgs.length; i++ ){
                        DtoReportingActivity offlineDtoAct = ( DtoReportingActivity )offlineMsgs[ i ];
                        offlineDtoAct.setRecTimeStamp( offlineDtoAct.getClientTime() + gap );
                        offlineMsgLst.add( offlineDtoAct );
                    }

                    offlineMsgLst.sort( ( o1, o2 ) -> o1.getRecTimeStamp() > o2.getRecTimeStamp() ? 1 : - 1 );

                    for( DtoReportingActivity anOfflineMsgLst : offlineMsgLst ){
                        onMessage( anOfflineMsgLst );
                    }
                }
                else{
                    throw new Error( "Unknown input:" + decoded );
                }
            }
            rt.put( "statusCode", 200 );
        }
        catch( Throwable e ){
            context.getLogger().log( ExceptionUtils.getStackTrace( e ) );
            rt.put( "statusCode", 500 );
        }
        return rt;
    }


    public static void main( String[] args ){
        long r = System.currentTimeMillis();
        new OnReportEventFromFlash();
        long t = System.currentTimeMillis() - r;
        //        System.out.println( ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + t );
    }
}
