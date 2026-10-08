
package app.jobs;
import app.service.MetadataDBOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailsReplySyncJob {
      private final MetadataDBOperationService dbOperationService;
    @Scheduled(cron = "0 0/2 * * * ?")
    public void executeTask() {
        try {
           // dbOperationService.fetchEmailReplyInfoAndSyncToDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

