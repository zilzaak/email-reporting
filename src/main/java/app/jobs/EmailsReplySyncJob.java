
package app.jobs;
import app.service.MetadataDBOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class EmailsReplySyncJob {
      private final MetadataDBOperationService dbOperationService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    //@Scheduled(cron = "0 0/50 * * * ?")
    @Scheduled(fixedRate = 7 * 60 * 60 * 1000L) // every 7 hour later
    public void executeTask() {
        try {
            if (!running.compareAndSet(false, true)) {
                System.out.println("Skipped — previous execution still running.");
                return;
            }
            try {
                dbOperationService.fetchEmailReplyInfoAndSyncToDB();
                System.out.println("done");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                running.set(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

