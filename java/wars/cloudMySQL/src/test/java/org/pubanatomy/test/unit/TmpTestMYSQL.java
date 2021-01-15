package org.pubanatomy.test.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/tmpTstMysql.xml" )
public class TmpTestMYSQL{


    @Value( "rslt.json" )
    private Resource rsltJson;

    @Autowired
    private JdbcTemplate nvJdbcTemplate;


    @Test
    public void ssss() throws IOException{


        String inputBody = "\n" + "{\n" + "    \"a\": \"D30B1D7C-6C1E-0439-FDDE-A68B06CC8447\"\n" + "}";


        //legacy-->>                _urlRequest.data = "{\"a\":\"" + cdr.toString() + "\"}";
        final TreeMap<String, String> tmpHeaderMap = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        tmpHeaderMap.put( "content-TYpe", "appliCAtion/JSON" );
        if( "application/json".equalsIgnoreCase( tmpHeaderMap.get( "content-type" ) ) ){
            try{
                Map<String,String> a= new ObjectMapper(  ).readValue( inputBody, Map.class );
                String tmp = a.get( "a" );
                if( tmp != null && tmp.length() > 3 ){
                    inputBody = tmp;
                }
            }
            catch( Throwable e ){
                e.printStackTrace();
            }
        }



        final ObjectMapper objectMapper = new ObjectMapper();
        List<Map> infoLst = objectMapper.readValue( rsltJson.getFile(), List.class );

        List<Map> sortedLst = infoLst.stream().sorted( ( a, b ) -> {
            return ( Integer )a.get( "bitrate" ) > ( Integer )b.get( "bitrate" ) ? - 1 : 1;
        } ).filter( k -> {
            return ( ! ( ( String )k.get( "username" ) ).endsWith( "sg.com" ) &&
                    ! ( ( String )k.get( "username" ) ).endsWith( "cs.cc" ) );
        } ).collect( Collectors.toList() );

        final String x = objectMapper.writeValueAsString( sortedLst );
        System.out.println( x );

    }
}
