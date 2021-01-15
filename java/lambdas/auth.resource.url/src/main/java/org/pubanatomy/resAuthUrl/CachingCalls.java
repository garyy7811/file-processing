package org.pubanatomy.resAuthUrl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.customshow.awsS3Download.AwsS3DownloadFuncs;
import com.customshow.awsS3Download.DynaTableNVResource;
import com.customshow.awsS3Download.DynamoLoginVerification;
import com.customshow.awsS3Download.ResourceContentToDynamoResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;

import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/15/2016
 * Time: 4:09 PM
 */
public class CachingCalls{

    @Autowired
    @Qualifier( "loginVerificationDynamoMapper" )
    private DynamoDBMapper loginVerificationDynamoMapper;


    @Cacheable( "loadLoginVerification" )
    public DynamoLoginVerification loadLoginVerification( String csSessionId ){
        return loginVerificationDynamoMapper.load( DynamoLoginVerification.class, csSessionId );
    }


    @Autowired
    private AwsS3DownloadFuncs              downloadFuncs;/*
    @Autowired
    private QueryNewVictoryMysql            mysql;*/
    @Autowired
    private ResourceContentToDynamoResource convert;


    @Cacheable( "loadResourceContent" )
    public DynaTableNVResource loadResourceContent( Long resourceContentId, Integer resourceVersion ){/*
        DynaTableNVResource resourceRecord = downloadFuncs.getDynamoDBMapper()
                .load( DynaTableNVResource.class, resourceContentId.toString(), resourceVersion.toString() );

        if( resourceRecord == null ){

            Map<String, Object> mysqlRslt = mysql.selectResourceContentToMigrateByScId( resourceContentId );

            resourceRecord = convert.doit( mysqlRslt );

            downloadFuncs.getDynamoDBMapper().save( resourceRecord );
        }
        return resourceRecord;*/
        return null;
    }
}
