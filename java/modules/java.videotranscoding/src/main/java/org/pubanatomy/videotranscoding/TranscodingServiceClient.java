package org.pubanatomy.videotranscoding;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaFunction;
import com.amazonaws.services.lambda.invoke.LambdaFunctionNameResolver;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactoryConfig;
import org.pubanatomy.videotranscoding.api.TranscodingServiceRequest;
import org.pubanatomy.videotranscoding.api.TranscodingServiceResponse;

import java.lang.reflect.Method;

/**
 * TranscodingServiceClient provides direct access to the "javaTranscodingService" lambda function, by defining an
 * internal LambdaInvoker interface which is lazy-loaded when making calls into the public methods of this class.
 *
 * Use of this class only requires providing the cs-cloud environment name, an AWS region where the target lambda
 * function is deployed, and an optional AWSCrendetialsProvider, which will use a default if not provided.
 *
 * The csCloudEnvironment name is the based upon the naming conventions used throughout cs-cloud, so for example,
 * for the following lambda function name:
 *
 *      cs-cloud-dev-greg--JavaTranscodingService
 *
 * the csCloudEnvironmentName would be "dev-greg".
 *
 * Created by greg on 3/1/17.
 */
public class TranscodingServiceClient {

    private static final String FUNCTION_ENVIRONMENT_PREFIX = "cs-cloud-env--";

    private String csCloudEnvironmentName;
    private String lambdaRegionName;
    private AWSCredentialsProvider credentialsProvider;

    private TranscodingServiceInvoker invoker;

    public TranscodingServiceClient(String csCloudEnvironmentName, String lambdaRegionName) {
        this(csCloudEnvironmentName, lambdaRegionName, null);
    }

    public TranscodingServiceClient(String csCloudEnvironmentName, String lambdaRegionName, AWSCredentialsProvider credentialsProvider) {
        this.csCloudEnvironmentName = csCloudEnvironmentName;
        this.lambdaRegionName = lambdaRegionName;
        this.credentialsProvider = credentialsProvider;
    }


    public TranscodingServiceResponse invokeTranscodingService( TranscodingServiceRequest request) {

        if (invoker == null) {
            buildInvoker();
        }

        return invoker.invokeTranscodingService(request);
    }


    private void buildInvoker() {

        EnvironmentFunctionNameResolver fnResolver = new EnvironmentFunctionNameResolver();

        Regions lamdbdaRegion = Regions.fromName(lambdaRegionName);

        AWSLambdaClientBuilder clientBuilder =  AWSLambdaClientBuilder.standard();
        clientBuilder.withRegion(lamdbdaRegion);

        if (credentialsProvider != null) {
            clientBuilder.withCredentials(credentialsProvider);
        }

        AWSLambda lambda = clientBuilder.build();

        invoker = LambdaInvokerFactory.builder()
                .lambdaClient(lambda)
                .lambdaFunctionNameResolver(fnResolver)
                .build(TranscodingServiceInvoker.class);
    }


    interface TranscodingServiceInvoker {

        @LambdaFunction(functionName = FUNCTION_ENVIRONMENT_PREFIX + "JavaTranscodingService")
        TranscodingServiceResponse invokeTranscodingService(TranscodingServiceRequest request);
    }

    class EnvironmentFunctionNameResolver implements LambdaFunctionNameResolver {


        private String replacementToken;

        public EnvironmentFunctionNameResolver() {
            replacementToken = "cs-cloud-"+csCloudEnvironmentName+"--";
        }

        @Override
        public String getFunctionName(Method method, LambdaFunction annotation, LambdaInvokerFactoryConfig config) {

            String annotatedName = annotation.functionName();

            String adjustedName = annotatedName.replace(FUNCTION_ENVIRONMENT_PREFIX, replacementToken);

            return adjustedName;
        }
    }

}
