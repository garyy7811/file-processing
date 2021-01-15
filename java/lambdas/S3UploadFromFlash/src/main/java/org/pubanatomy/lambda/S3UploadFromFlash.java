package org.pubanatomy.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.pubanatomy.awsutils.LambdaBase;
import org.apache.logging.log4j.ThreadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * User: flashflexpro@gmail.com
 * Date: 2/25/2016
 * Time: 2:24 PM
 */
public class S3UploadFromFlash extends LambdaBase<HashMap<String, Object>>
        implements RequestHandler<LambdaBase.ProxyInput, Map<String, Object>>{

    @Override
    public Map<String, Object> handleRequest(ProxyInput input, Context context ){

        // cache AWSRequestId for use in log4j
        ThreadContext.put("AWSRequestId", context.getAwsRequestId());

        // diagnostic logging
        reportHandlerInvocation();

        final Map<String, Object> rt = handleRpcInvokerRequest(input);

        return rt;
    }
}
