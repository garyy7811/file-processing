package com.customshow.labyrinth.fontgen;

import com.customshow.labyrinth.mysql.QueryNewVictoryMysql;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User: flashflexpro@gmail.com
 * Date: 4/11/2016
 * Time: 3:23 PM
 */
public class LoadAllCsFontsRes extends FindMissingFonts{

    public static final String NAME = "fontgenLoadAllFontsRes";


    public String fontFileHost;// = "https://app.cs.cc/font";

    public File cssAndFontOutputFolder;// = new File( "d:/tmp" );

    public String getFontFileHost(){
        return fontFileHost;
    }

    public void setFontFileHost( String fontFileHost ){
        this.fontFileHost = fontFileHost;
    }

    public File getCssAndFontOutputFolder(){
        return cssAndFontOutputFolder;
    }

    public void setCssAndFontOutputFolder( File cssAndFontOutputFolder ){
        this.cssAndFontOutputFolder = cssAndFontOutputFolder;
    }

    @TaskAction
    @Override
    public void actionNow() throws IOException{
        if( cssAndFontOutputFolder == null || ! cssAndFontOutputFolder.exists() ||
                ! cssAndFontOutputFolder.isDirectory() ){
            throw new IllegalArgumentException(
                    "ssAndFontOutputFolder must be a directory!" + cssAndFontOutputFolder.getAbsolutePath() );
        }
        initSpringContext( getRemoteMysqlWebserviceUrl(), getRemoteMysqlWebserviceTimeout() );
/*
        QueryNewVictoryMysql mySql = applicationContext.getBean( QueryNewVictoryMysql.class );
        List<Map<String, Object>> fonts = mySql.loadAllFonts( true );

        if( fonts.size() > 0 ){

        }
    }


    public static void main( String[] args ) throws IOException{
        initSpringContext( "http://localhost:8084/cloudMySQL/api", 5000L );*/

        QueryNewVictoryMysql mySql = applicationContext.getBean( QueryNewVictoryMysql.class );
        List<Map<String, Object>> fonts = mySql.loadAllFonts( true );

        String cssStr = "";
        System.out.println( fonts.size() + " fonts to load:" );
        if( fonts.size() > 0 ){
            if( ! fontFileHost.endsWith( "/" ) ){
                fontFileHost += "/";
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable( SerializationFeature.INDENT_OUTPUT );

            cssStr = fonts.stream().map( v -> {
                URL website = null;
                String swfFileName = ( String )v.get( "swf_file_name" );
                System.out.println( "processing " + swfFileName );
                try{
                    website = new URL( fontFileHost + swfFileName );
                    ReadableByteChannel rbc = Channels.newChannel( website.openStream() );
                    File fontFolder = new File( cssAndFontOutputFolder.getAbsoluteFile() + "/customFonts/" );
                    fontFolder.mkdirs();
                    FileOutputStream fos = new FileOutputStream( fontFolder.getAbsolutePath() + "/" + swfFileName );
                    fos.getChannel().transferFrom( rbc, 0, Long.MAX_VALUE );
                }
                catch( Exception e ){
                    System.out.println( "error downloading or writing swf font:" + swfFileName );
                    e.printStackTrace();
                }


                String jsonStr = null;
                try{
                    jsonStr = objectMapper.writeValueAsString( v );
                }
                catch( JsonProcessingException e ){
                    e.printStackTrace();
                    jsonStr = e.getMessage();
                }

                if( v.get( "has_reg" ).toString().equals( "0" ) ){
                    System.out.println( "ignoring :" + jsonStr );
                    return "/*IGNORING NO REG FONT " + jsonStr + "*/";
                }

                String css = "/*" + jsonStr + "*/\r\n@font-face{\r\n" +
                        "   src:url(\"customFonts/" + swfFileName + "\");\r\n" +
                        "   fontFamily:\"" + v.get( "font_face_name" ) + "\";\r\n" +
                        "}\r\n";
                if( v.get( "has_bold" ).toString().equals( "1" ) ){
                    css += "@font-face{\r\n" +
                            "   src:url(\"customFonts/" + swfFileName + "\");\r\n" +
                            "   fontFamily:\"" + v.get( "font_face_name" ) + "\";\r\n" +
                            "   fontWeight:bold;\r\n" +
                            "}\r\n";
                }
                if( v.get( "has_italic" ).toString().equals( "1" ) ){
                    css += "@font-face{\r\n" +
                            "   src:url(\"customFonts/" + swfFileName + "\");\r\n" +
                            "   fontFamily:\"" + v.get( "font_face_name" ) + "\";\r\n" +
                            "   fontStyle:italic;\r\n" +
                            "}\r\n";
                }
                if( v.get( "has_bi" ).toString().equals( "1" ) ){
                    css += "@font-face{\r\n" +
                            "   src:url(\"customFonts/" + swfFileName + "\");\r\n" +
                            "   fontFamily:\"" + v.get( "font_face_name" ) + "\";\r\n" +
                            "   fontWeight:bold;\r\n" +
                            "   fontStyle:italic;\r\n" +
                            "}";
                }
                return css;
            } ).sorted( ( a, b ) -> {
                boolean ai = a.indexOf( "/*IGNORING NO REG FONT " ) == 0;
                boolean bi = b.indexOf( "/*IGNORING NO REG FONT " ) == 0;
                return ( ai == bi ) ? 0 : ( ai ? - 1 : 1 );
            } ).collect( Collectors.joining( "\r\n\r\n\r\n" ) );
        }
        else{
            cssStr = "/*NO FONTS FOUND!*/";
        }
        cssStr = "/*generated by customshow fontgen from " + fontFileHost + "*/\r\n\r\n\r\n" + cssStr;

        String cssPath = cssAndFontOutputFolder.getAbsolutePath() + "/customFonts.css";
        Files.write( Paths.get( cssPath ), cssStr.getBytes() );
        System.out.println( "done:" + cssPath );
    }
}
