
package app.jobs;

import app.service.MetadataDBOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailMetadataFetchJob {
    private final MetadataDBOperationService emailMetadataPersistToDBService;
    @Scheduled(cron = "0 0/2 * * * ?")
    public void executeTask() {
        try {
            emailMetadataPersistToDBService.fetchEmailMetadataAndPersistToDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}






