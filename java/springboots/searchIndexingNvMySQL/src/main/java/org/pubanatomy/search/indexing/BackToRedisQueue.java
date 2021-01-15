package org.pubanatomy.search.indexing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.scripting.support.ResourceScriptSource;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 8/2/2017
 */
public class BackToRedisQueue{

    private Logger logger = LogManager.getLogger( BackToRedisQueue.class );


    public BackToRedisQueue( String searchIndexingRedisQueue, int maxSubResLstLen, int maxSubFolderLstLen,
                             int retryTimes, int redoAfterQueuingSecs, int giveupAfterQueuingSecs,
                             boolean indexFolderRecursively ){
        this.searchIndexingRedisQueue = searchIndexingRedisQueue;
        this.maxSubResLstLen = maxSubResLstLen;
        this.maxSubFolderLstLen = maxSubFolderLstLen;
        this.retryTimes = retryTimes;
        this.redoAfterQueuingSecs = redoAfterQueuingSecs;
        this.giveupAfterQueuingSecs = giveupAfterQueuingSecs;
        this.indexFolderRecursively = indexFolderRecursively;
    }

    private String searchIndexingRedisQueue;

    private int maxSubResLstLen    = 5;
    private int maxSubFolderLstLen = 5;

    private int retryTimes             = 5;
    private int redoAfterQueuingSecs   = 20;
    private int giveupAfterQueuingSecs = 30;

    private boolean indexFolderRecursively;


    @Autowired
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<String> redisScript;

    @Value( "getLstTX.lua" )
    private void setLuaScript( Resource f ){
        redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource( new ResourceScriptSource( f ) );
        redisScript.setResultType( String.class );
    }


    public String pickUUIDListFromRedis(){
        final String rt = redisTemplate.execute( redisScript,
                Arrays.asList( searchIndexingRedisQueue, searchIndexingRedisQueue + "-ING",
                        redoAfterQueuingSecs + "000000" ) );
        if( rt != null ){
            logger.info( rt );
        }
        return rt;
    }

    @Autowired
    private EsTransactionManager esTransactionManager;

    private ThreadLocal<Integer> inputSize = ThreadLocal.withInitial( () -> 0 );

    public void updateAfterCommit( String payload ){
        final String[] split = payload.split( "@" );
        final String ingKey = split[ 0 ];
        Long deleted = redisTemplate.opsForHash().delete( searchIndexingRedisQueue + "-ING", ingKey );

        final Integer processed = esTransactionManager.getCount().get();
        if( ! processed.equals( inputSize.get() ) ){
            logger.warn( "NOT_EQUALS:{}", payload );
        }
        logger.debug( "key:{},deleted:{}, tried:{}, input:{}, processed:{} ", ingKey, deleted, split.length - 1,
                inputSize.get(), processed );

        esTransactionManager.getCount().set( 0 );
        inputSize.set( 0 );
    }

    public boolean backToRedisAfterRollback( String payload ){
        logger.warn( payload );
        final String[] splits = payload.split( "@" );
        final String ingKey = splits[ 0 ];
        final int len = splits.length;
        boolean toSplit = ( inputSize.get() > 1 );
        if( toSplit ){
            Long deleted = redisTemplate.opsForHash().delete( searchIndexingRedisQueue + "-ING", ingKey );
            logger.error( "key:{},deleted:{}, splitting ", ingKey, deleted );
        }
        else{
            final long newSec = System.currentTimeMillis() / 1000;
            if( len - 2 > retryTimes ||
                    ( len > 2 && newSec - Long.parseLong( splits[ 2 ] ) > giveupAfterQueuingSecs ) ){
                Long deleted = redisTemplate.opsForHash().delete( searchIndexingRedisQueue + "-ING", ingKey );
                logger.error( "key:{},deleted:{},Give up msg after retry:{} times", ingKey, deleted, len - 2 );
            }
        }

        esTransactionManager.getCount().set( 0 );
        inputSize.set( 0 );
        return toSplit;
    }

    public Map<String, Object> rawInputStrToUuidLstTypeAndInputSize( String str ) throws IOException{
        final String[] split = str.split( "@" );
        final Map map = objectMapper.readValue( split[ 1 ], Map.class );
        List<String> uuidLst = ( List<String> )map.get( "uuidLst" );
        inputSize.set( uuidLst.size() );
        esTransactionManager.getCount().set( 0 );
        return map;
    }


    public List<String> splitIntoMultiLst( String str ) throws IOException{
        final String[] split = str.split( "@" );
        final Map map = objectMapper.readValue( split[ 1 ], Map.class );
        List<String> uuidLst = ( List<String> )map.get( "uuidLst" );

        final List<String> rt = uuidLst.stream().map( it -> {
            map.put( "uuidLst", Collections.singletonList( it ) );
            try{
                return objectMapper.writeValueAsString( map );
            }
            catch( JsonProcessingException e ){
                throw new RuntimeException( e );
            }
        } ).collect( Collectors.toList() );

        return rt;
    }

    @Autowired
    private ObjectMapper objectMapper;

    public String resIds( List<Map<String, String>> tmp ) throws JsonProcessingException{
        return uuIds( "listChangedResTime", tmp );
    }

    public String folderIds( List<Map<String, String>> tmp ) throws JsonProcessingException{
        return uuIds( "listChangedFolderTime", tmp );
    }

    public String uuIds( String type, List<Map<String, String>> tmp ) throws JsonProcessingException{
        if( tmp == null || tmp.size() == 0 ){
            logger.info( "{} 0 found ...", type );
            return null;
        }

        List<Object> uuidLst = tmp.stream().map( it -> it.get( "uuid" ) ).collect( Collectors.toList() );
        HashMap<String, Object> rt = new HashMap<>();
        rt.put( "uuidLst", uuidLst );
        rt.put( "type", type );
        return objectMapper.writeValueAsString( rt );
    }


    public List<List<Map<String, String>>> splitSubFolderLst( List<Map<String, String>> toSplit ){
        logger.debug( toSplit.size() );
        return splitLargeLst( toSplit, maxSubFolderLstLen );
    }

    public List<List<Map<String, String>>> splitSubResLst( List<Map<String, String>> toSplit ){
        logger.debug( toSplit.size() );
        return splitLargeLst( toSplit, maxSubResLstLen );
    }

    private List<List<Map<String, String>>> splitLargeLst( List<Map<String, String>> toSplit, int maxUuidLen ){

        final List<List<Map<String, String>>> rt = new LinkedList<>();

        List<Map<String, String>> chunk = null;
        for( int i = 0; i < toSplit.size(); i++ ){
            if( chunk == null ){
                chunk = new ArrayList<>( maxUuidLen );
                rt.add( chunk );
            }
            chunk.add( toSplit.get( i ) );
            if( chunk.size() >= maxUuidLen ){
                chunk = null;
            }
        }

        return rt;
    }


    public List<String> filterRecursiveFolderUUIDs( List<ResourceLibraryItem> folders ){
        return folders.stream()
                .filter( it -> ! it.getCreatedTime().equals( it.getModifiedTime() ) && indexFolderRecursively )
                .map( ResourceLibraryItem::getTargetUuid ).collect( Collectors.toList() );
    }

    public void onError( Message msg ){
        logger.error( msg );
    }

    public void onLogging( Message msg ){
        logger.debug( msg );
    }
}
