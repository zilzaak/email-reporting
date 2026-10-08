package app.repository;

import app.dto.metadata.ApprovedMailBoxDTO;
import app.entity.EmailReportingSetup;
import app.enums.ReportingSetupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EmailReportingSetupRepository extends JpaRepository<EmailReportingSetup, Long> {

    @Query("select new app.dto.metadata.ApprovedMailBoxDTO(" +
            " ei.employeeId , ei.email ) from  " +
            " EmployeeInfo ei " +
            " where ei.active = true  " +
            " and ei.email is not null " +
            " and ei.empTypeId = :employeeTypeId " +
            " and ei.email not in (select email from EmailReportingSetup where setupType = :setupId)" )
    List<ApprovedMailBoxDTO> getApprovedMailBoxes(@Param("employeeTypeId") Long employeeTypeId,
                                                  @Param("setupId") ReportingSetupType setupId);
}
