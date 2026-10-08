package app.dto.metadata;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotRepliedReciversDTO {
    private Long id;
    private String rootThreadId;
    private String receiverEmail;
    private String senderEmail;

    public NotRepliedReciversDTO(Long id, String rootThreadId, String receiverEmail, String senderEmail) {
        this.id = id;
        this.rootThreadId = rootThreadId;
        this.receiverEmail = receiverEmail;
        this.senderEmail = senderEmail;
    }
}
