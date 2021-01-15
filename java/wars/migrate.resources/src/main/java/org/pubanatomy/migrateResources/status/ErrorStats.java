package org.pubanatomy.migrateResources.status;

import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 10/7/16.
 */
@Log4j2
public class ErrorStats {

    @Getter
    private Map<String, Long> errorCounts = new HashMap<>();

    @Synchronized
    public void recordError(String errorId) {

        Long currentValue = errorCounts.getOrDefault(errorId, 0L);
        errorCounts.put(errorId, currentValue+1);

    }

    public void logErrors() {

        log.info("**** all recorded Errors: ****");

        for (Map.Entry<String, Long> entry : errorCounts.entrySet()) {
            log.error("{}={}",entry.getKey(), entry.getValue());
        }

    }

}
