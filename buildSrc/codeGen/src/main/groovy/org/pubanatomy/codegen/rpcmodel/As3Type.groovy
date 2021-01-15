package com.customshow.codegen.rpcmodel

/**
 * User: GaryY
 * Date: 6/29/2016*/
class As3Type{

    public static final As3Type INT = new As3Type( null, "int", Integer.valueOf( 0 ) );
    public static final As3Type UINT = new As3Type( null, "uint", Integer.valueOf( 0 ) );
    public static final As3Type BOOLEAN = new As3Type( null, "Boolean", Boolean.valueOf( false ) );
    public static final As3Type NUMBER = new As3Type( null, "Number", "Number.NaN" );
    public static final As3Type OBJECT = new As3Type( null, "Object" );
    public static final As3Type STRING = new As3Type( null, "String" );
    public static final As3Type ARRAY = new As3Type( null, "Array" );
    public static final As3Type DATE = new As3Type( null, "Date" );
    public static final As3Type XML = new As3Type( null, "XML" );
    public static final As3Type BYTE_ARRAY = new As3Type( "flash.utils", "ByteArray" );


    public static final As3Type ILIST = new As3Type( "mx.collections", "IList" );


    public static final As3Type VECTOR_UINT = new As3Type( null, "Vector.<uint>" );
    public static final As3Type VECTOR_INT = new As3Type( null, "Vector.<int>" );
    public static final As3Type VECTOR_Number = new As3Type( null, "Vector.<Number>" );
    public static final As3Type VECTOR_Object = new As3Type( null, "Vector.<Object>" );

    public static final As3Type APACHE_ARRAY_LIST = new As3Type( "org.apache.flex.collections", "ArrayList" );


    public As3Type( String packageName, String simpleName ){
        this( packageName, simpleName, null );
    }

    public As3Type( String packageName, String name, Object nullValue ){
        this.packageName = ( packageName != null ? packageName : "" );
        this.name = name;
        this.qualifiedName =
                ( ( packageName != null && packageName.length() > 0 ) ? ( packageName + '.' + name ) : name );
        this.nullValue = nullValue;
    }


    private final String packageName;
    private final String name;
    private final Object nullValue;
    private final String qualifiedName;

    public String getQualifiedName(){
        return qualifiedName;
    }

    public Object getNullValue(){
        return nullValue;
    }

    public String getName(){
        return name;
    }

    public String getPackageName(){
        return packageName;
    }
}
