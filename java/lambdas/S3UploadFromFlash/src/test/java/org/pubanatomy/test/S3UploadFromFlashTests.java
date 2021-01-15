package org.pubanatomy.test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.pubanatomy.awsS3Download.AwsS3DownloadDAO;
import org.pubanatomy.awsS3Download.DynaTableNVResource;
import org.pubanatomy.awsS3Upload.AwsS3UploadService;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.pubanatomy.processImgFla.ProcessImg;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.InputStream;

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
public class S3UploadFromFlashTests {

    @Value( "/tst.jpg" )
    private Resource tstJpgRes;


    @Autowired
    private ApplicationContext springContext;

    private Jackson2JsonObjectMapper jsonMapper;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private AwsS3UploadService uploadService;


    @Autowired
    private AwsS3DownloadDAO downloadDAO;

    @Autowired
    private ProcessImg processImgFla;


    // GREG: commenting out tesetUploadJpg, since we are no longer processing images
    // from this lambda function, but rather processing them from S3UploadFromFlash
    @Test
    public void testUploadJpg() throws Exception{

        jsonMapper = getObjJsonMapper();

        final DynaTableAwsS3Upload s3Upload = jsonMapper.fromJson( "{\n" + "          \"version\": 3,\n" +
                "          \"csSessionId\": \"B9931F9EC7A5E670C6FB1191D4B15CBA\",\n" +
                "          \"fileRefSizeBytes\": 14.683518409729004,\n" +
                "          \"fileRefCreationDate\": 1475520490257,\n" +
                "          \"fileRefModificationDate\": 1468268347249,\n" + "          \"fileRefCreator\": null,\n" +
                "          \"fileRefName\": \"trunk9.jpg\",\n" + "          \"fileRefType\": \".jpg\",\n" +
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
        s3Upload.setS3Bucket( uploadService.getAwsS3UploadBucket() );

        final String imageKey = "image/" + s3Upload.getS3BucketKey() + "/image.jpg";
        final String thumbKey = "thumbnail/" + s3Upload.getS3BucketKey() + "/thumb.jpg";

        final S3Object mockS3Obj = Mockito.mock( S3Object.class );
        final ObjectMetadata mockObjMetadata = Mockito.mock( ObjectMetadata.class );
        when( mockObjMetadata.getContentLength() ).thenReturn( tstJpgRes.contentLength() );
        when( mockObjMetadata.clone() ).thenReturn( mockObjMetadata );

        when( mockS3Obj.getObjectContent() ).thenReturn( new S3ObjectInputStream( tstJpgRes.getInputStream(), null ) );
        when( mockS3Obj.getObjectMetadata() ).thenReturn( mockObjMetadata );
        when( amazonS3.getObject( s3Upload.getS3Bucket(), s3Upload.getS3BucketKey() ) ).thenReturn( mockS3Obj );
        when( amazonS3.putObject( eq( processImgFla.getAwsS3DownloadBucket() ), eq( thumbKey ),
                any( InputStream.class ), any( ObjectMetadata.class ) ) ).thenReturn( new PutObjectResult() );


        DynaTableNVResource nvResRecord =        processImgFla.onUploadImgConfirmed(s3Upload);

        verify( amazonS3 ).copyObject( any( CopyObjectRequest.class ));

        verify( amazonS3 ).putObject( eq( processImgFla.getAwsS3DownloadBucket() ), eq( thumbKey ),
                any( InputStream.class ), any( ObjectMetadata.class ) );

//        verify( awsSQS ).sendMessage( "", "" );


        DynaTableNVResource loadedRes = downloadDAO.getDynamoDBMapper()
                .load( DynaTableNVResource.class, s3Upload.getS3BucketKey(), nvResRecord.getProcessId() );

        Assert.assertEquals( nvResRecord, loadedRes );
        Assert.assertFalse( nvResRecord == loadedRes );
        Assert.assertFalse( imageKey.contains( "//" ) );
        Assert.assertFalse( thumbKey.contains( "//" ) );
    }


    public Jackson2JsonObjectMapper getObjJsonMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        objectMapper.configure( DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true );
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        objectMapper.configure( DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false );
        return new Jackson2JsonObjectMapper( objectMapper );
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
