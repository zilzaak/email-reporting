package app.service;

import app.dto.ApiDTO;
import app.dto.setup.EmailReportingSetupProcDTO;
import app.dto.setup.EmailReportingSetupRequestDTO;
import app.enums.ReportingSetupType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReportingSetupService {

    private final JdbcTemplate jdbcTemplate;

    public ApiDTO createReportingSetup(EmailReportingSetupRequestDTO requestDto) {
        if (requestDto == null) {
            return ApiDTO.builder().status(false).message("Request body cannot be null.").build();
        }

        if (requestDto.getEmployeeIds() == null || requestDto.getEmployeeIds().isEmpty()
                || requestDto.getEmails() == null || requestDto.getEmails().isEmpty()) {
            return ApiDTO.builder().status(false).message("At least one employee ID and email is required.").build();
        }

        if (requestDto.getSetupType() == null) {
            return ApiDTO.builder().status(false).message("Setup type is required.").build();
        }

        String bulkEmployeeIds = String.join(",", requestDto.getEmployeeIds());
        String bulkEmails = String.join(",", requestDto.getEmails());

        return executeProcedure(EmailReportingSetupProcDTO.builder()
                .employeeIds(bulkEmployeeIds)
                .emails(bulkEmails)
                .setupType(requestDto.getSetupType())
                .description(requestDto.getDescription())
                .operation("I")
                .build());
    }

    public ApiDTO deleteReportingSetup(List<Long> ids, String user) {
        if (ids == null || ids.isEmpty()) {
            return ApiDTO.builder().status(false).message("At least one ID is required for deletion.").build();
        }

        List<Long> nonNullIds = ids.stream().filter(Objects::nonNull).toList();
        if (nonNullIds.isEmpty()) {
            return ApiDTO.builder().status(false).message("Valid IDs must be provided for deletion.").build();
        }

        String bulkIds = nonNullIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return executeProcedure(EmailReportingSetupProcDTO.builder()
                .ids(bulkIds)
                .user(user)
                .operation("D")
                .build());
    }

    public ApiDTO updateReportingSetup(EmailReportingSetupRequestDTO requestDto) {
        if (requestDto == null || requestDto.getIds() == null || requestDto.getIds().isEmpty()) {
            return ApiDTO.builder().status(false).message("At least one ID is required for update.").build();
        }

        List<Long> nonNullIds = requestDto.getIds().stream().filter(Objects::nonNull).toList();
        if (nonNullIds.isEmpty()) {
            return ApiDTO.builder().status(false).message("Valid IDs must be provided for update.").build();
        }

        String bulkIds = nonNullIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        return executeProcedure(EmailReportingSetupProcDTO.builder()
                .ids(bulkIds)
                .setupType(requestDto.getSetupType())
                .employeeIds(String.join(",",requestDto.getEmployeeIds()))
                .emails(String.join(",",requestDto.getEmails()))
                .description(requestDto.getDescription())
                .operation("U")
                .build());
    }

    public ApiDTO searchEmployee(String employeeId,String mail, Long facultyId, Long departmentId, Long designationId, Long categoryId, Long typeId, Integer pageNumber, Integer pageSize) {
        return executeProcedure(EmailReportingSetupProcDTO.builder()
                .employeeIds(employeeId)
                .emails(mail)
                .facultyId(facultyId)
                .departmentId(departmentId)
                .designationId(designationId)
                .categoryId(categoryId)
                .typeId(typeId)
                .pageNumber(pageNumber != null ? pageNumber : 1)
                .pageSize(pageSize != null ? pageSize : 10)
                .operation("S1")
                .build());
    }


    public ApiDTO searchSetup(String employeeId,String mail, Long facultyId, Long departmentId, Long designationId, Long categoryId, Long typeId, ReportingSetupType setupType, Integer pageNumber, Integer pageSize) {
        return executeProcedure(EmailReportingSetupProcDTO.builder()
                .employeeIds(employeeId)
                .emails(mail)
                .facultyId(facultyId)
                .departmentId(departmentId)
                .designationId(designationId)
                .categoryId(categoryId)
                .typeId(typeId)
                .setupType(setupType)
                .pageNumber(pageNumber != null ? pageNumber : 1)
                .pageSize(pageSize != null ? pageSize : 10)
                .operation("S2")
                .build());
    }



    /**
     * Reusable stored procedure execution method for SP_UM_HR_ESR_Email_Reporting_Setup_Save.
     * Supports operations:
     * - 'I': Insert / Bulk Create
     * - 'U': Update by ID(s)/bulk update
     * - 'D': Delete by ID(s)/bulk delete
     * - 'S1': Search Employee Information with pagination
     * - 'S2': Search Reporting Setup with pagination
     */
    public ApiDTO executeProcedure(EmailReportingSetupProcDTO params) {
        if (params == null || params.getOperation() == null || params.getOperation().isBlank()) {
            return ApiDTO.builder().status(false).message("Operation type is required.").build();
        }

        String operation = params.getOperation().trim().toUpperCase();

        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_UM_HR_ESR_Email_Reporting_Setup_Save")
                    .declareParameters(
                            new SqlParameter("in_ids",                 Types.NVARCHAR),
                            new SqlParameter("in_employee_ids",        Types.NVARCHAR),
                            new SqlParameter("in_emails",              Types.NVARCHAR),
                            new SqlParameter("in_setup_type",          Types.BIGINT),
                            new SqlParameter("in_description",         Types.VARCHAR),
                            new SqlParameter("in_faculty_id",          Types.BIGINT),
                            new SqlParameter("in_department_id",       Types.BIGINT),
                            new SqlParameter("in_designation_id",      Types.BIGINT),
                            new SqlParameter("in_category_id",         Types.BIGINT),
                            new SqlParameter("in_type_id",             Types.BIGINT),
                            new SqlParameter("in_page_number",         Types.INTEGER),
                            new SqlParameter("in_page_size",           Types.INTEGER),
                            new SqlParameter("in_user",                Types.VARCHAR),
                            new SqlParameter("in_operation",           Types.VARCHAR),
                            new SqlOutParameter("out_id",                  Types.BIGINT),
                            new SqlOutParameter("out_message_code",        Types.INTEGER),
                            new SqlOutParameter("out_message_description", Types.VARCHAR)
                    );

            boolean isSearch = "S1".equalsIgnoreCase(operation) || "S2".equalsIgnoreCase(operation);
            if (isSearch) {
                jdbcCall.returningResultSet("#result-set-1", new ColumnMapRowMapper());
            }

            Map<String, Object> inParams = new HashMap<>();
            inParams.put("in_ids",            params.getIds());
            inParams.put("in_employee_ids",   params.getEmployeeIds());
            inParams.put("in_emails",         params.getEmails());
            inParams.put("in_setup_type",     params.getSetupType()!=null?params.getSetupType().getValue():null);
            inParams.put("in_description",    params.getDescription());
            inParams.put("in_faculty_id",     params.getFacultyId());
            inParams.put("in_department_id",  params.getDepartmentId());
            inParams.put("in_designation_id", params.getDesignationId());
            inParams.put("in_category_id",    params.getCategoryId());
            inParams.put("in_type_id",        params.getTypeId());
            inParams.put("in_page_number",    params.getPageNumber() != null ? params.getPageNumber() : 1);
            inParams.put("in_page_size",      params.getPageSize() != null ? params.getPageSize() : 10);
            inParams.put("in_user",           (params.getUser() != null && !params.getUser().isBlank()) ? params.getUser() : "System");
            inParams.put("in_operation",      operation);

            Map<String, Object> result = jdbcCall.execute(inParams);

            int code = result.get("out_message_code") != null ? Integer.parseInt(result.get("out_message_code").toString()) : 0;
            String message = (String) result.get("out_message_description");
            Object outId = result.get("out_id");

            if (code > 0) {
                return ApiDTO.builder()
                        .status(false)
                        .message(message)
                        .data(outId)
                        .build();
            }

            if (isSearch) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("#result-set-1");
                Integer totalItems = 0;
                Double totalPages = 0d;
                if (!rows.isEmpty()) {
                    Map<String, Object> firstRow = rows.get(0);
                    if (firstRow.get("total_records") != null) {
                        totalItems = (Integer) firstRow.get("total_records");
                    }
                    if (firstRow.get("totalPages") != null) {
                        totalPages = (Double) firstRow.get("totalPages");
                    }
                }

                return ApiDTO.builder()
                        .status(true)
                        .message(message)
                        .totalItems(totalItems)
                        .totalPages(totalPages.intValue())
                        .pageNumber((Integer) inParams.get("in_page_number"))
                        .pageSize((Integer) inParams.get("in_page_size"))
                        .data(rows)
                        .build();
            }

            return ApiDTO.builder()
                    .status(true)
                    .message(message)
                    .data(outId)
                    .build();

        } catch (Exception e) {
            log.error("Error executing SP_UM_HR_ESR_Email_Reporting_Setup_Save for operation {}", operation, e);
            return ApiDTO.builder()
                    .status(false)
                    .message(e.getMessage())
                    .build();
        }
    }


}
