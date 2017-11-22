package constants;

import server.ServerProperties;

public class ServerConstants {
    // Start of Poll

    public static final boolean PollEnabled = false;
    public static final String Poll_Question = "Are you mudkiz?";
    public static final String[] Poll_Answers = {"test1", "test2", "test3"};
    // End of Poll
    public static final short MAPLE_VERSION = 79;
    public static final String MAPLE_PATCH = "1";
    public static final boolean Use_Fixed_IV = false;
    public static final int MIN_MTS = 110;
    public static final int MTS_BASE = 100; //+1000 to everything in MSEA but cash is costly here
    public static final int MTS_TAX = 10; //+% to everything
    public static final int MTS_MESO = 5000; //mesos needed
    public static final int CHANNEL_COUNT = 120;
    //服务端输出操作
    public static String PACKET_ERROR = "";
    public static final boolean PACKET_ERROR_OFF = Boolean.parseBoolean(ServerProperties.getProperty("ZeroMS.记录38错误", "false"));
    public static boolean 封包显示 = Boolean.parseBoolean(ServerProperties.getProperty("ZeroMS.封包显示", "false"));
    public static boolean 调试输出封包 = Boolean.parseBoolean(ServerProperties.getProperty("ZeroMS.调试输出封包", "false"));
    public static boolean 自动注册 = false;
    public static boolean Super_password = false;
    public static String superpw = "";

    public static boolean getAutoReg() {
        return 自动注册;
    }

    public static String ChangeAutoReg() {
        自动注册 = !getAutoReg();
        return 自动注册 ? "开启" : "关闭";
    }

    public void setPACKET_ERROR(String ERROR){
        PACKET_ERROR = ERROR;
    }

    public String getPACKET_ERROR() {
        return PACKET_ERROR;
    }
    
    public static final byte Class_Bonus_EXP(final int job) {
        switch (job) {
            case 3000: //whenever these arrive, they'll give bonus
            case 3200:
            case 3210:
            case 3211:
            case 3212:
            case 3300:
            case 3310:
            case 3311:
            case 3312:
            case 3500:
            case 3510:
            case 3511:
            case 3512:
                return 10;
        }
        return 0;
    }

    public static enum PlayerGMRank {

        NORMAL('@', 0),
        INTERN('!', 1),
        GM('!', 2),
        ADMIN('!', 3);
        //SUPERADMIN('!', 3);
        private char commandPrefix;
        private int level;

        private PlayerGMRank(char ch, int level) {
            commandPrefix = ch;
            this.level = level;
        }

        public char getCommandPrefix() {
            return commandPrefix;
        }

        public int getLevel() {
            return level;
        }
    }

    public static enum CommandType {

        NORMAL(0),
        TRADE(1);
        private int level;

        private CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }
}
