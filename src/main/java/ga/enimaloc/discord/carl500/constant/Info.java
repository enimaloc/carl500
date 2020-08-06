package ga.enimaloc.discord.carl500.constant;

import ga.enimaloc.discord.carl500.Main;

public class Info {
    public static final Integer MAJOR = 2;
    public static final Integer MINOR = 0;
    public static final Integer PATCH = 0;
    public static final String PRE_RELEASE = "alpha";
    public static final String BUILD = "1";
    public static final boolean inDev = Main.arguments.getOptionValue("environment", "production").equals("development");;
    @SuppressWarnings("ConstantConditions") // Can be changed with value above
    public static final String VERSION = MAJOR + "." + MINOR + "." + PATCH + (PRE_RELEASE.isEmpty() ? "" : "-"+PRE_RELEASE) + (BUILD.isEmpty() ? "" : "+"+BUILD);
}
