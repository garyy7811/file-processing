package org.pubanatomy.test.unit;

import com.amazonaws.util.IOUtils;
import org.pubanatomy.search.indexing.ElasticsearchREST;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 9/30/2017
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-test.xml" )
public class TestMatchMysqlVsElasticsearch{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ElasticsearchREST esRestAPI;

    @Autowired
    private ObjectMapper objectMapper;


    private List<Integer> rslt;


    @Value( "/countRangeRes.sql" )
    private Resource rangeSqlRes;
    @Value( "/countRangeFolder.sql" )
    private Resource rangeSqlFolder;

    @Value( "/esQlRes.json" )
    private Resource esQlRes;
    @Value( "/esQlFolder.json" )
    private Resource esQlFolder;
    @Value( "/detailRes.sql" )
    private Resource detailRes;
    @Value( "/detailFolder.sql" )
    private Resource detailFolder;

    @Test//so there is at least one test
//    public void dummy(){
//        System.out.println( "--dummy--");
//    }


    /**
     * Compare counts between MySQL and Elasticsearch with by divide and conquer
     *
     * @throws IOException
     */
    //    @Test
    public void tmpTst() throws IOException{
        compareMySqlAndEs( detailRes, rangeSqlRes, esQlRes );
        compareMySqlAndEs( detailFolder, rangeSqlFolder, esQlFolder );
    }

    private void compareMySqlAndEs( Resource detail, Resource rangeSql, Resource esQl ) throws IOException{
        rslt = new LinkedList<>();
        findMissing( rangeSql, esQl, 1000, 40000000 );
        if( rslt.size() == 0 ){
            System.out.println( "WOW  perfect match!" );
            return;
        }

        final String delimitedIDs = StringUtils.collectionToCommaDelimitedString( rslt );
        List<Map<String, Object>> id2uLst = jdbcTemplate
                .queryForList( IOUtils.toString( detail.getInputStream() ).replaceAll( "\\?", delimitedIDs ) );

        System.out.println( ">>>>>" + delimitedIDs );

        System.out.println( objectMapper.writeValueAsString( id2uLst.stream().map( it -> {
            return ( String )it.get( "uuid" );
        } ).collect( Collectors.toList() ) ) );


        System.out.println( "<<<<<" );
    }


    private void findMissing( Resource rangeSql, Resource esQl, Integer rangeFrom, Integer rangeTo ) throws IOException{
        if( rangeFrom.equals( rangeTo ) ){
            return;
        }
        Integer fromMySql = jdbcTemplate
                .queryForObject( IOUtils.toString( rangeSql.getInputStream() ), ( rs, rowNum ) -> rs.getInt( "c" ),
                        rangeFrom, rangeTo );

        String esStr = IOUtils.toString( esQl.getInputStream() );
        esStr = esStr.replaceAll( "22222", rangeFrom.toString() );
        esStr = esStr.replaceAll( "33333", rangeTo.toString() );
        Response esRslt = esRestAPI.getRestClient()
                .performRequest( "GET", "/" + esRestAPI.getIndexName() + "/_search", Collections.emptyMap(),
                        new NStringEntity( esStr ) );

        Map m = objectMapper.readValue( IOUtils.toString( esRslt.getEntity().getContent() ), Map.class );
        Map hits = ( Map )m.get( "hits" );
        Integer fromEs = ( Integer )hits.get( "total" );

        if( fromMySql.equals( fromEs ) ){
            return;
        }
        System.out.println(
                "rangeFrom:" + rangeFrom + ", rangeTo:" + rangeTo + ", MySQL:" + fromMySql + ", ES:" + fromEs );
        if( fromEs.equals( 0 ) && fromMySql == ( rangeTo - rangeFrom ) ){
            rslt.add( rangeFrom );
            return;
        }
        if( fromMySql.equals( 0 ) ){
            System.out.println( "OH NO!!!" );
            return;
        }


        final Double middle = Math.floor( rangeFrom + ( rangeTo - rangeFrom ) / 2 );
        findMissing( rangeSql, esQl, rangeFrom, middle.intValue() );
        findMissing( rangeSql, esQl, middle.intValue(), rangeTo );
    }
}
