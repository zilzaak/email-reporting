CREATE PROCEDURE [dbo].[SP_UM_HR_ESR_Email_Metadata_Save]
    @in_id NVARCHAR(MAX) = NULL,
    @in_subject NVARCHAR(500) = NULL,
    @in_sender_employee_id VARCHAR(150) = NULL,
    @in_sender_email varchar(150) = NULL,
    @in_receiver_email NVARCHAR(MAX) = NULL,
    @in_cc_emails  NVARCHAR(500) = NULL,
    @in_delivered_date VARCHAR(55) = NULL,
    @in_reply_date NVARCHAR(MAX) = NULL,
    @in_reply_delay_hour NVARCHAR(MAX) = NULL,
    @in_root_thread_id VARCHAR(255) = NULL,
    @in_root_message_id VARCHAR(255) = NULL,
    @in_reply_thread_id NVARCHAR(MAX) = NULL,
    @in_reply_message_id NVARCHAR(MAX) = NULL,
    @in_is_countable  BIT = NULL,
    @in_user varchar(100),
    @in_operation varchar(4),
    @out_id BIGINT OUTPUT,
    @out_message_code INT OUTPUT,
    @out_message_description varchar(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

BEGIN TRY
BEGIN TRANSACTION;

        IF @in_operation = 'I'
BEGIN

            IF EXISTS(select 1 from UM_HR_ESR_Email_Subject where root_thread_id=@in_root_thread_id)
BEGIN
                 SET @out_id = NULL;
                 SET @out_message_code = 1;
                 SET @out_message_description ='Email metadata records under root thread id '+@in_root_thread_id+' already exist ';
END
ELSE
BEGIN
                DECLARE @new_id bigint=null;
insert into UM_HR_ESR_Email_Subject( sender_employee_id,
                                     subject,
                                     sender_email,
                                     cc_mails,
                                     delivered_date,
                                     root_thread_id,
                                     root_message_id,
                                     created_at,
                                     created_by,
                                     updated_by)
values(@in_sender_employee_id,
       @in_subject,
       @in_sender_email,
       @in_cc_emails,
       @in_delivered_date,
       @in_root_thread_id,
       @in_root_message_id,
       sysdatetime(),
       'system',
       'system');
select @new_id=scope_identity();


CREATE TABLE #TempEmailMetadata
(
    id INT IDENTITY(1,1),
    employee_id varchar(150),
    receiver_email NVARCHAR(150),
    reply_date DATETIME NULL,
    response_delay_hour  NUMERIC(10,2) NULL,
    reply_thread_id NVARCHAR(255) NULL,
    reply_message_id NVARCHAR(255) NULL
);

INSERT INTO #TempEmailMetadata
(   employee_id,
    receiver_email,
    reply_date,
    response_delay_hour,
    reply_thread_id,
    reply_message_id
)
SELECT
    ei.employee_id,
    case when ( receiver.value IS NULL OR receiver.value ='null' OR receiver.value='')                then null else LTRIM(RTRIM(receiver.value)) end ,
    case when (reply_date.value IS NULL OR reply_date.value ='null' OR reply_date.value='' )          then NULL else CAST(LTRIM(RTRIM(reply_date.value)) AS DATETIME ) end,
    case when (reply_delay.value IS NULL OR reply_delay.value ='null' OR reply_delay.value='')        then NULL else CAST(LTRIM(RTRIM(reply_delay.value)) AS NUMERIC(10,2)) end,
    case when (reply_thread.value IS NULL OR reply_thread.value ='null' OR reply_thread.value='' )    then NULL else LTRIM(RTRIM(reply_thread.value)) end,
    case when (reply_message.value IS NULL OR reply_message.value ='null' OR reply_message.value='')  then NULL else LTRIM(RTRIM(reply_message.value)) end
FROM
    (
        SELECT
            value, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS id
        FROM dbo.fn_SplitString(@in_receiver_email, ',')
    ) receiver

        LEFT JOIN UM_HR_Employee_Info ei on ei.email=receiver.value

        LEFT JOIN
    (
        SELECT value, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS id
        FROM dbo.fn_SplitString(@in_reply_date, ',')
    ) reply_date
    ON receiver.id = reply_date.id

        LEFT JOIN
    (
        SELECT
            value,
            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS id
        FROM dbo.fn_SplitString(@in_reply_delay_hour, ',')
    ) reply_delay
    ON receiver.id = reply_delay.id
        LEFT JOIN
    (
        SELECT
            value,
            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS id
        FROM dbo.fn_SplitString(@in_reply_thread_id, ',')
    ) reply_thread
    ON receiver.id = reply_thread.id

        LEFT JOIN
    (
        SELECT
            value,
            ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS id
        FROM dbo.fn_SplitString(@in_reply_message_id, ',')
    ) reply_message
    ON receiver.id = reply_message.id;


INSERT INTO dbo.UM_HR_ESR_Email_Metadata
(
    employee_id,
    receiver_email,
    reply_date,
    response_delay_hour,
    reply_thread_id,
    reply_message_id,
    status,
    email_subject_id,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT   t.employee_id,
         t.receiver_email,
         t.reply_date,
         t.response_delay_hour,
         t.reply_thread_id,
         t.reply_message_id,
         case when t.reply_message_id is not null then 1 else 0 end,
         @new_id,
         SYSDATETIME(),
         SYSDATETIME(),
         @in_user,
         @in_user
FROM #TempEmailMetadata t
where t.receiver_email not in (
    select email from UM_HR_ESR_Email_Reporting_Setup where setup_type=2
)

    SET @out_id = NULL;
SET @out_message_code = 0;
            SET @out_message_description ='Email metadata records created successfully.';
DROP TABLE #TempEmailMetadata;
END
END


ELSE IF @in_operation = 'U' --- reply date update
BEGIN
                            ;WITH ids AS (
    SELECT
        LTRIM(RTRIM(value))                                AS raw_id,
        ROW_NUMBER() OVER (ORDER BY (SELECT NULL))         AS rn
    FROM dbo.fn_SplitString(@in_id, ',')
),
                                  reply_dates AS (
                                      SELECT
                                          NULLIF(LTRIM(RTRIM(value)), '')                    AS raw,
                                          ROW_NUMBER() OVER (ORDER BY (SELECT NULL))         AS rn
                                      FROM dbo.fn_SplitString(@in_reply_date, ',')
                                  ),
                                  delays AS (
                                      SELECT
                                          NULLIF(LTRIM(RTRIM(value)), '')                    AS raw,
                                          ROW_NUMBER() OVER (ORDER BY (SELECT NULL))         AS rn
                                      FROM dbo.fn_SplitString(@in_reply_delay_hour, ',')
                                  ),
                                  reply_threads AS (
                                      SELECT
                                          NULLIF(LTRIM(RTRIM(value)), '')                    AS raw,
                                          ROW_NUMBER() OVER (ORDER BY (SELECT NULL))         AS rn
                                      FROM dbo.fn_SplitString(@in_reply_thread_id, ',')
                                  ),
                                  reply_msgs AS (
                                      SELECT
                                          NULLIF(LTRIM(RTRIM(value)), '')                    AS raw,
                                          ROW_NUMBER() OVER (ORDER BY (SELECT NULL))         AS rn
                                      FROM dbo.fn_SplitString(@in_reply_message_id, ',')
                                  ),
                                  merged AS (
                                      SELECT
                                          CAST(i.raw_id AS BIGINT)                                              AS id,
                                          CAST(rd.raw AS DATETIME)                                              AS reply_date,
                                          CAST(d.raw  AS NUMERIC(10,2))                                         AS delay_hour,
                                          rt.raw                                                                AS reply_thread_id,
                                          rm.raw                                                                AS reply_message_id
                                      FROM ids i
                                               LEFT JOIN reply_dates   rd ON rd.rn = i.rn
                                               LEFT JOIN delays        d  ON d.rn  = i.rn
                                               LEFT JOIN reply_threads rt ON rt.rn = i.rn
                                               LEFT JOIN reply_msgs    rm ON rm.rn = i.rn
                                  )
UPDATE e
SET
    reply_date          = m.reply_date,
    response_delay_hour = m.delay_hour,
    reply_thread_id     = m.reply_thread_id,
    reply_message_id    = m.reply_message_id,
    updated_at          = SYSDATETIME(),
    updated_by          = @in_user,
    status              = 1
    FROM dbo.UM_HR_ESR_Email_Metadata e
                            INNER JOIN merged m ON m.id = e.id
    SET @out_id = NULL;
SET @out_message_code = 0;
                            SET @out_message_description = CAST(@@ROWCOUNT AS NVARCHAR(20))+ ' email metadata records updated.';
END


ELSE IF @in_operation = 'UC' -- UC  = UPDATE COUNTABILITY
BEGIN
                            ;WITH ids AS (
    SELECT
        cast(LTRIM(RTRIM(value)) as bigint)        AS id,
        ROW_NUMBER() OVER (ORDER BY (SELECT NULL))  AS rn
    FROM dbo.fn_SplitString(@in_id, ',')
)  UPDATE e
                             SET is_countable       = @in_is_countable
                             FROM dbo.UM_HR_ESR_Email_Metadata e
                                 INNER JOIN ids m ON m.id = e.id
                                 SET @out_id = NULL;
SET @out_message_code = 0;
                            SET @out_message_description = CAST(@@ROWCOUNT AS NVARCHAR(20))+ ' email metadata records updated.';
END



ELSE IF @in_operation = 'D'
BEGIN
DELETE FROM dbo.UM_HR_ESR_Email_Metadata
WHERE id = @in_id;

SET @out_id = @in_id;
            SET @out_message_code = 0;
            SET @out_message_description =
                'Email metadata deleted successfully.';
END
ELSE
BEGIN
            RAISERROR( 'Invalid operation. Supported operations are I, U and D.', 16, 1 );
END;
COMMIT TRANSACTION;
END TRY
BEGIN CATCH
IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;
        SET @out_id = ISNULL(@in_id, 0);
        SET @out_message_code = 1;
        SET @out_message_description = ERROR_MESSAGE();
END CATCH;
END
go

