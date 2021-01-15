package org.pubanatomy.test.unit;

import com.amazonaws.util.Base64;
import org.pubanatomy.awsS3Download.AwsS3DownloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashflexpro.graniteamf.SimpleGraniteConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
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
public class TestDownloadRpc{

    @Autowired
    private AwsS3DownloadService downloadService;

    @Test
    public void testDownloadAuthPolicy()
            throws SignatureException, IOException, NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, InvalidKeySpecException{
        String allNull = AwsS3DownloadService.buildPolicyForSignedUrl( null, null, null, null );

        Assert.assertFalse( allNull.contains( "$" ) );
        final Date epochDateGreaterThan = new Date();
        String singleGreaterThan =
                AwsS3DownloadService.buildPolicyForSignedUrl( null, null, null, epochDateGreaterThan );

        final Long epoGreater = epochDateGreaterThan.getTime() / 1000;
        final Long epoLess = epochDateGreaterThan.getTime() / 1000 + 48 * 3600;

        Assert.assertFalse( singleGreaterThan.contains( "$" ) );
        Assert.assertTrue( singleGreaterThan.contains( epoGreater + "" ) );
        Assert.assertTrue( singleGreaterThan.contains( epoLess + "" ) );
        Assert.assertTrue( singleGreaterThan.indexOf( epoGreater + "" ) > singleGreaterThan.indexOf( epoLess + "" ) );


        Map mp = new ObjectMapper().readValue( singleGreaterThan, HashMap.class );

        Map statmt = ( Map )( ( List )mp.get( "Statement" ) ).get( 0 );

        Assert.assertEquals( "*", statmt.get( "Resource" ) );

        Map cdtn = ( Map )statmt.get( "Condition" );

        Map greaterThan = ( Map )cdtn.get( "DateGreaterThan" );

        Assert.assertEquals( epoGreater.toString(), greaterThan.get( "AWS:EpochTime" ).toString() );

        Map lessThan = ( Map )cdtn.get( "DateLessThan" );

        Assert.assertEquals( epoLess.toString(), lessThan.get( "AWS:EpochTime" ).toString() );

        Map ipadr = ( Map )cdtn.get( "IpAddress" );
        Assert.assertEquals( "0.0.0.0/0", ipadr.get( "AWS:SourceIp" ) );

    }

    @Autowired
    private SimpleGraniteConfig simpleGraniteConfig;

    @Test
    public void testAMFdeserializing() throws IOException{
        String input64 = "CRMBBIElBIEmBIEjBIEkBIEnBAcEgSEEgRkEgRo=";
        final byte[] decode = Base64.decode( input64.getBytes( "UTF-8" ) );
        final Object obj = simpleGraniteConfig.decode( ByteBuffer.wrap( decode ) );

        Object[] objArr = ( Object[] )obj;

        Assert.assertArrayEquals( new Object[]{ 165, 166, 163, 164, 167, 7, 161, 153, 154 }, objArr );


    }


}
