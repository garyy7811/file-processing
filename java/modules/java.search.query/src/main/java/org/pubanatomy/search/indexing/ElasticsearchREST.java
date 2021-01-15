package org.pubanatomy.search.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 8/11/2017
 */
@Service
public class ElasticsearchREST{

    protected static Logger logger = LogManager.getLogger( ElasticsearchREST.class );

    public ElasticsearchREST( String elasticsearchRESTURLs, String indexName ){
        logger.debug( "{}, {}", elasticsearchRESTURLs, indexName );
        final String[] splitted = elasticsearchRESTURLs.split( ";" );
        HttpHost[] hosts = Arrays.stream( splitted ).map( it -> {
            String[] pp = it.split( ":" );
            return new HttpHost( pp[ 1 ].substring( 2 ), Integer.parseInt( pp[ 2 ] ), pp[ 0 ] );
        } ).collect( Collectors.toList() ).toArray( new HttpHost[ splitted.length ] );

        restClient = RestClient.builder( hosts ).setRequestConfigCallback(
                requestConfigBuilder -> requestConfigBuilder.setConnectTimeout( 5000 ).setSocketTimeout( 60000 )
                        .setConnectionRequestTimeout( - 1 ) ).setMaxRetryTimeoutMillis( 60000 ).build();

        this.indexName = indexName;

        highLevelClient = new RestHighLevelClient( restClient );
    }

    final protected RestClient restClient;

    final protected RestHighLevelClient highLevelClient;


    protected String indexName;


    @Autowired
    protected ObjectMapper objectMapper;


    public RestClient getRestClient(){
        return restClient;
    }

    public RestHighLevelClient getHighLevelClient(){
        return highLevelClient;
    }

    public String getIndexName(){
        return indexName;
    }


}
