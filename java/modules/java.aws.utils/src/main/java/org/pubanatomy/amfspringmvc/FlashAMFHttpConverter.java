package org.pubanatomy.amfspringmvc;

import com.amazonaws.util.IOUtils;
import org.granite.config.GraniteConfig;
import org.granite.context.GraniteContext;
import org.granite.context.SimpleGraniteContext;
import org.granite.messaging.amf.io.AMF3Deserializer;
import org.granite.messaging.amf.io.AMF3Serializer;
import org.granite.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.*;
import java.util.List;

/**
 * User: flashflexpro@gmail.com
 * Date: 10/23/13
 * Time: 2:13 PM
 */
public class FlashAMFHttpConverter extends AbstractHttpMessageConverter<Object>{

    public FlashAMFHttpConverter(){
        super( MediaType.parseMediaType( "application/x-amf" ), MediaType.parseMediaType( "text/plain" ) );
    }

    @Override
    protected boolean supports( Class<?> clazz ){
        return Serializable.class.isAssignableFrom( clazz );
    }

    @Autowired
    private GraniteConfig graniteConfig;

    @Override
    protected Object readInternal( Class<?> clazz, HttpInputMessage inputMessage )
            throws IOException, HttpMessageNotReadableException{
        SimpleGraniteContext.createThreadInstance( graniteConfig, null, null );

        final InputStream amfInputStream;
        List<String> contentTypeLst = inputMessage.getHeaders().get( "content-type" );
        if( contentTypeLst.stream().anyMatch( s -> s.indexOf( "/x-amf" ) > 0 ) ){
            amfInputStream = inputMessage.getBody();
        }
        else{
            final byte[] decode = Base64.decode( IOUtils.toByteArray( inputMessage.getBody() ) );
            amfInputStream = new ByteArrayInputStream( decode );
        }
        AMF3Deserializer deserializer = new AMF3Deserializer( amfInputStream );
        Object rt = deserializer.readObject();

        deserializer.close();
        GraniteContext.release();

        return rt;
    }

    @Override
    protected void writeInternal( Object o, HttpOutputMessage outputMessage )
            throws IOException, HttpMessageNotWritableException{
        SimpleGraniteContext.createThreadInstance( graniteConfig, null, null );

        ByteArrayOutputStream amfOutStream = new ByteArrayOutputStream();
        AMF3Serializer serializer = new AMF3Serializer( amfOutStream );
        serializer.writeObject( o );

        serializer.close();
        GraniteContext.release();


        List<String> contentTypeLst = outputMessage.getHeaders().get( "content-type" );
        if( ! contentTypeLst.stream().anyMatch( s -> s.indexOf( "/x-amf" ) > 0 ) ){
            final ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            tmp.write( Base64.encodeToByte( amfOutStream.toByteArray(), false ) );
            amfOutStream = tmp;
        }

        amfOutStream.writeTo( outputMessage.getBody() );
    }

    @Override
    public List<MediaType> getSupportedMediaTypes(){
        return super.getSupportedMediaTypes();
    }
}
