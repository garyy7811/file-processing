package com.flashflexpro.graniteds;

import flex.messaging.io.ArrayList;
import org.granite.messaging.amf.io.AMF3Serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/3/2014
 * Time: 1:38 PM
 */
public class CrAMF3Serializer extends AMF3Serializer{
    public CrAMF3Serializer( OutputStream out ){
        super( out );
    }

    public CrAMF3Serializer( OutputStream out, int capacity ){
        super( out, capacity );
    }

    @Override
    protected void writeAMF3Collection( Collection<?> c ) throws IOException{
        if( legacyCollectionSerialization ){
            writeAMF3ObjectArray( c.toArray() );
        }
        else{
            ensureCapacity( 1 );
            buffer[ position++ ] = AMF3_OBJECT;

            int index = storedObjects.putIfAbsent( c );
            if( index >= 0 ){
                writeAMF3UnsignedIntegerData( index << 1 );
            }
            else{

                if( c instanceof CrScList ){
                    writeAndGetAMF3Descriptor( CrScList.class );
                }
                else if( c instanceof CrList ){
                    writeAndGetAMF3Descriptor( CrList.class );
                }
                else{
                    writeAndGetAMF3Descriptor( ArrayList.class );
                }


                ensureCapacity( 1 );
                buffer[ position++ ] = AMF3_ARRAY;

                // Add an arbitrary object in the dictionary instead of the
                // array obtained via c.toArray(): c.toArray() must return a
                // new instance each time it is called, there is no way to
                // find the same instance later...
                storedObjects.putIfAbsent( new Object() );

                writeAMF3UnsignedIntegerData( c.size() << 1 | 0x01 );

                ensureCapacity( 1 );
                buffer[ position++ ] = 0x01;

                for( Object o : c ){
                    writeObject( o );
                }
            }
        }
    }
}
