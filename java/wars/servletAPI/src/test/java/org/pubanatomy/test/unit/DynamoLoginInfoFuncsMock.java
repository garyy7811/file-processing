package org.pubanatomy.test.unit;

import org.pubanatomy.loginverify.DynaLogInSessionInfo;
import org.pubanatomy.loginverify.DynamoLoginInfoDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User: flashflexpro@gmail.com
 * Date: 6/23/2016
 * Time: 5:26 PM
 */
public class DynamoLoginInfoFuncsMock extends DynamoLoginInfoDAO{

    @Autowired
    private DynaLogInSessionInfo mockingDynaLogInSessionInfo;

    public DynamoLoginInfoFuncsMock( String awsLoginVerificationDynamoTablename ){
        super( awsLoginVerificationDynamoTablename );
    }

    @Override
    public DynaLogInSessionInfo loadCsSessionInfo( String sessionId ){
        return mockingDynaLogInSessionInfo;
    }

    @Override
    public DynaLogInSessionInfo loadCsSessionInfo( String csSessionId, boolean requireUserId )
            throws IllegalAccessException{
        return mockingDynaLogInSessionInfo;
    }
}
