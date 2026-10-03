package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RootSenderDto {
    private String threadId;
    private String messageId;
    private String senderEmail;
    private String senderRaw;
    private String subject;
    private String foundInMailbox;

    private String receiverEmail;
    private String receiverRaw;
    private Instant deliveredAt;
    private Instant senderStampedAt;

    // NEW
    private boolean isReply;      // true if In-Reply-To or References present
    private boolean isForward;    // true if subject starts with Fwd:/FW:
}