/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools;

import java.util.Date;
import java.util.SimpleTimeZone;

/**
 * Provides a suite of tools for manipulating Korean Timestamps.
 *
 * @author Frz
 * @since Revision 746
 * @version 1.0
 */
public class KoreanDateUtil {

    private final static int ITEM_YEAR2000 = -1_085_019_342;
    private final static long REAL_YEAR2000 = 946_681_229_830l;
    private final static int QUEST_UNIXAGE = 27_111_908;
    private final static long FT_UT_OFFSET = 116_444_736_000_000_000L; // 100 nsseconds from 1/1/1601 -> 1/1/1970

    /**
     *
     */
    public final static long MAX_TIME = 150_842_304_000_000_000L; //00 80 05 BB 46 E6 17 02

    /**
     *
     */
    public final static long ZERO_TIME = 94_354_848_000_000_000L; //00 40 E0 FD 3B 37 4F 01

    /**
     *
     */
    public final static long PERMANENT = 150_841_440_000_000_000L; // 00 C0 9B 90 7D E5 17 02

    /**
     *
     * @param realTimestamp
     * @return
     */
    public static long getKoreanTimestamp(final long realTimestamp) {
        return getTime(realTimestamp);
    }

    /**
     *
     * @param realTimestamp
     * @return
     */
    public static long getTime(long realTimestamp) {
        if (realTimestamp == -1) {
            return MAX_TIME;
        } else if (realTimestamp == -2) {
            return ZERO_TIME;
        } else if (realTimestamp == -3) {
            return PERMANENT;
        }
        return ((realTimestamp * 10_000) + FT_UT_OFFSET);
    }

    /**
     * Converts a Unix Timestamp into File Time
     *
     * @param realTimestamp The actual timestamp in milliseconds.
     * @return A 64-bit long giving a filetime timestamp
     */
    public static final long getTempBanTimestamp(final long realTimestamp) {
        // long time = (realTimestamp / 1000);//seconds
        return ((realTimestamp * 10_000) + FT_UT_OFFSET);
    }

    /**
     * Gets a timestamp for item expiration.
     *
     * @param realTimestamp The actual timestamp in milliseconds.
     * @return The Korean timestamp for the real timestamp.
     */
    public static final int getItemTimestamp(final long realTimestamp) {
        final int time = (int) ((realTimestamp - REAL_YEAR2000) / 1_000 / 60); // convert to minutes
        return (int) (time * 35.762787) + ITEM_YEAR2000;
    }

    /**
     * Gets a timestamp for quest repetition.
     *
     * @param realTimestamp The actual timestamp in milliseconds.
     * @return The Korean timestamp for the real timestamp.
     */
    public static final int getQuestTimestamp(final long realTimestamp) {
        final int time = (int) (realTimestamp / 1_000 / 60); // convert to minutes
        return (int) (time * 0.1396987) + QUEST_UNIXAGE;
    }

    /**
     *
     * @return
     */
    public static boolean isDST() {
        return SimpleTimeZone.getDefault().inDaylightTime(new Date());
    }

    /**
     *
     * @param timeStampinMillis
     * @param roundToMinutes
     * @return
     */
    public static long getFileTimestamp(long timeStampinMillis, boolean roundToMinutes) {
        if (isDST()) {
            timeStampinMillis -= 3_600_000L;
        }
        long time;
        if (roundToMinutes) {
            time = (timeStampinMillis / 1_000 / 60) * 600_000_000;
        } else {
            time = timeStampinMillis * 10_000;
        }
        return time + FT_UT_OFFSET;
    }
}
