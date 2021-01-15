package org.pubanatomy.loginverify;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Data;

/**
 * User: GaryY
 * Date: 9/30/2016
 */
@DynamoDBTable( tableName = "$!overriding me!$" )
@Data
public class DynaLogInSessionInfo{


    public static final String ES_TYPE = "login-session-info";

    public DynaLogInSessionInfo(){
    }

    @DynamoDBHashKey
    private String csSessionId;

    private Long loginTime;

    private String userId;

    private String clientId;

}
