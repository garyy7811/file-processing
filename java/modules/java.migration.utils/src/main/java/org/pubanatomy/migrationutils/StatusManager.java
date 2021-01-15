package org.pubanatomy.migrationutils;

import java.util.Map;

/**
 * Created by greg on 10/27/16.
 */
public interface StatusManager {


    Boolean reportWorkerStats(String workerName, Map<String, Long> workerStats);
}
