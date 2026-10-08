package app.enums;

public enum ReportingSetupType {

    EXCLUDE_MAIL_BOX(1L), // to EXCLUDE A MAILBOX CALLING GOOGLE API WE USE THIS FLAG
    EXCLUDE_REPLY_COUNT(2L);// to EXCLUDE A COUNTABILITY OF A RECEIVER EMAIL WE USE THIS FLAG

    private final Long value;
    ReportingSetupType(Long value) {
        this.value = value;
    }
    public Long getValue() {
        return value;
    }
}
