package app.repository;

import app.dto.metadata.ApprovedMailBox;
import app.entity.EmailReportingSetup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EmailReportingSetupRepository extends JpaRepository<EmailReportingSetup, Long> {

    @Query(value = "select ei.employee_id as employeeId, ei.email as email from  " +
            " UM_HR_Employee_Info ei " +
            " where ei.active = 1  " +
            " and ei.email is not null " +
            " and ei.employee_type_id = :employeeTypeId " +
            " and ei.email not in (select email from UM_HR_ESR_Email_Reporting_Setup where setup_type = :setupId)",nativeQuery = true )
    List<ApprovedMailBox> getApprovedMailBoxes(@Param("employeeTypeId") Long employeeTypeId,
                                               @Param("setupId") Long setupId);
}
