package org.pubanatomy.test.unit;

import org.pubanatomy.configPerClient.ConfigPerClientDAO;
import org.pubanatomy.configPerClient.DynaTableClientConfig;
import org.pubanatomy.configPerClient.DynaTableClientConfigTranscodeFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 2/20/2017
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/migrate-videobitrates.xml" )
public class TestMigrateVideoBitrates{


    @Autowired
    private ConfigPerClientDAO clientConfigDAO;


    @Value( "test-env-SELECT_clineId__videoBitrates__h264prof.json" )
    private Resource sourceJson;

    /**
     * this is only supposed to run once locally to migrate multi bitrate configurations into Dynamodb;
     * Dump from MySQL to Json, and use it as input of this "test":
     * <p>
     * <p>
     * SELECT C.id, CC.video_bitrates, CC.h264_profile
     * FROM Magnet.Client C
     * INNER JOIN Magnet.ConversionConfig CC ON C.conversion_config_id = CC.id
     * <p>
     * MAKE SURE YOUR ENVs matches!!!!
     *
     * @throws IOException
     */
    @Test
    public void migrateVideoBitrates() throws IOException{
        if( true ){
            return;
        }

        final ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> srcConfigLst = objectMapper.readValue( sourceJson.getInputStream(), List.class );

        final List<String> processedLst = srcConfigLst.stream().filter( m -> {
            return ( m.get( "id" ) != null &&
                    ( m.get( "video_bitrates" ) != null || m.get( "h264_profile" ) != null ) );
        } ).map( m -> {
            final Object h264_profile = m.get( "h264_profile" );
            DynaTableClientConfig config = clientConfigDAO.loadConfig( m.get( "id" ).toString() );
            final Map<String, DynaTableClientConfigTranscodeFormat> fmtConfig = config.getTranscode().getFormats();

            DynaTableClientConfigTranscodeFormat defautVideo =
                    fmtConfig.get( DynaTableClientConfigTranscodeFormat.DEFAULT_VIDEO );

            final String bitRatesStr = ( String )m.get( "video_bitrates" );
            if( bitRatesStr != null ){

                config.getTranscode().getFormatCopies().stream().filter( v -> {
                    return ( v.getOutput().equalsIgnoreCase( "mp4" ) &&
                            ! DynaTableClientConfigTranscodeFormat.DEFAULT_VIDEO.equals( v.getIdentification() ) );
                } ).forEach( v -> {
                    fmtConfig.remove( v.getIdentification() );
                } );

                Arrays.stream( bitRatesStr.split( "," ) ).forEach( num -> {
                    if( ! defautVideo.getBitrate().equalsIgnoreCase( num + "k" ) ){
                        DynaTableClientConfigTranscodeFormat tmp = new DynaTableClientConfigTranscodeFormat();
                        tmp.setBaseConfigId( defautVideo.getIdentification() );
                        tmp.setIdentification( defautVideo.getIdentification() + num );
                        tmp.setConfigName( defautVideo.getIdentification() + num );
                        tmp.setBitrate( num + "k" );
                        if( h264_profile != null && h264_profile instanceof String ){
                            tmp.setProfile( ( String )h264_profile );
                        }
                        fmtConfig.put( tmp.getIdentification(), tmp );
                    }
                } );
            }
            else{
                defautVideo.setProfile( ( String )h264_profile );
            }

            clientConfigDAO.saveClientConfig( config );

            return config.getClientId();
        } ).collect( Collectors.toList() );

        System.out.println( processedLst.stream().collect( Collectors.joining( "," ) ) );
    }

}
