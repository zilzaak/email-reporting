
/*
package app.cronJobs;

import app.service.EmailMetadataPersistToDBService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailMetadataFetcherCronJob {
    private final EmailMetadataPersistToDBService emailMetadataPersistToDBService;
    @Scheduled(cron = "0 0/1 * * * ?")
    public void executeTask() {
        try {
            emailMetadataPersistToDBService.fetchEmailMetadataAndPersistToDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
*/


