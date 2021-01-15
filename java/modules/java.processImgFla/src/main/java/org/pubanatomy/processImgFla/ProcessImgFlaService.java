package org.pubanatomy.processImgFla;

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import org.pubanatomy.awsS3Upload.AwsS3UploadDAO;
import org.pubanatomy.awsS3Upload.DynaTableAwsS3Upload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * Created by greg on 2/15/17.
 */
public class ProcessImgFlaService {


    private static final Logger logger = LogManager.getLogger(ProcessImgFlaService.class);


    @Autowired
    protected AwsS3UploadDAO awsS3UploadDAO;


    @Autowired
    private ProcessImg processImgFla;


    public void processImage( String csSessionId, Long applyTimeStamp)
            throws IllegalAccessException{

        logger.info( "csSessionId:{}, applyTimeStamp:{}", csSessionId, applyTimeStamp );

        DynaTableAwsS3Upload asu = null;

        for (Integer updateAttempts = 1; updateAttempts <= 3; updateAttempts++) {
            try {
                asu = awsS3UploadDAO.loadUpload( csSessionId, applyTimeStamp );

                logger.info("setting uploaded time");
                asu.setUploadedByClientTime( System.currentTimeMillis() );

                logger.info("saving upload record");
                awsS3UploadDAO.saveUpload( asu );

                // if we make it this far, break out of the loop
                break;

            } catch (ConditionalCheckFailedException e) {
                // this can happen if another process is attempting to update the same upload record,
                // which can cause the asu.version field to become out of date
                if (updateAttempts == 3) {
                    logger.error("caught 3rd ConditionalCheckFailedException, aborting", e);
                    throw e;
                } else {
                    logger.warn("Caught ConditionalCheckFailedException, trying again");
                }
            }
        }

        try{

            logger.info("processing confirmed image upload");
            processImgFla.onUploadImgConfirmed(asu);
            logger.info("image processed");
        }
        catch (IOException e) {
            logger.error("Caught exception processing uploaded file", e);
            throw new RuntimeException( "internal.error", e );
        }

    }
}
