package app.dto.setup;

import app.enums.ReportingSetupType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailReportingSetupRequestDTO {
    private List<Long> ids;
    private List<String> employeeIds;
    private List<String> emails;
    private ReportingSetupType setupType;
    @Schema(description = "Description or remark regarding the condition setup", example = "Exempt from SLA rating")
    private String description;
}
