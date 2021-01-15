package com.customshow.labyrinth.fontgen;

import com.customshow.labyrinth.mysql.QueryNewVictoryMysql;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * User: flashflexpro@gmail.com
 * Date: 4/11/2016
 * Time: 3:23 PM
 */
public class FindMissingFonts extends DefaultTask{

    public static final String NAME = "fontgenFindMissingFonts";

    protected static ClassPathXmlApplicationContext applicationContext;


    private String remoteMysqlWebserviceUrl;
    private Long   remoteMysqlWebserviceTimeout = 5000L;

    public String getRemoteMysqlWebserviceUrl(){
        return remoteMysqlWebserviceUrl;
    }

    public void setRemoteMysqlWebserviceUrl( String remoteMysqlWebserviceUrl ){
        this.remoteMysqlWebserviceUrl = remoteMysqlWebserviceUrl;
    }

    public Long getRemoteMysqlWebserviceTimeout(){
        return remoteMysqlWebserviceTimeout;
    }

    public void setRemoteMysqlWebserviceTimeout( Long remoteMysqlWebserviceTimeout ){
        this.remoteMysqlWebserviceTimeout = remoteMysqlWebserviceTimeout;
    }

    public String resultJsonStr = null;
    public String getResultJsonStr(){
        return resultJsonStr;
    }

    @TaskAction
    public void actionNow() throws IOException{
        initSpringContext( remoteMysqlWebserviceUrl, remoteMysqlWebserviceTimeout);

        QueryNewVictoryMysql mySql = applicationContext.getBean( QueryNewVictoryMysql.class );
        List<Map<String, Object>> fonts = mySql.loadMissingFonts( true );

        if( fonts.size() > 0 ){
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable( SerializationFeature.INDENT_OUTPUT );

            resultJsonStr = objectMapper.writeValueAsString( fonts );
        }
        else{
            System.out.println( "Congrats!!! No missing fonts found from" + remoteMysqlWebserviceUrl );
        }
    }

    protected static void initSpringContext( String url, Long timeout ){
        if( url == null ){
            throw new IllegalArgumentException( "remoteMysqlWebserviceUrl can't be null" );
        }
        if( timeout == null ){
            throw new IllegalArgumentException( "remoteMysqlWebserviceTimeout can't be null" );
        }

        if( applicationContext == null ){
            applicationContext =
                    new ClassPathXmlApplicationContext( new String[]{ "classpath*:mysql.remote.query.xml" }, false );
            Properties source = new Properties();

            source.put( "remoteMysqlWebserviceUrl", url );
            source.put( "remoteMysqlWebserviceTimeout", timeout );

            applicationContext.getEnvironment().getPropertySources()
                    .addFirst( new PropertiesPropertySource( "options", source ) );
            applicationContext.refresh();
        }
    }


}
