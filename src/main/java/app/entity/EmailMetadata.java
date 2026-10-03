package app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="UM_HR_ESR_Email_Metadata")
@NamedStoredProcedureQuery(
        name="spEmailMetadataSave",procedureName = "SP_UM_HR_ESR_Email_Metadata_Save",
        parameters = {
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_id", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_sender_email", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_receiver_email", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_cc_emails", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_delivered_date", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_reply_date", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_reply_delay_hour", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_root_thread_id", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_root_message_id", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_reply_thread_id", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_reply_message_id", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_user", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "in_operation", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.OUT, name = "out_id", type = Long.class),
                @StoredProcedureParameter(mode = ParameterMode.OUT, name = "out_message_code", type = Integer.class),
                @StoredProcedureParameter(mode = ParameterMode.OUT, name = "out_message_description", type = String.class)
        }
)
public class EmailMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="sender_email")
    private String senderEmail;

    @Column(name="cc_emails")
    private String ccEmails;

    @Column(name="receiver_email")
    private String receiverEmail;

    @Column(name="delivered_date")
    private LocalDateTime deliveredDate;

    @Column(name="reply_date")
    private LocalDateTime replyDate;

    @Column(name="response_delay_hour")
    private Double responseDelayHour;

    @Column(name="root_thread_id")
    private String rootThreadId;

    @Column(name="root_message_id")
    private String rootMessageId;

    @Column(name="reply_thread_id")
    private String replyThreadId;

    @Column(name="reply_message_id")
    private String replyMessageId;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name="created_by")
    private String createdBy;

    @Column(name="updated_by")
    private String updatedBy;
}
