package app.controller;

import app.dto.ApiDTO;
import app.dto.setup.EmailReportingSetupRequestDTO;
import app.enums.ReportingSetupType;
import app.service.EmailReportingSetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/email-reporting-setup")
@RequiredArgsConstructor
@Tag(name = "Email reporting setup")
public class EmailReportingSetupController {

    private final EmailReportingSetupService reportingConditionSetupService;

    @PostMapping("/create")
    @Operation(summary = "Create reporting setup",
            description = "Insert reporting setup")
    public ResponseEntity<ApiDTO> create(@RequestBody EmailReportingSetupRequestDTO requestDto) {
        ApiDTO response = reportingConditionSetupService.createReportingSetup(requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete reporting setup by multiple IDs",
            description = "Deletes reporting setup")
    public ResponseEntity<ApiDTO> delete(@RequestParam List<Long> ids) {
        ApiDTO response = reportingConditionSetupService.deleteReportingSetup(ids, "system");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    @Operation(summary = "Update reporting setup by multiple IDs",
            description = "Updates reporting setup")
    public ResponseEntity<ApiDTO> update(@RequestBody EmailReportingSetupRequestDTO requestDto) {
        ApiDTO response = reportingConditionSetupService.updateReportingSetup(requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee-list")
    @Operation(summary = "Search employee information",
            description = "Search employee information")
    public ResponseEntity<ApiDTO> searchEmployee(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) Long categoryId, // employee category
            @RequestParam(required = false) Long typeId,    // employee type
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        ApiDTO response = reportingConditionSetupService.searchEmployee(
                employeeId,email, facultyId, departmentId, designationId, categoryId, typeId, pageNumber, pageSize
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/setup-list")
    @Operation(summary = "Search reporting setup",
            description = "Search reporting setup info")
    public ResponseEntity<ApiDTO> searchSetup(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String emails,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) Long categoryId, // employee category
            @RequestParam(required = false) Long typeId,    // employee type
            @RequestParam ReportingSetupType setupType,
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        ApiDTO response = reportingConditionSetupService.searchSetup(
                employeeId,emails, facultyId, departmentId, designationId, categoryId, typeId, setupType, pageNumber, pageSize
        );
        return ResponseEntity.ok(response);
    }
}
