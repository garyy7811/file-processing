package com.customshow.codegen.rpcmodel

import org.w3c.dom.Document

/**
 * User: GaryY
 * Date: 6/29/2016*/
class As3TypeFactory{

    ///////////////////////////////////////////////////////////////////////////
    // Fields.

    private static Map<Class<? extends Serializable>, As3Type> java2As3Type;

    ///////////////////////////////////////////////////////////////////////////
    // Constructors.

    public As3TypeFactory(){
        if( java2As3Type == null ){
            java2As3Type = new HashMap<>();
            java2As3Type.put( Double.class, As3Type.NUMBER );
            java2As3Type.put( Double.TYPE, As3Type.NUMBER );
            java2As3Type.put( Float.class, As3Type.NUMBER );
            java2As3Type.put( Float.TYPE, As3Type.NUMBER );
            java2As3Type.put( Long.class, As3Type.NUMBER );
            java2As3Type.put( Long.TYPE, As3Type.NUMBER );
            java2As3Type.put( Integer.class, As3Type.NUMBER );
            java2As3Type.put( Integer.TYPE, As3Type.INT );
            java2As3Type.put( Short.class, As3Type.NUMBER );
            java2As3Type.put( Short.TYPE, As3Type.INT );
            java2As3Type.put( Byte.class, As3Type.NUMBER );
            java2As3Type.put( Byte.TYPE, As3Type.INT );

            java2As3Type.put( Boolean.class, As3Type.BOOLEAN );
            java2As3Type.put( Boolean.TYPE, As3Type.BOOLEAN );

            java2As3Type.put( String.class, As3Type.STRING );
            java2As3Type.put( Character.class, As3Type.STRING );
            java2As3Type.put( Character.TYPE, As3Type.STRING );
            java2As3Type.put( Locale.class, As3Type.STRING );
            java2As3Type.put( URL.class, As3Type.STRING );
            java2As3Type.put( URI.class, As3Type.STRING );

            java2As3Type.put( Map.class, As3Type.OBJECT );
            java2As3Type.put( Object.class, As3Type.OBJECT );
        }

    }


    public As3Type getAs3Type( Class<?> jType ){
        As3Type as3Type = getFromCache( jType );

        if( as3Type == null ){
            if( Date.class.isAssignableFrom( jType ) || Calendar.class.isAssignableFrom( jType ) ){
                as3Type = As3Type.DATE;
            }
            else if( Number.class.isAssignableFrom( jType ) ){
                as3Type = As3Type.NUMBER;
            }
            else if( Document.class.isAssignableFrom( jType ) ){
                as3Type = As3Type.XML;
            }
            else if( jType.isArray() ){
                Class<?> componentType = jType.getComponentType();
                if( Byte.class.equals( componentType ) || Byte.TYPE.equals( componentType ) ){
                    as3Type = As3Type.BYTE_ARRAY;
                }
                else if( Character.class.equals( componentType ) || Character.TYPE.equals( componentType ) ){
                    as3Type = As3Type.STRING;
                }
                else{
                    as3Type = As3Type.ARRAY;
                }
            }
            else if( Collection.class.isAssignableFrom( jType ) ){
                as3Type = As3Type.APACHE_ARRAY_LIST;
            }
            else if( Iterable.class.isAssignableFrom( jType ) ){
                as3Type = As3Type.ILIST;
            }
            else if( Map.class.isAssignableFrom( jType ) ){
                as3Type = As3Type.OBJECT;
            }
            else{
                as3Type = createAs3Type( jType );
            }

            putInCache( jType, as3Type );
        }

        return as3Type;
    }

    protected As3Type createAs3Type( Class<?> jType ){
        String name = jType.getSimpleName();
        if( jType.isMemberClass() ){
            name = jType.getEnclosingClass().getSimpleName() + '$' + jType.getSimpleName();
        }

        return new As3Type( jType.getPackage() != null ? jType.getPackage().getName() : "", name );
    }

    protected As3Type getFromCache( Class<?> jType ){
        if( jType == null ){
            throw new NullPointerException( "jType must be non null" );
        }
        return java2As3Type.get( jType );
    }

    protected void putInCache( Class<?> jType, As3Type as3Type ){
        if( jType == null || as3Type == null ){
            throw new NullPointerException( "jType and as3Type must be non null" );
        }
        java2As3Type.put( jType, as3Type );
    }

}
