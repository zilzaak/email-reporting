package app.repository;

import app.entity.EmailMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface EmailMetadataRepository extends JpaRepository<EmailMetadata,Long> {

    @Procedure(name = "spEmailMetadataSave")
    Map<String, Object> spEmailMetadataSave(
            @Param("in_id") String in_id,
            @Param("in_sender_email") String senderEmail,
            @Param("in_receiver_email") String receiverEmails,
            @Param("in_cc_emails") String ccEmails,
            @Param("in_delivered_date") String deliveredDate,
            @Param("in_reply_date") String replyDates,
            @Param("in_reply_delay_hour") String replyDelayHours,
            @Param("in_root_thread_id") String rootThreadId,
            @Param("in_root_message_id") String rootMessageId,
            @Param("in_reply_thread_id") String replyThreadIds,
            @Param("in_reply_message_id") String replyMessageIds,
            @Param("in_user") String in_user,
            @Param("in_operation") String in_operation
    );

    @Query("select id as id ,rootThreadId as rootThreadId,receiverEmail  as receiverEmail , senderEmail as senderEmail from EmailMetadata where replyDate is null order by deliveredDate asc ")
    List<Map<String,Object>> unrepliedThreadList();

}
