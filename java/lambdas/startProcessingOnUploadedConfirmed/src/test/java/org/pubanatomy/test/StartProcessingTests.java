package org.pubanatomy.test;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.util.IOUtils;
import org.pubanatomy.awsS3Download.AwsS3DownloadDAO;
import org.pubanatomy.awsS3Upload.AwsS3UploadService;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.pubanatomy.videotranscoding.DynaTableVideoTranscoding;
import org.pubanatomy.videotranscoding.TranscodingDAO;
import org.pubanatomy.videotranscoding.TranscodingFunctions;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/22/2016
 * Time: 1:32 PM
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-test.xml" )
public class StartProcessingTests{

    @Value( "/tst.jpg" )
    private Resource tstJpgRes;

    private EncodingServlet encodingServlet;

    @Before
    public void beforeTest() throws Exception{

        ServletContextHandler servletContainer = new ServletContextHandler();
        encodingServlet = new EncodingServlet();
        servletContainer.addServlet( new ServletHolder( encodingServlet ), "/*" );

        encodingServer.setHandler( servletContainer );
        encodingServer.start();
    }

    @After
    public void afterTest() throws Exception{
        //        encodingServer.getHandler().destroy();
        encodingServer.stop();
        encodingServer.setHandler( null );
        encodingServer.setHandler( null );
    }


    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private Server encodingServer;

    @Autowired
    private TranscodingFunctions transfunc;


    @Autowired
    private TranscodingDAO transcodingDAO;


    private Jackson2JsonObjectMapper jsonMapper;

    @Autowired
    private AmazonDynamoDB dynamoDB;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private AmazonSQSClient awsSQS;

    @Autowired
    private AwsS3UploadService uploadService;


    @Test
    public void testUploadMp4() throws Exception{
        jsonMapper = transfunc.getObjJsonMapper();
        MessageChannel c = springContext.getBean( "uploadConfirmedFile", MessageChannel.class );
        final DynaTableAwsS3Upload s3Upload = jsonMapper.fromJson( "{\n" + "          \"version\": 3,\n" +
                "          \"csSessionId\": \"B9931F9EC7A5E670C6FB1191D4B15CBA\",\n" +
                "          \"fileRefSizeBytes\": 14.683518409729004,\n" +
                "          \"fileRefCreationDate\": 1475520490257,\n" +
                "          \"fileRefModificationDate\": 1468268347249,\n" + "          \"fileRefCreator\": null,\n" +
                "          \"fileRefName\": \"trunk9.mp4\",\n" + "          \"fileRefType\": \".mp4\",\n" +
                "          \"extraMsg\": null,\n" + "          \"errorMsg\": null,\n" +
                "          \"userId\": \"1\",\n" + "          \"clientId\": \"0\",\n" +
                "          \"s3Bucket\": \"cs-cloud-trunk--upload\",\n" +
                "          \"s3BucketKey\": \"0/1/B9931F9EC7A5E670C6FB1191D4B15CBA/1481043196681\",\n" +
                "          \"awSAccessKeyId\": \"aaabbbccc\",\n" +
                "          \"applyTimeStamp\": 1481043196681,\n" +
                "          \"uploadedByClientTime\": 1481043202438,\n" +
                "          \"uploadedConfirmTimeStamp\": 1481043202297\n" + "        }", DynaTableAwsS3Upload.class );

        final long currentTimeMillis = System.currentTimeMillis();
        s3Upload.setApplyTimeStamp( currentTimeMillis - 200 );
        s3Upload.setCsSessionId( "s3UploadSessionId" + s3Upload.getApplyTimeStamp() );
        s3Upload.setUploadedByClientTime( s3Upload.getApplyTimeStamp() - 100 );


        encodingServlet.setExpects( Arrays.asList( new EncodingExpect( "application/x-www-form-urlencoded",
                "{\"query\":{\"source\":\"https://host3232:1333file path 92831\",\"userid\":\"fakeUserIdForEncodingCom;\",\"userkey\":\"fakeUserkeyForEncodingCom;\",\"action\":\"AddMediaBenchmark\",\"notify_format\":\"json\",\"notify\":\"fakeUrlCalledByEncodingCom;\",\"notify_upload\":\"fakeUrlCalledByEncodingCom;\",\"notify_encoding_errors\":\"fakeUrlCalledByEncodingCom;\"}}",
                "application/x-www-form-urlencoded",
                "{\n" + "  \"response\": {\n" + "    \"message\": \"Added\",\n" + "    \"MediaID\": \"" +
                        currentTimeMillis + "\"\n" + "  }\n" + "}" ) ) );

        when( amazonS3
                .generatePresignedUrl( eq( s3Upload.getS3Bucket() ), eq( s3Upload.getS3BucketKey() ), any( Date.class ),
                        eq( HttpMethod.GET ) ) ).thenReturn( new URL( "https", "host3232", 1333, "file path 92831" ) );

        c.send( MessageBuilder.withPayload( s3Upload ).build() );

        DynaTableVideoTranscoding loaded = transcodingDAO.loadByMediaId( "" + currentTimeMillis );

        Assert.assertNotNull( loaded );
        Assert.assertNull( loaded.getMediaInfo() );
        Assert.assertNull( loaded.getFormats() );
        Assert.assertEquals( loaded.getUploadBucketKey(), s3Upload.getS3BucketKey() );
        Assert.assertEquals( loaded.getStatus(), TranscodingFunctions.Result.Status_new );
    }

    @Autowired
    private AwsS3DownloadDAO downloadFuncs;

    @Autowired
    private TranscodingFunctions transcodingFunctions;


    private static class EncodingServlet extends HttpServlet{
        @Override
        protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException{
            final String actalReqStr =
                    URLDecoder.decode( IOUtils.toString( req.getInputStream() ).substring( 5 ), "utf-8" );

            EncodingExpect c = expects.get( expectIndex );
            Assert.assertEquals( c.requestJson, actalReqStr );
            Assert.assertEquals( c.requestContentType, req.getContentType() );

            resp.setContentType( c.responseContentType );
            resp.getOutputStream().write( c.respondJson.getBytes() );

            expectIndex++;
        }

        private int expectIndex = 0;
        private List<EncodingExpect> expects;

        public EncodingExpect getCurrentExpect(){
            return expects.get( expectIndex );
        }

        public void setExpects( List<EncodingExpect> expects ){
            this.expects = expects;
        }

        public List<EncodingExpect> getExpects(){
            return expects;
        }
    }

    private static class EncodingExpect{
        public EncodingExpect( String requestJson, String respondJson ){
            this.requestJson = requestJson;
            this.respondJson = respondJson;
        }

        public EncodingExpect( String requestContentType, String requestJson, String responseContentType,
                               String respondJson ){
            this.requestContentType = requestContentType;
            this.requestJson = requestJson;
            this.responseContentType = responseContentType;
            this.respondJson = respondJson;
        }

        public String requestContentType;
        public String requestJson;
        public String responseContentType;
        public String respondJson;

    }

    public static class MockitFactory<T> implements FactoryBean<T>{

        public MockitFactory( Class<T> mockClass ){
            this.mockClass = mockClass;
        }

        private Class<T> mockClass;

        @Override
        public T getObject() throws Exception{
            return Mockito.mock( mockClass );
        }

        @Override
        public Class<T> getObjectType(){
            return mockClass;
        }

        @Override
        public boolean isSingleton(){
            return true;
        }
    }

}
