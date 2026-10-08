package app.controller;

import app.dto.ApiDTO;
import app.service.EmailMetadataService;
import app.service.MetadataDBOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/email-metadata")
@RequiredArgsConstructor
@Tag(name ="email sla reporting")
public class EmailMetadataController {

    private final EmailMetadataService fetchService;
    private final MetadataDBOperationService dbOperationService;

    @GetMapping("/list")
    @Operation(description = "fetch mail metadata list")
    public ResponseEntity<ApiDTO> threadList(
            @RequestParam(required = false) String employeeOrMailBox,
            @RequestParam(required = false)  LocalDate fromDate,
            @RequestParam(required = false)  LocalDate toDate,
            @RequestParam(defaultValue = "1")  Integer pageNumber,
            @RequestParam(defaultValue = "10")  Integer pageSize
            ) {
        ApiDTO response = fetchService.getMailMetaDataList(employeeOrMailBox,fromDate,toDate,pageNumber,pageSize);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update-countability")
    @Operation(description = "update countability")
    public ResponseEntity<ApiDTO> updateCountability(
            @RequestParam String bulkIds,
            @RequestParam Boolean isCountable
    ) {
        ApiDTO response = dbOperationService.updateCountability(bulkIds,isCountable);
        return ResponseEntity.ok(response);
      }


}
