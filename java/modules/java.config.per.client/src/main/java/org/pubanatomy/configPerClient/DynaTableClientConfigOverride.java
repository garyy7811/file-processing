package org.pubanatomy.configPerClient;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import org.pubanatomy.siutils.ClientInfo;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@DynamoDBDocument
public abstract class DynaTableClientConfigOverride implements Serializable{

    /**
     * if this start with "default", it is the default which can be edited, but not deleted
     * if this is not, it is the username + time when create this config, can be deleted then
     */
    @ClientInfo( readOnly = true )
    @NotNull
    @Field( type = FieldType.String )
    private String identification;
    @NotNull
    private String configName;

    @ClientInfo( readOnly = true)
    private String configGroup;

    /**
     * who when changed this last time
     */
    @ClientInfo( readOnly = true )
    private String overrideByUser;
    private String overrideComment;

    @ClientInfo( readOnly = true )
    @NotNull
    private Long   lastModifiedTime;

    private boolean enabled = true;
    @ClientInfo( readOnly = true )
    private String enabledChangedBy;
    @ClientInfo( readOnly = true )
    private Long   enabledChangedTime;


}
