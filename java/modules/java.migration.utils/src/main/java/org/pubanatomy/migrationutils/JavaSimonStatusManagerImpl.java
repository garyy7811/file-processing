package org.pubanatomy.migrationutils;

import lombok.extern.log4j.Log4j2;
import org.javasimon.Counter;
import org.javasimon.SimonManager;

import java.util.Map;

/**
 * Created by greg on 11/2/16.
 */
@Log4j2
public class JavaSimonStatusManagerImpl implements StatusManager {


    @Override
    public synchronized Boolean reportWorkerStats(String workerName, Map<String, Long> workerStats) {

        log.info("reportWorkerStats for {}", workerName);

        //lstOfMso.stream().forEachOrdered( mso -> {
        workerStats.entrySet().stream().forEach( entry -> {

            log.info("{}.{}={}", workerName, entry.getKey(), entry.getValue());

            String workerCounterName = entry.getKey() + ".W-" + workerName;
            Counter workerCounter = SimonManager.getCounter(workerCounterName);
            Long oldWorkerValue = workerCounter.getCounter();
            Long workerDelta = entry.getValue() -  oldWorkerValue;

            // set new worker value
            workerCounter.set(entry.getValue());

            // add delta to group value
            String groupCounterName = entry.getKey() + ".ALL";
            Counter groupCounter = SimonManager.getCounter(groupCounterName);
            groupCounter.increase(workerDelta);

            log.info("Added {} to {}", workerDelta, groupCounterName);

        });

        return true;
    }

}
