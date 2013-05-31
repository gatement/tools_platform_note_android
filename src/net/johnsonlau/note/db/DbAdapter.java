package net.johnsonlau.note.db;

import net.johnsonlau.note.Config;
import net.johnsonlau.note.model.Category;
import net.johnsonlau.tool.DateTime;
import net.johnsonlau.tool.Utilities;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapter {

	private final Context mContext;
	private DbOpenHelper mDbOpenHelper;
	private SQLiteDatabase mDb;

	public DbAdapter(Context content) {
		this.mContext = content;
	}

	public DbAdapter open() throws SQLException {
		mDbOpenHelper = new DbOpenHelper(mContext);
		mDb = mDbOpenHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbOpenHelper.close();
	}

	// == settings ===============================

	public boolean updateSettings(String service, String userId, String userPwd) {
		ContentValues args = new ContentValues();
		args.put(DbOpenHelper.TABLE_SETTINGS_SERVICE, service);
		args.put(DbOpenHelper.TABLE_SETTINGS_USER_ID, userId);
		args.put(DbOpenHelper.TABLE_SETTINGS_USER_PWD, userPwd);

		return mDb.update(DbOpenHelper.TABLE_SETTINGS, args, null, null) > 0;
	}

	public Cursor fetchSettings() throws SQLException {
		Cursor cursor = mDb.query(true, DbOpenHelper.TABLE_SETTINGS,
				new String[] { DbOpenHelper.TABLE_SETTINGS_SERVICE,
						DbOpenHelper.TABLE_SETTINGS_USER_ID,
						DbOpenHelper.TABLE_SETTINGS_USER_PWD }, null, null,
				null, null, null, null);

		return cursor;
	}

	// == notes ==================================

	public Cursor fetchNote(long rowId) throws SQLException {
		Cursor cursor = mDb.query(true, DbOpenHelper.TABLE_NOTES, new String[] {
				DbOpenHelper.TABLE_NOTES_ID, DbOpenHelper.TABLE_NOTES_NOTE,
				DbOpenHelper.TABLE_NOTES_COLOR },
				DbOpenHelper.TABLE_NOTES_ROWID + "=" + rowId, null, null, null,
				null, null);

		return cursor;
	}

	public Cursor fetchNotesByCategory(String categoryId) throws SQLException {
		return mDb.query(true, DbOpenHelper.TABLE_NOTES, new String[] {
				DbOpenHelper.TABLE_NOTES_ROWID, DbOpenHelper.TABLE_NOTES_ID,
				DbOpenHelper.TABLE_NOTES_NOTE, DbOpenHelper.TABLE_NOTES_COLOR,
				DbOpenHelper.TABLE_NOTES_LAST_UPDATED },
				DbOpenHelper.TABLE_NOTES_CATEGORY_ID + " = ?",
				new String[] { categoryId }, null, null,
				DbOpenHelper.TABLE_NOTES_Z_INDEX + " DESC", null);
	}

	public Cursor fetchAllUpdatedNotes() throws SQLException {
		return mDb.query(true, DbOpenHelper.TABLE_NOTES, new String[] {
				DbOpenHelper.TABLE_NOTES_ID,
				DbOpenHelper.TABLE_NOTES_CATEGORY_ID,
				DbOpenHelper.TABLE_NOTES_NOTE,
				DbOpenHelper.TABLE_NOTES_LAST_UPDATED },
				DbOpenHelper.TABLE_NOTES_UPDATED + " > 0", null, null, null,
				null, null);
	}

	public void purgeNotes() {
		mDb.delete(DbOpenHelper.TABLE_NOTES, null, null);
	}

	public long insertNote(String id, String categoryId, String note,
			int color, int zIndex, int updated,
			String lastUpdated) {
		ContentValues values = new ContentValues();
		values.put(DbOpenHelper.TABLE_NOTES_ID, id);
		values.put(DbOpenHelper.TABLE_NOTES_CATEGORY_ID, categoryId);
		values.put(DbOpenHelper.TABLE_NOTES_NOTE, note);
		values.put(DbOpenHelper.TABLE_NOTES_COLOR, color);
		values.put(DbOpenHelper.TABLE_NOTES_Z_INDEX, zIndex);
		values.put(DbOpenHelper.TABLE_NOTES_UPDATED, updated);
		values.put(DbOpenHelper.TABLE_NOTES_LAST_UPDATED, lastUpdated);

		return mDb.insert(DbOpenHelper.TABLE_NOTES, null, values);
	}

	public long createNote(String categoryId, String note) {
		String id = "";
		int color = Config.DEFAULT_NOTE_COLOR;
		int zIndex = Config.DEFAULT_NOTE_Z_INDEX;
		int updated = 1;
		String lastUpdated = DateTime.getUtcMilliSecondTimestampString();

		return insertNote(id, categoryId, note, color, zIndex, updated, lastUpdated);
	}

	public boolean updateNote(long rowId, String note) {
		ContentValues args = new ContentValues();
		args.put(DbOpenHelper.TABLE_NOTES_NOTE, note);
		args.put(DbOpenHelper.TABLE_NOTES_UPDATED, 1);
		args.put(DbOpenHelper.TABLE_NOTES_LAST_UPDATED,
				DateTime.getUtcMilliSecondTimestampString());

		return mDb.update(DbOpenHelper.TABLE_NOTES, args,
				DbOpenHelper.TABLE_NOTES_ROWID + " = " + rowId, null) > 0;
	}

	public boolean deleteNote(long rowId) {
		return mDb.delete(DbOpenHelper.TABLE_NOTES,
				DbOpenHelper.TABLE_NOTES_ROWID + " = " + rowId, null) > 0;
	}

	// == deleted notes ==================================

	public Cursor fetchAllDeletedNotes() throws SQLException {
		return mDb.query(true, DbOpenHelper.TABLE_DELETED_NOTES,
				new String[] { DbOpenHelper.TABLE_DELETED_NOTES_ID }, null,
				null, null, null, null, null);
	}

	public void purgeDeletedNotes() {
		mDb.delete(DbOpenHelper.TABLE_DELETED_NOTES, null, null);
	}

	public void insertDeletedNote(long rowId) {
		Cursor noteCursor = fetchNote(rowId);

		noteCursor.moveToFirst();
		String noteId = noteCursor.getString(noteCursor
				.getColumnIndexOrThrow(DbOpenHelper.TABLE_NOTES_ID));
		noteCursor.close();

		if (!Utilities.isEmptyOrNull(noteId)) {
			ContentValues values = new ContentValues();
			values.put(DbOpenHelper.TABLE_DELETED_NOTES_ID, noteId);

			mDb.insert(DbOpenHelper.TABLE_DELETED_NOTES, null, values);
		}
	}

	// == categories ==================================

	public Cursor fetchAllCategories() throws SQLException {
		return mDb.query(true, DbOpenHelper.TABLE_CATEGORIES, new String[] {
				DbOpenHelper.TABLE_CATEGORIES_ID,
				DbOpenHelper.TABLE_CATEGORIES_NAME,
				DbOpenHelper.TABLE_CATEGORIES_IS_DEFAULT,
				DbOpenHelper.TABLE_CATEGORIES_PERMISSION }, null, null, null,
				null, null, null);
	}

	public void purgeCategories() {
		mDb.delete(DbOpenHelper.TABLE_CATEGORIES, null, null);
	}

	public long insertCategory(String id, String name, int isDefault,
			String permission) {
		ContentValues values = new ContentValues();
		values.put(DbOpenHelper.TABLE_CATEGORIES_ID, id);
		values.put(DbOpenHelper.TABLE_CATEGORIES_NAME, name);
		values.put(DbOpenHelper.TABLE_CATEGORIES_IS_DEFAULT, isDefault);
		values.put(DbOpenHelper.TABLE_CATEGORIES_PERMISSION, permission);

		return mDb.insert(DbOpenHelper.TABLE_CATEGORIES, null, values);
	}

	public Category fetchDefaultCategory() {
		Category result = null;

		int index = 0;

		Cursor cursor = fetchAllCategories();
		while (cursor.moveToNext()) {

			String id = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_ID));
			String name = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_NAME));
			String permission = cursor
					.getString(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_PERMISSION));

			result = new Category(index, id, name, permission);

			boolean isDefault = cursor
					.getInt(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_IS_DEFAULT)) > 0 ? true
					: false;

			if (isDefault) {
				break;
			}

			index++;

		}
		cursor.close();

		return result;
	}

	public Category fetchCategoryByIndex(int index) throws SQLException {
		Cursor cursor = fetchAllCategories();
		int count = cursor.getCount();

		Category result = null;

		if (count > 0) {
			if (index >= count) {
				index = 0;
			} else if (index < 0) {
				index = count - 1;
			}

			int i = -1;
			do {
				cursor.moveToNext();
				i++;
			} while (i < index);

			String id = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_ID));
			String name = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_NAME));
			String permission = cursor
					.getString(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_PERMISSION));

			cursor.close();

			result = new Category(index, id, name, permission);
		}

		return result;
	}

	public Category fetchCategoryById(String categoryId) {
		Category result = null;

		int index = 0;

		Cursor cursor = fetchAllCategories();
		while (cursor.moveToNext()) {

			String id = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_ID));
			String name = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_NAME));
			String permission = cursor
					.getString(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_PERMISSION));

			result = new Category(index, id, name, permission);

			if (id.equals(categoryId)) {
				break;
			}

			index++;

		}
		cursor.close();

		return result;
	}
}
