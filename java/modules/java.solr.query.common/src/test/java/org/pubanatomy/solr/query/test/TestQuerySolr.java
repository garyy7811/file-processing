package org.pubanatomy.solr.query.test;

import org.pubanatomy.solr.query.common.FacetQuerySolr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.Timestamp;

/**
 * Created by gary.yang.customshow on 5/22/2015.
 */
//@RunWith( SpringJUnit4ClassRunner.class )
//@ContextConfiguration( locations = "classpath:/test-context.xml" )
public class TestQuerySolr{


    private static final Logger logger = LogManager.getLogger( TestQuerySolr.class );

    @Autowired
    private FacetQuerySolr querySolr;


//    @Test
    public void test5_1_subfacet() throws IOException, SolrServerException{

        HttpSolrClient solr = new HttpSolrClient( "http://127.0.0.1:8983/solr/slideshowsession5.3" );
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery( "*:*" );
        solrQuery.setRows( 0 );
        solrQuery.setFilterQueries( "javaClassType:CSReportingShowDurationSolr" );
        String column = "avg_dura";
        String desc = "asc";
        int offset = 20;
        int limit = 30;
        solrQuery.add( "json.facet", "{\n" +
                "    categories: {\n" +
                "        type: terms,\n" +
                "        field: slideShowId__l_t_t_f,\n" +
                "        sort: {" + column + ": " + desc + "},\n" +
                "        offset: " + offset + ",\n" +
                "        limit: " + limit + ",\n" +
                "        facet: {\n" +
                "            avg_dura: \"avg(duration__l_t_t_f)\",\n" +
                "            avg_viewedSlideNum: \"avg(viewedSlideNum__i_t_t_f)\",\n" +
                "            avg_slideNum: \"avg(slideNum__i_t_t_f)\",\n" +
                "            max_slideNum: \"max(lastUpdatedTime__l_t_t_f)\",\n" +
                "            uniq_viewerNum: \"unique(userId__l_t_t_f)\"\n" +
                "        }\n" +
                "    }\n" +
                "}" );
        QueryResponse rslt = solr.query( solrQuery );
        System.out.println( rslt );

    }

    @Test
    public void testtmp() throws Exception{
        logger.debug( "-> {}", new Timestamp( 1429336800000L ));

    }
}
