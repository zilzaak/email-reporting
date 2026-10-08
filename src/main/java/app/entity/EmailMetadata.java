package app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="UM_HR_ESR_Email_Metadata")
public class EmailMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_subject_id")
    private EmailSubject emailSubject;

    @Column(name="employee_id")
    private String employeeId;

    @Column(name="receiver_email")
    private String receiverEmail;

    @Column(name="reply_date")
    private LocalDateTime replyDate;

    @Column(name="response_delay_hour")
    private Double responseDelayHour;

    @Column(name="reply_thread_id")
    private String replyThreadId;

    @Column(name="reply_message_id")
    private String replyMessageId;

    @Column(name="status")
    private Boolean status;

    @Column(name="is_countable")
    private Boolean isCountable;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name="created_by")
    private String createdBy;

    @Column(name="updated_by")
    private String updatedBy;
}
