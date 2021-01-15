package com.flashflexpro.graniteds;

import org.granite.messaging.amf.types.AMFVectorObjectValue;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/1/2014
 * Time: 1:45 PM
 */
public class CrMap<K, V> extends HashMap<K, V> implements Externalizable{

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException{
        List<K> keyLst = new ArrayList<>( size() );
        List<V> valueLst = new ArrayList<>( size() );
        for( Map.Entry<K, V> entry : entrySet() ){
            keyLst.add( entry.getKey() );
            valueLst.add( entry.getValue() );
        }
        out.writeObject( new AMFVectorObjectValue( keyLst.toArray( new Object[ size() ] ), null ) );
        out.writeObject( new AMFVectorObjectValue( valueLst.toArray( new Object[ size() ] ), null ) );
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException{
        K[] keyArray = ( K[] ) in.readObject();
        V[] valueArray = ( V[] ) in.readObject();
        for( int i = 0; i < keyArray.length; i++ ){
            put( keyArray[ i ], valueArray[ i ] );
        }
    }
}
