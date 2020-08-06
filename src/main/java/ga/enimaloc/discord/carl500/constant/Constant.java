package ga.enimaloc.discord.carl500.constant;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Constant {
    public static final long OWNER_ENIMALOC = 136200628509605888L;
    public static final long OWNER_BIG_BOSS = 205064306482479104L;
    public static final long OWNER_HARTWEED = 214743141515788290L;
    public static final long OWNER_NESSTEA = 234760450028470272L;
    public static final long OWNER_TIORIS = 381718609006690304L;
    public static final long[] OWNERS_IDS;

    public static final long CHANNEL_TEST = 739716558259093595L;
    public static final long CHANNEL_RULES = Info.inDev ? 739716558259093595L : 501830958429765645L;
    public static final long CHANNEL_TICKET = 501840622538457098L;
    public static final long CATEGORY_TICKETS = 739714871406624839L;

    public static final long ROLE_ADMIN = 500679132229664789L;

    public static final long GUILD_MAIN = 500678477985349646L;


    static {
        List<Long> owners_ids = new ArrayList();

        for (Field field : Constant.class.getFields()) {
            if (field.getName().startsWith("OWNER_")) {
                try {
                    owners_ids.add(field.getLong(Constant.class));
                } catch (IllegalAccessException var6) {
                    var6.printStackTrace();
                }
            }
        }

        OWNERS_IDS = owners_ids.stream().mapToLong(Long::longValue).toArray();
    }
}
