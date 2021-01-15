/**
 * 
 */
package com.llnw.mediavault.test;

import com.llnw.mediavault.MediaVault;
import com.llnw.mediavault.MediaVaultRequest;

import java.util.Calendar;

/**
 * @author johnb
 *
 */
public class LLNWMediaVaultTester{
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MediaVault mv = new MediaVault("md5test");
        
        System.out.println(" -- No Parameters -- ");
        System.out.println(mv.compute(new MediaVaultRequest("/a39/o1/secure/wrath_of_charley2-1")));
        System.out.println("\n");
        
        System.out.println(" -- Time Parameters -- ");
        System.out.println(mv.compute(getTimeParameters()));
        System.out.println("\n");

        System.out.println(" -- IP (Range) and Page URL Parameters -- ");
        System.out.println(mv.compute(getIPandPageParameters()));
        System.out.println("\n");

        System.out.println(" -- IP (Exact) and Referrer Parameters -- ");
        System.out.println(mv.compute(getIPandReferrerParameters()));
        System.out.println("\n");

        System.out.println(" -- Referrer and Page URL Parameters -- ");
        System.out.println(mv.compute(getReferrerAndPageParameters()));
        System.out.println("\n");

        System.out.println(" -- All Parameters -- ");
        System.out.println(mv.compute(getAllParameters()));
	}
	
	 static MediaVaultRequest getTimeParameters()
     {
		 long unixNow = Calendar.getInstance().getTimeInMillis()/1000;
         MediaVaultRequest options = new MediaVaultRequest("/a39/o1/secure/wrath_of_charley2-1");
         options.setStartTime(unixNow);
         options.setEndTime(unixNow + 300); //5 minutes from now
         return options;
     }

     static MediaVaultRequest getIPandPageParameters()
     {
         MediaVaultRequest options = new MediaVaultRequest("/a39/o1/secure/wrath_of_charley2-1");
         options.setIPAddress("1.2.3.0/24");
         options.setPageURL("http://www.somesite.com/referrer.html");
         return options;
     }

     static MediaVaultRequest getIPandReferrerParameters()
     {
         MediaVaultRequest options = new MediaVaultRequest("/a39/o1/secure/wrath_of_charley2-1");
         options.setIPAddress("1.2.3.38");
         options.setReferrer("http://www.somesite.com/referrer.swf");
         return options;
     }

     static MediaVaultRequest getReferrerAndPageParameters()
     {
         MediaVaultRequest options = new MediaVaultRequest("/a39/o1/secure/wrath_of_charley2-1");
         options.setReferrer("http://www.somesite.com/referrer.swf");
         options.setPageURL("http://www.somesite.com/referrer.html");
         return options;
     }

     static MediaVaultRequest getAllParameters()
     {
    	 long unixNow = Calendar.getInstance().getTimeInMillis()/1000;
         MediaVaultRequest options = new MediaVaultRequest("/a39/o1/secure/wrath_of_charley2-1");
         options.setStartTime(unixNow);
         options.setEndTime(unixNow + 300); //5 minutes from now
         options.setIPAddress("1.2.3.0/24");
         options.setReferrer("http://www.somesite.com/referrer.swf");
         options.setPageURL("http://www.somesite.com/referrer.html");
         return options;
     }
}