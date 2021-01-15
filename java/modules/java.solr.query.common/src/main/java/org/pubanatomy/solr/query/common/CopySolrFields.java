package org.pubanatomy.solr.query.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * User: flashflexpro@gmail.com
 * Date: 4/2/2015
 * Time: 12:20 PM
 */
public class CopySolrFields<T extends SolrBean>{


    private static final Logger logger = LogManager.getLogger( CopySolrFields.class );

    public CopySolrFields( String queryPrefix ){
        CopySolrFields.queryPrefix = queryPrefix;
    }

    private static String queryPrefix = "query_";

    private static final Map<Class<? extends SolrBean>, Map<Method, Method>> strPropertyGet2QuerySetter =
            new HashMap<>();

    private static final Map<Class<? extends SolrBean>, Map<String, Method>> strProperty2Getter = new HashMap<>();

    public Message<Collection<T>> processBeforeIndexing( Message<Collection<T>> beans ){
        logger.info( ">>>>>" + beans.getPayload().size() );
        for( T next : beans.getPayload() ){
            processBeforeIndexing( next );
        }
        logger.info( "<<<<<" );
        return beans;
    }

    public T processBeforeIndexing( T bean ){
        Map<Method, Method> bb = strPropertyGet2QuerySetter.get( bean.getClass() );
        if( bb == null ){
            bb = addGetterAndSetterMethods( bean.getClass() );
        }

        List<String> allQrStr = new ArrayList<>();

        for( Map.Entry<Method, Method> propertyGetter2QuerySetter : bb.entrySet() ){
            try{
                Object obj = propertyGetter2QuerySetter.getKey().invoke( bean );
                if( obj == null ){
                    continue;
                }
                propertyGetter2QuerySetter.getValue().invoke( bean, obj );
                if( obj instanceof String[] ){
                    allQrStr.addAll( Arrays.asList( ( String[] ) obj ) );
                }
                else{
                    allQrStr.add( ( String ) obj );
                }
            }
            catch( Exception e ){
                logger.warn( bean.getClass().getCanonicalName() + ".getter:" + propertyGetter2QuerySetter.getKey().getName() + ", setter:" +
                        propertyGetter2QuerySetter.getValue().getName(), e );
            }
        }

        bean.setEnstcText( allQrStr.toArray( new String[ allQrStr.size() ] ) );
        if( ! bean.getUid().startsWith( bean.getJavaClassType() ) ){
            bean.setUid( bean.getJavaClassType() + ":" + bean.getUid() );
        }
        return bean;
    }

    public static HashMap<String, Object> convertToMap( SolrBean solrBean, HashMap<String, Object> rt  ){
        Map<String, Method> properties = getNoneQueryStrAndGetterMethods( solrBean.getClass() );
        try{
            for( Map.Entry<String, Method> n2g : properties.entrySet() ){
                rt.put( n2g.getKey(), n2g.getValue().invoke( solrBean ) );
            }
        }
        catch( Exception e ){
            throw new Error( "No!!" );
        }
        if( solrBean.getUid().startsWith( solrBean.getJavaClassType() + ":" ) ){
            rt.put( "uid", solrBean.getUid().substring( solrBean.getJavaClassType().length() + 1 ) );
        }
        return rt;
    }


    private static Map<Method, Method> addGetterAndSetterMethods( Class<? extends SolrBean> beanClass ){
        HashMap<Method, Method> rt = new HashMap<>();
        Map<String, Field> fields = getAllPropertyFields( beanClass, true );

        for( Map.Entry<String, Field> n2f : fields.entrySet() ){
            Field qf = n2f.getValue();
            if( qf.getName().startsWith( queryPrefix ) ){

                Assert.notNull( fields.get( qf.getName().substring( queryPrefix.length() ) ) );

                String pf = qf.getName().substring( queryPrefix.length() );

                Method pGetter = null;
                Method qSetter = null;
                try{
                    pGetter = beanClass.getMethod( "get" + pf.substring( 0, 1 ).toUpperCase() + pf.substring( 1 ) );
                    qSetter = beanClass.getMethod(
                            "set" + qf.getName().substring( 0, 1 ).toUpperCase() + qf.getName().substring( 1 ),
                            qf.getType() );
                }
                catch( NoSuchMethodException e ){
                    throw new Error(
                            "SolrBean defination error:" + beanClass.getCanonicalName() + "??" + qf + "??" + e );
                }

                rt.put( pGetter, qSetter );
            }
        }
        strPropertyGet2QuerySetter.put( beanClass, rt );
        return rt;
    }

    private static final HashMap<Class<? extends SolrBean>, Map<String, Field>> classToAllPropertyFields =
            new HashMap<>();

    public static Map<String, Field> getAllPropertyFields( Class<? extends SolrBean> beanClass, boolean ignoreStatic ){
        Map<String, Field> rt = classToAllPropertyFields.get( beanClass );
        if( rt == null ){
            rt = getAllPropertyFieldsIml( beanClass, ignoreStatic );
            classToAllPropertyFields.put( beanClass, rt );
        }
        return rt;
    }

    private static Map<String, Field> getAllPropertyFieldsIml( Class<? extends SolrBean> beanClass,
                                                               boolean ignoreStatic ){
        Class<?> superclass = beanClass.getSuperclass();
        Map<String, Field> rt;
        if( superclass != null && ! superclass.equals( SolrBean.class ) ){
            rt = getAllPropertyFieldsIml( ( Class<? extends SolrBean> ) superclass, ignoreStatic );
        }
        else{
            rt = new HashMap<>();
        }

        for( int i = 0; i < beanClass.getDeclaredFields().length; i++ ){
            Field field = beanClass.getDeclaredFields()[ i ];
            if( Modifier.isStatic( field.getModifiers() ) && ignoreStatic ){
                continue;
            }
            rt.put( field.getName(), field );
        }
        return rt;
    }


    private static Map<String, Method> getNoneQueryStrAndGetterMethods( Class<? extends SolrBean> beanClass ){

        Map<String, Method> rt = strProperty2Getter.get( beanClass );
        if( rt == null ){
            rt = addNoneQueryStrAndGetterMethods( beanClass );
            strProperty2Getter.put( beanClass, rt );
        }
        return rt;
    }

    private static Map<String, Method> addNoneQueryStrAndGetterMethods( Class<? extends SolrBean> beanClass ){
        HashMap<String, Method> rt = new HashMap<>();

        Map<String, Field> fields = getAllPropertyFields( beanClass, true );

        for( Map.Entry<String, Field> n2f : fields.entrySet() ){
            Field qf = n2f.getValue();
            String pf = qf.getName();
            if( pf.startsWith( queryPrefix ) ){
                continue;
            }
            try{
                Method pGetter = beanClass.getMethod( "get" + pf.substring( 0, 1 ).toUpperCase() + pf.substring( 1 ) );
                rt.put( pf, pGetter );
            }
            catch( NoSuchMethodException e ){
                throw new Error( "SolrBean defination error:" + beanClass.getCanonicalName() + "??" + qf + "??" + e );
            }

        }
        return rt;
    }

}
