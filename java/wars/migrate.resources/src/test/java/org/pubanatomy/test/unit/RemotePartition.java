package org.pubanatomy.test.unit;

import org.pubanatomy.batchpartition.RangePartitionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/27/2016
 * Time: 5:08 PM
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-test.xml" )
public class RemotePartition{

    @Autowired
    private RangePartitionService slideResourceContentPartitioning;


    @Autowired
    private RangePartitionService slideThumbnailPartitioning;

    @Test
    public void testPartitioningWithSample() throws InterruptedException{
        if( true ){
            return;
        }

        boolean exit = false;
        while( ! exit ){
            System.out.print( ">>>>>>" );
            Thread.currentThread().sleep( 2222 );
            System.out.println( "<<<<<<" );
        }
    }

    public static class DoJobMock{
        private static final Logger logger = LogManager.getLogger( DoJobMock.class );

        public DoJobMock( RangePartitionService partitioner ){
            this.partitioner = partitioner;
        }

        private RangePartitionService partitioner;


        public Long[] getRange(){
            final Long[] rt = partitioner.allocateRange( 2000L );
            logger.info( "returning {}", StringUtils.join( rt, "-" ) );
            return rt;
        }

        public Long[] work( Long[] range ) throws InterruptedException{
            long millis = new Double( Math.random() * 5000 ).longValue();
            millis += 3000L;
            final String rangStr = StringUtils.join( range, "-" );
            logger.info( "{}--------------------------------------------{}>>>>>", rangStr, millis );
            Thread.currentThread().sleep( millis );
            logger.info( "{}--------------------------------------------{}<<<<<", rangStr, millis );
            return range;
        }

        public void doneRange( Long[] range ){
            logger.info( "{}-{}>>==>>", range[ 0 ], range[ 1 ] );
            partitioner.doneRange( range[ 0 ], range[ 1 ], Thread.currentThread().getName() );
            logger.info( "{}-{}<<==<<", range[ 0 ], range[ 1 ] );
        }
    }


}
