package app.dto.setup;

import app.enums.ReportingSetupType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailReportingSetupProcDTO {
    private String ids;
    private String employeeIds;
    private String emails;
    private ReportingSetupType setupType;
    private String description;
    private Long facultyId;
    private Long departmentId;
    private Long designationId;
    private Long categoryId;
    private Long typeId;
    private Integer pageNumber;
    private Integer pageSize;
    private String user;
    private String operation;
}
