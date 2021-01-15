package org.pubanatomy.search.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 7/26/2017
 */
public class JavaCode{

    public JavaCode( String dateFormatPattern, String redisQueue, long redisQueueSizeLimit ){
        this.dateFormatPattern = dateFormatPattern;
        this.redisQueue = redisQueue;
        this.redisQueueSizeLimit = redisQueueSizeLimit;
    }

    protected Logger logger = LogManager.getLogger( this.getClass() );

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String dateFormatPattern;
    private String redisQueue;
    private long   redisQueueSizeLimit;

    private ThreadLocal<SimpleDateFormat> simpleDateFormatter = new ThreadLocal<SimpleDateFormat>(){
        @Override
        protected SimpleDateFormat initialValue(){
            return new SimpleDateFormat( dateFormatPattern );
        }
    };

    @Autowired
    private ObjectMapper objectMapper;

    public Timestamp getTimeFromRedis( String key ) throws ParseException{
        final Long size = redisTemplate.opsForList().size( redisQueue );
        if( size > redisQueueSizeLimit ){
            logger.warn( "Redis queue size {} over limit {} !!!", size, redisQueueSizeLimit );
            return null;
        }

        final String str = redisTemplate.opsForValue().get( key );
        if( str == null || str.isEmpty() ){
            return new Timestamp( 0L );
        }

        final Timestamp rt = new Timestamp( simpleDateFormatter.get().parse( str ).getTime() );

        if( key.indexOf( "Deleted" ) > 0 ){

            String chStr = redisTemplate.opsForValue().get( key.replaceAll( "Deleted", "Changed" ) );
            try{
                if( chStr == null || chStr.equals( "" ) ||
                        new Timestamp( simpleDateFormatter.get().parse( chStr ).getTime() ).getTime() < rt.getTime() ){
                    return null;
                }
            }
            catch( Exception e ){
                e.printStackTrace();
                return null;
            }

        }

        logger.debug( "{} --> {}, {} records", key, str, size );
        return rt;
    }

    public String outToRedisQueue( List<Map<String, Object>> tmp ) throws JsonProcessingException{
        final Set<String> keySet = tmp.get( 0 ).keySet();
        String tmpType = keySet.stream().filter( it -> ! it.equals( "uuid" ) ).collect( Collectors.toList() ).get( 0 );
        List<Object> uuidLst = tmp.stream().map( it -> it.get( "uuid" ) ).collect( Collectors.toList() );
        final Map<String, Object> last = tmp.get( tmp.size() - 1 );
        HashMap<String, Object> rt = new HashMap<>();
        rt.put( "type", tmpType );
        rt.put( "uuidLst", uuidLst );
        rt.put( "time", simpleDateFormatter.get().format( last.get( tmpType ) ) );

        return objectMapper.writeValueAsString( rt );
    }

    public void updateTimeAfterCommit( List<Map<String, Object>> payload ){
        final String uuidLst = StringUtils.collectionToCommaDelimitedString(
                payload.stream().map( it -> ( String )it.get( "uuid" ) ).collect( Collectors.toList() ) );
        Map<String, Object> el = payload.get( payload.size() - 1 );
        el.remove( "uuid" );
        Map.Entry<String, Object> pt = el.entrySet().iterator().next();
        redisTemplate.opsForValue().set( pt.getKey(), simpleDateFormatter.get().format( pt.getValue() ) );
        logger.info( "type:{}, {} uuids:{}", pt.getKey(), payload.size(), uuidLst );
    }

}
