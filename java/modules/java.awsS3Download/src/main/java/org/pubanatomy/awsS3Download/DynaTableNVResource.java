package org.pubanatomy.awsS3Download;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.List;


/**
 * User: flashflexpro@gmail.com
 * Date: 2/11/2016
 * Time: 2:56 PM
 */
@Data
@DynamoDBTable( tableName = "$!overriding me!$" )
@Document( indexName = "!!overrid me plz!!", type = DynaTableNVResource.ES_TYPE )
public class DynaTableNVResource implements Serializable{

    public static final String ES_TYPE = "download-nv-resource";

    public static final String SLIDE_RES_TYPE_image = "image";
    public static final String SLIDE_RES_TYPE_flash = "flash";
    public static final String SLIDE_RES_TYPE_video = "video";
    //    public static final String SLIDE_RES_TYPE_slide = "slide";

    @Field( type = FieldType.String, store = true )
    @DynamoDBHashKey
    private String sourceKey;

    @DynamoDBRangeKey
    private String processId;

    @DynamoDBIndexRangeKey( localSecondaryIndexName = "uploadAndType" )
    private String type;

    @Id
    @DynamoDBIgnore
    @Field( type = FieldType.String, store = true )
    private String esId;

    @DynamoDBVersionAttribute
    private Long version = null;

    //resource S3 download key
    private String thumbnailKey;

    private Long thumbnailFileSize;

    private Integer thumbnailWidth;
    private Integer thumbnailHeight;

    private String  fileName;
    private Integer width;
    private Integer height;
    private Long    fileSize;
    private String  originalFileName;


    /*>>>>> image*/
    //Image/Flash S3 download key
    private String downloadKey;
    /*<<<<<*/


    /*>>>>> video*/
    @Field( type = FieldType.Nested )
    private List<DynaTableNVResourceVideoStream> fileInfoLst;

    private String firstFramePosterframeKey;
    private Long   firstFramePosterframeFileSize;

    private String defaultPosterframeKey;
    private Long   defaultPosterframeFileSize;
    /*<<<<<*/

    private String errorMsg;

}
