package chuckree.weightsup.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import chuckree.weightsup.data.WeightLogContract.*;

/**
 * Created by com.chuckree on 11/05/17.
 */

public class WeightLogDbHelper extends SQLiteOpenHelper {

    // The database name
    private static final String DATABASE_NAME = "weightlog.db";

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 3;

    public WeightLogDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String SQL_CREATE_WEIGHTLOG_TABLE = "CREATE TABLE " + WeightLogEntry.TABLE_NAME + " (" +
                WeightLogEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                WeightLogEntry.COLUMN_WEIGHT + " FLOAT NOT NULL, " +
                WeightLogEntry.COLUMN_LOSS_GAIN + " FLOAT NOT NULL, " +
                WeightLogEntry.COLUMN_IMAGE_PATH + " TEXT, " +
                WeightLogEntry.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                "); ";

        db.execSQL(SQL_CREATE_WEIGHTLOG_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + WeightLogEntry.TABLE_NAME);
        onCreate(db);
    }

}
