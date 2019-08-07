/*
 * Copyright 2004-2010 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.ddl;

import org.h2.engine.*;
import org.h2.schema.Schema;
import org.h2.schema.SchemaObject;
import org.h2.table.Table;
import org.h2.util.ObjectArray;

import java.sql.SQLException;

/**
 * This class represents the statement
 * DROP ALL OBJECTS
 */
public class DropDatabase extends DefineCommand {

    private boolean dropAllObjects;
    private boolean deleteFiles;

    public DropDatabase(Session session) {
        super(session);
    }

    public int update() throws SQLException {
        if (dropAllObjects) {
            dropAllObjects();
        }
        if (deleteFiles) {
            session.getDatabase().setDeleteFilesOnDisconnect(true);
        }
        return 0;
    }

    private void dropAllObjects() throws SQLException {
        session.getUser().checkAdmin();
        session.commit(true);
        Database db = session.getDatabase();
        // TODO local temp tables are not removed
        for (Schema schema : db.getAllSchemas()) {
            if (schema.canDrop()) {
                db.removeDatabaseObject(session, schema);
            }
        }
        ObjectArray<Table> tables = db.getAllTablesAndViews(false);
        for (Table t : tables) {
            if (t.getName() != null && Table.VIEW.equals(t.getTableType())) {
                db.removeSchemaObject(session, t);
            }
        }
        for (Table t : tables) {
            if (t.getName() != null && Table.TABLE_LINK.equals(t.getTableType())) {
                db.removeSchemaObject(session, t);
            }
        }
        for (Table t : tables) {
            if (t.getName() != null && Table.TABLE.equals(t.getTableType())) {
                db.removeSchemaObject(session, t);
            }
        }
        session.findLocalTempTable(null);
        ObjectArray<SchemaObject> list = ObjectArray.newInstance(false);
        list.addAll(db.getAllSchemaObjects(DbObject.SEQUENCE));
        // maybe constraints and triggers on system tables will be allowed in
        // the future
        list.addAll(db.getAllSchemaObjects(DbObject.CONSTRAINT));
        list.addAll(db.getAllSchemaObjects(DbObject.TRIGGER));
        list.addAll(db.getAllSchemaObjects(DbObject.CONSTANT));
        for (SchemaObject obj : list) {
            db.removeSchemaObject(session, obj);
        }
        for (User user : db.getAllUsers()) {
            if (user != session.getUser()) {
                db.removeDatabaseObject(session, user);
            }
        }
        for (Role role : db.getAllRoles()) {
            String sql = role.getCreateSQL();
            // the role PUBLIC must not be dropped
            if (sql != null) {
                db.removeDatabaseObject(session, role);
            }
        }
        ObjectArray<DbObject> dbObjects = ObjectArray.newInstance(false);
        dbObjects.addAll(db.getAllRights());
        dbObjects.addAll(db.getAllFunctionAliases());
        dbObjects.addAll(db.getAllAggregates());
        dbObjects.addAll(db.getAllUserDataTypes());
        for (DbObject obj : dbObjects) {
            String sql = obj.getCreateSQL();
            // the role PUBLIC must not be dropped
            if (sql != null) {
                db.removeDatabaseObject(session, obj);
            }
        }
    }

    public void setDropAllObjects(boolean b) {
        this.dropAllObjects = b;
    }

    public void setDeleteFiles(boolean b) {
        this.deleteFiles = b;
    }

}
