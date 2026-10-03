
package app.cronJobs;

import app.service.EmailMetadataFetchService;
import app.service.EmailMetadataPersistToDBService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailsReplierInfoFetcherCronJob {
      private final EmailMetadataPersistToDBService emailMetadataPersistToDBService;
    @Scheduled(cron = "0 0/1 * * * ?")
    public void executeTask() {
        try {
           emailMetadataPersistToDBService.fetchEmailReplyInfoAndSyncToDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

