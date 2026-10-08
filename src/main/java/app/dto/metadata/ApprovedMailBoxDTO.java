package app.dto.metadata;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
public class ApprovedMailBoxDTO {
    private String employeeId;
    private String email;
    public ApprovedMailBoxDTO(String employeeId, String email) {
        this.employeeId = employeeId;
        this.email = email;
    }
}