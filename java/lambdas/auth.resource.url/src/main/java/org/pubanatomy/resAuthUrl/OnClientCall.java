package org.pubanatomy.resAuthUrl;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.customshow.awsS3Download.DynaTableNVResource;
import com.customshow.awsutils.LambdaBase;
import org.pubanatomy.resAuthUrl.dto.FlashMediaServerUrlItem;
import org.pubanatomy.resAuthUrl.dto.GetResourceUrlsRequest;
import org.pubanatomy.resAuthUrl.dto.GetResourceUrlsResponse;
import com.flashflexpro.graniteds.SimpleGraniteConfig;
import com.llnw.mediavault.MediaVault;
import com.llnw.mediavault.MediaVaultRequest;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class OnClientCall extends LambdaBase<String> implements RequestHandler<HashMap<String, String>, String>{


    @Override
    public String handleRequest( HashMap<String, String> inputJson, Context context ){
        getAppContext();

        //csSessionId = extraStr;
        String extraStr = inputJson.get( "e" );

        CachingCalls cachingCalls = getAppContext().getBean( CachingCalls.class );

        if( cachingCalls.loadLoginVerification( extraStr ) == null ){
            return null;
        }

        String base64 = inputJson.get( "b" );
        //        String serviceName = inputJson.get( "s" );
        //        String methodName = inputJson.get( "m" );

        GetResourceUrlsResponse response = null;
        try{
            ByteBuffer wrap = ByteBuffer.wrap( DatatypeConverter.parseBase64Binary( base64 ) );

            SimpleGraniteConfig graniteConfig = getAppContext().getBean( SimpleGraniteConfig.class );

            final Object[] args = ( Object[] )graniteConfig.decode( wrap );
            GetResourceUrlsRequest request = ( GetResourceUrlsRequest )args[ 0 ];
            response = loadPaths( request );
            ByteBuffer rtb = graniteConfig.encode( response );
            return DatatypeConverter.printBase64Binary( rtb.array() );
        }
        catch( IOException e ){
            context.getLogger().log( ExceptionUtils.getStackTrace( e ) );
        }

        return null;
    }

    private GetResourceUrlsResponse loadPaths( GetResourceUrlsRequest request ){

        LabyrinthCdnConfig cdnConfig = getAppContext().getBean( LabyrinthCdnConfig.class );
        CachingCalls cachingCalls = getAppContext().getBean( CachingCalls.class );

        DynaTableNVResource resouceRecord = cachingCalls
                .loadResourceContent( request.getResourceContentId().longValue(), request.getResourceContentVersion() );

        GetResourceUrlsResponse rt = new GetResourceUrlsResponse();

        if( resouceRecord.getType().equals( DynaTableNVResource.SLIDE_RES_TYPE_video ) ){
            rt.setCdnDefaultPosterframeUrl( cdnConfig.getCdnHttpUrl() + "/" + cdnConfig.getUnsecurePath() + "/" +
                    cdnConfig.getEnvDirectory() + "/" + cdnConfig.getPosterFrameContext() + "/" +
                    resouceRecord.getPostFrameFileName() );

            rt.setCdnFirstFramePosterframeUrl( cdnConfig.getCdnHttpUrl() + "/" + cdnConfig.getUnsecurePath() + "/" +
                    cdnConfig.getEnvDirectory() + "/" + cdnConfig.getPosterFrameContext() + "/" +
                    resouceRecord.getFirstFrameFileName() );

            List<FlashMediaServerUrlItem> streams = resouceRecord.getFileInfoLst().stream().map( d -> {

                final int dotIndex = d.getFileName().indexOf( "." );
                String ajustedFileName = d.getFileName().substring( 0, dotIndex ).toLowerCase();
                String extFileName = d.getFileName().substring( dotIndex + 1 ).toLowerCase();

                String pathToEncode = cdnConfig.getBaseDirectory() + d.getRelativePath() + ajustedFileName;
                String nonEncoded = extFileName + d.getRelativePath() + ajustedFileName;

                final MediaVaultRequest options = new MediaVaultRequest( pathToEncode );

                options.setEndTime( System.currentTimeMillis() / 1000 + 600 );
                final FlashMediaServerUrlItem tmp = new FlashMediaServerUrlItem();
                tmp.setBitrate( d.getBitRate().intValue() );
                tmp.setFlashMediaServerConnectionUrl( cdnConfig.getFlashMediaServerConnectionUrl() );
                tmp.setStreamUrl( nonEncoded + new MediaVault( cdnConfig.getSecret() ).compute( options ) );

                return tmp;

            } ).collect( Collectors.toList() );

            rt.setStreamingUrls( streams.toArray( new FlashMediaServerUrlItem[ streams.size() ] ) );
        }
        else{
            final String mediaURL = cdnConfig.getCdnHttpUrl() + "/" + cdnConfig.getSecurePath() + "/" +
                    cdnConfig.getEnvDirectory() + "/" +
                    ( resouceRecord.getType().equals( DynaTableNVResource.SLIDE_RES_TYPE_image ) ?
                            cdnConfig.getImageContext() : cdnConfig.getFlashContext() ) + "/" +
                    resouceRecord.getFileName();
            final MediaVaultRequest options = new MediaVaultRequest( mediaURL );

            options.setEndTime( cdnConfig.getCdnRequestTTL() );

            rt.setCdnMediaDownloadUrl( mediaURL + new MediaVault( cdnConfig.getSecret() ).compute( options ) );
        }
        return rt;
    }


    public static void main( String[] args ) throws IOException{
        ApplicationContext tmp = new OnClientCall().getAppContext();
        System.out.println( tmp );
    }

}
