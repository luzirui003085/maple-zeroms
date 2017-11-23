/*
 This file is part of the ZeroFusion MapleStory Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>
 ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

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
package server.events;

/**
 *
 * @author zjj
 */
public enum MapleEventType {

    /**
     *
     */
    打椰子比赛("椰子比赛", new int[]{109_080_000}), //Coconut

    /**
     *
     */
    打瓶盖比赛("打瓶盖", new int[]{109_080_010}), //CokePlay

    /**
     *
     */
    向高地("向高地", new int[]{109_040_000, 109_040_001, 109_040_002, 109_040_003, 109_040_004}),// Fitness

    /**
     *
     */
    上楼上楼("上楼~上楼~", new int[]{109_030_001, 109_030_002, 109_030_003}),// OlaOla

    /**
     *
     */
    快速0X猜题("快速OX猜题", new int[]{109_020_001}),//OxQuiz

    /**
     *
     */
    雪球赛("雪球赛", new int[]{109_060_000}); //Snowball//Snowball

    /**
     *
     */
    public String command;

    /**
     *
     */
    public int[] mapids;

    private MapleEventType(String comm, int[] mapids) {
        this.command = comm;
        this.mapids = mapids;
    }

    /**
     *
     * @param splitted
     * @return
     */
    public static final MapleEventType getByString(final String splitted) {
        for (MapleEventType t : MapleEventType.values()) {
            if (t.command.equalsIgnoreCase(splitted)) {
                return t;
            }
        }
        return null;
    }
}
