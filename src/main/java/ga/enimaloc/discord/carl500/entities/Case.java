package ga.enimaloc.discord.carl500.entities;

public class Case {
    private final long target;
    private final long author;
    private final Case.Type caseType;
    private final String reason;

    public Case(long target, long author, Case.Type caseType, String reason) {
        this.target = target;
        this.author = author;
        this.caseType = caseType;
        this.reason = reason;
    }

    public long getTarget() {
        return this.target;
    }

    public long getAuthor() {
        return this.author;
    }

    public Case.Type getCaseType() {
        return this.caseType;
    }

    public String getReason() {
        return this.reason;
    }

    public static enum Type {
        WARN,
        MUTE,
        TEMP_BAN,
        BAN;
    }
}
