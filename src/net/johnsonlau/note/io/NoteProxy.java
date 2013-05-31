package net.johnsonlau.note.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.johnsonlau.note.Config;
import net.johnsonlau.note.db.DbAdapter;
import net.johnsonlau.note.db.DbOpenHelper;
import net.johnsonlau.tool.HttpRequest;
import net.johnsonlau.tool.Utilities;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.util.Log;

public class NoteProxy {

	public static boolean syncLocalDeletedNotesToRemote(String serviceUrl,
			String sessionId, DbAdapter dbAdapter) throws IOException,
			JSONException {

		boolean result = false;

		// -- get deleted ids ----------------------------------------------
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		String ids = "";
		Cursor cursor = dbAdapter.fetchAllDeletedNotes();
		while (cursor.moveToNext()) {
			String id = cursor
					.getString(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_DELETED_NOTES_ID));

			if (Utilities.isEmptyOrNull(ids)) {
				ids = id;
			} else {
				ids += "," + id;
			}
		}
		cursor.close();
		nameValuePairs.add(new BasicNameValuePair("ids", ids));

		Log.i(Config.LOG_TAG, "deleting: " + ids);

		// -- start synchronizing --------------------------------------------
		if (!Utilities.isEmptyOrNull(ids)) // in case no words to delete
		{
			String url = serviceUrl + Config.URL_NOTE_DELETE_NOTES;
			String cookie = Config.SESSION_COOKIE_NAME + "=" + sessionId;
			String response = HttpRequest.doPost(url, nameValuePairs, cookie);

			Log.i(Config.LOG_TAG, "syncLocalDeletedNotesToRemote return: "
					+ response);

			JSONObject obj = new JSONObject(response);
			result = obj.getBoolean("success");
		} else {
			result = true;
		}

		return result;
	}

	public static boolean updateRemoteNotes(String serviceUrl,
			String sessionId, DbAdapter dbAdapter) throws IOException,
			JSONException {

		boolean result = true;

		Cursor cursor = dbAdapter.fetchAllUpdatedNotes();
		while (cursor.moveToNext()) {
			// -- prepare data ----------------------------------------------
			String id = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_NOTES_ID));
			String categoryId = cursor
					.getString(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_NOTES_CATEGORY_ID));
			String note = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_NOTES_NOTE));
			String lastUpdated = cursor
					.getString(cursor
							.getColumnIndexOrThrow(DbOpenHelper.TABLE_NOTES_LAST_UPDATED));

			Log.i(Config.LOG_TAG, "updateNote: " + id + "("+ note +")");
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("id", id));
			nameValuePairs
					.add(new BasicNameValuePair("category_id", categoryId));
			nameValuePairs.add(new BasicNameValuePair("note", note));
			nameValuePairs.add(new BasicNameValuePair("last_updated",
					lastUpdated));

			// -- start updating ----------------------------------------------
			String url = serviceUrl + Config.URL_NOTE_UPDATE_NOTE;
			String cookie = Config.SESSION_COOKIE_NAME + "=" + sessionId;
			String response = HttpRequest.doPost(url, nameValuePairs, cookie);
			Log.i(Config.LOG_TAG, "updateNote return: " + response);
			JSONObject obj = new JSONObject(response);
			result = result && obj.getBoolean("success");
		}
		cursor.close();

		return result;
	}

	public static JSONArray getCategories(String serviceUrl, String sessionId)
			throws IOException, JSONException {

		JSONArray result = null;

		String url = serviceUrl + Config.URL_NOTE_CATEGORY_LIST;
		String cookie = Config.SESSION_COOKIE_NAME + "=" + sessionId;
		String response = HttpRequest.doGet(url, cookie);

		Log.i(Config.LOG_TAG, "getCategories return: " + response);

		JSONObject obj = new JSONObject(response);
		if (obj.getBoolean("success")) {
			result = obj.getJSONArray("data");
		}

		return result;
	}

	public static JSONArray getNotesByCategory(String serviceUrl,
			String sessionId, String categoryId) throws IOException,
			JSONException {

		JSONArray result = null;

		String url = serviceUrl + Config.URL_NOTE_LIST + "?category_id=" + categoryId;
		String cookie = Config.SESSION_COOKIE_NAME + "=" + sessionId;
		String response = HttpRequest.doGet(url, cookie);

		Log.i(Config.LOG_TAG, "getNotesByCategory url: " + url);
		Log.i(Config.LOG_TAG, "getNotesByCategory(" + categoryId + ") return: "
				+ response);

		JSONObject obj = new JSONObject(response);
		if (obj.getBoolean("success")) {
			result = obj.getJSONArray("data");
		}

		return result;
	}
}
