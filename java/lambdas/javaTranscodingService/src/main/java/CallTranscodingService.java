import com.amazonaws.regions.Regions;
import org.pubanatomy.lambda.JavaTranscodingService;
import org.pubanatomy.videotranscoding.TranscodingReportingService;
import org.pubanatomy.videotranscoding.TranscodingServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pubanatomy.videotranscoding.api.*;
import org.springframework.context.ApplicationContext;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by greg on 2/21/17.
 */
public class CallTranscodingService {

    public static void main( String[] args ){

        System.out.println("CallTranscodingService entered at: " +  System.currentTimeMillis());

        try {

            //callSpring();

            callLambda();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void callSpring() throws Exception {

        System.out.println("callSpring - entered");

        long tmp = System.currentTimeMillis();

        JavaTranscodingService jts = new JavaTranscodingService();
        ApplicationContext springContext = jts.getAppContext();

        if (springContext == null) {
            System.out.println("context initialization failed!");
            return;
        }

        long t = System.currentTimeMillis() - tmp;
        System.out.println( "completed context initialization in " + t + " ms" );

        TranscodingReportingService transcodingReportingService = (TranscodingReportingService)springContext.getBean("transcodingReportingService");

        System.out.println("svc: " + transcodingReportingService);


        ObjectMapper objectMapper = new ObjectMapper();

//        FetchTranscodingJobsRequest fetchTranscodingJobsRequest = new FetchTranscodingJobsRequest();
//        fetchTranscodingJobsRequest.setFrom(0);
//        fetchTranscodingJobsRequest.setSize(10);
//        fetchTranscodingJobsRequest.setSortBy(FetchTranscodingJobsRequest.SORT_BY_createTime);
//        fetchTranscodingJobsRequest.setDescending(true);
//
////        fetchTranscodingJobsRequest.setClientIdFilter(Arrays.asList(1000023));
////        fetchTranscodingJobsRequest.setMediaIdFilter(101239053);
//        fetchTranscodingJobsRequest.setOriginalFileNameMatch("Bump");
//
//        TranscodingServiceRequest request = new TranscodingServiceRequest();
//        request.setRequestPayloadClass(fetchTranscodingJobsRequest.getClass().getName());
//        request.setRequestPayloadJson(objectMapper.writeValueAsString(fetchTranscodingJobsRequest));
//
//        System.out.println("Sending FetchTranscodingJobsRequest...");
//
//        FetchTranscodingJobsResponse response =  transcodingReportingService.fetchTranscodingJobs(fetchTranscodingJobsRequest);
//        System.out.println("response got " + response.getItems().size() + " items");
//        for (FetchTranscodingJobsResponse.UploadTranscodings item : response.getItems()) {
//            System.out.println("upload: "+ item.getUpload());
//            System.out.println("first transcode: "+ item.getTranscodings().get(0));
//        }

        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ssZ" );

        FetchJobsPerDayRequest jobsPerDayRequest = new FetchJobsPerDayRequest();

        Date startDate = sdf.parse("2017-02-22 00:00:00-0500");
        Date endDate = sdf.parse("2017-02-22 23:59:59-0500");
//        Date endDate = sdf.parse("2017-02-22 23:57:40-0500");

        jobsPerDayRequest.setStartDate(startDate);
        jobsPerDayRequest.setEndDate(endDate);

        TranscodingServiceRequest request = new TranscodingServiceRequest();
        request.setRequestPayloadClass(FetchJobsPerDayRequest.class.getName());
        request.setRequestPayloadJson(objectMapper.writeValueAsString(jobsPerDayRequest));

        System.out.println("sending jobsPerDayRequest...");

        FetchJobsPerDayResponse response = transcodingReportingService.fetchTranscodingJobsPerDay(jobsPerDayRequest);

        System.out.println("response got " + response.getItems().size() + " items");
        for (FetchJobsPerDayResponse.DateJobsPair item : response.getItems()) {
            System.out.println("upload: "+ item.getDate());
            System.out.println("total: "+ item.getJobsCount());
            System.out.println("pending: "+ item.getPendingJobs());
            System.out.println("complete: "+ item.getCompletedJobs());
            System.out.println("failed: "+ item.getFailedJobs());
        }


        /// again
        startDate = sdf.parse("2017-02-22 00:00:00-0500");
        endDate = sdf.parse("2017-02-22 00:00:00-0500");

        jobsPerDayRequest.setStartDate(startDate);
        jobsPerDayRequest.setEndDate(endDate);

        request = new TranscodingServiceRequest();
        request.setRequestPayloadClass(FetchJobsPerDayRequest.class.getName());
        request.setRequestPayloadJson(objectMapper.writeValueAsString(jobsPerDayRequest));

        System.out.println("sending jobsPerDayRequest...");

        response = transcodingReportingService.fetchTranscodingJobsPerDay(jobsPerDayRequest);

        System.out.println("response got " + response.getItems().size() + " items");
        for (FetchJobsPerDayResponse.DateJobsPair item : response.getItems()) {
            System.out.println("upload: "+ item.getDate());
            System.out.println("total: "+ item.getJobsCount());
            System.out.println("pending: "+ item.getPendingJobs());
            System.out.println("complete: "+ item.getCompletedJobs());
            System.out.println("failed: "+ item.getFailedJobs());
        }
    }




    public static void callLambda() throws Exception {

        System.out.println("callLambda - entered");

//        final TranscodingServiceApi serviceClient = TranscodingServiceApiFactory
//                .getTranscodingServiceApi("cs-cloud-dev-greg--", Regions.US_WEST_2.getName());

        final TranscodingServiceClient serviceClient =
                new TranscodingServiceClient("test", Regions.US_EAST_1.getName());

        System.out.println("calling invokeTranscodingService...");

        ObjectMapper objectMapper = new ObjectMapper();

        HelloWorldRequest helloWorldRequest = new HelloWorldRequest();
        helloWorldRequest.setName("blah");

        TranscodingServiceRequest request = new TranscodingServiceRequest();
        request.setRequestPayloadClass(helloWorldRequest.getClass().getName());
        request.setRequestPayloadJson(objectMapper.writeValueAsString(helloWorldRequest));

        TranscodingServiceResponse output = serviceClient.invokeTranscodingService(request);

        System.out.println("got result: [" + output.getStatus() + "]");

        if (output.isSuccess()) {
            HelloWorldResponse helloWorldResponse = objectMapper.readValue(output.getResponsePayloadJson(), HelloWorldResponse.class);
            System.out.println("got message " + helloWorldResponse.getMessage());
        } else {
            System.out.println("Got error: " + output.getErrorMessage());
        }

//        System.out.println("next request: FetchTranscodeJobs");
//
//        FetchTranscodingJobsRequest fetchTranscodingJobsRequest = new FetchTranscodingJobsRequest();
//        fetchTranscodingJobsRequest.setFrom(0);
//        fetchTranscodingJobsRequest.setSize(10);
//        fetchTranscodingJobsRequest.setSortBy(FetchTranscodingJobsRequest.SORT_BY_createTime);
//        fetchTranscodingJobsRequest.setDescending(true);
//
//        fetchTranscodingJobsRequest.setOriginalFileNameMatch("Bump");
//
//        request = new TranscodingServiceRequest();
//        request.setRequestPayloadClass(fetchTranscodingJobsRequest.getClass().getName());
//        request.setRequestPayloadJson(objectMapper.writeValueAsString(fetchTranscodingJobsRequest));
//
//        System.out.println("Sending FetchTranscodingJobsRequest...");
//        output = serviceClient.invokeTranscodingService(request);
//
//
//        System.out.println("got result: [" + output.getStatus() + "]");
//
//        if (output.isSuccess()) {
//            FetchTranscodingJobsResponse response = objectMapper.readValue(output.getResponsePayloadJson(), FetchTranscodingJobsResponse.class);
//            System.out.println("response got " + response.getItems().size() + " items");
//            for (FetchTranscodingJobsResponse.UploadTranscodings item : response.getItems()) {
//                System.out.println("upload: "+ item.getUpload());
//                System.out.println("first transcode: "+ item.getTranscodings().get(0));
//            }
//        } else {
//            System.out.println("Got error: " + output.getErrorMessage());
//        }


        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ssZ" );

        FetchJobsPerDayRequest jobsPerDayRequest = new FetchJobsPerDayRequest();

        Date startDate = sdf.parse("2017-02-20 00:00:00-0500");
        Date endDate = sdf.parse("2017-02-28 23:59:59-0500");
//        Date endDate = sdf.parse("2017-02-22 23:57:40-0500");

        jobsPerDayRequest.setStartDate(startDate);
        jobsPerDayRequest.setEndDate(endDate);

        request = new TranscodingServiceRequest();
        request.setRequestPayloadClass(FetchJobsPerDayRequest.class.getName());
        request.setRequestPayloadJson(objectMapper.writeValueAsString(jobsPerDayRequest));

        System.out.println("Sending jobsPerDayRequest...");
        output = serviceClient.invokeTranscodingService(request);

        if (output.isSuccess()) {
            FetchJobsPerDayResponse response = objectMapper.readValue(output.getResponsePayloadJson(), FetchJobsPerDayResponse.class);

            System.out.println("response got " + response.getItems().size() + " items");
            for (FetchJobsPerDayResponse.DateJobsPair item : response.getItems()) {
                System.out.println("upload: "+ item.getDate());
                System.out.println("total: "+ item.getJobsCount());
                System.out.println("pending: "+ item.getPendingJobs());
                System.out.println("complete: "+ item.getCompletedJobs());
                System.out.println("failed: "+ item.getFailedJobs());
            }
        } else {

            System.out.println("Got error: " + output.getErrorMessage());
        }


    }

}
