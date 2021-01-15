package org.pubanatomy.loginverify;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User: GaryY
 * Date: 9/30/2016
 */
public class DynamoLoginInfoDAO{

    public DynamoLoginInfoDAO( String awsLoginVerificationDynamoTablename ){
        this.awsLoginVerificationDynamoTablename = awsLoginVerificationDynamoTablename;
    }

    private String awsLoginVerificationDynamoTablename;


    private AmazonDynamoDB dynamoDB;

    @Autowired
    private void setDynamoDB( AmazonDynamoDB dynamoDB ){
        this.dynamoDB = dynamoDB;
        final DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder().withTableNameOverride(
                DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement( awsLoginVerificationDynamoTablename ) )
                .build();

        loginVerifyDynaMapper = new DynamoDBMapper( dynamoDB, mapperConfig );
    }

    private DynamoDBMapper loginVerifyDynaMapper;

    public DynaLogInSessionInfo loadCsSessionInfo( String sessionId ){
        return loginVerifyDynaMapper.load( DynaLogInSessionInfo.class, sessionId );
    }


    public DynaLogInSessionInfo loadCsSessionInfo( String csSessionId, boolean requireUserId )
            throws IllegalAccessException{
        if( csSessionId == null ){
            throw new IllegalAccessException( "requireSessionId" );
        }
        DynaLogInSessionInfo logInSessionInfo = loginVerifyDynaMapper.load( DynaLogInSessionInfo.class, csSessionId );
        if( logInSessionInfo == null ){
            throw new IllegalAccessException( "requireSessionInfo" );
        }
        if( requireUserId && logInSessionInfo.getUserId() == null ){
            throw new IllegalAccessException( "requireUser" );
        }
        return logInSessionInfo;
    }

}
