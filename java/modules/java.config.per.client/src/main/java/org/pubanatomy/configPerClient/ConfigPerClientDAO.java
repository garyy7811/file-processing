package org.pubanatomy.configPerClient;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Hashtable;
import java.util.stream.Stream;

public class ConfigPerClientDAO{

    private static final Logger logger = LogManager.getLogger( ConfigPerClientDAO.class );

    public ConfigPerClientDAO( String awsConfigPerClientDynamoTablename ){
        this.awsConfigPerClientDynamoTablename = awsConfigPerClientDynamoTablename;
    }

    private String awsConfigPerClientDynamoTablename;

    public String getAwsConfigPerClientDynamoTablename(){
        return awsConfigPerClientDynamoTablename;
    }

    public AmazonDynamoDB getDynamoDB(){
        return dynamoDB;
    }

    public DynamoDBMapper getDynamoDBMapper(){
        return dynamoDBMapper;
    }

    protected AmazonDynamoDB dynamoDB;
    private   DynamoDBMapper dynamoDBMapper;

    @Autowired
    private void setDynamoDB( AmazonDynamoDB dynamoDB ){
        this.dynamoDB = dynamoDB;
        final DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder().withTableNameOverride(
                DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement( awsConfigPerClientDynamoTablename ) ).build();

        dynamoDBMapper = new DynamoDBMapper( dynamoDB, mapperConfig );
    }

    public DynaTableClientConfig loadConfig( String clientId ){
        DynaTableClientConfig rt = dynamoDBMapper.load( DynaTableClientConfig.class, clientId );
        if( rt == null ){
            rt = getDefaultDynaTableClientConfig( clientId );
            logger.debug( "adding new config for client:{}, {}", clientId, rt );
            dynamoDBMapper.save( rt );
        }
        return rt;
    }

    public static DynaTableClientConfig getDefaultDynaTableClientConfig( String clientId ){
        DynaTableClientConfig rt;
        rt = new DynaTableClientConfig();
        rt.setClientId( clientId );
        rt.setInitialTime( System.currentTimeMillis() );
        if( rt.getUpload() == null ){
            rt.setUpload( new DynaTableClientConfigUpload() );
        }
        if( rt.getTranscode() == null ){
            rt.setTranscode( new DynaTableClientConfigTranscode() );
        }
        if( rt.getTranscode().getFormats() == null ){

            DynaTableClientConfigTranscodeFormat defaultPosterframeFormat = new DynaTableClientConfigTranscodeFormat();
            defaultPosterframeFormat.setIdentification( DynaTableClientConfigTranscodeFormat.DEFAULT_POSTER_FRAME );
            defaultPosterframeFormat.setConfigName( "defaultPosterFrame" );
            defaultPosterframeFormat.setOutput( "thumbnail" );
            defaultPosterframeFormat.setLastModifiedTime( 0L );
            // by default render the frame at 5% of the duration
            defaultPosterframeFormat.setTime( "5%" );


            DynaTableClientConfigTranscodeFormat firstFramePosterframeFormat = new DynaTableClientConfigTranscodeFormat();
            BeanUtils.copyProperties( defaultPosterframeFormat, firstFramePosterframeFormat );
            firstFramePosterframeFormat.setBaseConfigId( defaultPosterframeFormat.getIdentification() );
            firstFramePosterframeFormat.setIdentification( DynaTableClientConfigTranscodeFormat.DEFAULT_FIRST_FRAME );
            firstFramePosterframeFormat.setConfigName( "firstFramePosterFrame" );
            firstFramePosterframeFormat.setTime( "0.02" );


            DynaTableClientConfigTranscodeFormat videoThumbnailFormat = new DynaTableClientConfigTranscodeFormat();
            BeanUtils.copyProperties( defaultPosterframeFormat, videoThumbnailFormat );
            videoThumbnailFormat.setBaseConfigId( defaultPosterframeFormat.getIdentification() );
            videoThumbnailFormat.setIdentification( DynaTableClientConfigTranscodeFormat.DEFAULT_THUMB );
            videoThumbnailFormat.setConfigName("videoThumbnail");
            // set width only - encoding.com will automatically set height to match source aspect ratio
            videoThumbnailFormat.setWidth( "200" );


            DynaTableClientConfigTranscodeFormat videoFormatRoot = new DynaTableClientConfigTranscodeFormat();
            videoFormatRoot.setIdentification( DynaTableClientConfigTranscodeFormat.DEFAULT_VIDEO );
            videoFormatRoot.setConfigName( "video root" );
            videoFormatRoot.setLastModifiedTime( 0L );
            videoFormatRoot.setOutput( "mp4" );
            videoFormatRoot.setKeyframe( "90" );
            videoFormatRoot.setAudio_bitrate( "128k" );
            videoFormatRoot.setAudio_codec( "libfaac" );
            videoFormatRoot.setAudio_sample_rate( "44100" );
            videoFormatRoot.setAudio_volume( "100" );
            videoFormatRoot.setBitrate( "1000k" );//4500,2500,2000,1500,1250,1000
            videoFormatRoot.setFramerate( "29" );
            videoFormatRoot.setHint( "no" );
            videoFormatRoot.setKeep_aspect_ratio( "yes" );
            videoFormatRoot.setProfile( "baseline" );
            videoFormatRoot.setTurbo( "no" );
            videoFormatRoot.setTwo_pass( "yes" );
            videoFormatRoot.setVideo_codec( "libx264" );
            videoFormatRoot.setKeep_aspect_ratio( "yes" );

            Hashtable<String, DynaTableClientConfigTranscodeFormat> formats = new Hashtable<>( 4 );
            formats.put( defaultPosterframeFormat.getIdentification(), defaultPosterframeFormat );
            formats.put( firstFramePosterframeFormat.getIdentification(), firstFramePosterframeFormat );
            formats.put( videoThumbnailFormat.getIdentification(), videoThumbnailFormat );
            formats.put( videoFormatRoot.getIdentification(), videoFormatRoot );

            //don't forget the root -> 1000k
            Stream.of( 4500, 2500, 2000, 1500, 1250 ).forEach( num -> {
                DynaTableClientConfigTranscodeFormat tmp = new DynaTableClientConfigTranscodeFormat();
                tmp.setBaseConfigId( videoFormatRoot.getIdentification() );
                tmp.setIdentification( videoFormatRoot.getIdentification() + num );
                tmp.setConfigName( videoFormatRoot.getIdentification() + num );
                tmp.setBitrate( num + "k" );
                formats.put( tmp.getIdentification(), tmp );
            } );

            rt.getTranscode().setFormats( formats );
        }
        return rt;
    }

    //todo: make sure this does what we expect
    public void saveClientConfig( DynaTableClientConfig clientConfig ){
        dynamoDBMapper.save( clientConfig,
                new DynamoDBMapperConfig( DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES ) );
    }


}
