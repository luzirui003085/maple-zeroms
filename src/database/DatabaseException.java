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
package database;

import java.sql.SQLException;

/**
 *
 * @author zjj
 */
public class DatabaseException extends RuntimeException {

    private static final long serialVersionUID = -420_103_154_764_822_555L;

    /**
     *
     * @param msg
     */
    public DatabaseException(String msg) {
        super(msg);
    }

    /**
     *
     * @param message
     * @param cause
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    DatabaseException(SQLException e) {
        throw new UnsupportedOperationException(e); //To change body of generated methods, choose Tools | Templates.
    }
}
