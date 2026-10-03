package app.service;

import app.dto.ApiDTO;
import app.dto.EmailMetadataDTO;
import app.repository.EmailMetadataRepository;
import com.google.api.services.gmail.Gmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMetadataPersistToDBService {

    private final EmailMetadataRepository emailMetadataRepository;
    private final EmailMetadataFetchService emailMetadataFetchService;
    private  final GmailFactory gmailFactory;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("#{'${gmail.approved-mailboxes}'.split(',')}")
    private List<String> mailboxes;
    @Value("${gmail.lookback-days:1}")
    private int lookbackDays;
    @Value("${gmail.zone:Asia/Dhaka}")
    private String zone;


    public void fetchEmailMetadataAndPersistToDB() {
        //LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDate targetDate = LocalDate.parse("2026-09-02");
        log.info("Cron job triggered: persisting email metadata for target date: {}", targetDate);
        int totalSaved = 0;
        int totalFailed = 0;
        for (String mailbox : mailboxes) {
            String trimmedMailbox = mailbox.trim();
            if (trimmedMailbox.isEmpty()) continue;
            try {
                List<EmailMetadataDTO> records = emailMetadataFetchService.fetchMetadataForMailboxAndDate(trimmedMailbox, targetDate);
                for (EmailMetadataDTO metadataDTO : records) {
                    String receiverEmailStr = metadataDTO.getReceiverEmails() != null ? String.join(",", metadataDTO.getReceiverEmails()) : "";
                    String replyDateStr = metadataDTO.getReplyDate() != null ? String.join(",", metadataDTO.getReplyDate()) : "";
                    String replyDelayHourStr = metadataDTO.getReplyDelayHour() != null ? String.join(",", metadataDTO.getReplyDelayHour()) : "";
                    String replyThreadIdStr = metadataDTO.getReplyThreadId() != null ? String.join(",", metadataDTO.getReplyThreadId()) : "";
                    String replyMessageIdStr = metadataDTO.getReplyMessageId() != null ? String.join(",", metadataDTO.getReplyMessageId()) : "";
                    ApiDTO result = saveToDB(
                            null,
                            trimmedMailbox,
                            null,
                            metadataDTO.getRootThreadId(),
                            metadataDTO.getRootMessageId(),
                            metadataDTO.getDeliveredDate(),
                            receiverEmailStr,
                            replyDateStr,
                            replyDelayHourStr,
                            replyThreadIdStr,
                            replyMessageIdStr,
                            "I");
                    if (result.isStatus()) {
                        totalSaved++;
                    } else {
                        totalFailed++;
                    }
                }
            } catch (Exception e) {
                log.error("Error fetching/persisting emails for mailbox: {}", trimmedMailbox, e);
            }
        }
        log.info("Persist job completed. Total saved: {}, Total failed: {}", totalSaved, totalFailed);
    }

    public ApiDTO saveToDB(String ids ,
                           String sender,
                           String ccMails,
                           String rootThreadId,
                           String rootMessageId,
                           String deliveredDate,
                           String receiverEmailStr,
                           String replyDateStr,
                           String replyDelayHourStr,
                           String replyThreadIdStr,
                           String replyMessageIdStr,
                           String operation){
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_UM_HR_ESR_Email_Metadata_Save")
                .declareParameters(
                        new SqlParameter("in_id",                Types.NVARCHAR),
                        new SqlParameter("in_sender_email",      Types.VARCHAR),
                        new SqlParameter("in_receiver_email",    Types.NVARCHAR),
                        new SqlParameter("in_cc_emails",         Types.NVARCHAR),
                        new SqlParameter("in_delivered_date",    Types.VARCHAR),
                        new SqlParameter("in_reply_date",        Types.NVARCHAR),
                        new SqlParameter("in_reply_delay_hour",  Types.NVARCHAR),
                        new SqlParameter("in_root_thread_id",    Types.VARCHAR),
                        new SqlParameter("in_root_message_id",   Types.VARCHAR),
                        new SqlParameter("in_reply_thread_id",   Types.NVARCHAR),
                        new SqlParameter("in_reply_message_id",  Types.NVARCHAR),
                        new SqlParameter("in_user",              Types.VARCHAR),
                        new SqlParameter("in_operation",         Types.VARCHAR),
                        new SqlOutParameter("out_id",                  Types.BIGINT),
                        new SqlOutParameter("out_message_code",        Types.INTEGER),
                        new SqlOutParameter("out_message_description", Types.VARCHAR)
                );

        Map<String, Object> params = new HashMap<>();
        params.put("in_id",                ids);
        params.put("in_sender_email",      sender);
        params.put("in_receiver_email",    receiverEmailStr);
        params.put("in_cc_emails",         ccMails);
        params.put("in_delivered_date",    deliveredDate);
        params.put("in_reply_date",        replyDateStr);
        params.put("in_reply_delay_hour",  replyDelayHourStr);
        params.put("in_root_thread_id",    rootThreadId);
        params.put("in_root_message_id",   rootMessageId);
        params.put("in_reply_thread_id",   replyThreadIdStr);
        params.put("in_reply_message_id",  replyMessageIdStr);
        params.put("in_user",              "System");
        params.put("in_operation",         operation);

        Map<String, Object> result = jdbcCall.execute(params);

        int code = result.get("out_message_code") != null
                ? Integer.parseInt(result.get("out_message_code").toString()) : 0;
        if (code > 0) {
            return ApiDTO.builder().status(false).message((String) result.get("out_message_description")).build();
        }else{
            return ApiDTO.builder().status(false).message((String) result.get("out_message_description")).build();
        }
    }

    public void fetchEmailReplyInfoAndSyncToDB() {
        List<Map<String,Object>> notRepliedThreadList = emailMetadataRepository.unrepliedThreadList();
        String bulkIds=null;
        String bulkReplyThreadIds=null;
        String bulkReplyMessageIds=null;
        String bulkReplyDates=null;
        String bulkReplyDelayHours=null;

        Map<String,EmailMetadataDTO> replyCache = new HashMap<>();

        int i=0;

        for(Map<String,Object> mp : notRepliedThreadList ){
            Gmail gmail = gmailFactory.forGmailReadonly((String) mp.get("senderEmail"));
            String receiverEmail = mp.get("receiverEmail").toString();
            Long id = (Long) mp.get("id");
            EmailMetadataDTO dto=null;
            try{
                if(replyCache.containsKey((String) mp.get("rootThreadId"))){
                    dto =replyCache.get((String) mp.get("rootThreadId"));
                }else{
                    dto = emailMetadataFetchService.processThread(gmail, (String) mp.get("rootThreadId"),(String) mp.get("senderEmail"),ZoneId.of("Asia/Dhaka"));
                    replyCache.put((String) mp.get("rootThreadId"),dto);
                }

                int recieverIndex = dto.getReceiverEmails().indexOf(receiverEmail);

                if(dto!=null && dto.getReplyDate()[recieverIndex]!=null && !dto.getReplyDate()[recieverIndex].isEmpty()){
                    if(i==0){
                        bulkIds=id.toString();
                        bulkReplyThreadIds=dto.getReplyThreadId()[recieverIndex];
                        bulkReplyMessageIds=dto.getReplyMessageId()[recieverIndex];
                        bulkReplyDates=dto.getReplyDate()[recieverIndex];
                        bulkReplyDelayHours=dto.getReplyDelayHour()[recieverIndex];
                    }else{
                        bulkIds=bulkIds+","+id;
                        bulkReplyThreadIds=bulkReplyThreadIds+","+dto.getReplyThreadId()[recieverIndex];
                        bulkReplyMessageIds=bulkReplyMessageIds+","+dto.getReplyMessageId()[recieverIndex];
                        bulkReplyDates=bulkReplyDates+","+dto.getReplyDate()[recieverIndex];
                        bulkReplyDelayHours=bulkReplyDelayHours+","+dto.getReplyDelayHour()[recieverIndex];
                    }
                }

                i=i+1;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if(bulkIds!=null){
            this.saveToDB(bulkIds,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    bulkReplyDates,
                    bulkReplyDelayHours,
                    bulkReplyThreadIds,
                    bulkReplyMessageIds,
                    "U");
        }
    }
}
