package org.pubanatomy.configPerClient;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
public class DynaTableClientConfigTranscode extends DynaTableClientConfigOverride{

    private static final Logger logger = LogManager.getLogger( DynaTableClientConfigTranscode.class );

    private Boolean autoStartOnUploaded = true;

    private Long s3UploadReadAuthExpireDelayInSecs    = 7200L;
    private Long s3DownloadWriteAuthExpireDelayInSecs = 300L;

    private int maxVideoFormatsNum = 5;
    private int maxThumbFormatsNum = 8;

    @Field( type = FieldType.Nested, includeInParent = true )
    private Map<String, DynaTableClientConfigTranscodeFormat> formats;


    @DynamoDBIgnore
    public List<DynaTableClientConfigTranscodeFormat> getFormatCopies(){
        if( formats == null ){
            return null;
        }
        return formats.values().stream().map( v -> {
            final DynaTableClientConfigTranscodeFormat newInst = new DynaTableClientConfigTranscodeFormat();
            copyBaseFormats( v, newInst );
            return newInst;
        } ).collect( Collectors.toList() );
    }

    @DynamoDBIgnore
    public DynaTableClientConfigTranscodeFormat getACopyOfFormat( String formatId ){
        DynaTableClientConfigTranscodeFormat rt = new DynaTableClientConfigTranscodeFormat();
        copyBaseFormats( formats.get( formatId ), rt );
        return rt;
    }

    @DynamoDBIgnore
    private void copyBaseFormats( DynaTableClientConfigTranscodeFormat configFormat,
                                  DynaTableClientConfigTranscodeFormat newInst ){
        if( configFormat.getBaseConfigId() != null ){
            DynaTableClientConfigTranscodeFormat tmp = formats.get( configFormat.getBaseConfigId() );
            if( tmp != null ){
                copyBaseFormats( tmp, newInst );
            }
            else{
                logger.warn( "config{}, base {} not exist!", configFormat.getConfigName(),
                        configFormat.getBaseConfigId() );
            }
        }

        Set<String> lst = new HashSet<>();
        BeanWrapper wr = new BeanWrapperImpl( configFormat );
        for( PropertyDescriptor pd : wr.getPropertyDescriptors() ){
            if( wr.getPropertyValue( pd.getName() ) == null ){
                lst.add( pd.getName() );
            }
        }

        BeanUtils.copyProperties( configFormat, newInst, lst.toArray( new String[ lst.size() ] ) );
    }


}
