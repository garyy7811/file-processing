package org.pubanatomy.lambda;

import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import org.pubanatomy.awsutils.LambdaBase;
import org.pubanatomy.videotranscoding.DynaTableVideoTranscoding;
import org.pubanatomy.videotranscoding.TranscodingDAO;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.integration.support.MessageBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class DispatchingTranscodingEvent extends LambdaBase<Object> implements RequestHandler<DynamodbEvent, String>{



    @Override
    public String handleRequest( DynamodbEvent input, Context context ){

        // cache AWSRequestId for use in log4j
        ThreadContext.put("AWSRequestId", context.getAwsRequestId());

        // diagnostic logging
        reportHandlerInvocation();

        List<DynaTableVideoTranscoding> changedTranscodingRecord = new ArrayList<>();

        try{
            TranscodingDAO transcodingDAO = getAppContext().getBean( TranscodingDAO.class );

            logger.info( "change size:{} >>>>>", input.getRecords().size() );
            input.getRecords().forEach( s -> {
                StreamRecord change = s.getDynamodb();
                logger.debug( "change:{}", change.toString() );
                if( change.getNewImage() != null ){
                    change.getNewImage().forEach( ( k, v ) -> {
                        if( k.equals( "status" ) && v != null && s.getEventSourceARN()
                                .contains( "table/" + transcodingDAO.getAwsTranscodingDynamoTablename() ) ){
                            if( change.getOldImage() == null || ! v.equals( change.getOldImage().get( "status" ) ) ){
                                final String mediaId = change.getKeys().get( "mediaId" ).getS();
                                logger.info( "adding mediaId:{}, status: {}", mediaId, v );
                                DynaTableVideoTranscoding trancoRecord = transcodingDAO.getTranscodingDynamoMapper()
                                        .marshallIntoObject( DynaTableVideoTranscoding.class, change.getNewImage() );
                                changedTranscodingRecord.add( trancoRecord );
                            }
                        }
                    } );
                }
            } );

            if( changedTranscodingRecord.size() > 0 ){
                sendMessage( "onTranscodingStatusChanged",
                        MessageBuilder.withPayload( changedTranscodingRecord ).build() );
                logger.info( "onTranscodingStatusChanged:{}",
                        changedTranscodingRecord.stream().map( DynaTableVideoTranscoding::getMediaId )
                                .collect( Collectors.joining( ";" ) ) );
            }
            else{
                logger.info( "no changedTranscodingRecord change found !" );
            }
            logger.info( "change size:{} <<<<<", input.getRecords().size() );
            return "0";
        }
        catch( Throwable e ){
            logger.error( ExceptionUtils.getStackTrace( e ) );
            return "1";
        }
    }


    public static void main( String[] args ){
        TranscodingDAO transcodingDAO =
                new DispatchingTranscodingEvent().getAppContext().getBean( TranscodingDAO.class );
        System.out.println( transcodingDAO );
        /*
        List<Map<String, AttributeValue>> changedRecordKeyLst = new ArrayList<>();
        HashMap<String, AttributeValue> map = new HashMap<>();
        map.put( "csSessionId", new AttributeValue( "42D8E2C4-621F-1418-FE1B-D6D8394842DB" ) );
        map.put( "applyTimeStamp", new AttributeValue().withN( "1457629887891" ) );
        changedRecordKeyLst.add( map );
        new DispatchingTranscodingEvent()
                .sendMessage( "inputOnFileUploadConfirmed", MessageBuilder.withPayload( changedRecordKeyLst ).build() );
        System.out.println( "done" );*/
    }

}
