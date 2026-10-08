package app.dto;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ApiDTO {
    private long totalItems;
    private boolean status;
    private String message;
    private Integer totalPages;
    private Integer pageNumber;
    private Integer pageSize;
    private Object data;
}
