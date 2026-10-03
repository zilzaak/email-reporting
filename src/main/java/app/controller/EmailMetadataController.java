package app.controller;

import app.dto.ApiDTO;
import app.dto.EmailMetadataDTO;
import app.service.EmailMetadataFetchService;
import app.service.EmailMetadataPersistToDBService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/email-metadata")
@RequiredArgsConstructor
@Tag(name ="email sla reporting")
public class EmailMetadataController {

    private final EmailMetadataFetchService fetchService;
    private final EmailMetadataPersistToDBService emailMetadataPersistToDBService;

    @GetMapping("/sender")
    @Operation(description = "fetch the senders information on a specific date")
    public ResponseEntity<ApiDTO> senderInfo(
            @RequestParam String mailbox,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<EmailMetadataDTO> list = fetchService.fetchMetadataForMailboxAndDate(mailbox, date);
        return ResponseEntity.ok(ApiDTO.builder()
                .status(true)
                .message("Fetched " + list.size() + " metadata records successfully")
                .totalItems(list.size())
                .data(list)
                .build());
    }

    @GetMapping("/update")
    @Operation(description = "fetch the senders information on a specific date")
    public ResponseEntity<ApiDTO> updateReplyInfo(@RequestParam String mailbox, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<EmailMetadataDTO> list = fetchService.fetchMetadataForMailboxAndDate(mailbox, date);
        return ResponseEntity.ok(ApiDTO.builder()
                .status(true)
                .message("Fetched " + list.size() + " metadata records successfully")
                .totalItems(list.size())
                .data(list)
                .build());
    }


}
