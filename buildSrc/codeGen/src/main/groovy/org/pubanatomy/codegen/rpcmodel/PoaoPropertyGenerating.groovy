package com.customshow.codegen.rpcmodel

import com.customshow.siutils.ClientInfo
import org.granite.messaging.amf.types.AMFVectorInt
import org.granite.messaging.amf.types.AMFVectorNumber
import org.granite.messaging.amf.types.AMFVectorObject
import org.granite.messaging.amf.types.AMFVectorUint

import java.beans.PropertyDescriptor
import java.lang.reflect.Field

/**
 * User: GaryY
 * Date: 6/29/2016*/
class PoaoPropertyGenerating{
    private As3TypeFactory as3TypeFactory = new As3TypeFactory();

    public PoaoPropertyGenerating( Field field, PropertyDescriptor propertyDescriptor,
                                   PoaoClassGenerating classGenerating ){
        this.javaField = field;
        this.javaPropertyDesc = propertyDescriptor;
        this.classGenerating = classGenerating;
    }

    private Field javaField;
    private PropertyDescriptor javaPropertyDesc;
    private PoaoClassGenerating classGenerating;

    public String generateCode(){
        String rse = "";
        ClientInfo clientInfo = javaField.getAnnotation( ClientInfo.class );
        if( clientInfo != null ){
            rse += clientInfo.notEmpty() ? ", true, " : ", false,"
            rse += clientInfo.readOnly() ? " true, " : " false, "
            rse += '"' + clientInfo.stringRegexp().replaceAll( '\\\\', '\\\\\\\\' ) + '"'

            if( clientInfo.enumStrings().length > 0 ){
                rse += ', [ "' + clientInfo.enumStrings().join( '","' ) + '" ]'
            }
        }


        String desc = "    public static const PROP_DESC_" + javaPropertyDesc.getName() +
                ":PropertyDesc = new PropertyDesc( \"" + javaPropertyDesc.getName() + "\" " + rse + ");\r\n";
        String decla = "    public var " + javaPropertyDesc.getName() + ":" + getPropertyType() + ";\r\n\r\n";
        return desc + decla;
    }

    private String getPropertyType(){

        As3Type as3Type = as3TypeFactory.getAs3Type( javaField.type );

        if( as3Type.equals( As3Type.ARRAY ) ){
            if( javaField.isAnnotationPresent( AMFVectorUint.class ) ){
                as3Type = As3Type.VECTOR_UINT;
            }
            else if( javaField.isAnnotationPresent( AMFVectorInt.class ) ){
                as3Type = As3Type.VECTOR_INT;
            }
            else if( javaField.isAnnotationPresent( AMFVectorNumber.class ) ){
                as3Type = As3Type.VECTOR_Number;
            }
            else if( javaField.isAnnotationPresent( AMFVectorObject.class ) ){
                as3Type = As3Type.VECTOR_Object;
            }
        }
        classGenerating.addImport( "import " + as3Type.getQualifiedName() + ";" );
        return as3Type.getName();
    }

}
