package app.service;

import app.config.GmailFactory;
import app.dto.ApiDTO;
import app.dto.metadata.ApprovedMailBoxDTO;
import app.dto.metadata.EmailMetadataDTO;
import app.dto.metadata.NotRepliedReciversDTO;
import app.enums.ReportingSetupType;
import app.repository.EmailMetadataRepository;
import app.repository.EmailReportingSetupRepository;
import com.google.api.services.gmail.Gmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MetadataDBOperationService {

    private final EmailMetadataRepository emailMetadataRepository;
    private final EmailReportingSetupRepository emailReportingSetupRepository;
    private final EmailMetadataService emailMetadataFetchService;
    private  final GmailFactory gmailFactory;
    @Autowired
    private JdbcTemplate jdbcTemplate;



    public void fetchEmailMetadataAndPersistToDB() {
        LocalDate targetDate = LocalDate.now().minusDays(2);
        //LocalDate targetDate = LocalDate.parse("2026-09-02");
        log.info("Cron job triggered: persisting email metadata for target date: {}", targetDate);
        int totalSaved = 0;
        int totalFailed = 0;
        List<ApprovedMailBoxDTO> mailBoxDTOS=emailReportingSetupRepository.getApprovedMailBoxes(3L, ReportingSetupType.EXCLUDE_MAIL_BOX);
        for (ApprovedMailBoxDTO mailbox : mailBoxDTOS) {
            String trimmedMailbox = mailbox.getEmail().trim();
            if (trimmedMailbox.isEmpty()) continue;
            try {
                List<EmailMetadataDTO> records = emailMetadataFetchService.fetchMetadataByMailboxAndDate(trimmedMailbox, targetDate);
                for (EmailMetadataDTO metadataDTO : records) {
                    String bulkEmailTo = metadataDTO.getReceiverEmails() != null ? String.join(",", metadataDTO.getReceiverEmails()) : "";
                    String bulkReplyDate = metadataDTO.getReplyDate() != null ? String.join(",", metadataDTO.getReplyDate()) : "";
                    String bulkReplyDelayHour = metadataDTO.getReplyDelayHour() != null ? String.join(",", metadataDTO.getReplyDelayHour()) : "";
                    String bulkReplyThreadId = metadataDTO.getReplyThreadId() != null ? String.join(",", metadataDTO.getReplyThreadId()) : "";
                    String bulkReplyMessageId = metadataDTO.getReplyMessageId() != null ? String.join(",", metadataDTO.getReplyMessageId()) : "";
                    ApiDTO result = saveToDB(
                            null,
                            metadataDTO.getSubject(),
                            mailbox.getEmployeeId(),
                            trimmedMailbox,
                            null,
                            metadataDTO.getRootThreadId(),
                            metadataDTO.getRootMessageId(),
                            metadataDTO.getDeliveredDate(),
                            bulkEmailTo,
                            bulkReplyDate,
                            bulkReplyDelayHour,
                            bulkReplyThreadId,
                            bulkReplyMessageId,
                            null,
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
                           String subject,
                           String senderEmployee,
                           String senderEmail,
                           String ccMails,
                           String rootThreadId,
                           String rootMessageId,
                           String deliveredDate,
                           String receiverEmails,
                           String replyDates,
                           String replyDelayHours,
                           String replyThreadIds,
                           String replyMessageIds,
                           Boolean isCountable,
                           String operation){
        try{
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_UM_HR_ESR_Email_Metadata_Save")
                    .declareParameters(
                            new SqlParameter("in_id",                Types.NVARCHAR),
                            new SqlParameter("in_subject",           Types.NVARCHAR),
                            new SqlParameter("in_sender_employee_id",Types.VARCHAR),
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
                            new SqlParameter("in_is_countable",      Types.BIT),
                            new SqlParameter("in_user",              Types.VARCHAR),
                            new SqlParameter("in_operation",         Types.VARCHAR),
                            new SqlOutParameter("out_id",                  Types.BIGINT),
                            new SqlOutParameter("out_message_code",        Types.INTEGER),
                            new SqlOutParameter("out_message_description", Types.VARCHAR)
                    );

            Map<String, Object> params = new HashMap<>();
            params.put("in_id",                ids);
            params.put("in_subject",           subject);
            params.put("in_sender_employee_id",senderEmployee);
            params.put("in_sender_email",      senderEmail);
            params.put("in_receiver_email",    receiverEmails);
            params.put("in_cc_emails",         ccMails);
            params.put("in_delivered_date",    deliveredDate);
            params.put("in_reply_date",        replyDates);
            params.put("in_reply_delay_hour",  replyDelayHours);
            params.put("in_root_thread_id",    rootThreadId);
            params.put("in_root_message_id",   rootMessageId);
            params.put("in_reply_thread_id",   replyThreadIds);
            params.put("in_reply_message_id",  replyMessageIds);
            params.put("in_is_countable",      isCountable);
            params.put("in_user",              "System");
            params.put("in_operation",         operation);

            Map<String, Object> result = jdbcCall.execute(params);

            if (result.get("out_message_code").equals(1)) {
                return ApiDTO.builder().status(false).message((String) result.get("out_message_description")).build();
            }else{
                return ApiDTO.builder().status(true).message((String) result.get("out_message_description")).build();
            }
        } catch (Exception e) {
            return ApiDTO.builder().status(false).message(e.getMessage()).build();
        }

    }

    public void fetchEmailReplyInfoAndSyncToDB() {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);
        List<NotRepliedReciversDTO> notRepliedThreadList = emailMetadataRepository.unrepliedThreadList(null,toDate,ReportingSetupType.EXCLUDE_REPLY_COUNT);
        String bulkIds=null;
        String bulkReplyThreadIds=null;
        String bulkReplyMessageIds=null;
        String bulkReplyDates=null;
        String bulkReplyDelayHours=null;

        Map<String,EmailMetadataDTO> replyCache = new HashMap<>();

        for(NotRepliedReciversDTO notReplied : notRepliedThreadList ){
            Gmail gmail = gmailFactory.forGmailReadonly(notReplied.getSenderEmail());;
            EmailMetadataDTO dto=null;
            try{
                if(replyCache.containsKey(notReplied.getRootThreadId())){
                    dto =replyCache.get(notReplied.getRootThreadId());
                }else{
                    dto = emailMetadataFetchService.processThread(gmail, notReplied.getRootThreadId(),notReplied.getSenderEmail(),ZoneId.of("Asia/Dhaka"));
                    replyCache.put(notReplied.getRootThreadId(),dto);
                }

                int recieverIndex = dto.getReceiverEmails().indexOf(notReplied.getReceiverEmail());

                if(dto!=null && dto.getReplyDate()[recieverIndex]!=null && !dto.getReplyDate()[recieverIndex].isEmpty()){
                    if(bulkIds==null){
                        bulkIds=notReplied.getId().toString();
                        bulkReplyThreadIds=dto.getReplyThreadId()[recieverIndex];
                        bulkReplyMessageIds=dto.getReplyMessageId()[recieverIndex];
                        bulkReplyDates=dto.getReplyDate()[recieverIndex];
                        bulkReplyDelayHours=dto.getReplyDelayHour()[recieverIndex];
                    }else{
                        bulkIds=bulkIds+","+notReplied.getId();
                        bulkReplyThreadIds=bulkReplyThreadIds+","+dto.getReplyThreadId()[recieverIndex];
                        bulkReplyMessageIds=bulkReplyMessageIds+","+dto.getReplyMessageId()[recieverIndex];
                        bulkReplyDates=bulkReplyDates+","+dto.getReplyDate()[recieverIndex];
                        bulkReplyDelayHours=bulkReplyDelayHours+","+dto.getReplyDelayHour()[recieverIndex];
                    }
                }

            } catch (Exception e) {

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
                    null,
                    null,
                    bulkReplyDates,
                    bulkReplyDelayHours,
                    bulkReplyThreadIds,
                    bulkReplyMessageIds,
                    null,
                    "U");
        }
    }

    public ApiDTO updateCountability(String bulkIds, Boolean isCountable) {
        return this.saveToDB(bulkIds
                ,null
                ,null
                ,null
                ,null
                ,null
                ,null
                ,null
                ,null
                ,null
                ,null
                ,null
                ,null
                ,isCountable
                ,"UC"
        );
    }
}
