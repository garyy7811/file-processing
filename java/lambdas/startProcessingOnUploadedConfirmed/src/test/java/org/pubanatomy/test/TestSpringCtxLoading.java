package org.pubanatomy.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * User: GaryY
 * Date: 1/9/2017
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-context.xml" )
public class TestSpringCtxLoading{

    @Autowired
    private ApplicationContext springContext;

    @Test
    public void confirmLoad(){
        Assert.assertNotNull( springContext );
    }
}
