package org.pubanatomy.videotranscoding;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * encoding.com
 * User: flashflexpro@gmail.com
 * Date: 2/11/2016
 * Time: 2:56 PM
 */
@Data
@DynamoDBDocument
public class DynaTableVideoTranscodingMediaInfo implements Serializable{


    protected String format;
    protected String bitrate;
    protected String duration;
    protected String audio_bitrate;
    protected String audio_duration;
    protected String video_codec;
    protected String video_bitrate;
    protected String video_duration;
    @Field(type = FieldType.Float)
    protected String frame_rate;
    protected String size;
    protected String pixel_aspect_ratio;
    protected String display_aspect_ratio;
    protected String audio_codec;
    protected String audio_sample_rate;
    protected String audio_channels;
    @Field(type = FieldType.Long)
    protected String filesize;

}
