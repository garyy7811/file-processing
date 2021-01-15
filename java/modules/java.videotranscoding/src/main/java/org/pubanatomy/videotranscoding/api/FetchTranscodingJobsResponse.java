package org.pubanatomy.videotranscoding.api;

import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.pubanatomy.videotranscoding.DynaTableVideoTranscoding;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by greg on 2/21/17.
 */
@Data
public class FetchTranscodingJobsResponse {

    private List<UploadTranscodings> items;
    private Long totalResults;

    @Data
    public static class UploadTranscodings implements Serializable {

        private DynaTableAwsS3Upload upload;
        private List<DynaTableVideoTranscoding> transcodings;

        public void setSingleTranscodeRecord(DynaTableVideoTranscoding transcodeRecord) {
            transcodings = new ArrayList<>();
            transcodings.add(transcodeRecord);
        }
    }
}
