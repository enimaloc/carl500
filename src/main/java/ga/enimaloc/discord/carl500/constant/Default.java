package ga.enimaloc.discord.carl500.constant;

public class Default {
    public static final String ENVIRONMENT = "production";

    public static final String PREFIX_DEV = "b500!";
    public static final String PREFIX_PROD = "c500!";

    public static final String SQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String SQL_ENGINE = "mysql";
    public static final String SQL_ADDRESS = "localhost";
    public static final String SQL_PORT = "3306";
    public static final String SQL_DATABASE = "carl500";
    public static final String[] SQL_OPTIONS = new String[]{"zeroDateTimeBehavior=CONVERT_TO_NULL", "serverTimezone=UTC"};
    public static final String SQL_USERNAME = "carl500";

    public static final String LOGGER_LEVEL = "info";
}
