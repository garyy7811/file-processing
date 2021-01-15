package org.pubanatomy.test.unit;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/migrate-videobitrates.xml" )
public class TestTMP{


    @Value( "gr5000.json" )
    private Resource src_bitrates;


    @Value( "gr5000-m.json" )
    private Resource file_info;

    /**
     * {
     * "term": {
     * "s3BucketKey": "1000020/1000085/2EF6984386B8B19EBA426E58FF066C92/1489419759951"
     * }
     * }
     *
     * @throws IOException
     */

    @Test
    public void ssss() throws IOException{


        final ObjectMapper objectMapper = new ObjectMapper();
        List<Map> bitratesLst = objectMapper.readValue( src_bitrates.getFile(), List.class );

        List<Map> fileInfoLst = objectMapper.readValue( file_info.getFile(), List.class );

        List<Map> ggg = bitratesLst.stream().map( m -> {
            Map fl = ( Map )m.get( "fields" );
            List<String> lsk = ( List<String> )fl.get( "sourceKey" );
            List<Integer> lfb = ( List<Integer> )fl.get( "fileInfoLst.bitRate" );

            String key = lsk.get( 0 );
            Integer br = lfb.get( 6 );

            Map info = fileInfoLst.stream().filter( mf -> {
                return mf.get( "_id" ).equals( key );
            } ).findAny().get();


            final HashMap rt = new HashMap();

            Map<String, List> fields = ( Map )info.get( "fields" );

            rt.put( "clientId", fields.get( "clientId" ).get( 0 ) );
            rt.put( "fileRefName", fields.get( "fileRefName" ).get( 0 ) );
            rt.put( "userId", fields.get( "userId" ).get( 0 ) );
            final Long uploadedConfirmTimeStamp = ( Long )fields.get( "uploadedConfirmTimeStamp" ).get( 0 );
            rt.put( "uploadedConfirmTimeStamp", new Date( uploadedConfirmTimeStamp ).toString() );
            rt.put( "fileRefSizeBytes", fields.get( "fileRefSizeBytes" ).get( 0 ) );
            rt.put( "extraMsg", fields.get( "extraMsg" ).get( 0 ) );
            rt.put( "bitrate", br );

            return rt;
        } ).collect( Collectors.toList() );

        String rt = objectMapper.writeValueAsString( ggg );

        System.out.println( rt );

    }

}
