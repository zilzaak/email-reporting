package app.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Collections;

@Configuration
public class GmailConfig {

    @Value("${gmail.service-account-key}")
    private Resource serviceAccountKey;

    @Value("${gmail.scope_mail_readonly}")
    private String scope;

    private String impersonateUser="hroffice10@daffodilvarsity.edu.bd";

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public Gmail gmailService() throws Exception {
        InputStream keyStream = serviceAccountKey.getInputStream();
        GoogleCredentials credentials = ServiceAccountCredentials
                .fromStream(keyStream)
                .createScoped(Collections.singletonList(scope))
                .createDelegated(impersonateUser);   // DWD impersonation
        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(appName)
                .build();
    }
}