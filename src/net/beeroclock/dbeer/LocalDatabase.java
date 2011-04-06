package net.beeroclock.dbeer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A database for keeping our local settings in...
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-04-06
 */
public class LocalDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_HIDDEN_BARS = "hidden_bars";
    public static final String HB_ROWID = "_id";
    public static final String HB_PKUID = "bar_pkuid";
    public static final String HB_NAME = "bar_name";
    private static final String TABLE_HIDDEN_BARS_CREATE =
                "CREATE TABLE " + TABLE_HIDDEN_BARS + " (" +
                HB_ROWID + " integer primary key autoincrement, " +
                HB_PKUID + " integer not null, " +
                HB_NAME + " TEXT not null);";

    public LocalDatabase(Context context) {
        super(context, "PintyDbName", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_HIDDEN_BARS_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        throw new UnsupportedOperationException("We don't support database version upgrades at this time...");
    }
}
