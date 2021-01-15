import com.amazonaws.services.lambda.runtime.*;
import org.pubanatomy.awsutils.LambdaBase;
import org.pubanatomy.lambda.S3UploadFromFlash;

/**
 * Created by greg on 2/7/17.
 */
public class RunIt {

    public static void main( String[] args ){

        System.out.println(">>>>> RunIt entered");

        try {
            S3UploadFromFlash ocff = new S3UploadFromFlash();

            System.out.println(">>>>> got instance, calling handler");

            ocff.handleRequest(new LambdaBase.ProxyInput(), new MockContext());
        } catch (Exception e) {
            System.out.println(">>>>> RunIt caught exception...");
            e.printStackTrace();
        }

        System.out.println(">>>>> RunIt exiting");
    }


    private static class MockContext implements Context {
        @Override
        public String getAwsRequestId() {
            return "mock!";
        }

        @Override
        public String getLogGroupName() {
            return null;
        }

        @Override
        public String getLogStreamName() {
            return null;
        }

        @Override
        public String getFunctionName() {
            return null;
        }

        @Override
        public String getFunctionVersion() {
            return null;
        }

        @Override
        public String getInvokedFunctionArn() {
            return null;
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 0;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 0;
        }

        @Override
        public LambdaLogger getLogger() {
            return null;
        }
    }
}
