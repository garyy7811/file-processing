package org.pubanatomy.test.unit;

import org.pubanatomy.labyrinth.mysql.QueryNewVictoryMysql;
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
public class TestMySQLRpc{

    @Autowired
    private QueryNewVictoryMysql queryNewVictoryMysql;


    @Test
    public void testUploadAuthen() throws Exception{
        Long aa = queryNewVictoryMysql.loadResourceMaxRecordId( true );
        System.out.println( "" );

    }

}
