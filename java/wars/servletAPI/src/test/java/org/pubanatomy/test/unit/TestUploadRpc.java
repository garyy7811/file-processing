package org.pubanatomy.test.unit;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.util.Base64;
import org.pubanatomy.awsS3Upload.AwsS3UploadDAO;
import org.pubanatomy.awsS3Upload.AwsS3UploadService;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.pubanatomy.loginverify.DynaLogInSessionInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/27/2016
 * Time: 5:08 PM
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-test.xml" )
public class TestUploadRpc{

    @Autowired
    private MessageChannel inputChannel;


    @Autowired
    private PollableChannel outputChannel;

    @Autowired
    private AwsS3UploadDAO uploadDAO;

    @Autowired
    private AwsS3UploadService uploadService;


    @Autowired
    protected AWSCredentialsProvider credentialsProvider;


    @Autowired
    private AmazonDynamoDBClient dynamoDBClient;


    @Autowired
    private DynaLogInSessionInfo mockingDynaLogInSessionInfo;

    @Test
    public void testUploadAuthen() throws Exception{


        final Date fileCreationDate = new Date();
        final Date fileModifiedDate = new Date();

        final Object[] payload =
                { fileCreationDate, fileModifiedDate, "filecreator", "z5.mp3", 31415L, ".mp4", "extramessage" };
        final HashMap<String, Object> headersRaw = new HashMap<>();

        final String fakeCsSessionId = "fakeCsSessionId" + System.currentTimeMillis();
        mockingDynaLogInSessionInfo.setCsSessionId( fakeCsSessionId );

        headersRaw.put( "path", "anything" );
        headersRaw.put( "serviceName", "awsS3UploadService" );
        headersRaw.put( "methodName", "authenticateUploading" );
        headersRaw.put( "csSessionId", fakeCsSessionId );


        final MessageHeaders headers = new MessageHeaders( headersRaw );
        new MessagingTemplate().send( inputChannel, MessageBuilder.createMessage( payload, headers ) );

        Object[] authResult = ( Object[] )outputChannel.receive().getPayload();

        Assert.assertEquals( 7, authResult.length );


        DynaTableAwsS3Upload uploadRecordBeforeConfirm =
                uploadDAO.loadUpload( fakeCsSessionId, ( Long )authResult[ 5 ] );

        final String policyBase64 = ( String )authResult[ 3 ];

        final Jackson2JsonObjectMapper jackson2JsonObjectMapper = new Jackson2JsonObjectMapper();
        HashMap policyInMap =
                jackson2JsonObjectMapper.fromJson( new String( Base64.decode( policyBase64 ) ), HashMap.class );

        Assert.assertEquals( 2, policyInMap.size() );
        long timeGap = System.currentTimeMillis() -
                AwsS3UploadService.getFormat().parse( ( String )policyInMap.get( "expiration" ) ).getTime();

        System.out.println( "timeGap>>>::::" + timeGap );

        Assert.assertTrue( timeGap < - 10000 );

        List conditions = ( List )policyInMap.get( "conditions" );
        Assert.assertEquals( 5, conditions.size() );

        Assert.assertEquals( uploadService.getAwsS3UploadBucket(), ( ( Map )conditions.get( 0 ) ).get( "bucket" ) );
        Assert.assertEquals( uploadService.getAwsS3UploadBucket(), uploadRecordBeforeConfirm.getS3Bucket() );


        Assert.assertEquals( "private", ( ( Map )conditions.get( 2 ) ).get( "acl" ) );
        Assert.assertEquals(
                mockingDynaLogInSessionInfo.getClientId() + "/" + mockingDynaLogInSessionInfo.getUserId() + "/" +
                        fakeCsSessionId + "/" + authResult[ 5 ], ( ( Map )conditions.get( 1 ) ).get( "key" ) );
        Assert.assertEquals(
                mockingDynaLogInSessionInfo.getClientId() + "/" + mockingDynaLogInSessionInfo.getUserId() + "/" +
                        fakeCsSessionId + "/" + authResult[ 5 ], uploadRecordBeforeConfirm.getS3BucketKey() );

        List cd3 = ( List )conditions.get( 3 );
        Assert.assertEquals( "AWS upload to S3 have to have this", "starts-with", cd3.get( 0 ) );
        Assert.assertEquals( "AWS upload to S3 have to have this", "$Filename", cd3.get( 1 ) );
        Assert.assertEquals( "AWS upload to S3 have to have this", "", cd3.get( 2 ) );

        List cd4 = ( List )conditions.get( 4 );
        Assert.assertEquals( "AWS upload to S3 have to have this", "eq", cd4.get( 0 ) );
        Assert.assertEquals( "AWS upload to S3 have to have this", "$success_action_status", cd4.get( 1 ) );
        Assert.assertEquals( "AWS upload to S3 have to have this", "201", cd4.get( 2 ) );


        Mac hmac = Mac.getInstance( "HmacSHA1" );
        hmac.init( new SecretKeySpec( credentialsProvider.getCredentials().getAWSSecretKey().getBytes( "UTF-8" ),
                "HmacSHA1" ) );


        Assert.assertEquals( "signature wrong",
                new String( Base64.encode( hmac.doFinal( policyBase64.getBytes( "UTF-8" ) ) ), "UTF-8" ),
                authResult[ 4 ] );

        Assert.assertEquals( credentialsProvider.getCredentials().getAWSAccessKeyId(),
                uploadRecordBeforeConfirm.getAwSAccessKeyId() );
        Assert.assertEquals( mockingDynaLogInSessionInfo.getUserId(), uploadRecordBeforeConfirm.getUserId() );
        Assert.assertEquals( mockingDynaLogInSessionInfo.getClientId(), uploadRecordBeforeConfirm.getClientId() );

        Assert.assertEquals( authResult[ 5 ], uploadRecordBeforeConfirm.getApplyTimeStamp() );
        Assert.assertEquals( payload[ 6 ], uploadRecordBeforeConfirm.getExtraMsg() );
        Assert.assertEquals( new Long( fileCreationDate.getTime() ),
                uploadRecordBeforeConfirm.getFileRefCreationDate() );
        Assert.assertEquals( new Long( fileModifiedDate.getTime() ),
                uploadRecordBeforeConfirm.getFileRefModificationDate() );
        Assert.assertEquals( payload[ 3 ], uploadRecordBeforeConfirm.getFileRefName() );
        Assert.assertEquals( payload[ 2 ], uploadRecordBeforeConfirm.getFileRefCreator() );
        Assert.assertEquals( payload[ 4 ], uploadRecordBeforeConfirm.getFileRefSizeBytes() );
        Assert.assertEquals( payload[ 5 ], uploadRecordBeforeConfirm.getFileRefType() );

        Assert.assertNull( uploadRecordBeforeConfirm.getUploadedByClientTime() );
        Assert.assertNull( uploadRecordBeforeConfirm.getUploadedConfirmTimeStamp() );
        Assert.assertNull( uploadRecordBeforeConfirm.getErrorMsg() );

        uploadService.uploadResult( fakeCsSessionId, ( Long )authResult[ 5 ], null, null, null );


        DynaTableAwsS3Upload uploadRecordAfterConfirm =
                uploadDAO.loadUpload( fakeCsSessionId, ( Long )authResult[ 5 ] );

        Assert.assertNotNull( uploadRecordAfterConfirm.getUploadedByClientTime() );

        uploadService.uploadResult( fakeCsSessionId, ( Long )authResult[ 5 ], "errorId", "errorMsg", "errorTxt" );

        uploadRecordAfterConfirm = uploadDAO.loadUpload( fakeCsSessionId, ( Long )authResult[ 5 ] );

        Assert.assertNotNull( uploadRecordAfterConfirm.getErrorMsg() );

        uploadRecordBeforeConfirm.setVersion( uploadRecordAfterConfirm.getVersion() );
        uploadRecordBeforeConfirm.setErrorMsg( uploadRecordAfterConfirm.getErrorMsg() );
        uploadRecordBeforeConfirm.setUploadedByClientTime( uploadRecordAfterConfirm.getUploadedByClientTime() );

        Assert.assertEquals( jackson2JsonObjectMapper.toJson( uploadRecordAfterConfirm ),
                jackson2JsonObjectMapper.toJson( uploadRecordBeforeConfirm ) );


    }
/* http://api.encoding.com/#SubAccountManagement_Request
    @Autowired
    private TranscodingFunctions transcodingFunctions;

    @Test
    public void testEncoding() throws Exception{

        final Hashtable<Object, Object> root = new Hashtable<>();
        final Hashtable<Object, Object> query = new Hashtable<>();

        query.put( "userid", "6198" );
        query.put( "userkey", "8e14717b948d739e4d1e6f7a1a194f97" );
        query.put( "action", "AddSubUser" );
        final HashMap<Object, Object> userData = new HashMap<>();
        userData.put( "email", "gary.yang@cs.cc" );
        userData.put( "login", "gary.yang@cs.cc" );
        userData.put( "password", "qwerqasdf" );
        userData.put( "first_name", "garyy" );
        userData.put( "last_name", "yy" );
        query.put( "user_data", userData );



        root.put( "query", query);
        Object rt = transcodingFunctions.sendInJson( root, Hashtable.class );
        System.out.println( rt );
    }
*/

}
