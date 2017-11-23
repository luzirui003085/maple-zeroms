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
package handling.world;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author zjj
 */
public class MapleMessenger implements Serializable {

    private static final long serialVersionUID = 9_179_541_993_413_738_569L;
    private MapleMessengerCharacter[] members = new MapleMessengerCharacter[3];
    private String[] silentLink = new String[3];
    private int id;

    /**
     *
     * @param id
     * @param chrfor
     */
    public MapleMessenger(int id, MapleMessengerCharacter chrfor) {
        this.id = id;
        addMem(0, chrfor);
    }

    /**
     *
     * @param pos
     * @param chrfor
     */
    public void addMem(int pos, MapleMessengerCharacter chrfor) {
        if (members[pos] != null) {
            return;
        }
        members[pos] = chrfor;
    }

    /**
     *
     * @param member
     * @return
     */
    public boolean containsMembers(MapleMessengerCharacter member) {
        return getPositionByName(member.getName()) < 4;
    }

    /**
     *
     * @param member
     */
    public void addMember(MapleMessengerCharacter member) {
        int position = getLowestPosition();
        if (position > -1 && position < 4) {
            addMem(position, member);
        }
    }

    /**
     *
     * @param member
     */
    public void removeMember(MapleMessengerCharacter member) {
        final int position = getPositionByName(member.getName());
        if (position > -1 && position < 4) {
            members[position] = null;
        }
    }

    /**
     *
     * @param member
     */
    public void silentRemoveMember(MapleMessengerCharacter member) {
        final int position = getPositionByName(member.getName());
        if (position > -1 && position < 4) {
            members[position] = null;
            silentLink[position] = member.getName();
        }
    }

    /**
     *
     * @param member
     */
    public void silentAddMember(MapleMessengerCharacter member) {
        for (int i = 0; i < silentLink.length; i++) {
            if (silentLink[i] != null && silentLink[i].equalsIgnoreCase(member.getName())) {
                addMem(i, member);
                silentLink[i] = null;
                return;
            }
        }
    }

    /**
     *
     * @param member
     */
    public void updateMember(MapleMessengerCharacter member) {
        for (int i = 0; i < members.length; i++) {
            MapleMessengerCharacter chr = members[i];
            if (chr.equals(member)) {
                members[i] = null;
                addMem(i, member);
                return;
            }
        }
    }

    /**
     *
     * @return
     */
    public int getLowestPosition() {
        for (int i = 0; i < members.length; i++) {
            if (members[i] == null) {
                return i;
            }
        }
        return 4;
    }

    /**
     *
     * @param name
     * @return
     */
    public int getPositionByName(String name) {
        for (int i = 0; i < members.length; i++) {
            MapleMessengerCharacter messengerchar = members[i];
            if (messengerchar != null && messengerchar.getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return 4;
    }

    /**
     *
     * @return
     */
    public int getId() {
        return id;
    }

    /**
     *
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return 31 + id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MapleMessenger other = (MapleMessenger) obj;
        return id == other.id;
    }

    /**
     *
     * @return
     */
    public Collection<MapleMessengerCharacter> getMembers() {
        return Arrays.asList(members);
    }
}
