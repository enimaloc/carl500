package ga.enimaloc.discord.carl500.utils;

import net.dv8tion.jda.api.entities.Member;

public class UserUtils {

    public static boolean isAdministrator(Member member) {
        return member.getRoles().contains(member.getGuild().getRoleById(500679132229664789L));
    }
}