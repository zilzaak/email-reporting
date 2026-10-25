CREATE PROCEDURE [dbo].[SP_UM_HR_ESR_Email_Reporting_Setup_Save]
    @in_ids                    NVARCHAR(MAX) = NULL,
    @in_employee_ids           NVARCHAR(MAX) = NULL,
    @in_emails                 NVARCHAR(MAX) = NULL,
    @in_setup_type             BIGINT        = NULL,
    @in_description            VARCHAR(300) = NULL,
    @in_faculty_id             BIGINT = NULL,
    @in_department_id          BIGINT = NULL,
    @in_designation_id         BIGINT = NULL,
    @in_category_id            BIGINT = NULL,
    @in_type_id                BIGINT = NULL,
    @in_from_date              DATE = NULL,
    @in_to_date                DATE = NULL,
    @in_page_number            INT = NULL,
    @in_page_size              INT = NULL,
    @in_user                   VARCHAR(50)  = 'System',
    @in_operation              VARCHAR(4),
    @out_id                    BIGINT        OUTPUT,
    @out_message_code          INT           OUTPUT,
    @out_message_description   VARCHAR(255)  OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @today   DATETIME= SYSDATETIME();
        IF @in_operation = 'I'
        BEGIN
            IF @in_setup_type=1 AND ( @in_employee_ids IS NULL OR LTRIM(RTRIM(@in_employee_ids)) = '')
            BEGIN
                SET @out_id = NULL;
                SET @out_message_code = 1;
                SET @out_message_description = 'Employee ID(s) cannot be empty.';
                ROLLBACK TRANSACTION;
                RETURN;
            END

            IF @in_setup_type IS NULL OR @in_setup_type NOT IN(1,2)
            BEGIN
                SET @out_id = NULL;
                SET @out_message_code = 1;
                SET @out_message_description = 'Setup type is required.';
                ROLLBACK TRANSACTION;
                RETURN;
            END

            -- Parse bulk employee IDs into a temporary table
            CREATE TABLE #TempInputEmpIds
            (
                employee_id VARCHAR(50),
                email VARCHAR(100)
            );

            IF @in_setup_type=1
                BEGIN
                    INSERT INTO #TempInputEmpIds (employee_id,email)
                    SELECT  LTRIM(RTRIM(emp.value)),LTRIM(RTRIM(mail.value))
                    FROM (select value , ROW_NUMBER() over (ORDER BY(select null) ) as id  from dbo.fn_SplitString(@in_employee_ids, ','))
                        emp
                    inner join ( select value , ROW_NUMBER() over (ORDER BY(select null) ) as id  from dbo.fn_SplitString(@in_emails,',')
                     ) mail on mail.id=emp.id
                    WHERE NULLIF(LTRIM(RTRIM(emp.value)), '') IS NOT NULL
                      and not exists( select 1 from UM_HR_ESR_Email_Reporting_Setup
                                      where employee_id= emp.value and setup_type=@in_setup_type);
                END
                
            IF @in_setup_type=2
                BEGIN
                    INSERT INTO #TempInputEmpIds (email)
                    SELECT  LTRIM(RTRIM(mail.value))
                    FROM ( select value , ROW_NUMBER() over (ORDER BY(select null) ) as id  from dbo.fn_SplitString(@in_emails,',')
                     ) mail 
                    WHERE NULLIF(LTRIM(RTRIM(mail.value)), '') IS NOT NULL
                      and not exists( select 1 from UM_HR_ESR_Email_Reporting_Setup
                                      where  setup_type=@in_setup_type and email=mail.value);
                END


            -- Insert bulk employee records
            INSERT INTO dbo.UM_HR_ESR_Email_Reporting_Setup
            (
                employee_id,
                email,
                setup_type,
                description,
                created_at,
                updated_at,
                created_by,
                updated_by
            )
            SELECT
                employee_id,
                email,
                @in_setup_type,
                @in_description,
                SYSDATETIME(),
                SYSDATETIME(),
                ISNULL(@in_user, 'System'),
                ISNULL(@in_user, 'System')
            FROM #TempInputEmpIds;
            SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description = CAST(@@ROWCOUNT AS NVARCHAR(20)) + ' items created';
            DROP TABLE #TempInputEmpIds;
        END

        -- =====================================================================
        -- 2. UPDATE OPERATION (Update records by multiple IDs)
        -- =====================================================================
        ELSE IF @in_operation = 'U'
        BEGIN
            IF @in_ids IS NULL OR LTRIM(RTRIM(@in_ids)) = ''
               OR @in_emails IS NULL OR LTRIM(RTRIM(@in_emails)) = ''
               OR (@in_setup_type=1 and @in_employee_ids IS NULL OR LTRIM(RTRIM(@in_employee_ids)) = '')
               OR @in_setup_type NOT IN (1, 2)
            BEGIN
                SET @out_id = NULL;
                SET @out_message_code = 1;
                SET @out_message_description =
                    'ID(s), employee ids, emails, setup type are required for update operation.';
                ROLLBACK TRANSACTION;
                RETURN;
            END
        
            CREATE TABLE #TempUpdateIds
            (   id       BIGINT,
                emp_email    VARCHAR(100),
                employee VARCHAR(50)
            );
        
            IF @in_setup_type=1
                BEGIN
                    INSERT INTO #TempUpdateIds (id, employee, emp_email)
                    SELECT
                        idlist.value,
                        employeelist.value,
                        emaillist.value
                    FROM
                    (
                        SELECT
                            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn,
                            CAST(LTRIM(RTRIM(value)) AS BIGINT) AS value
                        FROM dbo.fn_SplitString(@in_ids, ',')
                        WHERE NULLIF(LTRIM(RTRIM(value)), '') IS NOT NULL
                    ) idlist
                    INNER JOIN
                    (
                        SELECT
                            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn,
                            CAST(LTRIM(RTRIM(value)) AS VARCHAR(50)) AS value
                        FROM dbo.fn_SplitString(@in_employee_ids, ',')
                        WHERE NULLIF(LTRIM(RTRIM(value)), '') IS NOT NULL
                    ) employeelist
                        ON idlist.rn = employeelist.rn
                    INNER JOIN
                    (
                        SELECT
                            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn,
                            CAST(LTRIM(RTRIM(value)) AS VARCHAR(100)) AS value
                        FROM dbo.fn_SplitString(@in_emails, ',')
                        WHERE NULLIF(LTRIM(RTRIM(value)), '') IS NOT NULL
                    ) emaillist
                        ON emaillist.rn = idlist.rn;
                END
                
                
            IF @in_setup_type=2
                BEGIN
                    INSERT INTO #TempUpdateIds (id, emp_email)
                    SELECT
                        idlist.value,
                        emaillist.value
                    FROM
                    (
                        SELECT
                            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn,
                            CAST(LTRIM(RTRIM(value)) AS BIGINT) AS value
                        FROM dbo.fn_SplitString(@in_ids, ',')
                        WHERE NULLIF(LTRIM(RTRIM(value)), '') IS NOT NULL
                    ) idlist
                    INNER JOIN
                    (
                        SELECT
                            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn,
                            CAST(LTRIM(RTRIM(value)) AS VARCHAR(100)) AS value
                        FROM dbo.fn_SplitString(@in_emails, ',')
                        WHERE NULLIF(LTRIM(RTRIM(value)), '') IS NOT NULL
                    ) emaillist
                        ON emaillist.rn = idlist.rn;
                END


            DECLARE 
                @emailvalue VARCHAR(100),
                @employeevalue VARCHAR(50), 
                @idvalue BIGINT,@rows_updated INT;
            
            WHILE 0 < (SELECT count(*) FROM #TempUpdateIds) 
            BEGIN
                SELECT TOP 1 @emailvalue = emp_email, @employeevalue = employee,  @idvalue = id FROM #TempUpdateIds ORDER BY id;
                IF @in_setup_type=1 
                       BEGIN 
                                IF NOT EXISTS( SELECT 1 FROM dbo.UM_HR_ESR_Email_Reporting_Setup WHERE  email=@emailvalue AND employee_id = @employeevalue AND setup_type = 1 AND id <> @idvalue )
                                    BEGIN
                                        UPDATE dbo.UM_HR_ESR_Email_Reporting_Setup
                                        SET email = @emailvalue, employee_id = @employeevalue, setup_type = @in_setup_type,updated_at = SYSDATETIME(),updated_by = @in_user WHERE id = @idvalue;
                                        SET @rows_updated = @@ROWCOUNT;
                                    END
                       END
                IF @in_setup_type=2
                       BEGIN 
                                IF NOT EXISTS( SELECT 1 FROM dbo.UM_HR_ESR_Email_Reporting_Setup WHERE  email=@emailvalue  AND setup_type = 2 AND id <> @idvalue )
                                    BEGIN
                                        UPDATE dbo.UM_HR_ESR_Email_Reporting_Setup
                                        SET email = @emailvalue, setup_type = @in_setup_type,updated_at = SYSDATETIME(),updated_by = @in_user WHERE id = @idvalue;
                                        SET @rows_updated = @@ROWCOUNT;
                                    END
                       END

                DELETE FROM #TempUpdateIds WHERE id = @idvalue;      
            END
           
            DROP TABLE #TempUpdateIds;
        
            SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description =cast(@rows_updated as varchar(20))+' items updated ';
        END
        -- =====================================================================
        -- 3. search operation to fetch employee from UM_HR_EMPLOYEE INFO  table
        -- =====================================================================
        ELSE IF @in_operation = 'EMS' --MAIL BOX EXCLUDE SEARCH
        BEGIN
            DECLARE @start_index INT= (@in_page_number-1)*@in_page_size+1;
            DECLARE @end_index   INT= @in_page_number*@in_page_size;
            ;WITH FilteredEmployees as(
                select ei.employee_id as employeeId,
                       ei.full_name as fullName,
                       ei.email as email ,
                       stp.email as setupedEmail,
                       dg.name as designation ,
                       fac.name as faculty ,
                       dp.name as department ,
                       dp.id as departmentId ,
                       stp.setup_type as setup,
                       et.name as type,
                       et.id as typeId,
                       ec.name as category ,
                       ec.id as categoryId ,
                       ROW_NUMBER() over (ORDER BY ei.employee_id) as rn,
                       count(*) over () as total_records,
                       CASE 
                           when stp.setup_type IS NOT NULL then 'EXCLUDE_MAIL_BOX'
                           else 'NOT_EXIST' end as setupType,
                       stp.id as id,
                       stp.created_at as createdAt
                from dbo.UM_HR_Employee_Info ei
                left join UM_HR_ESR_Email_Reporting_Setup stp on stp.employee_id=ei.employee_id and stp.setup_type=1
                left join UM_AS_Departments dp on dp.id=ei.department_id
                left join dbo.UM_AS_Faculties fac on fac.id=ei.faculty_id
                left join dbo.UM_HR_Employee_Categories ec on ec.id=ei.employee_category_id
                left join dbo.UM_HR_Employee_Types et on et.id=ei.employee_type_id
                left join dbo.UM_HR_Designations dg on dg.id=ei.designation_id
                where ei.active=1 AND 
                    (@in_setup_type IS NULL OR stp.setup_type=@in_setup_type)
                    and ( @in_faculty_id is null or fac.id=@in_faculty_id)
                    and ( @in_department_id is null or dp.id=@in_department_id  )
                    and ( @in_designation_id is null or dg.id=@in_designation_id  )
                    and ( @in_category_id is null or ec.id=@in_category_id)
                    and ( @in_type_id is null or et.id=@in_type_id)
                    and ( @in_emails is null or ei.email in (select cast(LTRIM(RTRIM(value)) as varchar(100)) from  dbo.fn_SplitString(@in_emails,',')))
                    and ( @in_employee_ids is null or ei.employee_id in
                     ( select cast(LTRIM(RTRIM(value)) as varchar(50)) from dbo.fn_SplitString(@in_employee_ids,',')))
            )
             SELECT * ,CEILING(CAST(total_records AS FLOAT) / @in_page_size) AS totalPages
              from FilteredEmployees where rn between @start_index and @end_index;

            SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description = 'Successfully filtered employee information';
        END



        ELSE IF @in_operation = 'RCS' --REPLY COUNT SEARCH
        BEGIN
            DECLARE @start_index2 INT= (@in_page_number-1)*@in_page_size+1;
            DECLARE @end_index2   INT= @in_page_number*@in_page_size;
            ;WITH FilteredEmployees as(
                select ei.employee_id as employeeId,
                       ei.full_name as fullName,
                       ei.email as email ,
                       stp.email as setupedEmail,
                       dg.name as designation ,
                       fac.name as faculty ,
                       dp.name as department ,
                       dp.id as departmentId ,
                       stp.setup_type as setup,
                       et.name as type,
                       et.id as typeId,
                       ec.name as category ,
                       ec.id as categoryId ,
                       ROW_NUMBER() over (ORDER BY ei.employee_id) as rn,
                       count(*) over () as total_records,
                       CASE 
                           when stp.setup_type IS NOT NULL then 'EXCLUDE_REPLY_COUNT'
                           else 'NOT_EXIST' end as setupType,
                       stp.id as id,
                       stp.created_at as createdAt
                from dbo.UM_HR_Employee_Info ei
                left join UM_HR_ESR_Email_Reporting_Setup stp on stp.employee_id=ei.employee_id and stp.setup_type=2
                left join UM_AS_Departments dp on dp.id=ei.department_id
                left join dbo.UM_AS_Faculties fac on fac.id=ei.faculty_id
                left join dbo.UM_HR_Employee_Categories ec on ec.id=ei.employee_category_id
                left join dbo.UM_HR_Employee_Types et on et.id=ei.employee_type_id
                left join dbo.UM_HR_Designations dg on dg.id=ei.designation_id
                where ei.active=1 AND 
                    (@in_setup_type IS NULL OR stp.setup_type=@in_setup_type)
                    and ( @in_faculty_id is null or fac.id=@in_faculty_id)
                    and ( @in_department_id is null or dp.id=@in_department_id  )
                    and ( @in_designation_id is null or dg.id=@in_designation_id  )
                    and ( @in_category_id is null or ec.id=@in_category_id)
                    and ( @in_type_id is null or et.id=@in_type_id)
                    and ( @in_emails is null or ei.email in (select cast(LTRIM(RTRIM(value)) as varchar(100)) from  dbo.fn_SplitString(@in_emails,',')))
                    and ( @in_employee_ids is null or ei.employee_id in
                     ( select cast(LTRIM(RTRIM(value)) as varchar(50)) from dbo.fn_SplitString(@in_employee_ids,',')))
            )
             SELECT * ,CEILING(CAST(total_records AS FLOAT) / @in_page_size) AS totalPages
              from FilteredEmployees where rn between @start_index2 and @end_index2;

            SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description = 'Successfully filtered employee information';
        END

        -- =====================================================================
        -- 3. search operation to fetch employee from UM_HR_ESR_Email_Reporting_Setup  table
        -- =====================================================================

      ELSE IF @in_operation = 'SL' --SETUP LIST
        BEGIN
            DECLARE @start_index3 INT= (@in_page_number-1)*@in_page_size+1;
            DECLARE @end_index3   INT= @in_page_number*@in_page_size;
            IF @in_setup_type=1
            BEGIN 
            ;WITH FilteredEmployees as(
                select ers.id as id ,
                       ers.created_at as createdAt,
                       ers.setup_type as setupType,
                       ers.employee_id ,
                       ei.full_name,
                       ers.email ,
                       dg.name as designation ,
                       fac.name as faculty ,
                       dp.name as department ,
                       et.name as type,
                       ec.name as category ,
                       ROW_NUMBER() over (ORDER BY ei.employee_id) as rn,
                       count(*) over () as total_records
                from UM_HR_ESR_Email_Reporting_Setup ers
                left join dbo.UM_HR_Employee_Info ei on ei.employee_id=ers.employee_id
                left join UM_AS_Departments dp on dp.id=ei.department_id
                left join dbo.UM_AS_Faculties fac on fac.id=ei.faculty_id
                left join dbo.UM_HR_Employee_Categories ec on ec.id=ei.employee_category_id
                left join dbo.UM_HR_Employee_Types et on et.id=ei.employee_type_id
                left join dbo.UM_HR_Designations dg on dg.id=ei.designation_id
                    where ( @in_faculty_id is null or fac.id=@in_faculty_id)
                    and ( @in_department_id is null or dp.id=@in_department_id  )
                    and ( @in_designation_id is null or dg.id=@in_designation_id  )
                    and ( @in_category_id is null or ec.id=@in_category_id)
                    and ( @in_type_id is null or et.id=@in_type_id)
                    and ( @in_emails is null or ers.email in (select cast(LTRIM(RTRIM(value)) as varchar(100)) from  dbo.fn_SplitString(@in_emails,',')))
                    and ers.setup_type=1
                    and ( @in_employee_ids is null or ei.employee_id in
                     ( select cast(LTRIM(RTRIM(value)) as varchar(50)) from dbo.fn_SplitString(@in_employee_ids,',')))
            )
             SELECT * ,CEILING(CAST(total_records AS FLOAT) / @in_page_size) AS totalPages
              from FilteredEmployees where rn between @start_index3 and @end_index3;
            END
            
            IF @in_setup_type=2
            BEGIN 
            ;WITH FilteredEmployees as(
                select ers.id as id ,
                       ers.created_at as createdAt,
                       ers.setup_type as setupType,
                       ers.employee_id ,
                       ers.email ,
                       ROW_NUMBER() over (ORDER BY ers.id) as rn,
                       count(*) over () as total_records
                from UM_HR_ESR_Email_Reporting_Setup ers
                    where 
                     ( @in_emails is null or ers.email in (select cast(LTRIM(RTRIM(value)) as varchar(100)) from  dbo.fn_SplitString(@in_emails,',')))
                     and ers.setup_type=2
            )
             SELECT * ,CEILING(CAST(total_records AS FLOAT) / @in_page_size) AS totalPages
              from FilteredEmployees where rn between @start_index3 and @end_index3;
            END

            SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description = 'Successfully filtered employee information';
        END


        ELSE IF @in_operation = 'SPR' -- STRIKE POINT REPORT BY SENDER & ORG INPUT PARAMS
        BEGIN
            DECLARE @start_index4 INT = (@in_page_number - 1) * @in_page_size + 1;
            DECLARE @end_index4   INT = @in_page_number * @in_page_size;

            ;WITH FilteredEmployees AS (
                SELECT
                    COUNT(DISTINCT es.id)          AS totalStrikePoint,
                    em.employee_id                 AS recieverEmployeeId,
                    ei.full_name                   AS recieverEmployeeName,
                    em.receiver_email              AS recieverEmail,
                    dg.name                        AS recieverDesignation,
                    dp.name                        AS recieverDepartment,
                    ROW_NUMBER() OVER (ORDER BY em.employee_id) AS rn,
                    COUNT(*) OVER ()               AS total_records
                FROM UM_HR_ESR_Email_Metadata em
                INNER JOIN UM_HR_ESR_Email_Subject es
                    ON es.id = em.email_subject_id
                INNER JOIN dbo.UM_HR_Employee_Info ei
                    ON ei.employee_id = em.employee_id
                LEFT JOIN UM_AS_Departments dp
                    ON dp.id = ei.department_id
                LEFT JOIN dbo.UM_AS_Faculties fac
                    ON fac.id = ei.faculty_id
                LEFT JOIN dbo.UM_HR_Employee_Categories ec
                    ON ec.id = ei.employee_category_id
                LEFT JOIN dbo.UM_HR_Employee_Types et
                    ON et.id = ei.employee_type_id
                LEFT JOIN dbo.UM_HR_Designations dg
                    ON dg.id = ei.designation_id
                WHERE
                        (@in_faculty_id     IS NULL OR fac.id = @in_faculty_id)
                    AND (@in_department_id  IS NULL OR dp.id  = @in_department_id)
                    AND (@in_designation_id IS NULL OR dg.id  = @in_designation_id)
                    AND (@in_category_id    IS NULL OR ec.id  = @in_category_id)
                    AND (@in_type_id        IS NULL OR et.id  = @in_type_id)
                    AND (@in_emails         IS NULL OR es.sender_email = @in_emails)
                    AND (@in_employee_ids   IS NULL OR es.sender_employee_id = @in_employee_ids)
                    AND (@in_from_date      IS NULL OR CAST(es.delivered_date AS DATE) >= @in_from_date)
                    AND (@in_to_date        IS NULL OR CAST(es.delivered_date AS DATE) <= @in_to_date)
                    AND (
                            (em.reply_date IS NULL     AND DATEDIFF(HOUR, es.delivered_date, @today)        > 8)
                         OR (em.reply_date IS NOT NULL AND DATEDIFF(HOUR, es.delivered_date, em.reply_date) > 8)
                        )
                GROUP BY em.employee_id, ei.full_name, em.receiver_email, dg.name, dp.name
            )
            SELECT *,
                   CEILING(CAST(total_records AS FLOAT) / @in_page_size) AS totalPages
            FROM FilteredEmployees
            WHERE rn BETWEEN @start_index4 AND @end_index4;

            SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description = 'Successfully filtered employee information';
        END


        ELSE IF @in_operation = 'RD' -- REPORT DETAILS BY RECIEVER INFO
        BEGIN
            DECLARE @start_index5 INT = (@in_page_number - 1) * @in_page_size + 1;
            DECLARE @end_index5   INT = @in_page_number * @in_page_size;

            ;WITH FilteredEmployees AS (
                SELECT
                       em.id as id,
                       es.id as subjectId,
                       es.cc_mails as cc,
                       es.subject as emailSubject,
                       es.sender_email as senderEmail,
                       es.sender_employee_id as senderEmployeeId,
                       em.employee_id as recieverEmployeeId,
                       ei.full_name as recieverEmployeeName,
                       em.receiver_email as recieverEmail,
                       dg.name as recieverDesignation ,
                       fac.name as recieverFaculty ,
                       dp.name as recieverDepartment ,
                       et.name as recieverEmployeeType,
                       ec.name as recieverEmployeeCategory ,
                       es.created_at as createdAt,
                       es.delivered_date as deliveredDate,
                       em.reply_date as replyDate,
                       em.response_delay_hour as replyDelayHour ,
                    ROW_NUMBER() OVER (ORDER BY em.employee_id) AS rn,
                    COUNT(*) OVER ()               AS total_records
                FROM UM_HR_ESR_Email_Metadata em
                INNER JOIN UM_HR_ESR_Email_Subject es
                    ON es.id = em.email_subject_id
                INNER JOIN dbo.UM_HR_Employee_Info ei
                    ON ei.employee_id = em.employee_id
                LEFT JOIN UM_AS_Departments dp
                    ON dp.id = ei.department_id
                LEFT JOIN dbo.UM_AS_Faculties fac
                    ON fac.id = ei.faculty_id
                LEFT JOIN dbo.UM_HR_Employee_Categories ec
                    ON ec.id = ei.employee_category_id
                LEFT JOIN dbo.UM_HR_Employee_Types et
                    ON et.id = ei.employee_type_id
                LEFT JOIN dbo.UM_HR_Designations dg
                    ON dg.id = ei.designation_id
                WHERE
                        (@in_emails         IS NULL OR es.sender_email = @in_emails)
                    AND (@in_employee_ids   IS NULL OR em.employee_id = @in_employee_ids)
                    AND (@in_from_date      IS NULL OR CAST(es.delivered_date AS DATE) >= @in_from_date)
                    AND (@in_to_date        IS NULL OR CAST(es.delivered_date AS DATE) <= @in_to_date)
                    AND (
                            (em.reply_date IS NULL     AND DATEDIFF(HOUR, es.delivered_date, @today)        > 8)
                         OR (em.reply_date IS NOT NULL AND DATEDIFF(HOUR, es.delivered_date, em.reply_date) > 8)
                        )

            )
            SELECT *,
                   CEILING(CAST(total_records AS FLOAT) / @in_page_size) AS totalPages
            FROM FilteredEmployees
            WHERE rn BETWEEN @start_index5 AND @end_index5;

            SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description = 'Successfully filtered employee information';
        END


        ELSE IF @in_operation = 'D'
        BEGIN
            IF @in_ids IS NULL OR LTRIM(RTRIM(@in_ids)) = ''
            BEGIN
                SET @out_id = NULL;
                SET @out_message_code = 1;
                SET @out_message_description = 'ID(s) are required for delete operation.';
                ROLLBACK TRANSACTION;
                RETURN;
            END

            DELETE FROM dbo.UM_HR_ESR_Email_Reporting_Setup
            WHERE id  IN (
                SELECT cast(LTRIM(RTRIM(value)) as bigint)
                FROM dbo.fn_SplitString(@in_ids, ',')
                WHERE NULLIF(LTRIM(RTRIM(value)), '') IS NOT NULL
            );

            SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description = CAST(@@ROWCOUNT AS NVARCHAR(20)) + ' Reporting setup record(s) deleted successfully.';
        END

        ELSE
        BEGIN
            RAISERROR('Invalid operation. Supported operations are I, U and D.', 16, 1);
        END;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;
        SET @out_id = NULL;
        SET @out_message_code = 1;
        SET @out_message_description = ERROR_MESSAGE();
    END CATCH;
END
go

