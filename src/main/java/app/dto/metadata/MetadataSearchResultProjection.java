package app.dto.metadata;

public interface MetadataSearchResultProjection {
    Long   getId();
    Long   getSubjectId();
    Long   getTotalCount();
    String getSenderEmail();
    String getSenderEmployeeId();
    String getEmployeeId();
    String getDeliveredDate();
    String getSubject();
    String getCc();
    String getReceiverEmail();
    String getRootThreadId();
    String getRootMessageId();
    String getReplyThreadId();
    String getReplyMessageId();
    String getReplyDate();
    Double getReplyDelayHour();
    Boolean getIsCountable();
    Boolean getStatus();
    Long getTotalRecords();
}