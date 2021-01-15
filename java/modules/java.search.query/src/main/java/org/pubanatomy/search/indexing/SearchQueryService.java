package org.pubanatomy.search.indexing;

import com.amazonaws.util.IOUtils;
import org.pubanatomy.loginverify.DynamoLoginInfoDAO;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 8/11/2017
 */
@Service
public class SearchQueryService extends ElasticsearchREST{

    protected static Logger logger = LogManager.getLogger( SearchQueryService.class );


    public SearchQueryService( String elasticsearchRESTURLs, String indexName ){
        super( elasticsearchRESTURLs, indexName );
    }


    @Autowired
    private DynamoLoginInfoDAO loginInfoDAO;

    public List<ResourceLibraryItem> queryByString( String csSessionId, String queryStr )
            throws IllegalAccessException, IOException{
        logger.info( "session:{}, queryStr:{}", csSessionId, queryStr );
        loginInfoDAO.loadCsSessionInfo( csSessionId, true );
        Response resp = restClient
                .performRequest( "GET", "/" + indexName + "/resource_library_item/_search", Collections.emptyMap(),
                        new NStringEntity( queryStr ) );

        final String rsltStr = IOUtils.toString( resp.getEntity().getContent() );
        logger.info( "rsltStr:{}", indexName, rsltStr );
        Map<String, Object> aa = objectMapper.readValue( rsltStr, new TypeReference<Map<String, Object>>(){
        } );

        Map<String, Object> bb = ( Map<String, Object> )aa.get( "hits" );
        List<Map<String, Object>> cc = ( List<Map<String, Object>> )bb.get( "hits" );

        List<ResourceLibraryItem> srcLst = cc.stream().map( it -> {
            try{
                return objectMapper
                        .readValue( objectMapper.writeValueAsBytes( it.get( "_source" ) ), ResourceLibraryItem.class );
            }
            catch( IOException e ){
                throw new Error( e );
            }
        } ).collect( Collectors.toList() );
        logger.info( "returning:{} records.", srcLst.size() );
        return srcLst;
    }
}
