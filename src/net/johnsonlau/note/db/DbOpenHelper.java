package net.johnsonlau.note.db;

import net.johnsonlau.note.Config;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbOpenHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "data";
	private static final int DATABASE_VERSION = 1;

	// == table settings =============================================
	public static final String TABLE_SETTINGS = "settings";
	public static final String TABLE_SETTINGS_ROWID = "_id";
	public static final String TABLE_SETTINGS_SERVICE = "service";
	public static final String TABLE_SETTINGS_USER_ID = "user_id";
	public static final String TABLE_SETTINGS_USER_PWD = "user_pwd";
	
	private static final String TABLE_SETTINGS_CREATE = "CREATE TABLE "
			+ TABLE_SETTINGS 
			+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+ ", service TEXT NOT NULL" 
			+ ", user_id TEXT NOT NULL"
			+ ", user_pwd TEXT NOT NULL);";
	
	private static final String TABLE_SETTINGS_INITIALIZE = "INSERT INTO settings "
			+ "(service, user_id, user_pwd)"
			+ " VALUES('https://tools.johnson.uicp.net', '', '');";


	// table notes
	public static final String TABLE_NOTES = "notes";
	public static final String TABLE_NOTES_ROWID = "_id";
	public static final String TABLE_NOTES_ID = "id";
	public static final String TABLE_NOTES_CATEGORY_ID = "category_id";
	public static final String TABLE_NOTES_NOTE = "note";
	public static final String TABLE_NOTES_COLOR = "color";
	public static final String TABLE_NOTES_Z_INDEX = "z_index";
	public static final String TABLE_NOTES_UPDATED = "updated";
	public static final String TABLE_NOTES_LAST_UPDATED = "last_updated";
	
	private static final String TABLE_NOTES_CREATE = "CREATE TABLE "
			+ TABLE_NOTES 
			+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+ ", id TEXT NOT NULL" 
			+ ", category_id TEXT NOT NULL" 
			+ ", note TEXT NOT NULL"
			+ ", color INTEGER NOT NULL" 
			+ ", z_index INTEGER NOT NULL"
			+ ", updated INTEGER NOT NULL" 
			+ ", last_updated DATETIME NOT NULL);";

	
	// table deleted_notes
	public static final String TABLE_DELETED_NOTES = "deleted_notes";
	public static final String TABLE_DELETED_NOTES_ROWID = "_id";
	public static final String TABLE_DELETED_NOTES_ID = "id";
	
	private static final String TABLE_DELETED_NOTES_CREATE = "CREATE TABLE "
			+ TABLE_DELETED_NOTES 
			+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+ ", id TEXT NOT NULL);";


	// == table categories =================================================

	public static final String TABLE_CATEGORIES = "categories";
	public static final String TABLE_CATEGORIES_ROWID = "_id";
	public static final String TABLE_CATEGORIES_ID = "id";
	public static final String TABLE_CATEGORIES_NAME = "name";
	public static final String TABLE_CATEGORIES_IS_DEFAULT = "is_default";
	public static final String TABLE_CATEGORIES_PERMISSION = "permission";
	
	private static final String TABLE_CATEGORIES_CREATE = "CREATE TABLE "
			+ TABLE_CATEGORIES 
			+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+ ", id TEXT NOT NULL"
			+ ", name TEXT NOT NULL"
			+ ", is_default INTEGER NOT NULL"
			+ ", permission TEXT NOT NULL);";
	
	
	// == ovreride methods =========================================

	DbOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_SETTINGS_CREATE);
		db.execSQL(TABLE_SETTINGS_INITIALIZE);
		db.execSQL(TABLE_NOTES_CREATE);
		db.execSQL(TABLE_DELETED_NOTES_CREATE);
		db.execSQL(TABLE_CATEGORIES_CREATE);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion == 1 && newVersion == 2) {
			upgradeToVersion2();
		}
		if (oldVersion == 2 && newVersion == 3) {
			upgradeToVersion3();
		}

		Log.i(Config.LOG_TAG, "Upgraded database " + DATABASE_NAME + " from version "
				+ oldVersion + " to " + newVersion);
	}

	private void upgradeToVersion2() {
		// do upgrading job
	}

	private void upgradeToVersion3() {
		// do upgrading job
	}
}
