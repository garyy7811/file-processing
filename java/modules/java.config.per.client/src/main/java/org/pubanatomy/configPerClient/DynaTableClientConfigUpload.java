package org.pubanatomy.configPerClient;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import org.pubanatomy.siutils.ClientInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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
public class DynaTableClientConfigUpload extends DynaTableClientConfigOverride{


    @Field( type = FieldType.Integer )

    @ClientInfo( readOnly = true )
    private Integer uploadConcurrentNum = 5;

    private Long uploadSizeLimitPerFileInM = 5368709120L; // 5GB

    @ClientInfo( readOnly = true )
    private Double uploadTotalUploadLimitInG = - 1.0;


}
