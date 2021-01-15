package org.pubanatomy.test.unit;

import org.pubanatomy.search.indexing.ElasticsearchREST;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: GaryY
 * Date: 9/30/2017
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = "/root-test.xml" )
public class TestMissingRecords{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ElasticsearchREST esRestAPI;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private SourcePollingChannelAdapter pullingFromRedis;


    @Autowired
    private DirectChannel errorChannel;


    @Autowired
    private DirectChannel       esItemLst;
    @Autowired
    private EventDrivenConsumer addToEs;

    @Autowired
    private DirectChannel                    outToQueueStrNoNull;
    @Autowired
    private RedisQueueOutboundChannelAdapter redisOutboundAdapter;


    @Test//so there is at least one test
    public void dummy(){
        System.out.println( "--dummy--" );
    }

    public void tstSingle() throws InterruptedException{
        pullingFromRedis.setAutoStartup( false );
        pullingFromRedis.setSource( new AbstractMessageSource<String>(){
            @Override
            protected String doReceive(){
                return "1506701068185897@{\"uuidLst\":[\"518f4da359d28a280159d63c7a7b0007\",\"518f4da359d28a280159d67451a50008\",\"518f4da359d28a280159d67454e30009\",\"518f4da359d28a280159d6752b44000a\",\"518f4da359d28a280159d67681db000b\",\"518f4da359d6cf3a0159d6e54e5d0003\",\"518f4da359d6cf3a0159d728b5f70004\",\"518f4da359d6cf3a0159d72a20240005\",\"518f4da359d6cf3a0159d72e631c0006\",\"518f4da359d6cf3a0159d72eb9600007\",\"518f4da359d6cf3a0159d72f89ac0008\",\"518f4da359d6cf3a0159d7385c35000c\",\"518f4da359d6cf3a0159d738c22b000d\",\"518f4da359d6cf3a0159d738c4e1000e\",\"518f4da359d6cf3a0159d77a631f0012\",\"518f4da359d6cf3a0159d785d31d0014\",\"518f4da359d6cf3a0159d78a8b6b001e\",\"518f4da359d6cf3a0159d78ae0cc001f\",\"518f4da359d6cf3a0159d79772390020\",\"518f4da359d6cf3a0159d7979f3d0021\",\"518f4da359d6cf3a0159d797a9370022\",\"518f4da359d6cf3a0159d798b17c0024\",\"518f4da359d6cf3a0159d799e8480025\",\"518f4da359db0ead0159dbde4704000e\",\"518f4da359db0ead0159dbdeb9fe000f\",\"518f4da359db0ead0159dbded10b0010\",\"518f4da359db0ead0159dc4ce9ad001b\",\"518f4da359db0ead0159dc4d662b001c\",\"518f4da359db0ead0159dc4faac0001d\",\"518f4da359dd12830159e07556110004\",\"518f4da359dd12830159e07599740006\",\"518f4da359dd12830159e07629110008\",\"518f4da359dd12830159e076322b000a\",\"518f4da359dd12830159e076f579000b\",\"518f4da359dd12830159e078cca7000d\",\"518f4da359dd12830159e09abac90012\",\"518f4da359dd12830159e09aff2c0013\",\"518f4da359dd12830159e09cdcbb0015\",\"518f4da359dd12830159e09ce1c20016\",\"518f4da359dd12830159e1423d91002b\",\"518f4da359dd12830159e1a377e9002f\",\"518f4da359dd12830159f5618712006f\",\"518f4da359dd12830159f56c1cb20071\",\"518f4da359dd12830159f56c54280072\",\"518f4da359dd12830159f56efd530075\",\"518f4da359dd12830159f5ae6fd9020f\",\"518f4da359dd12830159f5ef97af0239\",\"518f4da359dd12830159f5efea84023c\",\"518f4da359dd12830159f5eff6ca023d\",\"518f4da359dd12830159f5f0489c023e\",\"518f4da359dd12830159f5f065b9023f\",\"518f4da359dd12830159f61e239c0255\",\"518f4da359dd12830159f622a40a0257\",\"518f4da359dd12830159f63141050265\",\"518f4da359dd12830159f63144240266\"],\"time\":\"2014-07-11 13:02:20.000 +0000\",\"type\":\"listChangedResTime\"}";
            }

            @Override
            public String getComponentType(){
                return "inbound-channel-adapter";
            }
        } );

        final List<Message> rsltMsgLst = new LinkedList<>();
        final List<Message> backLstMsgLst = new LinkedList<>();
        addToEs.stop();
        esItemLst.subscribe( new MessageHandler(){
            @Override
            public void handleMessage( Message<?> message ) throws MessagingException{
                rsltMsgLst.add( message );
                System.out.println( message );
            }
        } );

        outToQueueStrNoNull.unsubscribe( redisOutboundAdapter );
        outToQueueStrNoNull.subscribe( new MessageHandler(){
            @Override
            public void handleMessage( Message<?> message ) throws MessagingException{
                backLstMsgLst.add( message );
            }
        } );

        tstWire.subscribe( new MessageHandler(){
            @Override
            public void handleMessage( Message<?> message ) throws MessagingException{
                MessageHistory history = ( MessageHistory )message.getHeaders().get( "history" );
                if( history != null && history.size() > 1 ){
                    System.out.println(
                            "history>>>" + StringUtils.collectionToCommaDelimitedString( history.stream().map( it -> {
                                return it.get( "name" );
                            } ).collect( Collectors.toList() ) ) );
                }
                System.out.println( "payload>>>" + message.getPayload() );
            }
        } );


        pullingFromRedis.start();

        while( true ){
            Thread.currentThread().sleep( 1111 );
        }


    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DirectChannel tstWire;


}
