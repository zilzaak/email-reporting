package app.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.reports.Reports;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GmailFactory {

    @Value("${gmail.scope_mail_readonly}")
    private String gmailReadonly;

    @Value("${gmail.scope_mail_metadata}")
    private String gmailMetadata;

    @Value("${gmail.scope_admin_report}")
    private String gmailAdminReport;

    @Value("${gmail.scope_admin_directory}")
    private String gmailAdminDirectory;

    @Value("${gmail.service-account-key}")
    private Resource keyResource;

    private static final String APP_NAME = "DIU-Workspace-Email-SLA-Reporting";

    private final Map<String, Gmail> cache = new ConcurrentHashMap<>();
    private final Map<String, Gmail>     gmailCache     = new ConcurrentHashMap<>();
    private final Map<String, Gmail>     gmailMetaCache = new ConcurrentHashMap<>();
    private final Map<String, Directory> directoryCache = new ConcurrentHashMap<>();
    private final Map<String, Reports>   reportsCache   = new ConcurrentHashMap<>();

    public Gmail forGmailReadonly(String mailbox) {
        return cache.computeIfAbsent(mailbox, mb -> {
            try {
                GoogleCredentials creds = GoogleCredentials
                    .fromStream(keyResource.getInputStream())
                    .createScoped(gmailReadonly)
                    .createDelegated(mb);

                return new Gmail.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(creds))
                       .setApplicationName("DIU-Email-SLA")
                       .build();
            } catch (Exception e) {
                throw new RuntimeException("Gmail init failed for " + mb, e);
            }
        });
    }

    /* ---------- 1. Gmail METADATA scope ---------- */
    public Gmail forGmailMetadata(String mailbox) {
        return gmailMetaCache.computeIfAbsent(mailbox, mb -> {
            try {
                GoogleCredentials creds = GoogleCredentials
                        .fromStream(keyResource.getInputStream())
                        .createScoped(gmailMetadata)     // ✅ uses the scope string from properties
                        .createDelegated(mb);

                return new Gmail.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(creds))
                        .setApplicationName(APP_NAME)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Gmail Metadata init failed for " + mb, e);
            }
        });
    }

    /* ---------- 2. Admin Directory READONLY scope ---------- */
    public Directory forAdminDirectoryReadonly(String mailbox) {
        return directoryCache.computeIfAbsent(mailbox, mb -> {
            try {
                GoogleCredentials creds = GoogleCredentials
                        .fromStream(keyResource.getInputStream())
                        .createScoped(gmailAdminDirectory)
                        .createDelegated(mb);

                return new Directory.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(creds))
                        .setApplicationName(APP_NAME)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Directory init failed for " + mb, e);
            }
        });
    }

    /* ---------- 3. Admin Reports AUDIT READONLY scope ---------- */
    public Reports forAdminReportsReadonly(String mailbox) {
        return reportsCache.computeIfAbsent(mailbox, mb -> {
            try {
                GoogleCredentials creds = GoogleCredentials
                        .fromStream(keyResource.getInputStream())
                        .createScoped(gmailAdminReport)
                        .createDelegated(mb);

                return new Reports.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(creds))
                        .setApplicationName(APP_NAME)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Reports init failed for " + mb, e);
            }
        });
    }
}