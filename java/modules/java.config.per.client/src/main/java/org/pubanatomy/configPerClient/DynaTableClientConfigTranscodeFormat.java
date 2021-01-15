package org.pubanatomy.configPerClient;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import org.pubanatomy.siutils.ClientInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * encoding.com
 * User: flashflexpro@gmail.com
 * Date: 2/11/2016
 * Time: 2:56 PM
 */
@Data
@ToString( callSuper = true )
@EqualsAndHashCode( callSuper = true )
@DynamoDBDocument
public class DynaTableClientConfigTranscodeFormat extends DynaTableClientConfigOverride{

    @DynamoDBIgnore
    public static final String DEFAULT_VIDEO        = "default_video";
    @DynamoDBIgnore
    public static final String DEFAULT_THUMB        = "default_thumbnail";
    @DynamoDBIgnore
    public static final String DEFAULT_POSTER_FRAME = "default_posterframe";
    @DynamoDBIgnore
    public static final String DEFAULT_FIRST_FRAME  = "default_first_frame_posterframe";

    @DynamoDBIgnore
    public static final Set<String> DEFAULT_4 =
            new HashSet<>( Arrays.asList( DEFAULT_FIRST_FRAME, DEFAULT_POSTER_FRAME, DEFAULT_THUMB, DEFAULT_VIDEO ) );

    @ClientInfo( readOnly = true )
    private String baseConfigId;

    @ClientInfo( readOnly = true )
    private String taskid;
    private String noise_reduction;

    @ClientInfo( readOnly = true )
    private String status;
    private String description;

    @ClientInfo( readOnly = true )
    private String suggestion;
    private String fade_in;
    private String fade_out;
    private String crop_left;
    private String crop_top;
    private String crop_right;
    private String crop_bottom;
    private String set_aspect_ratio;
    private String rc_init_occupancy;
    @ClientInfo( stringRegexp = "[1-9]\\d*" )
    private String minrate;
    @ClientInfo( stringRegexp = "[1-9]\\d*" )
    private String maxrate;
    private String bufsize;
    private String start;
    @ClientInfo( readOnly = true )
    private String duration;
    private String force_keyframes;
    private String bframes;
    private String gop;
    private String video_codec_parameters;
    private String rotate;
    private String set_rotate;
    private String audio_sync;
    private String video_sync;
    private String force_interlaced;
    private String strip_chapters;
    private String time;

    @Field( type = FieldType.Integer )
    private String width;

    @Field( type = FieldType.Integer )
    private String height;

    @ClientInfo( enumStrings = { "mp4", "thumbnail" } )
    @Field( type = FieldType.String )
    private String output;

    @ClientInfo( stringRegexp = "[1-9]\\d*" )
    private String keyframe;
    @ClientInfo( readOnly = true )
    private String destination;
    private String add_meta;
    private String audio_bitrate;
    private String audio_codec;
    private String audio_sample_rate;
    private String audio_volume;
    private String bitrate;
    private String framerate;
    private String hint;
    private String keep_aspect_ratio;
    private String profile;

    @ClientInfo( stringRegexp = "[1-9]\\d*x[1-9]\\d*" )
    private String size;
    private String turbo;
    private String two_pass;
    private String video_codec;

    private String cbr;

    private String isDefault;

    @DynamoDBIgnore
    public static Integer getBitRateInK( String bitrate ){
        bitrate = bitrate.trim();
        return Integer.parseInt( bitrate.substring( 0, bitrate.length() - 1 ) );
    }

}
