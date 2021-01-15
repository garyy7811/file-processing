package org.pubanatomy.search.indexing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.io.IOException;

/**
 * User: GaryY
 * Date: 10/12/2017
 */
public class EsTransactionManager extends PseudoTransactionManager{

    @Autowired
    private OutToElasticsearch esApi;

    private ThreadLocal<Integer> count = ThreadLocal.withInitial( () -> ( 0 ) );

    public ThreadLocal<Integer> getCount(){
        return count;
    }

    @Override
    protected void doCommit( DefaultTransactionStatus status ) throws TransactionException{
        try{
            count.set( esApi.commitEsItems() );
        }
        catch( IOException e ){
            throw new RuntimeException( e );
        }
    }
}
