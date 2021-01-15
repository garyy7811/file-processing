package org.pubanatomy.awsS3Download;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/7/2016
 * Time: 5:10 PM
 */
public class ResourceContentToDynamoResource{

    private static final Logger logger = LogManager.getLogger( ResourceContentToDynamoResource.class );


    public DynaTableNVResource doit( Map<String, Object> mso ){
        DynaTableNVResource tmp = new DynaTableNVResource();
        tmp.setType( DynaTableNVResource.SLIDE_RES_TYPE_video );

        tmp.setSourceKey( mso.get( "contentId" ).toString() );
        tmp.setProcessId( mso.get( "resourceVersion" ).toString() );
        //        tmp.setResourceId( Long.parseLong( mso.get( "resourceId" ).toString() ) );
        tmp.setFileName( ( String )mso.get( "resource_fileName" ) );
        tmp.setFileSize( Long.parseLong( mso.get( "resource_fileSize" ).toString() ) );
        tmp.setWidth( Integer.parseInt( mso.get( "width" ).toString() ) );
        tmp.setHeight( Integer.parseInt( mso.get( "height" ).toString() ) );
        tmp.setType( ( String )mso.get( "resourceType" ) );
        tmp.setOriginalFileName( ( String )mso.get( "resource_org_name" ) );
        tmp.setThumbnailKey( ( String )mso.get( "thumb_fileName" ) );
        tmp.setDefaultPosterframeKey( ( String )mso.get( "postFrame_fileName" ) );
        tmp.setFirstFramePosterframeKey( ( String )mso.get( "firstFrame_fileName" ) );

        String metadata = ( String )mso.get( "metadata" );

        logger.info( metadata );

        if( tmp.getType().equals( DynaTableNVResource.SLIDE_RES_TYPE_video ) ){

            try{
                NodeList paths = ( NodeList )XPathFactory.newInstance().newXPath()
                        .evaluate( "/metaData/streams/stream/@path", new InputSource( new StringReader( metadata ) ),
                                XPathConstants.NODESET );
                NodeList fileNames = ( NodeList )XPathFactory.newInstance().newXPath()
                        .evaluate( "/metaData/streams/stream/@fileName",
                                new InputSource( new StringReader( metadata ) ), XPathConstants.NODESET );
                NodeList sizes = ( NodeList )XPathFactory.newInstance().newXPath()
                        .evaluate( "/metaData/streams/stream/@contentSize",
                                new InputSource( new StringReader( metadata ) ), XPathConstants.NODESET );
                NodeList rates = ( NodeList )XPathFactory.newInstance().newXPath()
                        .evaluate( "/metaData/streams/stream/@bitRate", new InputSource( new StringReader( metadata ) ),
                                XPathConstants.NODESET );


                int length = paths.getLength();
                List<DynaTableNVResourceVideoStream> llPath = new ArrayList<>( length );
                for( int i = 0; i < length; i++ ){
                    DynaTableNVResourceVideoStream rstr = new DynaTableNVResourceVideoStream();
                    try{
                        final Node item = sizes.item( i );
                        final String textContent = item.getTextContent();
                        rstr.setFileSize( Long.parseLong( textContent ) );
                    }
                    catch( Exception e ){
                        e.printStackTrace();
                    }
                    try{
                        final Node item = rates.item( i );
                        final String textContent = item.getTextContent();
                        rstr.setBitRate( Long.parseLong( textContent ) );
                    }
                    catch( Exception e ){
                        e.printStackTrace();
                    }
                    try{
                        final Node item = paths.item( i );
                        final String textContent = item.getTextContent();
                        rstr.setRelativePath( textContent );
                    }
                    catch( Exception e ){
                        e.printStackTrace();
                    }
                    try{
                        final Node item = fileNames.item( i );
                        final String textContent = item.getTextContent();
//                        rstr.setFileName( textContent );
                    }
                    catch( Exception e ){
                        e.printStackTrace();
                    }
                    llPath.add( rstr );
                }
                tmp.setFileInfoLst( llPath );
            }
            catch( XPathExpressionException e ){
                logger.warn( ExceptionUtils.getStackTrace( e ) );
                return null;
            }
        }
        else if( tmp.getType().equals( DynaTableNVResource.SLIDE_RES_TYPE_image ) ){
            String fileName = null;

            try{
                NodeList fxpths = ( NodeList )XPathFactory.newInstance().newXPath()
                        .evaluate( "/metaData/common/@fileName", new InputSource( new StringReader( metadata ) ),
                                XPathConstants.NODESET );
                fileName = fxpths.item( 0 ).getTextContent();
            }
            catch( XPathExpressionException e ){
                logger.warn( ExceptionUtils.getStackTrace( e ) );
                return null;
            }

            DynaTableNVResourceVideoStream imgStream = new DynaTableNVResourceVideoStream();
//            imgStream.setFileName( fileName );
            tmp.setFileInfoLst( Arrays.asList( imgStream ) );
        }
        return tmp;
    }
}
