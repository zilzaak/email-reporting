package app.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMetadataDTO {
    private String senderEmail;
    @Builder.Default
    private List<String> receiverEmails = new ArrayList<>();
    private String deliveredDate;
    private String ccEmails;
    @Builder.Default
    private String[] replyDate=null;
    @Builder.Default
    private String[] replyDelayHour=null;
    private String rootThreadId;
    private String rootMessageId;
    @Builder.Default
    private String[] replyThreadId=null;
    @Builder.Default
    private String[] replyMessageId=null;
    private String subject;
    private String mailbox;
}
