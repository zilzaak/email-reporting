package app.repository;

import app.dto.metadata.MetadataSearchResultProjection;
import app.dto.metadata.NotRepliedReciversDTO;
import app.entity.EmailMetadata;
import app.enums.ReportingSetupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface EmailMetadataRepository extends JpaRepository<EmailMetadata,Long> {
    @Query("select new app.dto.metadata.NotRepliedReciversDTO(em.id  ," +
            "es.rootThreadId," +
            "em.receiverEmail, " +
            "es.senderEmail )" +
            " from EmailSubject  es " +
            " inner join EmailMetadata  em on em.emailSubject.id=es.id" +
            " where em.status=false  " +
            " and em.isCountable=true " +
            " and ( :fromDate is null or cast(es.deliveredDate as date) >=  cast(:fromDate as date) ) " +
            " and ( :toDate is null or cast(es.deliveredDate as date) <=  cast(:toDate as date) )  " +
            " and em.receiverEmail not in (" +
            " select email from EmailReportingSetup  where setupType=:setupType)" +
            " order by es.deliveredDate asc ")
    List<NotRepliedReciversDTO> unrepliedThreadList(@Param("fromDate") LocalDate fromDate ,
                                                    @Param("toDate") LocalDate toDate,
                                                    @Param("setupType") ReportingSetupType setupType);
    @Query(value =
            ";with PaginatedResults as (select em.id                       as id, " +
                    "       es.sender_email                                as senderEmail, " +
                    "       es.id                                          as subjectId, " +
                    "       es.sender_employee_id                          as senderEmployeeId, " +
                    "       em.employee_id                                 as employeeId, " +
                    "       convert(varchar(23), es.delivered_date, 121)   as deliveredDate, " +
                    "       es.subject                                     as subject, " +
                    "       es.cc_mails                                    as cc, " +
                    "       em.receiver_email                              as receiverEmail, " +
                    "       es.root_thread_id                              as rootThreadId, " +
                    "       es.root_message_id                             as rootMessageId, " +
                    "       em.reply_thread_id                             as replyThreadId, " +
                    "       em.reply_message_id                            as replyMessageId, " +
                    "       convert(varchar(23), em.reply_date, 121)       as replyDate, " +
                    "       em.response_delay_hour                         as replyDelayHour, " +
                    "       em.is_countable                                as isCountable, " +
                    "       em.status                                      as status , " +
                    "       ROW_NUMBER() OVER(ORDER BY es.root_thread_id) as rn , " +
                    "       COUNT(*) OVER() as totalRecords " +
                    "       from UM_HR_ESR_Email_Subject es  " +
                    "       INNER JOIN UM_HR_ESR_Email_Metadata em  on es.id=em.email_subject_id " +
                    "       where ( :employeeOrMailBox is null or coalesce(em.receiver_email,'-') = :employeeOrMailBox or coalesce(em.employee_id,'-') = :employeeOrMailBox  ) " +
                    "        and  ( :fromDate is null or cast(es.delivered_date as date) >= cast(:fromDate as date) )" +
                    "        and  ( :toDate is null or cast(es.delivered_date as date) <= cast(:toDate as date) )" +
                    " ) select * FROM PaginatedResults where rn between :startRow and :endRow  ",
            nativeQuery = true)
    List<MetadataSearchResultProjection> getMailMetaDataList(
            @Param("employeeOrMailBox") String employeeOrMailBox,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("startRow") Integer startRow,
            @Param("endRow") Integer endRow
            );
}
