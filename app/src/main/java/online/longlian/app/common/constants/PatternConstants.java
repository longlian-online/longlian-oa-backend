package online.longlian.app.common.constants;

public class PatternConstants {
    public static final String EMAIL_PATTERN = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$";
    public static final String FILE_EXT_PATTERN = "(?i)jpg|jpeg|png|gif|pdf|doc|docx|xls|xlsx|zip";
    public static final String FILE_MIME_PATTERN = "(?i)image/(jpeg|png|gif)|application/pdf|application/msword|application/vnd.openxmlformats-officedocument.wordprocessingml.document|application/vnd.ms-excel|application/vnd.openxmlformats-officedocument.spreadsheetml.sheet|application/zip|application/x-zip-compressed";
    public static final String FILE_BIZ_TYPE_PATTERN = "avatar|cover|task_submit";
    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
}
