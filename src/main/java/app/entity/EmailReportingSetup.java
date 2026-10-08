package app.entity;

import app.config.ReportingSetupTypeConverter;
import app.enums.ReportingSetupType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="UM_HR_ESR_Email_Reporting_Setup")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailReportingSetup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="employee_id")
    private String employeeId;

    @Column(name="email")
    private String email;

    @Column(name = "setup_type")
    @Convert(converter = ReportingSetupTypeConverter.class)
    private ReportingSetupType setupType; // 1 = exclude mail box to call google metadata api , 2 = exclude receiver email from countability

    @Column(name="description")
    private String description;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name="created_by")
    private String createdBy;

    @Column(name="updated_by")
    private String updatedBy;

}
