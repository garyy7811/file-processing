package org.pubanatomy.reporting.solrbean.test;

import org.pubanatomy.reporting.solr.bean.CSReportingActivitySolr;
import org.pubanatomy.reporting.solr.bean.CSReportingPresDurationSolr;
import org.pubanatomy.reporting.solr.bean.CSReportingShowDurationSolr;
import org.pubanatomy.reporting.solr.bean.CSReportingSlideDurationSolr;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

/**
 * User: flashflexpro@gmail.com
 * Date: 1/8/2015
 * Time: 11:31 AM
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration
public class TestSolrBeanAndDtoSchema{

    @Configuration
    @PropertySource( "/test.config.properties" )
    static class Config{

        @Bean
        public static PropertySourcesPlaceholderConfigurer getPropertySourcesPlaceholderConfigurer(){
            return new PropertySourcesPlaceholderConfigurer();
        }


        @Value( "${solr.home.folder.build}" )
        private String solrBuild;

        @Value( "${solr.core.name}" )
        private String solrCoreName;

        public String getSolrCoreSchemaPath(){
            return solrBuild + File.separator + solrCoreName + File.separator
                    + "conf" + File.separator + "schema.xml";
        }
    }

    @Autowired
    private ApplicationContext context;


    @Test
    public void testBeanSolrXmlMatchingActivitySolr() throws IntrospectionException, IOException, SAXException{

        matchFields( CSReportingActivitySolr.class, getSolrCoreFields() );
        matchFields( CSReportingSlideDurationSolr.class, getSolrCoreFields() );
        matchFields( CSReportingPresDurationSolr.class, getSolrCoreFields() );
        matchFields( CSReportingShowDurationSolr.class, getSolrCoreFields() );
    }


    private void matchFields( Class solrBeanClazz, Node activity ){

        Map<String, Field> solrFieldsMapLeftInClazz =
                getSolrFieldAnnotationsMap( new HashMap<String, Field>(), solrBeanClazz );

        NodeList solrXmlLst = activity.getChildNodes();

        List<Element> leftInXml = new ArrayList<>();
        Map<String, Element> xmlNameToEle = new HashMap<>();

        Collection<Field> allSolrBeanFields = new ArrayList<>();
        allSolrBeanFields.addAll( solrFieldsMapLeftInClazz.values() );

        for( int i = solrXmlLst.getLength() - 1; i >= 0; i-- ){
            Node xField = solrXmlLst.item( i );
            if( "field".equals( xField.getLocalName() ) || "dynamicField".equals( xField.getLocalName() ) ){
                Element element = ( Element )xField;
                xmlNameToEle.put( element.getAttribute( "name" ), element );
                Field removingField = solrFieldsMapLeftInClazz.remove( element.getAttribute( "name" ) );
                if( removingField == null ){
                    leftInXml.add( element );
                }
            }
        }

        if( solrFieldsMapLeftInClazz.size() > 0 ){
            for( Iterator<Field> iterator = solrFieldsMapLeftInClazz.values().iterator(); iterator.hasNext(); ){
                Field next = iterator.next();
                org.apache.solr.client.solrj.beans.Field sjField =
                        next.getDeclaredAnnotation( org.apache.solr.client.solrj.beans.Field.class );
                Assert.isTrue( ( sjField.value().indexOf( "__" ) > 0 ), solrBeanClazz + " has extra solr fields:" +
                        StringUtils.arrayToCommaDelimitedString( solrFieldsMapLeftInClazz.keySet().toArray() ) );
                if( next.getType().isArray() ){
                    Assert.isTrue( sjField.value().endsWith( "_t" ) );
                }
            }
        }
        if( leftInXml.size() > 0 ){

            for( int i = leftInXml.size() - 1; i >= 0; i-- ){
                Element element = leftInXml.get( i );
                String eleName = element.getAttribute( "name" );
                if( eleName.startsWith( "query_" ) ){
                    Element rEle = xmlNameToEle.get( eleName.substring( "query_".length() ) );
                    Assert.notNull( rEle );

                    Assert.isTrue( "single_word".equals( element.getAttribute( "type" ) ), eleName + "type wrong" );
                    Assert.isTrue( "true".equals( element.getAttribute( "indexed" ) ), eleName + "if indexed wrong" );
                    Assert.isTrue( "false".equals( element.getAttribute( "stored" ) ), eleName + "if stored wrong" );

                    Assert.isTrue( rEle.getAttribute( "multiValued" ).equals( element.getAttribute( "multiValued" ) ),
                            eleName + "if multiValued wrong" );
                    leftInXml.remove( element );
                }
                else if( eleName.equals( "_version_" ) || eleName.equals( "text" ) ||
                        eleName.equals( "javaClassType" ) ){
                    leftInXml.remove( element );
                }
            }
        }


        Map<Class, List<String[]>> classToJavaCode = new HashMap<>();

        for( Field beanField : allSolrBeanFields ){
            if( beanField.getName().equals( "javaClassType" ) || beanField.getName().startsWith( "query_" ) ){
                continue;
            }

            Element fieldElement;
            String beanAnnotation;
            if( beanField.getName().equals( "uid" ) ){
                fieldElement = xmlNameToEle.get( beanField.getName() );
                beanAnnotation = "uid";
            }
            else if( beanField.getName().equals( "enstcText" ) ){
                fieldElement = xmlNameToEle.get( beanField.getName() );
                beanAnnotation = "enstcText";
            }
            else{
                org.apache.solr.client.solrj.beans.Field declaredAnnotation =
                        beanField.getDeclaredAnnotation( org.apache.solr.client.solrj.beans.Field.class );
                beanAnnotation = declaredAnnotation.value();
                Assert.isTrue( beanAnnotation.startsWith( beanField.getName() ) );
                int tmpIdx = beanAnnotation.indexOf( "__" );
                Assert.isTrue( tmpIdx >= 0, "No solr type for " + beanAnnotation );

                fieldElement = xmlNameToEle.get( "*" + beanAnnotation.substring( tmpIdx ) );
            }
            Assert.notNull( fieldElement, "can't find solr declaration for " + beanAnnotation );

            Class eleClass = beanField.getDeclaringClass();

            List<String[]> eleClassFieldsCode = classToJavaCode.get( eleClass );
            if( eleClassFieldsCode == null ){
                eleClassFieldsCode = new ArrayList<>();
                classToJavaCode.put( eleClass, eleClassFieldsCode );
            }

            String eleType = fieldElement.getAttribute( "type" );
            String fieldDeclareStr = null;
            String asField = null;

            boolean eleMulti = "true".equals( fieldElement.getAttribute( "multiValued" ) );

            if( "string".equals( eleType ) || "single_id".equals( eleType ) || "single_word".equals( eleType ) ){
                asField =
                        "<reporting:QueryFieldT id=\"" + beanAnnotation + "\" type=\"{QueryField.type_single_word}\" " +
                                "label=\"" + beanField.getName() + "\" searchModel=\"{this}\" ";
                fieldDeclareStr = "private String" + ( eleMulti ? "[] " : " " ) + beanField.getName() + ";";
            }
            else if( "sentences".equals( eleType ) ){
                asField = "<reporting:QueryFieldT id=\"" + beanAnnotation + "\" type=\"{QueryField.type_sentences}\" " +
                        "label=\"" + beanField.getName() + "\" searchModel=\"{this}\" ";
                fieldDeclareStr = "private String" + ( eleMulti ? "[] " : " " ) + beanField.getName() + ";";
            }
            else if( "tint".equals( eleType ) || "tlong".equals( eleType ) ){
                Boolean sortable = ! eleMulti;
                asField = "<reporting:QueryFieldR id=\"" + beanAnnotation + "\" type=\"{QueryField.type_number}\" " +
                        "label=\"" + beanField.getName() + "\"  sortable=\"" + sortable +
                        "\" searchModel=\"{this}\" ";
                String tmpType = null;
                if( "tint".equals( eleType ) ){
                    tmpType = "Integer";
                }
                else{
                    tmpType = "Long";
                }
                fieldDeclareStr = "private " + tmpType + ( eleMulti ? "[] " : " " ) + beanField.getName() + ";";
            }
            else if( "boolean".equals( eleType ) ){
                asField = "<reporting:QueryFieldT id=\"" + beanAnnotation + "\" type=\"{QueryField.type_number}\" " +
                        "label=\"" + beanField.getName() + "\" searchModel=\"{this}\" ";
                fieldDeclareStr = "private Boolean" + ( eleMulti ? "[] " : " " ) + beanField.getName() + ";";
            }
            else{
                System.out.println( " type:" + eleType );
            }


            String segetter = "solrBean.sget" + beanField.getName().substring( 0, 1 ).toUpperCase() +
                    beanField.getName().substring( 1 ) + "();";

            Field qFld = solrFieldsMapLeftInClazz.get( "query_" + beanField.getName() );
            if( qFld != null ){

                org.apache.solr.client.solrj.beans.Field qAnno =
                        qFld.getDeclaredAnnotation( org.apache.solr.client.solrj.beans.Field.class );

                asField += " queryField=\"" + qAnno.value() + "\"";
            }
            asField += " />";

            eleClassFieldsCode.add( new String[]{ segetter, fieldDeclareStr, asField } );

        }
        System.out.println( "SolrBean--->>>>>>" + solrBeanClazz );

        for( Map.Entry<Class, List<String[]>> clzToCode : classToJavaCode.entrySet() ){
            System.out.println(
                    "\n\n\n\n\n\n\n\n\n\n=====================================================" + clzToCode.getKey() );
            System.out.println( "------------segetters>>>>" + clzToCode.getKey() );
            List<String[]> codeLst = clzToCode.getValue();
            codeLst.sort( new Comparator<String[]>(){
                @Override
                public int compare( String o1[], String o2[] ){
                    return o1[ 0 ].compareTo( o2[ 0 ] );
                }
            } );
            for( String[] eachCode : codeLst ){
                System.out.println( eachCode[ 0 ] );
            }


            System.out.println( "------------segetters query copy fields<<<<" + clzToCode.getKey() );


            System.out.println( "------------declarations>>>>" + clzToCode.getKey() );
            codeLst.sort( new Comparator<String[]>(){
                @Override
                public int compare( String o1[], String o2[] ){
                    return o1[ 1 ].compareTo( o2[ 1 ] );
                }
            } );
            for( String[] eachCode : codeLst ){
                System.out.println( eachCode[ 1 ] );
            }
            System.out.println( "\n\n\n\n\n------------declarations<<<<" + clzToCode.getKey() );


            System.out.println( "------------Flex Mxml fields>>>>" + clzToCode.getKey() );

            codeLst.sort( new Comparator<String[]>(){
                @Override
                public int compare( String o1[], String o2[] ){
                    return o1[ 2 ].compareTo( o2[ 2 ] );
                }
            } );
            for( String[] eachCode : codeLst ){
                System.out.println( eachCode[ 2 ] );
            }

            System.out.println( "------------Flex Mxml fields<<<<" + clzToCode.getKey() );


        }


        System.out.println( "SolrBean---<<<<<<" + solrBeanClazz );
    }

    public static Map<String, Field> getSolrFieldAnnotationsMap( Map<String, Field> rt, Class clazz ){
        Class superclass = clazz.getSuperclass();
        if( superclass != null ){
            getSolrFieldAnnotationsMap( rt, superclass );
        }

        Field[] clazzFieldArr = clazz.getDeclaredFields();

        for( Field clazzField : clazzFieldArr ){
            Annotation[] clazzFieldAnnArr = clazzField.getDeclaredAnnotations();
            for( Annotation annotation : clazzFieldAnnArr ){
                if( annotation.annotationType().equals( org.apache.solr.client.solrj.beans.Field.class ) ){
                    rt.put( clazzField.getName(), clazzField );
                }
            }
        }
        return rt;
    }


    private Node getSolrCoreFields() throws IOException, SAXException{
        DOMParser parser = new DOMParser();
        Config config = context.getBean( Config.class );
        String path = config.getSolrCoreSchemaPath( );

        parser.parse( new InputSource( new FileInputStream( path ) ) );

        Document document = parser.getDocument();

        NodeList tmp = document.getChildNodes();

        for( int i = tmp.getLength() - 1; i >= 0; i-- ){
            Node c = tmp.item( i );
            if( c instanceof Element ){
                Element e = ( Element )c;
                if( e.getNodeName().equals( "schema" ) ){
                    return c;
                }
            }
        }
        Assert.notNull( null, "Solr:" + path + " has no fields definition!" );
        return null;
    }
}
