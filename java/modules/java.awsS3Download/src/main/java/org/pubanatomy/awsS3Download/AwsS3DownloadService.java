package org.pubanatomy.awsS3Download;

import com.amazonaws.util.Base64;
import com.amazonaws.util.IOUtils;
import org.pubanatomy.loginverify.DynaLogInSessionInfo;
import org.pubanatomy.loginverify.DynamoLoginInfoDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

/**
 * User: GaryY
 * Date: 8/19/2016
 */

@Service
public class AwsS3DownloadService{

    private static final Logger logger = LogManager.getLogger( AwsS3DownloadService.class );

    private byte[] derPrivateKey;

    private String keyPairId;

    public AwsS3DownloadService( Resource privateKeyDerFile, String rootUserId ) throws IOException{

        Security.addProvider( new org.bouncycastle.jce.provider.BouncyCastleProvider() );
        derPrivateKey = IOUtils.toByteArray( privateKeyDerFile.getInputStream() );

        keyPairId = privateKeyDerFile.getFilename().substring( 0, privateKeyDerFile.getFilename().length() - 4 );
        this.rootUserId = rootUserId;
    }

    @Autowired
    private DynamoLoginInfoDAO loginInfoFuncs;

    private String rootUserId;


    //todo: more controls
    public String[] signAll( @NotEmpty String csSessionId )
            throws UnsupportedEncodingException, InvalidKeySpecException, NoSuchProviderException,
            NoSuchAlgorithmException, InvalidKeyException, SignatureException, IllegalAccessException{

        DynaLogInSessionInfo logInSessionInfo = loginInfoFuncs.loadCsSessionInfo( csSessionId, true );

        String accessResPath = "*";
        /*
        if( !rootUserId.equals( logInSessionInfo.getUserId() ) ){
            accessResPath = "/" + logInSessionInfo.getClientId() + "*//*";
        }*/

        String policyJson =
                buildPolicyForSignedUrl( accessResPath, new Date( System.currentTimeMillis() + 60 * 1000 ), "0.0.0.0/0",
                        new Date( 0L ) );

        logger.info( policyJson );

        byte[] signatureBytes = signWithRsaSha1( derPrivateKey, policyJson.getBytes( "UTF-8" ) );

        return new String[]{ new String( Base64.encode( policyJson.getBytes( "UTF-8" ) ), "UTF-8" ),
                new String( Base64.encode( signatureBytes ), "UTF-8" ), keyPairId };
    }


    private static byte[] signWithRsaSha1( byte[] derPrivateKeyBytes, byte[] dataToSign )
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidKeySpecException,
            NoSuchProviderException{
        // Build an RSA private key from private key data
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec( derPrivateKeyBytes );
        KeyFactory keyFactory = KeyFactory.getInstance( "RSA" );
        RSAPrivateKey privateKey = ( RSAPrivateKey )keyFactory.generatePrivate( privSpec );

        // Sign data
        Signature signature = Signature.getInstance( "SHA1withRSA", "BC" );
        signature.initSign( privateKey, new SecureRandom() );
        signature.update( dataToSign );

        return signature.sign();
    }

    public static final String POLICY =
            "{\n" + "    \"Statement\": [\n" + "        {\n" + "            \"Resource\": \"${resPath}$\",\n" +
                    "            \"Condition\": {\n" + "                \"DateLessThan\": {\n" +
                    "                    \"AWS:EpochTime\": ${dateLess}$\n" + "                },\n" +
                    "                \"DateGreaterThan\": {\n" +
                    "                    \"AWS:EpochTime\": ${dateGreater}$\n" + "                },\n" +
                    "                \"IpAddress\": {\n" + "                    \"AWS:SourceIp\": \"${ipPath}$\"\n" +
                    "                }\n" + "            }\n" + "        }\n" + "    ]\n" + "}";

    public static String buildPolicyForSignedUrl( String resourcePath, Date epochDateLessThan,
                                                  String limitToIpAddressCIDR, Date epochDateGreaterThan ){
        if( resourcePath == null ){
            resourcePath = "*";
        }
        if( limitToIpAddressCIDR == null ){
            limitToIpAddressCIDR = "0.0.0.0/0";
        }
        if( epochDateLessThan == null ){
            epochDateLessThan = new Date( System.currentTimeMillis() + 48 * 3600 * 1000 );
        }

        if( epochDateGreaterThan == null ){
            epochDateGreaterThan = new Date( System.currentTimeMillis() );
        }

        return POLICY.replace( "${resPath}$", resourcePath ).
                replace( "${dateLess}$", epochDateLessThan.getTime() / 1000 + "" )
                .replace( "${dateGreater}$", epochDateGreaterThan.getTime() / 1000 + "" )
                .replace( "${ipPath}$", limitToIpAddressCIDR );
    }

}
