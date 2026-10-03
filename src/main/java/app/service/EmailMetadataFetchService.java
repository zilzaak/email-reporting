package app.service;

import app.dto.EmailMetadataDTO;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.Users;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;
import com.google.api.services.reports.Reports;
import com.google.api.services.reports.model.Activities;
import com.google.api.services.reports.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMetadataFetchService {

    private final GmailFactory gmailFactory;

    @Value("#{'${gmail.approved-mailboxes}'.split(',')}")
    private List<String> mailboxes;

    @Value("#{'${gmail.tracked-senders}'.split(',')}")
    private List<String> senders;

    @Value("${gmail.lookback-days:1}")
    private int lookbackDays;

    @Value("${gmail.sla-target-hours:8}")
    private int slaTargetHours;

    @Value("${gmail.zone:Asia/Dhaka}")
    private String zone;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final List<String> HEADERS_TO_FETCH = List.of("From", "To", "Cc", "Date", "Subject", "Message-ID", "In-Reply-To");


    public List<EmailMetadataDTO> fetchMetadataForMailboxAndDate(String mailbox, LocalDate targetDate) {
        ZoneId zoneId = getZoneId();
        long startEpochSeconds = targetDate.atStartOfDay(zoneId).toEpochSecond();
        long endEpochSeconds = targetDate.plusDays(1).atStartOfDay(zoneId).toEpochSecond();
        StringBuilder qBuilder = new StringBuilder();
        qBuilder.append(String.format("after:%d before:%d", startEpochSeconds - 1, endEpochSeconds));
        qBuilder.append(" from:").append(mailbox.trim());
        String query = qBuilder.toString();
        log.info("Fetching threads for mailbox: {} with query: [{}]", mailbox, query);

        List<EmailMetadataDTO> metadataList = new ArrayList<>();
        try {
            Gmail gmail = gmailFactory.forGmailReadonly(mailbox);
            String pageToken = null;
            do {
                ListThreadsResponse threadsResponse = gmail.users().threads().list("me").setQ(query).setMaxResults(100L).setPageToken(pageToken).execute();
                List<Thread> threadSummaries = threadsResponse.getThreads();
                if (threadSummaries != null) {
                    for (Thread summary : threadSummaries) {
                        try {
                            EmailMetadataDTO dto = processThread(gmail, summary.getId(), mailbox, zoneId);
                            if (dto != null) {
                                metadataList.add(dto);
                            }
                        } catch (Exception ex) {
                            log.error("Error processing thread {} in mailbox {}", summary.getId(), mailbox, ex);
                        }
                    }
                }
                pageToken = threadsResponse.getNextPageToken();
            } while (pageToken != null && !pageToken.isEmpty());

        } catch (Exception e) {
            log.error("Failed executing Gmail API request for mailbox: {}", mailbox, e);
            throw new RuntimeException("Gmail API call failed for " + mailbox + ": " + e.getMessage(), e);
        }
        log.info("Successfully fetched {} metadata records for mailbox {} on {}", metadataList.size(), mailbox, targetDate);
        return metadataList;
    }



    public EmailMetadataDTO processThread(Gmail gmail,String threadId, String mailbox, ZoneId zoneId) throws Exception {
      Thread thread = gmail.users().threads().get("me", threadId).setFormat("metadata").setMetadataHeaders(HEADERS_TO_FETCH).execute();
        List<Message> messages = thread.getMessages();
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        Message rootMessage = messages.get(0);
        Long rootEpochMillis = rootMessage.getInternalDate();
        LocalDateTime rootDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(rootEpochMillis), zoneId);
        String rawFrom = getHeader(rootMessage, "From");
        String senderEmail = extractFirstEmail(rawFrom);
        if (senderEmail == null || senderEmail.isBlank()) {
            return null;
        }
        List<String> receiverEmails = extractAllEmails(getHeader(rootMessage, "To"));
        if(receiverEmails.size()<1){
            return null;
        }
        List<String> ccEmails = extractAllEmails(getHeader(rootMessage, "Cc"));
        String deliveredDateStr = rootDateTime.format(DATE_TIME_FORMATTER);
        String subject = getHeader(rootMessage, "Subject");
        String rootMessageId = rootMessage.getId();
        String rootThreadId = thread.getId();

        String[] replyThreadIds   = new String[receiverEmails.size()];
        String[] replyMessageIds  = new String[receiverEmails.size()];
        String[] replyDates       = new String[receiverEmails.size()];
        String[] replyDelayHours  = new String[receiverEmails.size()];

        if (messages.size() > 1) {
            for (int i = 1; i < messages.size(); i++) {
                Message replyMsg = messages.get(i);
                Long replyEpochMillis = replyMsg.getInternalDate();
                String replyFrom = extractFirstEmail(getHeader(replyMsg, "From"));
                int receiverIndex=receiverEmails.indexOf(replyFrom);
                if (receiverIndex<0) {
                    continue;
                }
                if (replyEpochMillis != null) {
                    LocalDateTime replyDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(replyEpochMillis), zoneId);
                    long diffMillis = Math.max(0L, replyEpochMillis - rootEpochMillis); double delayHours = diffMillis / (1000.0 * 60.0 * 60.0);
                    if(replyDelayHours[receiverIndex]!=null && Double.parseDouble(replyDelayHours[receiverIndex])>delayHours){
                        replyDelayHours[receiverIndex]=String.format(Locale.US, "%.2f", delayHours);
                        replyDates[receiverIndex]=replyDateTime.format(DATE_TIME_FORMATTER);
                    }else{
                        replyDelayHours[receiverIndex]=String.format(Locale.US, "%.2f", delayHours);
                        replyDates[receiverIndex]=replyDateTime.format(DATE_TIME_FORMATTER);
                        replyThreadIds[receiverIndex]= replyMsg.getThreadId();
                        replyMessageIds[receiverIndex]=replyMsg.getId();
                    }
                } else {
                    replyDates[receiverIndex]="";
                    replyDelayHours[receiverIndex]="";
                }
            }
        }

        return EmailMetadataDTO.builder()
                .mailbox(mailbox)
                .senderEmail(senderEmail)
                .receiverEmails(receiverEmails)
                .ccEmails((ccEmails!=null && ccEmails.size()>0)?String.join(",",ccEmails):null)
                .deliveredDate(deliveredDateStr)
                .replyDate(replyDates)
                .replyDelayHour(replyDelayHours)
                .rootThreadId(rootThreadId)
                .rootMessageId(rootMessageId)
                .replyThreadId(replyThreadIds)
                .replyMessageId(replyMessageIds)
                .subject(subject)
                .build();
    }

    private ZoneId getZoneId() {
        try {
            return ZoneId.of(zone.trim());
        } catch (Exception e) {
            return ZoneId.of("Asia/Dhaka");
        }
    }

    private String getHeader(Message message, String headerName) {
        if (message == null || message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return null;
        }
        for (MessagePartHeader header : message.getPayload().getHeaders()) {
            if (headerName.equalsIgnoreCase(header.getName())) {
                return header.getValue();
            }
        }
        return null;
    }

    private String extractFirstEmail(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(headerValue);
        if (matcher.find()) {
            return matcher.group().toLowerCase().trim();
        }
        return headerValue.trim();
    }

    private List<String> extractAllEmails(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return new ArrayList<>();
        }
        List<String> emails = new ArrayList<>();
        Matcher matcher = EMAIL_PATTERN.matcher(headerValue);
        while (matcher.find()) {
            String email = matcher.group().toLowerCase().trim();
            if (!emails.contains(email)) {
                emails.add(email);
            }
        }
        return emails;
    }

}


