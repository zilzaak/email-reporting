package app.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CommonService {

    @Value("#{'${gmail.approved-mailboxes}'.split(',')}")
    public List<String> mailboxes;

    @Value("${gmail.lookback-days:1}")
    public int lookbackDays;

    @Value("${gmail.sla-target-hours:8}")
    public int slaTargetHours;

    @Value("${gmail.zone:Asia/Dhaka}")
    private String zone;

    public ZoneId zoneId;

    @PostConstruct
    public void init() {
        this.zoneId = ZoneId.of(zone);
    }

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    public static final List<String> HEADERS_TO_FETCH = List.of("From", "To", "Cc", "Date", "Subject", "Message-ID", "In-Reply-To");

}
