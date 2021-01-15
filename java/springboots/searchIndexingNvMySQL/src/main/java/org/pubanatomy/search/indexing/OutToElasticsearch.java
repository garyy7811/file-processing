package org.pubanatomy.search.indexing;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * User: GaryY
 * Date: 8/2/2017
 */
public class OutToElasticsearch extends ElasticsearchREST{

    protected Logger logger = LogManager.getLogger( this.getClass() );

    public OutToElasticsearch( String elasticsearchRESTURLs, String indexName ){
        super( elasticsearchRESTURLs, indexName );
    }

    private ThreadLocal<List<ResourceLibraryItem>> esItemLst = ThreadLocal.withInitial( LinkedList::new );
    private ThreadLocal<List<String>>              esDelLst  = ThreadLocal.withInitial( LinkedList::new );

    private final static String resource_library_item = "resource_library_item";

    public void items( List<ResourceLibraryItem> items ){
        esItemLst.get().addAll( items );
    }

    public int commitEsItems() throws IOException{
        Date now = new Date( System.currentTimeMillis() );

        BulkRequest br = new BulkRequest();
        final int toInsertCount = esItemLst.get().size();
        if( toInsertCount > 0 ){
            esItemLst.get().forEach( it -> {
                final IndexRequest request = new IndexRequest( indexName, resource_library_item, it.getTargetUuid() );
                try{
                    it.setIndexedTime( now );
                    request.source( objectMapper.writeValueAsString( it ), XContentType.JSON );
                }
                catch( JsonProcessingException e ){
                    throw new RuntimeException( e );
                }
                br.add( request );
            } );


        }
        final int toDeleteCount = esDelLst.get().size();
        if( toDeleteCount > 0 ){
            esDelLst.get().forEach( it -> {
                br.add( new DeleteRequest( indexName, resource_library_item, it ) );
            } );
        }

        if( toInsertCount + toDeleteCount == 0 ){
            return 0;
        }

        final BulkResponse blkRsps = highLevelClient.bulk( br );

        if( blkRsps.hasFailures() ){
            throw new RuntimeException( blkRsps.buildFailureMessage() );
        }
        final int deletedCount = toDeleteCount;
        logger.info( "{} inserted into, {} deleted from ES", toInsertCount, deletedCount );
        esItemLst.get().clear();
        esDelLst.get().clear();
        return toInsertCount + deletedCount;
    }

    public void delItemsUuidLst( List<String> itemUuidLst ){
        esDelLst.get().addAll( itemUuidLst );
    }

}
