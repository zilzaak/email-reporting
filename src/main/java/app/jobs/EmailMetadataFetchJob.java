
package app.jobs;

import app.service.MetadataDBOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class EmailMetadataFetchJob {
    private final MetadataDBOperationService emailMetadataPersistToDBService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Integer backDate=0;
    //@Scheduled(cron = "0 0/3 * * * ?")  //evry 2 minutes later
    @Scheduled(cron = "0 0 2 * * ?") //at late night 2 am
    public void executeTask() {
        if (!running.compareAndSet(false, true)) {
            System.out.println("Skipped — previous execution still running.");
            return;
        }
        try {
            emailMetadataPersistToDBService.fetchEmailMetadataAndPersistToDB(backDate);
            backDate += 1;
            System.out.println("Fetched " + backDate + " days previous data");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            running.set(false);
        }
    }
}






