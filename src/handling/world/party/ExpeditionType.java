package handling.world.party;

/**
 *
 * @author zjj
 */
public enum ExpeditionType {

    /**
     *
     */
    Normal_Balrog(15, 2_000, 50, 250), //蝙蝠怪远征队

    /**
     *
     */
    Zakum(30, 2_002, 50, 250), //扎昆

    /**
     *
     */
    Horntail(30, 2_003, 80, 250), //暗黑龙王

    /**
     *
     */
    Pink_Bean(30, 2_004, 140, 250), //时间的宠儿－品克缤远征队

    /**
     *
     */
    Chaos_Zakum(30, 2_005, 100, 250), //进阶扎昆

    /**
     *
     */
    ChaosHT(30, 2_006, 110, 250), //进阶暗黑龙王

    /**
     *
     */
    Von_Leon(18, 2_007, 120, 250), //班·雷昂远征队

    /**
     *
     */
    Cygnus(18, 2_008, 170, 250), //希纳斯女皇 - 冒险岛骑士团远征队

    /**
     *
     */
    Akyrum(18, 2_009, 120, 250), //阿卡伊勒远征队

    /**
     *
     */
    Hillah(6, 2_010, 120, 250), //希拉远征队

    /**
     *
     */
    Chaos_Pink_Bean(6, 2_011, 170, 250), //混沌品克缤远征队

    /**
     *
     */
    CWKPQ(30, 2_011, 90, 250);

    /**
     *
     */
    public final int maxMembers;

    /**
     *
     */
    public final int maxParty;

    /**
     *
     */
    public final int exped;

    /**
     *
     */
    public final int minLevel;

    /**
     *
     */
    public final int maxLevel;

    ExpeditionType(int maxMembers, int exped, int minLevel, int maxLevel) {
        this.maxMembers = maxMembers;
        this.exped = exped;
        this.maxParty = (maxMembers / 2) + (maxMembers % 2 > 0 ? 1 : 0);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    /**
     *
     * @param id
     * @return
     */
    public static ExpeditionType getById(int id) {
        for (ExpeditionType pst : ExpeditionType.values()) {
            if (pst.exped == id) {
                return pst;
            }
        }
        return null;
    }
}
