package org.pubanatomy.configPerClient;


import org.pubanatomy.loginverify.DynaLogInSessionInfo;
import org.pubanatomy.loginverify.DynamoLoginInfoDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 3/21/2016
 * Time: 1:36 PM
 */
@Service
public class ConfigPerClientService{

    private static final Logger logger = LogManager.getLogger( ConfigPerClientService.class );

    public ConfigPerClientService( String rootUserId ){
        this.rootUserId = rootUserId;
    }

    @Autowired
    private ConfigPerClientDAO perClientDAO;

    private String rootUserId;

    @Autowired
    private DynamoLoginInfoDAO loginInfoFuncs;

    public Object[] loadDynaTableClientConfigTranscFormats( @NotNull String csSessionId, @NotNull String clientId )
            throws IllegalAccessException{
        final DynaTableClientConfig c = loadDynaTableClientConfig( csSessionId, clientId );

        return c.getTranscode().getFormatCopies().toArray();
    }


    public DynaTableClientConfig loadDynaTableClientConfig( @NotNull String csSessionId, @NotNull String clientId )
            throws IllegalAccessException{
        DynaLogInSessionInfo logInSessionInfo = loginInfoFuncs.loadCsSessionInfo( csSessionId, true );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) &&
                ! clientId.equals( logInSessionInfo.getClientId() ) ){
            logger.warn( "User {} tried to root", logInSessionInfo.getUserId() );
            throw new IllegalArgumentException( "illegal" );
        }

        return perClientDAO.loadConfig( clientId );
    }

    /**
     * this is for the cs_cloud console
     *
     * @param csSessionId
     * @param clientId
     * @param newFormat
     * @return
     */
    public String saveTranscodeFormat( @NotNull String csSessionId, @NotNull String clientId,
                                       @NotNull DynaTableClientConfigTranscodeFormat newFormat ){

        DynaLogInSessionInfo logInSessionInfo = loginInfoFuncs.loadCsSessionInfo( csSessionId );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            throw new IllegalArgumentException( "only root can access" );
        }

        DynaTableClientConfig record = perClientDAO.loadConfig( clientId );
        if( record == null ){
            throw new IllegalArgumentException( "client config not exist" );
        }

        //new
        if( newFormat.getIdentification() == null ){
            newFormat.setIdentification( logInSessionInfo.getUserId() + "_" + System.currentTimeMillis() );
        }
        //edit existing
        else if( record.getTranscode().getFormats().get( newFormat.getIdentification() ) == null ){
            throw new IllegalArgumentException( "can't find format to override" + newFormat.getIdentification() );
        }

        //extending
        if( newFormat.getBaseConfigId() != null ){
            if( record.getTranscode().getFormats().get( newFormat.getBaseConfigId() ) == null ){
                throw new IllegalArgumentException( " can't find base format" );
            }
        }
        //error if not editing default
        else if( ! DynaTableClientConfigTranscodeFormat.DEFAULT_4.contains( newFormat.getIdentification() ) ){
            throw new IllegalArgumentException( "unknown format without base id" );
        }


        record.getTranscode().getFormats().put( newFormat.getIdentification(), newFormat );
        record.getTranscode().setLastModifiedTime( System.currentTimeMillis() );
        record.getTranscode().setOverrideByUser( logInSessionInfo.getUserId() );
        perClientDAO.saveClientConfig( record );
        return newFormat.getIdentification();
    }


    /**
     * this is specially for video transcoding format configuration in Labyrinth NV admin console
     *
     * @param csSessionId
     * @param clientId
     * @param formatsFromClient
     * @return
     */
    public boolean saveLabyrinthFormats( @NotNull String csSessionId, @NotNull String clientId,
                                         @NotNull Object[] formatsFromClient ){

        DynaLogInSessionInfo logInSessionInfo = loginInfoFuncs.loadCsSessionInfo( csSessionId );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            logger.warn( logInSessionInfo.getUserId() + " tried to root" );
            throw new IllegalArgumentException( "illegal" );
        }

        DynaTableClientConfig record = perClientDAO.loadConfig( clientId );
        if( record == null ){
            throw new IllegalArgumentException( "client config not exist" );
        }

        //must be extension of default value
        Map<String, DynaTableClientConfigTranscodeFormat> newFormats = new HashMap<>();
        for( Object f : formatsFromClient ){
            DynaTableClientConfigTranscodeFormat format = ( DynaTableClientConfigTranscodeFormat )f;
            if( ! DynaTableClientConfigTranscodeFormat.DEFAULT_VIDEO.equals( format.getBaseConfigId() ) ){
                throw new IllegalArgumentException( "without base config id:" + format.getIdentification() );
            }
            format.setIdentification( logInSessionInfo.getUserId() + "_" + format.getBitrate().trim() );
            newFormats.put( format.getIdentification(), format );
        }

        //add back thumbnails and default video
        Map<String, DynaTableClientConfigTranscodeFormat> existingFormats = record.getTranscode().getFormats();
        for( Map.Entry<String, DynaTableClientConfigTranscodeFormat> existFormatEntry : existingFormats.entrySet() ){
            if( record.getTranscode().getACopyOfFormat( existFormatEntry.getKey() ).getOutput().equals( "thumbnail" ) ||
                    existFormatEntry.getKey().equals( DynaTableClientConfigTranscodeFormat.DEFAULT_VIDEO ) ){
                newFormats.put( existFormatEntry.getKey(), existFormatEntry.getValue() );
            }
        }
        record.getTranscode().setFormats( newFormats );
        record.getTranscode().setLastModifiedTime( System.currentTimeMillis() );
        record.getTranscode().setOverrideByUser( logInSessionInfo.getUserId() );
        perClientDAO.saveClientConfig( record );
        return true;
    }

    public boolean deleteTranscodeFormat( @NotNull String csSessionId, @NotNull String clientId,
                                          @NotNull String formatId ) throws IllegalAccessException{
        if( DynaTableClientConfigTranscodeFormat.DEFAULT_4.contains( formatId ) ){
            throw new IllegalArgumentException( "can't delete default!" );
        }

        DynaLogInSessionInfo logInSessionInfo = loginInfoFuncs.loadCsSessionInfo( csSessionId, true );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            logger.warn( "User:{} tried to root", logInSessionInfo.getUserId() );
            throw new IllegalArgumentException( "illegal" );
        }

        DynaTableClientConfig record = perClientDAO.loadConfig( clientId );
        if( record == null ){
            throw new IllegalArgumentException( "client config not exist" );
        }
        final DynaTableClientConfigTranscodeFormat deletingFormat = record.getTranscode().getFormats().get( formatId );
        if( deletingFormat == null ){
            throw new IllegalArgumentException( "format not exist" );
        }

        record.getTranscode().getFormats().values().stream().forEach( eachFormat -> {
            if( formatId.equals( eachFormat.getBaseConfigId() ) ){
                throw new IllegalArgumentException( "children" );
            }
        } );

        record.getTranscode().getFormats().remove( formatId );

        record.getTranscode().setLastModifiedTime( System.currentTimeMillis() );
        record.getTranscode().setOverrideByUser( logInSessionInfo.getUserId() );
        perClientDAO.saveClientConfig( record );

        return true;
    }

    public boolean saveUploadConfig( @NotNull String csSessionId, @NotNull String clientId,
                                     @NotNull DynaTableClientConfigUpload uploadConfig ) throws IllegalAccessException{

        DynaLogInSessionInfo logInSessionInfo = loginInfoFuncs.loadCsSessionInfo( csSessionId, true );

        if( ! rootUserId.equals( logInSessionInfo.getUserId() ) ){
            logger.warn( "User:{} tried to root", logInSessionInfo.getUserId() );
            throw new IllegalArgumentException( "illegal" );
        }

        DynaTableClientConfig record = perClientDAO.loadConfig( clientId );
        record.setUpload( uploadConfig );
        perClientDAO.saveClientConfig( record );

        return true;
    }

}
