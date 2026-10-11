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
    @in_page_number            INT = NULL,
    @in_page_size              INT = NULL,
    @in_user                   VARCHAR(150)  = 'System',
    @in_operation              VARCHAR(4),
    @out_id                    BIGINT        OUTPUT,
    @out_message_code          INT           OUTPUT,
    @out_message_description   VARCHAR(255)  OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

BEGIN TRY
BEGIN TRANSACTION;

        IF @in_operation = 'I'
BEGIN
            IF @in_employee_ids IS NULL OR LTRIM(RTRIM(@in_employee_ids)) = ''
BEGIN
                SET @out_id = NULL;
                SET @out_message_code = 1;
                SET @out_message_description = 'Employee ID(s) cannot be empty.';
ROLLBACK TRANSACTION;
RETURN;
END

            IF @in_setup_type IS NULL
BEGIN
                SET @out_id = NULL;
                SET @out_message_code = 1;
                SET @out_message_description = 'Condition type is required.';
ROLLBACK TRANSACTION;
RETURN;
END

            -- Parse bulk employee IDs into a temporary table
CREATE TABLE #TempInputEmpIds
(
    employee_id VARCHAR(150),
    email VARCHAR(150)
);

INSERT INTO #TempInputEmpIds (employee_id,email)
SELECT  LTRIM(RTRIM(emp.value)),LTRIM(RTRIM(mail.value))
FROM (select value , ROW_NUMBER() over (ORDER BY(select null) ) as id  from dbo.fn_SplitString(@in_employee_ids, ','))
         emp
         inner join ( select value , ROW_NUMBER() over (ORDER BY(select null) ) as id  from dbo.fn_SplitString(@in_emails,',')
) mail on mail.id=emp.id
WHERE NULLIF(LTRIM(RTRIM(emp.value)), '') IS NOT NULL
  and not exists( select 1 from UM_HR_ESR_Email_Reporting_Setup
                  where email=mail.value and employee_id= emp.value and setup_type=@in_setup_type);

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
            SET @out_message_description = CAST(@@ROWCOUNT AS NVARCHAR(20)) + ' reporting condition setup record(s) created successfully.';
DROP TABLE #TempInputEmpIds;
END

        -- =====================================================================
        -- 2. UPDATE OPERATION (Update records by multiple IDs)
        -- =====================================================================
ELSE IF @in_operation = 'U'
BEGIN
            IF @in_ids IS NULL OR LTRIM(RTRIM(@in_ids)) = ''
               OR @in_emails IS NULL OR LTRIM(RTRIM(@in_emails)) = ''
               OR @in_employee_ids IS NULL OR LTRIM(RTRIM(@in_employee_ids)) = ''
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
    email    VARCHAR(150),
    employee VARCHAR(150)
);

INSERT INTO #TempUpdateIds (id, employee, email)
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
            CAST(LTRIM(RTRIM(value)) AS VARCHAR(150)) AS value
        FROM dbo.fn_SplitString(@in_employee_ids, ',')
        WHERE NULLIF(LTRIM(RTRIM(value)), '') IS NOT NULL
    ) employeelist
    ON idlist.rn = employeelist.rn
        INNER JOIN
    (
        SELECT
            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn,
            CAST(LTRIM(RTRIM(value)) AS VARCHAR(150)) AS value
        FROM dbo.fn_SplitString(@in_emails, ',')
        WHERE NULLIF(LTRIM(RTRIM(value)), '') IS NOT NULL
    ) emaillist
    ON emaillist.rn = idlist.rn;

DECLARE
@emailvalue VARCHAR(150),
                @employeevalue VARCHAR(150),
                @idvalue BIGINT,@rows_updated INT;

            WHILE EXISTS (SELECT * FROM #TempUpdateIds)
BEGIN
SELECT TOP 1 @emailvalue = email, @employeevalue = employee,  @idvalue = id FROM #TempUpdateIds ORDER BY id;
IF NOT EXISTS( SELECT 1 FROM dbo.UM_HR_ESR_Email_Reporting_Setup WHERE employee_email = @emailvalue AND employee_id = @employeevalue AND setup_type = @in_setup_type AND id <> @idvalue )
BEGIN
UPDATE dbo.UM_HR_ESR_Email_Reporting_Setup
SET employee_email = @emailvalue, employee_id = @employeevalue, setup_type = @in_setup_type,updated_at = SYSDATETIME(),updated_by = ISNULL(@in_user, updated_by) WHERE id = @idvalue;
SET @rows_updated = @@ROWCOUNT;
END
DELETE FROM #TempUpdateIds WHERE id = @idvalue;
END

DROP TABLE #TempUpdateIds;

SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description ='Reporting setup record(s) updated successfully.';
END
        -- =====================================================================
        -- 3. search operation to fetch employee from UM_HR_EMPLOYEE INFO  table
        -- =====================================================================
ELSE IF @in_operation = 'S1'
BEGIN
            DECLARE @start_index INT= (@in_page_number-1)*@in_page_size+1;
            DECLARE @end_index   INT= @in_page_number*@in_page_size;
            ;WITH FilteredEmployees as(
    select ei.employee_id ,
           ei.full_name,
           ei.email ,
           dg.name as designation ,
           fac.name as faculty ,
           dp.name as department ,
           et.name as type,
           ec.name as category ,
           ROW_NUMBER() over (ORDER BY ei.employee_id) as rn,
        count(*) over () as total_records
    from dbo.UM_HR_Employee_Info ei
             left join UM_AS_Departments dp on dp.id=ei.department_id
             left join dbo.UM_AS_Faculties fac on fac.id=ei.faculty_id
             left join dbo.UM_HR_Employee_Categories ec on ec.id=ei.employee_category_id
             left join dbo.UM_HR_Employee_Types et on et.id=ei.employee_type_id
             left join dbo.UM_HR_Designations dg on dg.id=ei.designation_id
    where
        ( @in_faculty_id is null or fac.id=@in_faculty_id)
      and ( @in_department_id is null or dp.id=@in_department_id  )
      and ( @in_designation_id is null or dg.id=@in_designation_id  )
      and ( @in_category_id is null or ec.id=@in_category_id)
      and ( @in_type_id is null or et.id=@in_type_id)
      and ( @in_employee_ids is null or ei.employee_id in
                                        ( select cast(LTRIM(RTRIM(value)) as varchar(50)) from dbo.fn_SplitString(@in_employee_ids,',')))
)
             SELECT * ,CEILING(CAST(total_records AS FLOAT) / @in_page_size) AS totalPages
             from FilteredEmployees where rn between @start_index and @end_index;

SET @out_id = NULL;
            SET @out_message_code = 0;
            SET @out_message_description = 'Successfully filtered employee information';
END

        -- =====================================================================
        -- 3. search operation to fetch employee from UM_HR_ESR_Email_Reporting_Setup  table
        -- =====================================================================

ELSE IF @in_operation = 'S2'
BEGIN
            DECLARE @start_index2 INT= (@in_page_number-1)*@in_page_size+1;
            DECLARE @end_index2   INT= @in_page_number*@in_page_size;
            ;WITH FilteredEmployees as(
    select ers.id as id ,
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
             inner join dbo.UM_HR_Employee_Info ei on ei.employee_id=ers.employee_id
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
      and ( @in_emails is null or ers.email in (select cast(LTRIM(RTRIM(value)) as varchar(150)) from  dbo.fn_SplitString(@in_emails,',')))
      and ers.setup_type=@in_setup_type
      and ( @in_employee_ids is null or ei.employee_id in
                                        ( select cast(LTRIM(RTRIM(value)) as varchar(50)) from dbo.fn_SplitString(@in_employee_ids,',')))
)
             SELECT * ,CEILING(CAST(total_records AS FLOAT) / @in_page_size) AS totalPages
             from FilteredEmployees where rn between @start_index2 and @end_index2;

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
            SET @out_message_description = CAST(@@ROWCOUNT AS NVARCHAR(20)) + ' reporting condition setup record(s) deleted successfully.';
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

