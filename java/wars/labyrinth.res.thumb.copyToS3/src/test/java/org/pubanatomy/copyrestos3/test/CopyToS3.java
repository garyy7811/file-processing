package org.pubanatomy.copyrestos3.test;

import com.llnw.mediavault.MediaVault;
import com.llnw.mediavault.MediaVaultRequest;
import org.junit.Test;

import javax.xml.xpath.XPathExpressionException;

/**
 * User: GaryY
 * Date: 7/28/2016
 */
public class CopyToS3{

    @Test
    public void testNow() throws XPathExpressionException{

        String url = "https://sgraphics.hs.llnwd.net/o43/s/test/video/mb/184713_1_1000.mp4";
        final MediaVaultRequest options = new MediaVaultRequest( url );
        options.setEndTime( System.currentTimeMillis() + 600000 );
        String uuu = new MediaVault( "jkOP42jU24fIc" ).compute( options );

        //        String httpUrl = "https://sgraphics.hs.llnwd.net/o43/" + uuu;
        //https://sgraphics.hs.llnwd.net/o43/s/test/video/mb/184713_1_1000.mp4?e=1469812279112&h=e712509966ded6113d51653a84e79506
        //https://sgraphics.hs.llnwd.net/o43/s/test/video/mb/184713_1_1000.mp4?e=1469948400&h=280732b652c209fed337acdb2d2332dc
        //https://sgraphics.hs.llnwd.net/o43/s/test/video/mb/184713_1_1000.mp4?h=0364f3d69ec4c0eab5c2c6969c869725
        //https://sgraphics.hs.llnwd.net/o43/s/test/video/mb/184713_1_1000.mp4?h=95c005d8d2cb633e92892dd329a9b8d7


        System.out.println( uuu );

    }
}
