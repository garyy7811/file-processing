package org.pubanatomy.test;

import org.pubanatomy.videotranscoding.DynaTableVideoTranscoding;
import org.pubanatomy.videotranscoding.TranscodingDAO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * User: GaryY
 * Date: 1/9/2017
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-test.xml" )
public class TestCallFromEncoding{

    @Autowired
    private ApplicationContext springContext;


    @Autowired
    private TranscodingDAO transcodingDAO;

    @Test
    public void confirmLoad(){

        DynaTableVideoTranscoding videoTranscoding = transcodingDAO.loadByMediaId( "95373153" );
        if( videoTranscoding == null ){
            videoTranscoding = new DynaTableVideoTranscoding();
            videoTranscoding.setMediaId( "95373153" );
            transcodingDAO.save( videoTranscoding );
        }
        else{
            videoTranscoding.setStatus( "sss" );
        }

        final String payload =
                "json={\"result\":{\"mediaid\":\"95373153\",\"source\":\"https://cs-cloud-dev-gary--upload.s3.amazonaws.com/1/1/3D4338426E101EB8FB8F97949293B264/1484072891389.mp4?x-amz-security-token=FQoDYXdzEHwaDIRu1haVrlpjNXv8hSKOAhPPpHYBlM0jhwTVxQill%2BagQY%2FNq41bl5KOBkl1LJdPuTXIZWkT5ZCvTesxBf8C%2FxCVVTclGz9jvEml3xwCQa9MJe%2Fkr6fCirJjX97KXFiab0QDj5OdkE7fuJ%2BGa6J6g1FP94%2FpTMP0oAZEAKaV3z3EHreAbcTR1SBtWx5p%2FGQa1VN0Qh6acuWVkgJcyEOhXDqbEPPRQVg8YZURtVWV1T%2BhX85nhldxBs6vD6DpjFBiiw%2B2w0vgGWIw11SbJVGSa2rYTuxQlPl7VWZA%2BsrtM3%2FcSGyejwq3uJHLhur84wmpAybaOCfQyPiQpTM%2FzaRivIWHiTGDLXSgjI%2BF142toO3dpqsss4%2F4pXUH0rp94yi6z9TDBQ%3D%3D&AWSAccessKeyId=ASIAJD23KJ23CALARPTQ&Expires=1484080112&Signature=quQ4Tg%2FR%2FlVW3%2F5d8q9Wn9O5YIM%3D\",\"status\":\"Finished\",\"description\":[],\"encodinghost\":\"http://manage.encoding.com/\",\"format\":[{\"taskid\":\"336127806\",\"output\":\"thumbnail\",\"status\":\"Finished\",\"destination\":\"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/1/1/3D4338426E101EB8FB8F97949293B264/1484072891389.mp4/640x360_5p.jpeg\"},{\"taskid\":\"336127809\",\"output\":\"thumbnail\",\"status\":\"Finished\",\"destination\":\"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/1/1/3D4338426E101EB8FB8F97949293B264/1484072891389.mp4/200x112_5p.jpeg\"},{\"taskid\":\"336127812\",\"output\":\"thumbnail\",\"status\":\"Finished\",\"destination\":\"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/1/1/3D4338426E101EB8FB8F97949293B264/1484072891389.mp4/640x360_0_02.jpeg\"},{\"taskid\":\"336127815\",\"output\":\"mp4\",\"status\":\"Finished\",\"destination\":\"https://AKIAI5OSSQS7K27QGEMA:2soHBdIMcXX%2FoLiAvehY8YpBuwEoqTk2SPqHAXHL@cs-cloud-dev-gary--download.s3.amazonaws.com/video/1/1/3D4338426E101EB8FB8F97949293B264/1484072891389.mp4/605k.mp4\"}]}}";

        MessageChannel inCh = springContext.getBean( "inputCallFromEncodingComChannel", MessageChannel.class );
        MessagingTemplate msgTemp = new MessagingTemplate();

        msgTemp.send( inCh, MessageBuilder.withPayload( payload ).build() );

        DynaTableVideoTranscoding g = transcodingDAO.loadByMediaId( "95373153" );

        Assert.assertEquals( g.getStatus(), "Finished" );
    }
}
