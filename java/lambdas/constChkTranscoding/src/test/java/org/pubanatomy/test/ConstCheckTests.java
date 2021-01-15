package org.pubanatomy.test;

import com.amazonaws.util.IOUtils;
import org.pubanatomy.configPerClient.ConfigPerClientDAO;
import org.pubanatomy.configPerClient.DynaTableClientConfig;
import org.pubanatomy.videotranscoding.DynaTableVideoTranscoding;
import org.pubanatomy.videotranscoding.DynaTableVideoTranscodingMediaInfo;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
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
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/22/2016
 * Time: 1:32 PM
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-test.xml" )
public class ConstCheckTests{


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
    private ConfigPerClientDAO configPerClientDAO;

    @Autowired
    private TranscodingFunctions transfunc;


    @Autowired
    private TranscodingDAO transcodingDAO;


    private Jackson2JsonObjectMapper jsonMapper;

    @Test
    public void testRetryAndReady() throws Exception{
        jsonMapper = transfunc.getObjJsonMapper();

        final DynaTableVideoTranscodingMediaInfo mediaInfo = new DynaTableVideoTranscodingMediaInfo();
        mediaInfo.setSize( "1920x1080" );
        mediaInfo.setBitrate( "1000k" );

        DynaTableVideoTranscoding transcodeRecord = new DynaTableVideoTranscoding();
        transcodeRecord.setMediaInfo( mediaInfo );
        transcodeRecord.setStatus( TranscodingFunctions.Result.STATUS_retry_421 );
        transcodeRecord.setMediaId( "yeah" + System.currentTimeMillis() );
        transcodeRecord.setLastUpdateTime( System.currentTimeMillis() - 5 * 60000 );

        DynaTableClientConfig clientConfig = configPerClientDAO.loadConfig( "-2" );
        transcodeRecord.setFormats( clientConfig.getTranscode().getFormatCopies() );
        transcodingDAO.save( transcodeRecord );


        TranscodingFunctions.GetStatusResponseExtended firstResponse = jsonMapper.fromJson(
                "{\n" + "  \"response\": {\n" + "    \"job\": {\n" + "      \"id\": \"92369973\",\n" +
                        "      \"userid\": \"85296\",\n" +
                        "      \"sourcefile\": \"https:\\\\/\\\\/cs-cloud-trunk--upload.s3.amazonaws.com\\\\/0\\\\/1\\\\/23EDB57DBA8E4CB930700F94E411EC32\\\\/1481600940359?x-amz-security-token=FQoDYXdzEJ3%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaDLMT140aAjahjJA8uyKLAnliFmvLqAXvuVoYg0yOcRD2HkFFMxLpENz320mBYSYWZ50v%2BHHOfg6qrKi1%2Bg3jx1krpxpVO50tgyU3Yw%2FCPBNW0J2l1nt9xjkQDbFP%2FYEc3BkdbVO%2BDohJZ3F1S5jaS6z6bM6h0NdDBdAlBpaQpT9%2BR9ofiOFfG5FNbgICcTrspObGr1nJjTvI%2BKfO5Dm1oNufv2mCijDwyWUwVshL3mRca%2FEuMkDY5X%2BYGtm6RW7aQ2gMn6Wkvf8yJPGRTo7ctfKocbgT7iIKYMBtCSfgwLC82i6IGaf%2Fhhi174SEaOHtvRV%2BslD0AFXC%2B3rK8P%2BNWZsNv2vJOzqRCZg34p%2Ba9WEZt9zlJg2CGXOy%2Fiit373CBQ%3D%3D&AWSAccessKeyId=ASIAIU7VVEIXGCZI7Y7Q&Expires=1481608191&Signature=VSzLirKYfPMdFNMsXAUIJoXHrCY%3D\",\n" +
                        "      \"status\": \"Ready to process\",\n" +
                        "      \"notifyurl\": \"https:\\\\/\\\\/hr7fu0poyf.execute-api.us-east-1.amazonaws.com\\\\/a\\\\/encoding-com\\\\/-dummy-\",\n" +
                        "      \"created\": \"2016-12-13 03:49:53\",\n" +
                        "      \"started\": \"2016-12-13 03:49:53\",\n" +
                        "      \"finished\": \"0000-00-00 00:00:00\",\n" +
                        "      \"downloaded\": \"2016-12-13 03:50:12\",\n" + "      \"filesize\": \"15396785\",\n" +
                        "      \"processor\": \"AMAZON\",\n" + "      \"region\": \"oak-private-clive\",\n" +
                        "      \"time_left\": \"0\",\n" + "      \"progress\": \"100.0\",\n" +
                        "      \"time_left_current\": \"0\",\n" + "      \"progress_current\": \"0.0\",\n" +
                        "      \"queue_time\": \"0\"\n" + "    }\n" + "  }\n" + "}",
                TranscodingFunctions.GetStatusResponseExtended.class );

        firstResponse.getResponse().getJob()[ 0 ].setId( transcodeRecord.getMediaId() );
        firstResponse.getResponse().getJob()[ 0 ].setStatus( TranscodingFunctions.Result.STATUS_readToProcess );


        final TranscodingFunctions.GetMediaInfoResponse secondGetMediaInfoResp =
                new TranscodingFunctions.GetMediaInfoResponse();
        secondGetMediaInfoResp.setResponse( new DynaTableVideoTranscodingMediaInfo() );
        secondGetMediaInfoResp.getResponse().setBitrate( "1234k" );
        secondGetMediaInfoResp.getResponse().setSize( "2x3" );
        encodingServlet.setExpects( Arrays.asList( new EncodingExpect( "", jsonMapper.toJson( firstResponse ) ),
                new EncodingExpect( "", jsonMapper.toJson( secondGetMediaInfoResp ) ), new EncodingExpect( "",
                        "{\n" + "  \"response\": {\n" + "    \"message\": \"Updated\"\n" + "  }\n" + "}\n" ),
                new EncodingExpect("", "{\n" + "  \"response\": {\n" + "    \"id\": \"99848271\",\n" +
                        "    \"userid\": \"85296\",\n" +
                        "    \"sourcefile\": \"https://cs-cloud-dev-gary--upload.s3.amazonaws.com/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666?AWSAccessKeyId=aaabbbccc&Expires=1487116394&Signature=fBVVhS9xxblDMjbo6X%2FVUSnvpLw%3D\",\n" +
                        "    \"status\": \"New\",\n" +
                        "    \"notifyurl\": \"https://h3i2thf8bl.execute-api.us-east-1.amazonaws.com/a/encoding-com/-dummy-\",\n" +
                        "    \"created\": \"2017-02-14 21:53:13\",\n" + "    \"started\": \"2017-02-14 21:53:32\",\n" +
                        "    \"finished\": \"0000-00-00 00:00:00\",\n" +
                        "    \"downloaded\": \"2017-02-14 21:53:29\",\n" + "    \"filesize\": \"15396785\",\n" +
                        "    \"processor\": \"AMAZON\",\n" + "    \"region\": \"oak-private-clive\",\n" +
                        "    \"time_left\": \"0\",\n" + "    \"progress\": \"100.0\",\n" +
                        "    \"time_left_current\": \"0\",\n" + "    \"progress_current\": \"100.0\",\n" +
                        "    \"format\": [\n" + "      {\n" + "        \"id\": \"354831207\",\n" +
                        "        \"status\": \"New\",\n" + "        \"created\": \"2017-02-14 21:53:32\",\n" +
                        "        \"started\": \"0000-00-00 00:00:00\",\n" +
                        "        \"finished\": \"0000-00-00 00:00:00\",\n" + "        \"output\": \"mp4\",\n" +
                        "        \"output_preset\": { },\n" +
                        "        \"destination\": \"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/mb/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666/1000k.mp4\",\n" +
                        "        \"protected_public_result\": { },\n" + "        \"size\": \"640x360\",\n" +
                        "        \"bitrate\": \"1000k\",\n" + "        \"audio_bitrate\": \"128k\",\n" +
                        "        \"audio_sample_rate\": \"44100\",\n" + "        \"audio_channels_number\": \"2\",\n" +
                        "        \"downmix_mode\": \"pl2\",\n" + "        \"framerate\": \"29\",\n" +
                        "        \"framerate_upper_threshold\": { },\n" + "        \"fade_in\": { },\n" +
                        "        \"fade_out\": { },\n" + "        \"crop_top\": { },\n" +
                        "        \"crop_left\": { },\n" + "        \"crop_right\": { },\n" +
                        "        \"crop_bottom\": { },\n" + "        \"padd_top\": { },\n" +
                        "        \"padd_left\": { },\n" + "        \"padd_right\": { },\n" +
                        "        \"padd_bottom\": { },\n" + "        \"set_aspect_ratio\": { },\n" +
                        "        \"keep_aspect_ratio\": \"yes\",\n" + "        \"video_codec\": \"libx264\",\n" +
                        "        \"profile\": \"baseline\",\n" + "        \"VCodecParameters\": { },\n" +
                        "        \"video_codec_parameters\": {\n" + "          \"coder\": \"0\",\n" +
                        "          \"flags\": \"+loop\",\n" + "          \"flags2\": \"-wpred-dct8x8\",\n" +
                        "          \"cmp\": \"+chroma\",\n" +
                        "          \"partitions\": \"+parti8x8+parti4x4+partp8x8+partb8x8\",\n" +
                        "          \"me_method\": \"hex\",\n" + "          \"subq\": \"7\",\n" +
                        "          \"me_range\": \"16\",\n" + "          \"bf\": \"0\",\n" +
                        "          \"keyint_min\": \"25\",\n" + "          \"sc_threshold\": \"40\",\n" +
                        "          \"i_qfactor\": \"0.71\",\n" + "          \"b_strategy\": \"1\",\n" +
                        "          \"qcomp\": \"0.6\",\n" + "          \"qmin\": \"10\",\n" +
                        "          \"qmax\": \"51\",\n" + "          \"qdiff\": \"4\",\n" +
                        "          \"directpred\": \"1\",\n" + "          \"trellis\": \"1\",\n" +
                        "          \"level\": \"13\",\n" + "          \"refs\": \"3\",\n" +
                        "          \"wpredp\": \"0\",\n" + "          \"vprofile\": \"baseline\"\n" + "        },\n" +
                        "        \"audio_codec\": \"libfaac\",\n" + "        \"two_pass\": \"yes\",\n" +
                        "        \"turbo\": \"no\",\n" + "        \"twin_turbo\": \"no\",\n" +
                        "        \"cbr\": \"no\",\n" + "        \"hard_cbr\": \"no\",\n" +
                        "        \"minrate\": { },\n" + "        \"maxrate\": { },\n" + "        \"bufsize\": { },\n" +
                        "        \"audio_minrate\": { },\n" + "        \"audio_maxrate\": { },\n" +
                        "        \"audio_bufsize\": { },\n" + "        \"rc_init_occupancy\": { },\n" +
                        "        \"deinterlacing\": \"auto\",\n" + "        \"video_sync\": \"old\",\n" +
                        "        \"keyframe\": \"90\",\n" + "        \"start\": { },\n" + "        \"finish\": { },\n" +
                        "        \"duration\": { },\n" + "        \"audio_volume\": \"100\",\n" +
                        "        \"audio_normalization\": { },\n" + "        \"dmg_alerts\": { },\n" +
                        "        \"loudness_mode\": { },\n" + "        \"input_speech\": { },\n" +
                        "        \"clipmode\": { },\n" + "        \"prolimiter_max_peak\": { },\n" +
                        "        \"input_dialnorm\": { },\n" + "        \"adjust_gain_for_dialnorm\": { },\n" +
                        "        \"dialnorm_threshold\": { },\n" + "        \"dc_repair\": { },\n" +
                        "        \"phase_repair\": { },\n" + "        \"dmix_center_level\": { },\n" +
                        "        \"dmix_center_level_ltrt\": { },\n" + "        \"dmix_center_level_loro\": { },\n" +
                        "        \"dmix_surround_level\": { },\n" + "        \"dmix_surround_level_ltrt\": { },\n" +
                        "        \"dmix_surround_level_loro\": { },\n" + "        \"audio_sync\": { },\n" +
                        "        \"rotate\": \"def\",\n" + "        \"noise_reduction\": { },\n" +
                        "        \"two_pass_decoding\": { },\n" + "        \"force_keyframes\": { },\n" +
                        "        \"metadata_copy\": \"no\",\n" + "        \"strip_chapters\": \"no\",\n" +
                        "        \"pix_format\": \"yuv420p\",\n" + "        \"pan\": { },\n" +
                        "        \"encoder\": \"v1\",\n" + "        \"burnin_timecode\": { },\n" +
                        "        \"copy_timestamps\": \"yes\",\n" + "        \"encryption\": \"no\",\n" +
                        "        \"encryption_method\": \"aes-128-cbc\",\n" + "        \"encryption_key\": { },\n" +
                        "        \"encryption_key_file\": { },\n" + "        \"encryption_iv\": { },\n" +
                        "        \"encryption_password\": { },\n" + "        \"slices\": { },\n" +
                        "        \"audio_stream\": { },\n" + "        \"file_extension\": \"mp4\",\n" +
                        "        \"ftyp\": { },\n" + "        \"hint\": \"no\",\n" + "        \"extends\": { },\n" +
                        "        \"set_rotate\": \"0\",\n" + "        \"copy_nielsen_metadata\": \"no\",\n" +
                        "        \"nielsen_breakout_code\": { },\n" + "        \"nielsen_distributor_id\": { },\n" +
                        "        \"drm\": \"no\",\n" + "        \"drm-content-id\": { },\n" +
                        "        \"drm-common-key\": { },\n" + "        \"drm-license-server-url\": { },\n" +
                        "        \"drm-license-server-cert\": { },\n" + "        \"drm-transport-cert\": { },\n" +
                        "        \"drm-packager-credential\": { },\n" + "        \"drm-credential-pwd\": { },\n" +
                        "        \"drm-policy-file\": { },\n" + "        \"convertedsize\": \"0\",\n" +
                        "        \"queued\": \"0000-00-00 00:00:00\",\n" + "        \"converttime\": \"0\"\n" +
                        "      },\n" + "      {\n" + "        \"id\": \"354831210\",\n" +
                        "        \"status\": \"New\",\n" + "        \"created\": \"2017-02-14 21:53:32\",\n" +
                        "        \"started\": \"0000-00-00 00:00:00\",\n" +
                        "        \"finished\": \"0000-00-00 00:00:00\",\n" + "        \"output\": \"thumbnail\",\n" +
                        "        \"output_preset\": { },\n" +
                        "        \"destination\": \"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/posterframe/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666/default_frame.jpg\",\n" +
                        "        \"protected_public_result\": { },\n" + "        \"size\": \"640x360\",\n" +
                        "        \"bitrate\": { },\n" + "        \"audio_bitrate\": \"64k\",\n" +
                        "        \"audio_sample_rate\": { },\n" + "        \"audio_channels_number\": \"2\",\n" +
                        "        \"downmix_mode\": \"pl2\",\n" + "        \"framerate\": { },\n" +
                        "        \"framerate_upper_threshold\": { },\n" + "        \"fade_in\": { },\n" +
                        "        \"fade_out\": { },\n" + "        \"crop_top\": { },\n" +
                        "        \"crop_left\": { },\n" + "        \"crop_right\": { },\n" +
                        "        \"crop_bottom\": { },\n" + "        \"padd_top\": { },\n" +
                        "        \"padd_left\": { },\n" + "        \"padd_right\": { },\n" +
                        "        \"padd_bottom\": { },\n" + "        \"set_aspect_ratio\": { },\n" +
                        "        \"keep_aspect_ratio\": \"yes\",\n" + "        \"video_codec\": \"mjpeg\",\n" +
                        "        \"profile\": { },\n" + "        \"VCodecParameters\": { },\n" +
                        "        \"video_codec_parameters\": { },\n" + "        \"audio_codec\": { },\n" +
                        "        \"two_pass\": \"no\",\n" + "        \"turbo\": \"no\",\n" +
                        "        \"twin_turbo\": \"no\",\n" + "        \"cbr\": \"no\",\n" +
                        "        \"hard_cbr\": \"no\",\n" + "        \"minrate\": { },\n" +
                        "        \"maxrate\": { },\n" + "        \"bufsize\": { },\n" +
                        "        \"audio_minrate\": { },\n" + "        \"audio_maxrate\": { },\n" +
                        "        \"audio_bufsize\": { },\n" + "        \"rc_init_occupancy\": { },\n" +
                        "        \"deinterlacing\": \"auto\",\n" + "        \"video_sync\": \"old\",\n" +
                        "        \"keyframe\": \"300\",\n" + "        \"start\": { },\n" +
                        "        \"finish\": { },\n" + "        \"duration\": { },\n" +
                        "        \"audio_volume\": \"100\",\n" + "        \"audio_normalization\": { },\n" +
                        "        \"dmg_alerts\": { },\n" + "        \"loudness_mode\": { },\n" +
                        "        \"input_speech\": { },\n" + "        \"clipmode\": { },\n" +
                        "        \"prolimiter_max_peak\": { },\n" + "        \"input_dialnorm\": { },\n" +
                        "        \"adjust_gain_for_dialnorm\": { },\n" + "        \"dialnorm_threshold\": { },\n" +
                        "        \"dc_repair\": { },\n" + "        \"phase_repair\": { },\n" +
                        "        \"dmix_center_level\": { },\n" + "        \"dmix_center_level_ltrt\": { },\n" +
                        "        \"dmix_center_level_loro\": { },\n" + "        \"dmix_surround_level\": { },\n" +
                        "        \"dmix_surround_level_ltrt\": { },\n" +
                        "        \"dmix_surround_level_loro\": { },\n" + "        \"audio_sync\": { },\n" +
                        "        \"rotate\": \"def\",\n" + "        \"noise_reduction\": { },\n" +
                        "        \"two_pass_decoding\": { },\n" + "        \"force_keyframes\": { },\n" +
                        "        \"metadata_copy\": \"no\",\n" + "        \"strip_chapters\": \"no\",\n" +
                        "        \"pix_format\": \"yuv420p\",\n" + "        \"pan\": { },\n" +
                        "        \"encoder\": \"v1\",\n" + "        \"burnin_timecode\": { },\n" +
                        "        \"copy_timestamps\": \"yes\",\n" + "        \"encryption\": \"no\",\n" +
                        "        \"encryption_method\": \"aes-128-cbc\",\n" + "        \"encryption_key\": { },\n" +
                        "        \"encryption_key_file\": { },\n" + "        \"encryption_iv\": { },\n" +
                        "        \"encryption_password\": { },\n" + "        \"slices\": { },\n" +
                        "        \"audio_stream\": { },\n" + "        \"time\": \"5%\",\n" +
                        "        \"width\": \"640\",\n" + "        \"height\": \"360\",\n" +
                        "        \"file_extension\": \"jpg\",\n" + "        \"use_vtt\": \"no\",\n" +
                        "        \"vtt_line_size\": \"4\",\n" + "        \"convertedsize\": \"0\",\n" +
                        "        \"queued\": \"0000-00-00 00:00:00\",\n" + "        \"converttime\": \"0\"\n" +
                        "      },\n" + "      {\n" + "        \"id\": \"354831213\",\n" +
                        "        \"status\": \"New\",\n" + "        \"created\": \"2017-02-14 21:53:32\",\n" +
                        "        \"started\": \"0000-00-00 00:00:00\",\n" +
                        "        \"finished\": \"0000-00-00 00:00:00\",\n" + "        \"output\": \"mp4\",\n" +
                        "        \"output_preset\": { },\n" +
                        "        \"destination\": \"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/mb/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666/1500k.mp4\",\n" +
                        "        \"protected_public_result\": { },\n" + "        \"size\": \"640x360\",\n" +
                        "        \"bitrate\": \"1500k\",\n" + "        \"audio_bitrate\": \"128k\",\n" +
                        "        \"audio_sample_rate\": \"44100\",\n" + "        \"audio_channels_number\": \"2\",\n" +
                        "        \"downmix_mode\": \"pl2\",\n" + "        \"framerate\": \"29\",\n" +
                        "        \"framerate_upper_threshold\": { },\n" + "        \"fade_in\": { },\n" +
                        "        \"fade_out\": { },\n" + "        \"crop_top\": { },\n" +
                        "        \"crop_left\": { },\n" + "        \"crop_right\": { },\n" +
                        "        \"crop_bottom\": { },\n" + "        \"padd_top\": { },\n" +
                        "        \"padd_left\": { },\n" + "        \"padd_right\": { },\n" +
                        "        \"padd_bottom\": { },\n" + "        \"set_aspect_ratio\": { },\n" +
                        "        \"keep_aspect_ratio\": \"yes\",\n" + "        \"video_codec\": \"libx264\",\n" +
                        "        \"profile\": \"baseline\",\n" + "        \"VCodecParameters\": { },\n" +
                        "        \"video_codec_parameters\": {\n" + "          \"coder\": \"0\",\n" +
                        "          \"flags\": \"+loop\",\n" + "          \"flags2\": \"-wpred-dct8x8\",\n" +
                        "          \"cmp\": \"+chroma\",\n" +
                        "          \"partitions\": \"+parti8x8+parti4x4+partp8x8+partb8x8\",\n" +
                        "          \"me_method\": \"hex\",\n" + "          \"subq\": \"7\",\n" +
                        "          \"me_range\": \"16\",\n" + "          \"bf\": \"0\",\n" +
                        "          \"keyint_min\": \"25\",\n" + "          \"sc_threshold\": \"40\",\n" +
                        "          \"i_qfactor\": \"0.71\",\n" + "          \"b_strategy\": \"1\",\n" +
                        "          \"qcomp\": \"0.6\",\n" + "          \"qmin\": \"10\",\n" +
                        "          \"qmax\": \"51\",\n" + "          \"qdiff\": \"4\",\n" +
                        "          \"directpred\": \"1\",\n" + "          \"trellis\": \"1\",\n" +
                        "          \"level\": \"13\",\n" + "          \"refs\": \"3\",\n" +
                        "          \"wpredp\": \"0\",\n" + "          \"vprofile\": \"baseline\"\n" + "        },\n" +
                        "        \"audio_codec\": \"libfaac\",\n" + "        \"two_pass\": \"yes\",\n" +
                        "        \"turbo\": \"no\",\n" + "        \"twin_turbo\": \"no\",\n" +
                        "        \"cbr\": \"no\",\n" + "        \"hard_cbr\": \"no\",\n" +
                        "        \"minrate\": { },\n" + "        \"maxrate\": { },\n" + "        \"bufsize\": { },\n" +
                        "        \"audio_minrate\": { },\n" + "        \"audio_maxrate\": { },\n" +
                        "        \"audio_bufsize\": { },\n" + "        \"rc_init_occupancy\": { },\n" +
                        "        \"deinterlacing\": \"auto\",\n" + "        \"video_sync\": \"old\",\n" +
                        "        \"keyframe\": \"90\",\n" + "        \"start\": { },\n" + "        \"finish\": { },\n" +
                        "        \"duration\": { },\n" + "        \"audio_volume\": \"100\",\n" +
                        "        \"audio_normalization\": { },\n" + "        \"dmg_alerts\": { },\n" +
                        "        \"loudness_mode\": { },\n" + "        \"input_speech\": { },\n" +
                        "        \"clipmode\": { },\n" + "        \"prolimiter_max_peak\": { },\n" +
                        "        \"input_dialnorm\": { },\n" + "        \"adjust_gain_for_dialnorm\": { },\n" +
                        "        \"dialnorm_threshold\": { },\n" + "        \"dc_repair\": { },\n" +
                        "        \"phase_repair\": { },\n" + "        \"dmix_center_level\": { },\n" +
                        "        \"dmix_center_level_ltrt\": { },\n" + "        \"dmix_center_level_loro\": { },\n" +
                        "        \"dmix_surround_level\": { },\n" + "        \"dmix_surround_level_ltrt\": { },\n" +
                        "        \"dmix_surround_level_loro\": { },\n" + "        \"audio_sync\": { },\n" +
                        "        \"rotate\": \"def\",\n" + "        \"noise_reduction\": { },\n" +
                        "        \"two_pass_decoding\": { },\n" + "        \"force_keyframes\": { },\n" +
                        "        \"metadata_copy\": \"no\",\n" + "        \"strip_chapters\": \"no\",\n" +
                        "        \"pix_format\": \"yuv420p\",\n" + "        \"pan\": { },\n" +
                        "        \"encoder\": \"v1\",\n" + "        \"burnin_timecode\": { },\n" +
                        "        \"copy_timestamps\": \"yes\",\n" + "        \"encryption\": \"no\",\n" +
                        "        \"encryption_method\": \"aes-128-cbc\",\n" + "        \"encryption_key\": { },\n" +
                        "        \"encryption_key_file\": { },\n" + "        \"encryption_iv\": { },\n" +
                        "        \"encryption_password\": { },\n" + "        \"slices\": { },\n" +
                        "        \"audio_stream\": { },\n" + "        \"file_extension\": \"mp4\",\n" +
                        "        \"ftyp\": { },\n" + "        \"hint\": \"no\",\n" + "        \"extends\": { },\n" +
                        "        \"set_rotate\": \"0\",\n" + "        \"copy_nielsen_metadata\": \"no\",\n" +
                        "        \"nielsen_breakout_code\": { },\n" + "        \"nielsen_distributor_id\": { },\n" +
                        "        \"drm\": \"no\",\n" + "        \"drm-content-id\": { },\n" +
                        "        \"drm-common-key\": { },\n" + "        \"drm-license-server-url\": { },\n" +
                        "        \"drm-license-server-cert\": { },\n" + "        \"drm-transport-cert\": { },\n" +
                        "        \"drm-packager-credential\": { },\n" + "        \"drm-credential-pwd\": { },\n" +
                        "        \"drm-policy-file\": { },\n" + "        \"convertedsize\": \"0\",\n" +
                        "        \"queued\": \"0000-00-00 00:00:00\",\n" + "        \"converttime\": \"0\"\n" +
                        "      },\n" + "      {\n" + "        \"id\": \"354831216\",\n" +
                        "        \"status\": \"New\",\n" + "        \"created\": \"2017-02-14 21:53:32\",\n" +
                        "        \"started\": \"0000-00-00 00:00:00\",\n" +
                        "        \"finished\": \"0000-00-00 00:00:00\",\n" + "        \"output\": \"mp4\",\n" +
                        "        \"output_preset\": { },\n" +
                        "        \"destination\": \"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/mb/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666/2500k.mp4\",\n" +
                        "        \"protected_public_result\": { },\n" + "        \"size\": \"640x360\",\n" +
                        "        \"bitrate\": \"2500k\",\n" + "        \"audio_bitrate\": \"128k\",\n" +
                        "        \"audio_sample_rate\": \"44100\",\n" + "        \"audio_channels_number\": \"2\",\n" +
                        "        \"downmix_mode\": \"pl2\",\n" + "        \"framerate\": \"29\",\n" +
                        "        \"framerate_upper_threshold\": { },\n" + "        \"fade_in\": { },\n" +
                        "        \"fade_out\": { },\n" + "        \"crop_top\": { },\n" +
                        "        \"crop_left\": { },\n" + "        \"crop_right\": { },\n" +
                        "        \"crop_bottom\": { },\n" + "        \"padd_top\": { },\n" +
                        "        \"padd_left\": { },\n" + "        \"padd_right\": { },\n" +
                        "        \"padd_bottom\": { },\n" + "        \"set_aspect_ratio\": { },\n" +
                        "        \"keep_aspect_ratio\": \"yes\",\n" + "        \"video_codec\": \"libx264\",\n" +
                        "        \"profile\": \"baseline\",\n" + "        \"VCodecParameters\": { },\n" +
                        "        \"video_codec_parameters\": {\n" + "          \"coder\": \"0\",\n" +
                        "          \"flags\": \"+loop\",\n" + "          \"flags2\": \"-wpred-dct8x8\",\n" +
                        "          \"cmp\": \"+chroma\",\n" +
                        "          \"partitions\": \"+parti8x8+parti4x4+partp8x8+partb8x8\",\n" +
                        "          \"me_method\": \"hex\",\n" + "          \"subq\": \"7\",\n" +
                        "          \"me_range\": \"16\",\n" + "          \"bf\": \"0\",\n" +
                        "          \"keyint_min\": \"25\",\n" + "          \"sc_threshold\": \"40\",\n" +
                        "          \"i_qfactor\": \"0.71\",\n" + "          \"b_strategy\": \"1\",\n" +
                        "          \"qcomp\": \"0.6\",\n" + "          \"qmin\": \"10\",\n" +
                        "          \"qmax\": \"51\",\n" + "          \"qdiff\": \"4\",\n" +
                        "          \"directpred\": \"1\",\n" + "          \"trellis\": \"1\",\n" +
                        "          \"level\": \"13\",\n" + "          \"refs\": \"3\",\n" +
                        "          \"wpredp\": \"0\",\n" + "          \"vprofile\": \"baseline\"\n" + "        },\n" +
                        "        \"audio_codec\": \"libfaac\",\n" + "        \"two_pass\": \"yes\",\n" +
                        "        \"turbo\": \"no\",\n" + "        \"twin_turbo\": \"no\",\n" +
                        "        \"cbr\": \"no\",\n" + "        \"hard_cbr\": \"no\",\n" +
                        "        \"minrate\": { },\n" + "        \"maxrate\": { },\n" + "        \"bufsize\": { },\n" +
                        "        \"audio_minrate\": { },\n" + "        \"audio_maxrate\": { },\n" +
                        "        \"audio_bufsize\": { },\n" + "        \"rc_init_occupancy\": { },\n" +
                        "        \"deinterlacing\": \"auto\",\n" + "        \"video_sync\": \"old\",\n" +
                        "        \"keyframe\": \"90\",\n" + "        \"start\": { },\n" + "        \"finish\": { },\n" +
                        "        \"duration\": { },\n" + "        \"audio_volume\": \"100\",\n" +
                        "        \"audio_normalization\": { },\n" + "        \"dmg_alerts\": { },\n" +
                        "        \"loudness_mode\": { },\n" + "        \"input_speech\": { },\n" +
                        "        \"clipmode\": { },\n" + "        \"prolimiter_max_peak\": { },\n" +
                        "        \"input_dialnorm\": { },\n" + "        \"adjust_gain_for_dialnorm\": { },\n" +
                        "        \"dialnorm_threshold\": { },\n" + "        \"dc_repair\": { },\n" +
                        "        \"phase_repair\": { },\n" + "        \"dmix_center_level\": { },\n" +
                        "        \"dmix_center_level_ltrt\": { },\n" + "        \"dmix_center_level_loro\": { },\n" +
                        "        \"dmix_surround_level\": { },\n" + "        \"dmix_surround_level_ltrt\": { },\n" +
                        "        \"dmix_surround_level_loro\": { },\n" + "        \"audio_sync\": { },\n" +
                        "        \"rotate\": \"def\",\n" + "        \"noise_reduction\": { },\n" +
                        "        \"two_pass_decoding\": { },\n" + "        \"force_keyframes\": { },\n" +
                        "        \"metadata_copy\": \"no\",\n" + "        \"strip_chapters\": \"no\",\n" +
                        "        \"pix_format\": \"yuv420p\",\n" + "        \"pan\": { },\n" +
                        "        \"encoder\": \"v1\",\n" + "        \"burnin_timecode\": { },\n" +
                        "        \"copy_timestamps\": \"yes\",\n" + "        \"encryption\": \"no\",\n" +
                        "        \"encryption_method\": \"aes-128-cbc\",\n" + "        \"encryption_key\": { },\n" +
                        "        \"encryption_key_file\": { },\n" + "        \"encryption_iv\": { },\n" +
                        "        \"encryption_password\": { },\n" + "        \"slices\": { },\n" +
                        "        \"audio_stream\": { },\n" + "        \"file_extension\": \"mp4\",\n" +
                        "        \"ftyp\": { },\n" + "        \"hint\": \"no\",\n" + "        \"extends\": { },\n" +
                        "        \"set_rotate\": \"0\",\n" + "        \"copy_nielsen_metadata\": \"no\",\n" +
                        "        \"nielsen_breakout_code\": { },\n" + "        \"nielsen_distributor_id\": { },\n" +
                        "        \"drm\": \"no\",\n" + "        \"drm-content-id\": { },\n" +
                        "        \"drm-common-key\": { },\n" + "        \"drm-license-server-url\": { },\n" +
                        "        \"drm-license-server-cert\": { },\n" + "        \"drm-transport-cert\": { },\n" +
                        "        \"drm-packager-credential\": { },\n" + "        \"drm-credential-pwd\": { },\n" +
                        "        \"drm-policy-file\": { },\n" + "        \"convertedsize\": \"0\",\n" +
                        "        \"queued\": \"0000-00-00 00:00:00\",\n" + "        \"converttime\": \"0\"\n" +
                        "      },\n" + "      {\n" + "        \"id\": \"354831219\",\n" +
                        "        \"status\": \"New\",\n" + "        \"created\": \"2017-02-14 21:53:32\",\n" +
                        "        \"started\": \"0000-00-00 00:00:00\",\n" +
                        "        \"finished\": \"0000-00-00 00:00:00\",\n" + "        \"output\": \"mp4\",\n" +
                        "        \"output_preset\": { },\n" +
                        "        \"destination\": \"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666/4500k.mp4\",\n" +
                        "        \"protected_public_result\": { },\n" + "        \"size\": \"640x360\",\n" +
                        "        \"bitrate\": \"4500k\",\n" + "        \"audio_bitrate\": \"128k\",\n" +
                        "        \"audio_sample_rate\": \"44100\",\n" + "        \"audio_channels_number\": \"2\",\n" +
                        "        \"downmix_mode\": \"pl2\",\n" + "        \"framerate\": \"29\",\n" +
                        "        \"framerate_upper_threshold\": { },\n" + "        \"fade_in\": { },\n" +
                        "        \"fade_out\": { },\n" + "        \"crop_top\": { },\n" +
                        "        \"crop_left\": { },\n" + "        \"crop_right\": { },\n" +
                        "        \"crop_bottom\": { },\n" + "        \"padd_top\": { },\n" +
                        "        \"padd_left\": { },\n" + "        \"padd_right\": { },\n" +
                        "        \"padd_bottom\": { },\n" + "        \"set_aspect_ratio\": { },\n" +
                        "        \"keep_aspect_ratio\": \"yes\",\n" + "        \"video_codec\": \"libx264\",\n" +
                        "        \"profile\": \"baseline\",\n" + "        \"VCodecParameters\": { },\n" +
                        "        \"video_codec_parameters\": {\n" + "          \"coder\": \"0\",\n" +
                        "          \"flags\": \"+loop\",\n" + "          \"flags2\": \"-wpred-dct8x8\",\n" +
                        "          \"cmp\": \"+chroma\",\n" +
                        "          \"partitions\": \"+parti8x8+parti4x4+partp8x8+partb8x8\",\n" +
                        "          \"me_method\": \"hex\",\n" + "          \"subq\": \"7\",\n" +
                        "          \"me_range\": \"16\",\n" + "          \"bf\": \"0\",\n" +
                        "          \"keyint_min\": \"25\",\n" + "          \"sc_threshold\": \"40\",\n" +
                        "          \"i_qfactor\": \"0.71\",\n" + "          \"b_strategy\": \"1\",\n" +
                        "          \"qcomp\": \"0.6\",\n" + "          \"qmin\": \"10\",\n" +
                        "          \"qmax\": \"51\",\n" + "          \"qdiff\": \"4\",\n" +
                        "          \"directpred\": \"1\",\n" + "          \"trellis\": \"1\",\n" +
                        "          \"level\": \"13\",\n" + "          \"refs\": \"3\",\n" +
                        "          \"wpredp\": \"0\",\n" + "          \"vprofile\": \"baseline\"\n" + "        },\n" +
                        "        \"audio_codec\": \"libfaac\",\n" + "        \"two_pass\": \"yes\",\n" +
                        "        \"turbo\": \"no\",\n" + "        \"twin_turbo\": \"no\",\n" +
                        "        \"cbr\": \"no\",\n" + "        \"hard_cbr\": \"no\",\n" +
                        "        \"minrate\": { },\n" + "        \"maxrate\": { },\n" + "        \"bufsize\": { },\n" +
                        "        \"audio_minrate\": { },\n" + "        \"audio_maxrate\": { },\n" +
                        "        \"audio_bufsize\": { },\n" + "        \"rc_init_occupancy\": { },\n" +
                        "        \"deinterlacing\": \"auto\",\n" + "        \"video_sync\": \"old\",\n" +
                        "        \"keyframe\": \"90\",\n" + "        \"start\": { },\n" + "        \"finish\": { },\n" +
                        "        \"duration\": { },\n" + "        \"audio_volume\": \"100\",\n" +
                        "        \"audio_normalization\": { },\n" + "        \"dmg_alerts\": { },\n" +
                        "        \"loudness_mode\": { },\n" + "        \"input_speech\": { },\n" +
                        "        \"clipmode\": { },\n" + "        \"prolimiter_max_peak\": { },\n" +
                        "        \"input_dialnorm\": { },\n" + "        \"adjust_gain_for_dialnorm\": { },\n" +
                        "        \"dialnorm_threshold\": { },\n" + "        \"dc_repair\": { },\n" +
                        "        \"phase_repair\": { },\n" + "        \"dmix_center_level\": { },\n" +
                        "        \"dmix_center_level_ltrt\": { },\n" + "        \"dmix_center_level_loro\": { },\n" +
                        "        \"dmix_surround_level\": { },\n" + "        \"dmix_surround_level_ltrt\": { },\n" +
                        "        \"dmix_surround_level_loro\": { },\n" + "        \"audio_sync\": { },\n" +
                        "        \"rotate\": \"def\",\n" + "        \"noise_reduction\": { },\n" +
                        "        \"two_pass_decoding\": { },\n" + "        \"force_keyframes\": { },\n" +
                        "        \"metadata_copy\": \"no\",\n" + "        \"strip_chapters\": \"no\",\n" +
                        "        \"pix_format\": \"yuv420p\",\n" + "        \"pan\": { },\n" +
                        "        \"encoder\": \"v1\",\n" + "        \"burnin_timecode\": { },\n" +
                        "        \"copy_timestamps\": \"yes\",\n" + "        \"encryption\": \"no\",\n" +
                        "        \"encryption_method\": \"aes-128-cbc\",\n" + "        \"encryption_key\": { },\n" +
                        "        \"encryption_key_file\": { },\n" + "        \"encryption_iv\": { },\n" +
                        "        \"encryption_password\": { },\n" + "        \"slices\": { },\n" +
                        "        \"audio_stream\": { },\n" + "        \"file_extension\": \"mp4\",\n" +
                        "        \"ftyp\": { },\n" + "        \"hint\": \"no\",\n" + "        \"extends\": { },\n" +
                        "        \"set_rotate\": \"0\",\n" + "        \"copy_nielsen_metadata\": \"no\",\n" +
                        "        \"nielsen_breakout_code\": { },\n" + "        \"nielsen_distributor_id\": { },\n" +
                        "        \"drm\": \"no\",\n" + "        \"drm-content-id\": { },\n" +
                        "        \"drm-common-key\": { },\n" + "        \"drm-license-server-url\": { },\n" +
                        "        \"drm-license-server-cert\": { },\n" + "        \"drm-transport-cert\": { },\n" +
                        "        \"drm-packager-credential\": { },\n" + "        \"drm-credential-pwd\": { },\n" +
                        "        \"drm-policy-file\": { },\n" + "        \"convertedsize\": \"0\",\n" +
                        "        \"queued\": \"0000-00-00 00:00:00\",\n" + "        \"converttime\": \"0\"\n" +
                        "      },\n" + "      {\n" + "        \"id\": \"354831222\",\n" +
                        "        \"status\": \"New\",\n" + "        \"created\": \"2017-02-14 21:53:32\",\n" +
                        "        \"started\": \"0000-00-00 00:00:00\",\n" +
                        "        \"finished\": \"0000-00-00 00:00:00\",\n" + "        \"output\": \"mp4\",\n" +
                        "        \"output_preset\": { },\n" +
                        "        \"destination\": \"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/mb/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666/1250k.mp4\",\n" +
                        "        \"protected_public_result\": { },\n" + "        \"size\": \"640x360\",\n" +
                        "        \"bitrate\": \"1250k\",\n" + "        \"audio_bitrate\": \"128k\",\n" +
                        "        \"audio_sample_rate\": \"44100\",\n" + "        \"audio_channels_number\": \"2\",\n" +
                        "        \"downmix_mode\": \"pl2\",\n" + "        \"framerate\": \"29\",\n" +
                        "        \"framerate_upper_threshold\": { },\n" + "        \"fade_in\": { },\n" +
                        "        \"fade_out\": { },\n" + "        \"crop_top\": { },\n" +
                        "        \"crop_left\": { },\n" + "        \"crop_right\": { },\n" +
                        "        \"crop_bottom\": { },\n" + "        \"padd_top\": { },\n" +
                        "        \"padd_left\": { },\n" + "        \"padd_right\": { },\n" +
                        "        \"padd_bottom\": { },\n" + "        \"set_aspect_ratio\": { },\n" +
                        "        \"keep_aspect_ratio\": \"yes\",\n" + "        \"video_codec\": \"libx264\",\n" +
                        "        \"profile\": \"baseline\",\n" + "        \"VCodecParameters\": { },\n" +
                        "        \"video_codec_parameters\": {\n" + "          \"coder\": \"0\",\n" +
                        "          \"flags\": \"+loop\",\n" + "          \"flags2\": \"-wpred-dct8x8\",\n" +
                        "          \"cmp\": \"+chroma\",\n" +
                        "          \"partitions\": \"+parti8x8+parti4x4+partp8x8+partb8x8\",\n" +
                        "          \"me_method\": \"hex\",\n" + "          \"subq\": \"7\",\n" +
                        "          \"me_range\": \"16\",\n" + "          \"bf\": \"0\",\n" +
                        "          \"keyint_min\": \"25\",\n" + "          \"sc_threshold\": \"40\",\n" +
                        "          \"i_qfactor\": \"0.71\",\n" + "          \"b_strategy\": \"1\",\n" +
                        "          \"qcomp\": \"0.6\",\n" + "          \"qmin\": \"10\",\n" +
                        "          \"qmax\": \"51\",\n" + "          \"qdiff\": \"4\",\n" +
                        "          \"directpred\": \"1\",\n" + "          \"trellis\": \"1\",\n" +
                        "          \"level\": \"13\",\n" + "          \"refs\": \"3\",\n" +
                        "          \"wpredp\": \"0\",\n" + "          \"vprofile\": \"baseline\"\n" + "        },\n" +
                        "        \"audio_codec\": \"libfaac\",\n" + "        \"two_pass\": \"yes\",\n" +
                        "        \"turbo\": \"no\",\n" + "        \"twin_turbo\": \"no\",\n" +
                        "        \"cbr\": \"no\",\n" + "        \"hard_cbr\": \"no\",\n" +
                        "        \"minrate\": { },\n" + "        \"maxrate\": { },\n" + "        \"bufsize\": { },\n" +
                        "        \"audio_minrate\": { },\n" + "        \"audio_maxrate\": { },\n" +
                        "        \"audio_bufsize\": { },\n" + "        \"rc_init_occupancy\": { },\n" +
                        "        \"deinterlacing\": \"auto\",\n" + "        \"video_sync\": \"old\",\n" +
                        "        \"keyframe\": \"90\",\n" + "        \"start\": { },\n" + "        \"finish\": { },\n" +
                        "        \"duration\": { },\n" + "        \"audio_volume\": \"100\",\n" +
                        "        \"audio_normalization\": { },\n" + "        \"dmg_alerts\": { },\n" +
                        "        \"loudness_mode\": { },\n" + "        \"input_speech\": { },\n" +
                        "        \"clipmode\": { },\n" + "        \"prolimiter_max_peak\": { },\n" +
                        "        \"input_dialnorm\": { },\n" + "        \"adjust_gain_for_dialnorm\": { },\n" +
                        "        \"dialnorm_threshold\": { },\n" + "        \"dc_repair\": { },\n" +
                        "        \"phase_repair\": { },\n" + "        \"dmix_center_level\": { },\n" +
                        "        \"dmix_center_level_ltrt\": { },\n" + "        \"dmix_center_level_loro\": { },\n" +
                        "        \"dmix_surround_level\": { },\n" + "        \"dmix_surround_level_ltrt\": { },\n" +
                        "        \"dmix_surround_level_loro\": { },\n" + "        \"audio_sync\": { },\n" +
                        "        \"rotate\": \"def\",\n" + "        \"noise_reduction\": { },\n" +
                        "        \"two_pass_decoding\": { },\n" + "        \"force_keyframes\": { },\n" +
                        "        \"metadata_copy\": \"no\",\n" + "        \"strip_chapters\": \"no\",\n" +
                        "        \"pix_format\": \"yuv420p\",\n" + "        \"pan\": { },\n" +
                        "        \"encoder\": \"v1\",\n" + "        \"burnin_timecode\": { },\n" +
                        "        \"copy_timestamps\": \"yes\",\n" + "        \"encryption\": \"no\",\n" +
                        "        \"encryption_method\": \"aes-128-cbc\",\n" + "        \"encryption_key\": { },\n" +
                        "        \"encryption_key_file\": { },\n" + "        \"encryption_iv\": { },\n" +
                        "        \"encryption_password\": { },\n" + "        \"slices\": { },\n" +
                        "        \"audio_stream\": { },\n" + "        \"file_extension\": \"mp4\",\n" +
                        "        \"ftyp\": { },\n" + "        \"hint\": \"no\",\n" + "        \"extends\": { },\n" +
                        "        \"set_rotate\": \"0\",\n" + "        \"copy_nielsen_metadata\": \"no\",\n" +
                        "        \"nielsen_breakout_code\": { },\n" + "        \"nielsen_distributor_id\": { },\n" +
                        "        \"drm\": \"no\",\n" + "        \"drm-content-id\": { },\n" +
                        "        \"drm-common-key\": { },\n" + "        \"drm-license-server-url\": { },\n" +
                        "        \"drm-license-server-cert\": { },\n" + "        \"drm-transport-cert\": { },\n" +
                        "        \"drm-packager-credential\": { },\n" + "        \"drm-credential-pwd\": { },\n" +
                        "        \"drm-policy-file\": { },\n" + "        \"convertedsize\": \"0\",\n" +
                        "        \"queued\": \"0000-00-00 00:00:00\",\n" + "        \"converttime\": \"0\"\n" +
                        "      },\n" + "      {\n" + "        \"id\": \"354831225\",\n" +
                        "        \"status\": \"New\",\n" + "        \"created\": \"2017-02-14 21:53:32\",\n" +
                        "        \"started\": \"0000-00-00 00:00:00\",\n" +
                        "        \"finished\": \"0000-00-00 00:00:00\",\n" + "        \"output\": \"mp4\",\n" +
                        "        \"output_preset\": { },\n" +
                        "        \"destination\": \"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/mb/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666/2000k.mp4\",\n" +
                        "        \"protected_public_result\": { },\n" + "        \"size\": \"640x360\",\n" +
                        "        \"bitrate\": \"2000k\",\n" + "        \"audio_bitrate\": \"128k\",\n" +
                        "        \"audio_sample_rate\": \"44100\",\n" + "        \"audio_channels_number\": \"2\",\n" +
                        "        \"downmix_mode\": \"pl2\",\n" + "        \"framerate\": \"29\",\n" +
                        "        \"framerate_upper_threshold\": { },\n" + "        \"fade_in\": { },\n" +
                        "        \"fade_out\": { },\n" + "        \"crop_top\": { },\n" +
                        "        \"crop_left\": { },\n" + "        \"crop_right\": { },\n" +
                        "        \"crop_bottom\": { },\n" + "        \"padd_top\": { },\n" +
                        "        \"padd_left\": { },\n" + "        \"padd_right\": { },\n" +
                        "        \"padd_bottom\": { },\n" + "        \"set_aspect_ratio\": { },\n" +
                        "        \"keep_aspect_ratio\": \"yes\",\n" + "        \"video_codec\": \"libx264\",\n" +
                        "        \"profile\": \"baseline\",\n" + "        \"VCodecParameters\": { },\n" +
                        "        \"video_codec_parameters\": {\n" + "          \"coder\": \"0\",\n" +
                        "          \"flags\": \"+loop\",\n" + "          \"flags2\": \"-wpred-dct8x8\",\n" +
                        "          \"cmp\": \"+chroma\",\n" +
                        "          \"partitions\": \"+parti8x8+parti4x4+partp8x8+partb8x8\",\n" +
                        "          \"me_method\": \"hex\",\n" + "          \"subq\": \"7\",\n" +
                        "          \"me_range\": \"16\",\n" + "          \"bf\": \"0\",\n" +
                        "          \"keyint_min\": \"25\",\n" + "          \"sc_threshold\": \"40\",\n" +
                        "          \"i_qfactor\": \"0.71\",\n" + "          \"b_strategy\": \"1\",\n" +
                        "          \"qcomp\": \"0.6\",\n" + "          \"qmin\": \"10\",\n" +
                        "          \"qmax\": \"51\",\n" + "          \"qdiff\": \"4\",\n" +
                        "          \"directpred\": \"1\",\n" + "          \"trellis\": \"1\",\n" +
                        "          \"level\": \"13\",\n" + "          \"refs\": \"3\",\n" +
                        "          \"wpredp\": \"0\",\n" + "          \"vprofile\": \"baseline\"\n" + "        },\n" +
                        "        \"audio_codec\": \"libfaac\",\n" + "        \"two_pass\": \"yes\",\n" +
                        "        \"turbo\": \"no\",\n" + "        \"twin_turbo\": \"no\",\n" +
                        "        \"cbr\": \"no\",\n" + "        \"hard_cbr\": \"no\",\n" +
                        "        \"minrate\": { },\n" + "        \"maxrate\": { },\n" + "        \"bufsize\": { },\n" +
                        "        \"audio_minrate\": { },\n" + "        \"audio_maxrate\": { },\n" +
                        "        \"audio_bufsize\": { },\n" + "        \"rc_init_occupancy\": { },\n" +
                        "        \"deinterlacing\": \"auto\",\n" + "        \"video_sync\": \"old\",\n" +
                        "        \"keyframe\": \"90\",\n" + "        \"start\": { },\n" + "        \"finish\": { },\n" +
                        "        \"duration\": { },\n" + "        \"audio_volume\": \"100\",\n" +
                        "        \"audio_normalization\": { },\n" + "        \"dmg_alerts\": { },\n" +
                        "        \"loudness_mode\": { },\n" + "        \"input_speech\": { },\n" +
                        "        \"clipmode\": { },\n" + "        \"prolimiter_max_peak\": { },\n" +
                        "        \"input_dialnorm\": { },\n" + "        \"adjust_gain_for_dialnorm\": { },\n" +
                        "        \"dialnorm_threshold\": { },\n" + "        \"dc_repair\": { },\n" +
                        "        \"phase_repair\": { },\n" + "        \"dmix_center_level\": { },\n" +
                        "        \"dmix_center_level_ltrt\": { },\n" + "        \"dmix_center_level_loro\": { },\n" +
                        "        \"dmix_surround_level\": { },\n" + "        \"dmix_surround_level_ltrt\": { },\n" +
                        "        \"dmix_surround_level_loro\": { },\n" + "        \"audio_sync\": { },\n" +
                        "        \"rotate\": \"def\",\n" + "        \"noise_reduction\": { },\n" +
                        "        \"two_pass_decoding\": { },\n" + "        \"force_keyframes\": { },\n" +
                        "        \"metadata_copy\": \"no\",\n" + "        \"strip_chapters\": \"no\",\n" +
                        "        \"pix_format\": \"yuv420p\",\n" + "        \"pan\": { },\n" +
                        "        \"encoder\": \"v1\",\n" + "        \"burnin_timecode\": { },\n" +
                        "        \"copy_timestamps\": \"yes\",\n" + "        \"encryption\": \"no\",\n" +
                        "        \"encryption_method\": \"aes-128-cbc\",\n" + "        \"encryption_key\": { },\n" +
                        "        \"encryption_key_file\": { },\n" + "        \"encryption_iv\": { },\n" +
                        "        \"encryption_password\": { },\n" + "        \"slices\": { },\n" +
                        "        \"audio_stream\": { },\n" + "        \"file_extension\": \"mp4\",\n" +
                        "        \"ftyp\": { },\n" + "        \"hint\": \"no\",\n" + "        \"extends\": { },\n" +
                        "        \"set_rotate\": \"0\",\n" + "        \"copy_nielsen_metadata\": \"no\",\n" +
                        "        \"nielsen_breakout_code\": { },\n" + "        \"nielsen_distributor_id\": { },\n" +
                        "        \"drm\": \"no\",\n" + "        \"drm-content-id\": { },\n" +
                        "        \"drm-common-key\": { },\n" + "        \"drm-license-server-url\": { },\n" +
                        "        \"drm-license-server-cert\": { },\n" + "        \"drm-transport-cert\": { },\n" +
                        "        \"drm-packager-credential\": { },\n" + "        \"drm-credential-pwd\": { },\n" +
                        "        \"drm-policy-file\": { },\n" + "        \"convertedsize\": \"0\",\n" +
                        "        \"queued\": \"0000-00-00 00:00:00\",\n" + "        \"converttime\": \"0\"\n" +
                        "      },\n" + "      {\n" + "        \"id\": \"354831228\",\n" +
                        "        \"status\": \"New\",\n" + "        \"created\": \"2017-02-14 21:53:32\",\n" +
                        "        \"started\": \"0000-00-00 00:00:00\",\n" +
                        "        \"finished\": \"0000-00-00 00:00:00\",\n" + "        \"output\": \"thumbnail\",\n" +
                        "        \"output_preset\": { },\n" +
                        "        \"destination\": \"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/posterframe/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666/first_frame.jpg\",\n" +
                        "        \"protected_public_result\": { },\n" + "        \"size\": \"640x360\",\n" +
                        "        \"bitrate\": { },\n" + "        \"audio_bitrate\": \"64k\",\n" +
                        "        \"audio_sample_rate\": { },\n" + "        \"audio_channels_number\": \"2\",\n" +
                        "        \"downmix_mode\": \"pl2\",\n" + "        \"framerate\": { },\n" +
                        "        \"framerate_upper_threshold\": { },\n" + "        \"fade_in\": { },\n" +
                        "        \"fade_out\": { },\n" + "        \"crop_top\": { },\n" +
                        "        \"crop_left\": { },\n" + "        \"crop_right\": { },\n" +
                        "        \"crop_bottom\": { },\n" + "        \"padd_top\": { },\n" +
                        "        \"padd_left\": { },\n" + "        \"padd_right\": { },\n" +
                        "        \"padd_bottom\": { },\n" + "        \"set_aspect_ratio\": { },\n" +
                        "        \"keep_aspect_ratio\": \"yes\",\n" + "        \"video_codec\": \"mjpeg\",\n" +
                        "        \"profile\": { },\n" + "        \"VCodecParameters\": { },\n" +
                        "        \"video_codec_parameters\": { },\n" + "        \"audio_codec\": { },\n" +
                        "        \"two_pass\": \"no\",\n" + "        \"turbo\": \"no\",\n" +
                        "        \"twin_turbo\": \"no\",\n" + "        \"cbr\": \"no\",\n" +
                        "        \"hard_cbr\": \"no\",\n" + "        \"minrate\": { },\n" +
                        "        \"maxrate\": { },\n" + "        \"bufsize\": { },\n" +
                        "        \"audio_minrate\": { },\n" + "        \"audio_maxrate\": { },\n" +
                        "        \"audio_bufsize\": { },\n" + "        \"rc_init_occupancy\": { },\n" +
                        "        \"deinterlacing\": \"auto\",\n" + "        \"video_sync\": \"old\",\n" +
                        "        \"keyframe\": \"300\",\n" + "        \"start\": { },\n" +
                        "        \"finish\": { },\n" + "        \"duration\": { },\n" +
                        "        \"audio_volume\": \"100\",\n" + "        \"audio_normalization\": { },\n" +
                        "        \"dmg_alerts\": { },\n" + "        \"loudness_mode\": { },\n" +
                        "        \"input_speech\": { },\n" + "        \"clipmode\": { },\n" +
                        "        \"prolimiter_max_peak\": { },\n" + "        \"input_dialnorm\": { },\n" +
                        "        \"adjust_gain_for_dialnorm\": { },\n" + "        \"dialnorm_threshold\": { },\n" +
                        "        \"dc_repair\": { },\n" + "        \"phase_repair\": { },\n" +
                        "        \"dmix_center_level\": { },\n" + "        \"dmix_center_level_ltrt\": { },\n" +
                        "        \"dmix_center_level_loro\": { },\n" + "        \"dmix_surround_level\": { },\n" +
                        "        \"dmix_surround_level_ltrt\": { },\n" +
                        "        \"dmix_surround_level_loro\": { },\n" + "        \"audio_sync\": { },\n" +
                        "        \"rotate\": \"def\",\n" + "        \"noise_reduction\": { },\n" +
                        "        \"two_pass_decoding\": { },\n" + "        \"force_keyframes\": { },\n" +
                        "        \"metadata_copy\": \"no\",\n" + "        \"strip_chapters\": \"no\",\n" +
                        "        \"pix_format\": \"yuv420p\",\n" + "        \"pan\": { },\n" +
                        "        \"encoder\": \"v1\",\n" + "        \"burnin_timecode\": { },\n" +
                        "        \"copy_timestamps\": \"yes\",\n" + "        \"encryption\": \"no\",\n" +
                        "        \"encryption_method\": \"aes-128-cbc\",\n" + "        \"encryption_key\": { },\n" +
                        "        \"encryption_key_file\": { },\n" + "        \"encryption_iv\": { },\n" +
                        "        \"encryption_password\": { },\n" + "        \"slices\": { },\n" +
                        "        \"audio_stream\": { },\n" + "        \"time\": \"0.02\",\n" +
                        "        \"width\": \"640\",\n" + "        \"height\": \"360\",\n" +
                        "        \"file_extension\": \"jpg\",\n" + "        \"use_vtt\": \"no\",\n" +
                        "        \"vtt_line_size\": \"4\",\n" + "        \"convertedsize\": \"0\",\n" +
                        "        \"queued\": \"0000-00-00 00:00:00\",\n" + "        \"converttime\": \"0\"\n" +
                        "      },\n" + "      {\n" + "        \"id\": \"354831231\",\n" +
                        "        \"status\": \"New\",\n" + "        \"created\": \"2017-02-14 21:53:32\",\n" +
                        "        \"started\": \"0000-00-00 00:00:00\",\n" +
                        "        \"finished\": \"0000-00-00 00:00:00\",\n" + "        \"output\": \"thumbnail\",\n" +
                        "        \"output_preset\": { },\n" +
                        "        \"destination\": \"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/thumbnail/1/1/3D4338426E101EB8FB8F97949293B264/1487108538666/thumb.jpg\",\n" +
                        "        \"protected_public_result\": { },\n" + "        \"size\": \"640x360\",\n" +
                        "        \"bitrate\": { },\n" + "        \"audio_bitrate\": \"64k\",\n" +
                        "        \"audio_sample_rate\": { },\n" + "        \"audio_channels_number\": \"2\",\n" +
                        "        \"downmix_mode\": \"pl2\",\n" + "        \"framerate\": { },\n" +
                        "        \"framerate_upper_threshold\": { },\n" + "        \"fade_in\": { },\n" +
                        "        \"fade_out\": { },\n" + "        \"crop_top\": { },\n" +
                        "        \"crop_left\": { },\n" + "        \"crop_right\": { },\n" +
                        "        \"crop_bottom\": { },\n" + "        \"padd_top\": { },\n" +
                        "        \"padd_left\": { },\n" + "        \"padd_right\": { },\n" +
                        "        \"padd_bottom\": { },\n" + "        \"set_aspect_ratio\": { },\n" +
                        "        \"keep_aspect_ratio\": \"yes\",\n" + "        \"video_codec\": \"mjpeg\",\n" +
                        "        \"profile\": { },\n" + "        \"VCodecParameters\": { },\n" +
                        "        \"video_codec_parameters\": { },\n" + "        \"audio_codec\": { },\n" +
                        "        \"two_pass\": \"no\",\n" + "        \"turbo\": \"no\",\n" +
                        "        \"twin_turbo\": \"no\",\n" + "        \"cbr\": \"no\",\n" +
                        "        \"hard_cbr\": \"no\",\n" + "        \"minrate\": { },\n" +
                        "        \"maxrate\": { },\n" + "        \"bufsize\": { },\n" +
                        "        \"audio_minrate\": { },\n" + "        \"audio_maxrate\": { },\n" +
                        "        \"audio_bufsize\": { },\n" + "        \"rc_init_occupancy\": { },\n" +
                        "        \"deinterlacing\": \"auto\",\n" + "        \"video_sync\": \"old\",\n" +
                        "        \"keyframe\": \"300\",\n" + "        \"start\": { },\n" +
                        "        \"finish\": { },\n" + "        \"duration\": { },\n" +
                        "        \"audio_volume\": \"100\",\n" + "        \"audio_normalization\": { },\n" +
                        "        \"dmg_alerts\": { },\n" + "        \"loudness_mode\": { },\n" +
                        "        \"input_speech\": { },\n" + "        \"clipmode\": { },\n" +
                        "        \"prolimiter_max_peak\": { },\n" + "        \"input_dialnorm\": { },\n" +
                        "        \"adjust_gain_for_dialnorm\": { },\n" + "        \"dialnorm_threshold\": { },\n" +
                        "        \"dc_repair\": { },\n" + "        \"phase_repair\": { },\n" +
                        "        \"dmix_center_level\": { },\n" + "        \"dmix_center_level_ltrt\": { },\n" +
                        "        \"dmix_center_level_loro\": { },\n" + "        \"dmix_surround_level\": { },\n" +
                        "        \"dmix_surround_level_ltrt\": { },\n" +
                        "        \"dmix_surround_level_loro\": { },\n" + "        \"audio_sync\": { },\n" +
                        "        \"rotate\": \"def\",\n" + "        \"noise_reduction\": { },\n" +
                        "        \"two_pass_decoding\": { },\n" + "        \"force_keyframes\": { },\n" +
                        "        \"metadata_copy\": \"no\",\n" + "        \"strip_chapters\": \"no\",\n" +
                        "        \"pix_format\": \"yuv420p\",\n" + "        \"pan\": { },\n" +
                        "        \"encoder\": \"v1\",\n" + "        \"burnin_timecode\": { },\n" +
                        "        \"copy_timestamps\": \"yes\",\n" + "        \"encryption\": \"no\",\n" +
                        "        \"encryption_method\": \"aes-128-cbc\",\n" + "        \"encryption_key\": { },\n" +
                        "        \"encryption_key_file\": { },\n" + "        \"encryption_iv\": { },\n" +
                        "        \"encryption_password\": { },\n" + "        \"slices\": { },\n" +
                        "        \"audio_stream\": { },\n" + "        \"time\": \"5%\",\n" +
                        "        \"width\": \"640\",\n" + "        \"height\": \"360\",\n" +
                        "        \"file_extension\": \"jpg\",\n" + "        \"use_vtt\": \"no\",\n" +
                        "        \"vtt_line_size\": \"4\",\n" + "        \"convertedsize\": \"0\",\n" +
                        "        \"queued\": \"0000-00-00 00:00:00\",\n" + "        \"converttime\": \"0\"\n" +
                        "      }\n" + "    ],\n" + "    \"queue_time\": \"0\"\n" + "  }\n" + "}" ) ) );


        MessageChannel inCh = springContext.getBean( "inputUpdateStatusChannel", MessageChannel.class );
        MessagingTemplate msgTemp = new MessagingTemplate();

        msgTemp.send( inCh, MessageBuilder.withPayload( TranscodingFunctions.Result.STATUS_retry_421 ).build());

        DynaTableVideoTranscoding updatedRecorded = transcodingDAO.loadByMediaId( transcodeRecord.getMediaId() );

        Assert.assertEquals( TranscodingFunctions.Result.STATUS_error, updatedRecorded.getStatus() );
        Assert.assertEquals( transcodeRecord.getMediaId(), updatedRecorded.getMediaId() );


        final EncodingExpect encodingExpect0 = encodingServlet.getExpects().get( 0 );
        TranscodingFunctions.Query q0 =
                jsonMapper.fromJson( encodingExpect0.getRequestJson(), TranscodingFunctions.Query.class );

        Assert.assertTrue( q0.getQuery().getMediaid().contains( transcodeRecord.getMediaId() ) );
        Assert.assertEquals( TranscodingFunctions.Query.GetStatus, q0.getQuery().getAction() );
        Assert.assertEquals( "yes", q0.getQuery().getExtended() );
        Assert.assertEquals( "json", q0.getQuery().getNotify_format() );
        Assert.assertEquals( transfunc.getUrlCalledByEncodingCom(), q0.getQuery().getNotify() );
        Assert.assertEquals( transfunc.getUrlCalledByEncodingCom(), q0.getQuery().getNotify_upload() );
        Assert.assertEquals( transfunc.getUrlCalledByEncodingCom(), q0.getQuery().getNotify_encoding_errors() );


        final EncodingExpect encodingExpect1 = encodingServlet.getExpects().get( 1 );
        TranscodingFunctions.Query q1 =
                jsonMapper.fromJson( encodingExpect1.getRequestJson(), TranscodingFunctions.Query.class );

        Assert.assertEquals( TranscodingFunctions.Query.GetMediaInfo, q1.getQuery().getAction() );
        Assert.assertEquals( transcodeRecord.getMediaId(), q1.getQuery().getMediaid() );


        final EncodingExpect encodingExpect2 = encodingServlet.getExpects().get( 2 );
        TranscodingFunctions.Query q2 =
                jsonMapper.fromJson( encodingExpect2.getRequestJson(), TranscodingFunctions.Query.class );

        Assert.assertEquals( TranscodingFunctions.Query.UpdateMedia, q2.getQuery().getAction() );
        Assert.assertEquals( transcodeRecord.getMediaId(), q2.getQuery().getMediaid() );
    }

    private static class EncodingServlet extends HttpServlet{
        @Override
        protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException{
            final String requestJson =
                    URLDecoder.decode( IOUtils.toString( req.getInputStream() ).substring( 5 ), "utf-8" );

            EncodingExpect c = expects.get( expectIndex );

            c.setRequestContentType( req.getContentType() );
            c.setRequestJson( requestJson );

            resp.setContentType( c.responseContentType );
            resp.getOutputStream().write( c.responseStr.getBytes() );

            expectIndex++;
        }

        private int expectIndex = 0;
        private List<EncodingExpect> expects;

        public List<EncodingExpect> getExpects(){
            return expects;
        }

        public void setExpects( List<EncodingExpect> expects ){
            this.expects = expects;
        }
    }

    private static class EncodingExpect{

        public EncodingExpect( String responseContentType, String responseStr ){
            this.responseContentType = responseContentType;
            this.responseStr = responseStr;
        }

        private String requestContentType;
        private String requestJson;

        public String getRequestContentType(){
            return requestContentType;
        }

        public void setRequestContentType( String requestContentType ){
            this.requestContentType = requestContentType;
        }

        public String getRequestJson(){
            return requestJson;
        }

        public void setRequestJson( String requestJson ){
            this.requestJson = requestJson;
        }

        private String responseContentType = "";
        private String responseStr;

        public String getResponseContentType(){
            return responseContentType;
        }

        public String getResponseStr(){
            return responseStr;
        }
    }


}
