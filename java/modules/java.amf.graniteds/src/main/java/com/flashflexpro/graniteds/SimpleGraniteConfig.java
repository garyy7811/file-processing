package com.flashflexpro.graniteds;

import org.granite.config.GraniteConfig;
import org.granite.context.GraniteContext;
import org.granite.context.SimpleGraniteContext;
import org.granite.messaging.AliasRegistry;
import org.granite.messaging.amf.io.AMF3Deserializer;
import org.granite.messaging.amf.io.AMF3DeserializerSecurizer;
import org.granite.messaging.amf.io.AMF3Serializer;
import org.granite.messaging.amf.io.RegexAMF3DeserializerSecurizer;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * User: flashflexpro@gmail.com
 * Date: 10/18/2015
 * Time: 10:16 PM
 */
public class SimpleGraniteConfig extends GraniteConfig{

    public SimpleGraniteConfig( AMF3DeserializerSecurizer deserializerSecurizer ) throws IOException, SAXException{
        this( deserializerSecurizer, null );
    }

    public SimpleGraniteConfig( AMF3DeserializerSecurizer deserializerSecurizer, AliasRegistry registry )
            throws IOException, SAXException{
        super( null, null, null, null );
        if( deserializerSecurizer != null ){
            setAmf3DeserializerSecurizer( deserializerSecurizer );
        }
        if( registry != null ){
            setAliasRegistry( registry );
        }
    }


    public Object decode( ByteBuffer message ) throws IOException{
        SimpleGraniteContext.createThreadInstance( this, null, null );

        byte[] bytes = new byte[ message.remaining() ];
        message.get( bytes );
        AMF3Deserializer deserializer = new AMF3Deserializer( new ByteArrayInputStream( bytes ) );
        Object rt = deserializer.readObject();

        deserializer.close();
        GraniteContext.release();

        return rt;
    }

    public ByteBuffer encode( Object message ) throws IOException{
        SimpleGraniteContext.createThreadInstance( this, null, null );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AMF3Serializer serializer = new CrAMF3Serializer( out );
        serializer.writeObject( message );
        serializer.flush();
        out.flush();
        byte[] bytes = out.toByteArray();

        serializer.close();
        GraniteContext.release();

        return ByteBuffer.wrap( bytes );
    }


}

