package com.molvix.android.database;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * This actually sets up the MolvixDB Database on the local
 * device to handle all things local
 */
@Database(name = MolvixDB.NAME, version = MolvixDB.VERSION, backupEnabled = true)
public class MolvixDB {
    static final String NAME = "MolvixDB";
    public static final int VERSION = 1;
}
