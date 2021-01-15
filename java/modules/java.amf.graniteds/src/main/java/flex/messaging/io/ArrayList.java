package flex.messaging.io;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/1/2014
 * Time: 1:45 PM
 */
public class ArrayList<T> extends java.util.ArrayList<T> implements Externalizable{

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException{
        out.writeObject( toArray() );
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException{
        Object tmp = in.readObject();
        if( tmp instanceof  Collection<?> ){
            addAll( ( Collection<? extends T> ) tmp );
        }
        else if( tmp != null && tmp.getClass().isArray() ){
            Object[] arr = ( Object[] ) tmp;
            for( Object o: arr ){
                add( ( T ) o );
            }
        }
        else{
            throw new RuntimeException( "No no no!!!" );
        }
    }


}
