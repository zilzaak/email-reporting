package app.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="UM_HR_ESR_Email_Subject")
public class EmailSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="sender_employee_id")
    private String senderEmployeeId;

    @Column(name="subject")
    private String subject;

    @Column(name="sender_email")
    private String senderEmail;

    @Column(name="cc_mails")
    private String ccEmails;

    @Column(name="delivered_date")
    private LocalDateTime deliveredDate;

    @Column(name="root_thread_id")
    private String rootThreadId;

    @Column(name="root_message_id")
    private String rootMessageId;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name="created_by")
    private String createdBy;

    @Column(name="updated_by")
    private String updatedBy;
}
