package org.pubanatomy.amfspringmvc.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * User: flashflexpro@gmail.com
 * Date: 12/5/13
 * Time: 11:24 AM
 */

@Controller
@RequestMapping( "/flashworkerdebuglogging" )
public class FlashPrintLogging{

    private static final Logger log = LogManager.getLogger( FlashPrintLogging.class );

    private static final CopyOnWriteArrayList<Object[]> logLst = new CopyOnWriteArrayList<>();

    private static Thread thread = getThread();

    @RequestMapping( value = "", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    protected void flashworkerdebuglogging( @RequestBody Object[] arr ) throws IOException{
        /**
         * [ getTimer(), index, Worker.current.isPrimordial, level, l ]
         */
        logLst.add( new Object[]{ arr[ 0 ], arr[ 1 ], arr[ 4 ], arr[ 2 ], System.currentTimeMillis(), arr[ 3 ] } );
    }

    public static synchronized Thread getThread(){

        if( thread == null ){
            thread = new Thread( new Runnable(){
                @Override
                public void run(){
                    while( true ){
                        try{
                            Thread.sleep( 300 );
                        }
                        catch( InterruptedException e ){
                            e.printStackTrace();
                            return;
                        }
                        if( logLst.size() <= 0 ){
                            continue;
                        }

                        List<Object[]> lst = new ArrayList<>();

                        for( int li = logLst.size() - 1; li >= 0; li-- ){
                            Object[] tmp = logLst.get( li );
                            long tmpNow = System.currentTimeMillis();
                            if( ( long )tmp[ 4 ] - tmpNow < 1000 ){
                                lst.add( logLst.remove( li ) );
                            }
                        }

                        Collections.sort( lst, new Comparator<Object[]>(){
                            @Override
                            public int compare( Object[] o1, Object[] o2 ){
                                int t1 = ( int )o1[ 0 ];
                                int i1 = ( int )o1[ 1 ];
                                int t2 = ( int )o2[ 0 ];
                                int i2 = ( int )o2[ 1 ];
                                return t1 > t2 ? 1 : ( t1 < t2 ? - 1 : ( i1 > i2 ? 1 : - 1 ) );
                            }
                        } );

                        while( lst.size() > 0 ){
                            Object[] toPrint = lst.remove( 0 );
                            Object rawLog = toPrint[ 2 ];
                            log.debug( "[[[level" + toPrint[ 5 ] + "]]]|||" + toPrint[ 0 ] + "-" + toPrint[ 1 ] + "|" +
                                    toPrint[ 3 ] + "|" + toPrint[ 4 ] + "|" + ":::" + rawLog );
                        }
                    }
                }
            } );
        }
        thread.start();
        return thread;
    }


}
