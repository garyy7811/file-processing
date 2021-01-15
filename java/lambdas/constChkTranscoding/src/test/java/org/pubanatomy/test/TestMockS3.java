package org.pubanatomy.test;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * User: GaryY
 * Date: 12/12/2016
 */
public class TestMockS3 extends AmazonS3Client{

    @Override
    public URL generatePresignedUrl( String bucketName, String key, Date expiration, HttpMethod method )
            throws SdkClientException{
        try{
            return new URL( "https", "host", 1234, "" );
        }
        catch( MalformedURLException e ){
            return null;
        }
    }
}
