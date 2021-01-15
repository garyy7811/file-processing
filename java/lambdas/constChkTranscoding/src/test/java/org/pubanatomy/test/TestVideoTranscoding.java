package org.pubanatomy.test;

import org.pubanatomy.configPerClient.ConfigPerClientDAO;
import org.pubanatomy.configPerClient.DynaTableClientConfigTranscode;
import org.pubanatomy.configPerClient.DynaTableClientConfigTranscodeFormat;
import org.pubanatomy.videotranscoding.DynaTableVideoTranscodingMediaInfo;
import org.pubanatomy.videotranscoding.TranscodingFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 12/12/2016
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-test.xml" )
public class TestVideoTranscoding{

    @Value( "addMediaBenchMark.json" )
    private Resource addMediaBenchMark;
    @Value( "addMediaBenchMark_result.json" )
    private Resource addMediaBenchMark_result;
    @Value( "fromEncodingCom_finished.json" )
    private Resource fromEncodingCom_finished;
    @Value( "fromEncodingCom_saved.json" )
    private Resource fromEncodingCom_saved;
    @Value( "getMediaInfo.json" )
    private Resource getMediaInfo;
    @Value( "getMediaInfo_result.json" )
    private Resource getMediaInfo_result;
    @Value( "updateMedia.json" )
    private Resource updateMedia;
    @Value( "updateMedia_result.json" )
    private Resource updateMedia_result;

    @Test
    public void testFomratsFromMediaInfo() throws IOException{

        TranscodingFunctions.GetMediaInfoResponse mediaInfoResponse = new ObjectMapper()
                .readValue( getMediaInfo_result.getInputStream(), TranscodingFunctions.GetMediaInfoResponse.class );

        final DynaTableClientConfigTranscode clientConfigTranscode =
                configPerClientDAO.loadConfig( System.currentTimeMillis() + "a" ).getTranscode();

        final List<DynaTableClientConfigTranscodeFormat> formatCopies = clientConfigTranscode.getFormatCopies();
        formatCopies.stream().filter( i -> i.getOutput().equals( "mp4" ) ).sorted( ( a, b ) -> {
            Assert.assertNotEquals( a.getBitrate(), b.getBitrate() );
            return DynaTableClientConfigTranscodeFormat.getBitRateInK( a.getBitrate() ) >
                    DynaTableClientConfigTranscodeFormat.getBitRateInK( b.getBitrate() ) ? 1 : - 1;
        } ).collect( Collectors.toList() );

        final DynaTableVideoTranscodingMediaInfo transcodingMediaInfo = mediaInfoResponse.getResponse();

        List<DynaTableClientConfigTranscodeFormat> rsltFmts =
                functions.genFormatsByConfigAndMediaInfo( transcodingMediaInfo, clientConfigTranscode );

        Assert.assertEquals( 1, rsltFmts.stream().filter(
                i -> i.getOutput().equals( "mp4" ) && i.getBitrate().equals( transcodingMediaInfo.getBitrate() ) )
                .collect( Collectors.toList() ).size() );


        Assert.assertEquals( 0, rsltFmts.stream().filter( i -> i.getOutput().equals( "mp4" ) &&
                DynaTableClientConfigTranscodeFormat.getBitRateInK( i.getBitrate() ) >
                        DynaTableClientConfigTranscodeFormat.getBitRateInK( transcodingMediaInfo.getBitrate() ) )
                .collect( Collectors.toList() ).size() );

        Assert.assertEquals( 6,
                rsltFmts.stream().filter( i -> i.getOutput().equals( "mp4" ) ).collect( Collectors.toList() ).size() );

        Assert.assertEquals( formatCopies.stream().filter( i -> i.getOutput().equals( "mp4" ) &&
                DynaTableClientConfigTranscodeFormat.getBitRateInK( i.getBitrate() ) <
                        DynaTableClientConfigTranscodeFormat.getBitRateInK( transcodingMediaInfo.getBitrate() ) )
                .collect( Collectors.toList() ).size(), rsltFmts.stream().filter( i -> i.getOutput().equals( "mp4" ) &&
                DynaTableClientConfigTranscodeFormat.getBitRateInK( i.getBitrate() ) <
                        DynaTableClientConfigTranscodeFormat.getBitRateInK( transcodingMediaInfo.getBitrate() ) )
                .collect( Collectors.toList() ).size() );


        Assert.assertEquals(
                formatCopies.stream().filter( i -> i.getOutput().equals( "thumbnail" ) ).collect( Collectors.toList() )
                        .size(),
                rsltFmts.stream().filter( i -> i.getOutput().equals( "thumbnail" ) ).collect( Collectors.toList() )
                        .size() );

        transcodingMediaInfo.setBitrate( "99999k" );
        rsltFmts = functions.genFormatsByConfigAndMediaInfo( transcodingMediaInfo, clientConfigTranscode );

        final List<DynaTableClientConfigTranscodeFormat> formatCopiesBk = clientConfigTranscode.getFormatCopies();

        Assert.assertEquals( rsltFmts.size(), formatCopies.size() );

        final List<DynaTableClientConfigTranscodeFormat> cpRsltFmts = rsltFmts;
        formatCopies.forEach( i -> {
            if( cpRsltFmts.stream().anyMatch( j -> {
                if( i.getOutput().equals( j.getOutput() ) ){
                    if( i.getBitrate() != null && i.getBitrate().equals( j.getBitrate() ) ){
                        return true;
                    }
                    if( i.getSize() != null && i.getSize().equals( j.getSize() ) ){
                        return true;
                    }
                    if( i.getTime() != null && i.getTime().equals( j.getTime() ) ){
                        return true;
                    }
                    if( i.getWidth() != null && i.getWidth().equals( j.getWidth() ) ){
                        return true;
                    }
                }
                return false;
            } ) ){
                formatCopiesBk.remove( i );
            }
        } );

        Assert.assertEquals( 0, formatCopiesBk.size() );

        transcodingMediaInfo.setBitrate( "99k" );
        rsltFmts = functions.genFormatsByConfigAndMediaInfo( transcodingMediaInfo, clientConfigTranscode );
        Assert.assertEquals( 1,
                rsltFmts.stream().filter( i -> transcodingMediaInfo.getBitrate().equals( i.getBitrate() ) )
                        .collect( Collectors.toList() ).size() );

        Assert.assertEquals( 1,
                rsltFmts.stream().filter( i -> i.getOutput().equals( "mp4" ) ).collect( Collectors.toList() ).size() );

    }

    @Autowired
    private ConfigPerClientDAO configPerClientDAO;

    @Autowired
    private TranscodingFunctions functions;


}
