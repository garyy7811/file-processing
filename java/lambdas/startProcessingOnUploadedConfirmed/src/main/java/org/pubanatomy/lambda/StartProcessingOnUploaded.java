package org.pubanatomy.lambda;

import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import org.pubanatomy.awsS3Upload.AwsS3UploadDAO;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.pubanatomy.awsutils.LambdaBase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.integration.support.MessageBuilder;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class StartProcessingOnUploaded extends LambdaBase<Object> implements RequestHandler<DynamodbEvent, String>{

    @Override
    public String handleRequest( DynamodbEvent input, Context context ){
        try{
            // cache AWSRequestId for use in log4j
            ThreadContext.put("AWSRequestId", context.getAwsRequestId());

            // diagnostic logging
            reportHandlerInvocation();

            AwsS3UploadDAO uploadDAO = getAppContext().getBean( AwsS3UploadDAO.class );

            logger.info( " processing {} records>>>>", input.getRecords().size() );
            input.getRecords().stream().forEach( s -> {

                StreamRecord change = s.getDynamodb();
                logger.info( "change:{}", change.toString() );
                if( change.getNewImage() != null ){
                    change.getNewImage().forEach( ( k, v ) -> {
                        if( k.equals( "uploadedConfirmTimeStamp" ) && v != null && s.getEventSourceARN()
                                .contains( "table/" + uploadDAO.getAwsS3UploadDynamoTablename() ) ){
                            if( change.getOldImage() == null || change.getOldImage().get( k ) == null ){
                                DynaTableAwsS3Upload uploadFile = uploadDAO.getDynamoDBMapper()
                                        .marshallIntoObject( DynaTableAwsS3Upload.class, change.getNewImage() );
                                sendMessage( "uploadConfirmedFile", MessageBuilder.withPayload( uploadFile ).build() );
                            }
                        }
                    } );
                }
            } );
            logger.info( " processed {} records <<<<", input.getRecords().size() );
            return "0";
        }
        catch( Throwable e ){
            logger.error( ExceptionUtils.getStackTrace( e ) );
            //so that it will retry
            throw e;
        }

    }


    public static void main( String[] args ){
        new StartProcessingOnUploaded().getAppContext();
        /*
        List<Map<String, AttributeValue>> changedRecordKeyLst = new ArrayList<>();
        HashMap<String, AttributeValue> map = new HashMap<>();
        map.put( "csSessionId", new AttributeValue( "42D8E2C4-621F-1418-FE1B-D6D8394842DB" ) );
        map.put( "applyTimeStamp", new AttributeValue().withN( "1457629887891" ) );
        changedRecordKeyLst.add( map );
        new StartVideoEncodingOnUploaded()
                .sendMessage( "inputOnFileUploadConfirmed", MessageBuilder.withPayload( changedRecordKeyLst ).build() );
        System.out.println( "done" );*/
    }

}
