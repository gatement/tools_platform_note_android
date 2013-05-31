package net.johnsonlau.note.activity;

import net.johnsonlau.note.Config;
import net.johnsonlau.note.Helpers;
import net.johnsonlau.note.R;
import net.johnsonlau.note.db.DbAdapter;
import net.johnsonlau.note.db.DbOpenHelper;
import net.johnsonlau.note.io.NoteProxy;
import net.johnsonlau.note.io.SessionProxy;
import net.johnsonlau.note.model.Category;
import net.johnsonlau.tool.CmdMessage;
import net.johnsonlau.tool.Utilities;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class MainActivity extends Activity implements OnTouchListener,
		SimpleCursorAdapter.ViewBinder {

	private static final int ACTIVITY_NOTE_EDIT = 0;

	private static final int MENU_ID_SETTINGS = Menu.FIRST;
	private static final int MENU_ID_ABOUT = Menu.FIRST + 1;
	private static final int MENU_ID_REVERT = Menu.FIRST + 2;
	private static final int MENU_ID_SYNC = Menu.FIRST + 3;

	private float mDownXValue;

	private DbAdapter mDbAdapter;
	private Handler mMainHandler;

	private int mCurrentCategoryIndex = -1;
	private String mCurrentCategoryId;
	private String mCurrentCategoryName;
	private String mCurrentCategoryPermission;

	private String mServiceUrl;
	private String mUserId;
	private String mUserPwd;

	private TextView mMsgTextView;
	private ViewFlipper mViewFlipper;
	private ListView mListView1;
	private ListView mListView2;
	private Button mCategoryButton1;
	private Button mCategoryButton2;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		initMembers();
		bindEvents();
		populateData();
	}

	// == initialization methods
	// ===============================================================

	private void initMembers() {
		mDbAdapter = new DbAdapter(this).open();

		mMsgTextView = (TextView) findViewById(R.id.main_msg);
		mViewFlipper = (ViewFlipper) findViewById(R.id.main_flipper);
		mListView1 = (ListView) findViewById(R.id.main_list1);
		mListView2 = (ListView) findViewById(R.id.main_list2);
		mCategoryButton1 = (Button) findViewById(R.id.main_category_btn1);
		mCategoryButton2 = (Button) findViewById(R.id.main_category_btn2);

		mMainHandler = new Handler() {
			public void handleMessage(Message msg) {
				CmdMessage message = (CmdMessage) msg.obj;

				if (message.getCmd() == "PopulateData") {
					populateDataAfterSync();
				} else if (message.getCmd() == "Message") {
					mMsgTextView.setText(message.getValue());
				}
			}
		};
	}

	private void bindEvents() {
		mViewFlipper.setOnTouchListener((OnTouchListener) this); // this.onTouch()
		mListView1.setOnTouchListener((OnTouchListener) this); // this.onTouch()
		mListView2.setOnTouchListener((OnTouchListener) this); // this.onTouch()

		mListView1
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						goToNoteActivity(id);
					}
				});

		mListView2
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						goToNoteActivity(id);
					}
				});

		this.mCategoryButton1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (mCurrentCategoryPermission.equals("owner")
						|| mCurrentCategoryPermission.equals("rw")) {
					goToNoteActivity(null);
				}
			}
		});
		this.mCategoryButton2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (mCurrentCategoryPermission.equals("owner")
						|| mCurrentCategoryPermission.equals("rw")) {
					goToNoteActivity(null);
				}
			}
		});
	}

	public void populateData() {
		populateDataAfterPageLoaded();
	}

	// == override methods
	// =====================================================================

	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == Activity.RESULT_OK
				&& requestCode == ACTIVITY_NOTE_EDIT) {
			String categoryId = intent
					.getStringExtra(DbOpenHelper.TABLE_CATEGORIES_ID);
			populateDataReturnFromNoteEdit(categoryId);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ID_SYNC, 0, R.string.main_menu_sync);
		menu.add(0, MENU_ID_REVERT, 1, R.string.main_menu_revert);
		menu.add(0, MENU_ID_SETTINGS, 2, R.string.main_menu_settings);
		menu.add(0, MENU_ID_ABOUT, 3, R.string.main_menu_about);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		mMsgTextView.setText("");

		switch (item.getItemId()) {

		case MENU_ID_SETTINGS:
			goToSettingsActivity();
			return true;

		case MENU_ID_ABOUT:
			goToAboutActivity();
			return true;

		case MENU_ID_REVERT:
			new RevertChangesThread().start();
			return true;

		case MENU_ID_SYNC:
			new SyncThread().start();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	protected void onDestroy() {
		super.onDestroy();

		mDbAdapter.close();
	}

	// == implement OnTouchListener
	// =============================================================================

	public boolean onTouch(View arg0, MotionEvent arg1) {
		boolean result = false;

		switch (arg1.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			mDownXValue = arg1.getX();
			result = true;
			break;
		}

		case MotionEvent.ACTION_UP: {
			float currentX = arg1.getX();

			if (Math.abs(mDownXValue - currentX) > 100) {
				if (mDownXValue < currentX) {
					mCurrentCategoryIndex--;
					populateDataBeforeViewFlipping();

					mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(
							this, R.anim.push_left_in));
					mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(
							this, R.anim.push_left_out));
					mViewFlipper.showPrevious();
				} else {
					mCurrentCategoryIndex++;
					populateDataBeforeViewFlipping();

					mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(
							this, R.anim.push_right_in));
					mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(
							this, R.anim.push_right_out));
					mViewFlipper.showNext();
				}

				result = true;
			}
			// accept the event if movement > 0, so it won't trigger list view click event any more
			else if (Math.abs(mDownXValue - currentX) > 1) {
				result = true;
			}
			break;
		}
		}

		return result;
	}

	// == new threads
	// ==========================================================================

	private class SyncThread extends Thread {
		public void run() {
			sync(false);
			sendMessage(new CmdMessage("PopulateData", ""));
		}
	}

	private class RevertChangesThread extends Thread {
		public void run() {
			sync(true);
			sendMessage(new CmdMessage("PopulateData", ""));
		}
	}

	// == sync
	// =================================================================================

	private void sync(boolean isReverting) {
		try {
			boolean success = true;

			getSettings();

			// -- create session
			// ----------------------------------------------------------
			sendMessage(new CmdMessage("Message", "Start creating session..."));

			String sessionId = "";
			try {
				sessionId = SessionProxy.createSession(mServiceUrl, mUserId,
						mUserPwd);
			} catch (Exception ex) {
				Log.i(Config.LOG_TAG,
						"createSession exception: " + ex.getMessage());
				sendMessage(new CmdMessage("Message", "Authorization error."));
				success = false;
			}
			if (sessionId == "") {
				sendMessage(new CmdMessage("Message", "Authorization error."));
				success = false;
			}

			Log.i(Config.LOG_TAG, "sessionId: " + sessionId);

			// -- sync deleted notes to remote
			// -------------------------------------------------------------
			if (success && !isReverting) {
				sendMessage(new CmdMessage("Message",
						"Start synchronizing local deleted note to remote..."));
				success = syncLocalDeletedNotesToRemote(sessionId);

			}
			if (success) // purge local records
			{
				Log.i(Config.LOG_TAG,
						"success synchronizing deleted notes to remote!");
				mDbAdapter.purgeDeletedNotes();
			}

			// -- sync notes to remote
			// --------------------------------------------------
			if (success && !isReverting) {
				sendMessage(new CmdMessage("Message",
						"Start synchronizing local note to remote..."));
				success = updateRemoteNotes(sessionId);
			}
			if (success) // purge local records
			{
				Log.i(Config.LOG_TAG, "success uploading remote notes!");
				sendMessage(new CmdMessage("Message",
						"Start purging local records..."));
				mDbAdapter.purgeNotes();
				mDbAdapter.purgeCategories();
			}

			// -- download categories
			// --------------------------------------------------------
			if (success) {
				sendMessage(new CmdMessage("Message",
						"Start downloading remote categories..."));
				success = downloadCategories(sessionId);

				Log.i(Config.LOG_TAG, "success downloading categories!");
			}

			// -- download notes
			// --------------------------------------------------------
			if (success) {
				sendMessage(new CmdMessage("Message",
						"Start downloading remote notes..."));
				success = downloadNotes(sessionId);

				Log.i(Config.LOG_TAG, "success downloading notes!");
			}

			// -- finish
			// ------------------------------------------------------------
			if (success) {
				sendMessage(new CmdMessage("Message", "Sync succeeded."));
			}

		} catch (Exception ex) {
			Log.i(Config.LOG_TAG, "sync exception: " + ex.getMessage());
			sendMessage(new CmdMessage("Message", "Sync error."));
		}
	}

	private boolean syncLocalDeletedNotesToRemote(String sessionId) {
		boolean result = true;

		try {
			result = NoteProxy.syncLocalDeletedNotesToRemote(mServiceUrl,
					sessionId, mDbAdapter);
		} catch (Exception ex) {
			Log.i(Config.LOG_TAG, "syncLocalDeletedNotesToRemote exception: "
					+ ex.getMessage());
			sendMessage(new CmdMessage("Message",
					"sync local deleted notes to remote error."));
			result = false;
		}

		return result;
	}

	private boolean updateRemoteNotes(String sessionId) {
		boolean result = true;

		try {
			result = NoteProxy.updateRemoteNotes(mServiceUrl, sessionId,
					mDbAdapter);
		} catch (Exception ex) {
			Log.i(Config.LOG_TAG,
					"updateRemoteNotes exception: " + ex.getMessage());
			sendMessage(new CmdMessage("Message", "Update remote notes error."));
			result = false;
		}

		return result;
	}

	private boolean downloadCategories(String sessionId) {
		boolean result = true;

		// -- get categories -----------------------------------------------
		JSONArray categories = null;
		try {
			categories = NoteProxy.getCategories(mServiceUrl, sessionId);
		} catch (Exception ex) {
			Log.i(Config.LOG_TAG,
					"downloadCategories exception: " + ex.getMessage());
			sendMessage(new CmdMessage("Message", "Download categories error."));
			result = false;
		}
		if (categories == null) {
			sendMessage(new CmdMessage("Message", "Download categories error."));
			result = false;
		}

		// -- save categories to local DB ----------------------------------
		try {
			for (int i = 0; i < categories.length(); i++) {
				JSONObject category = categories.getJSONObject(i);
				mDbAdapter
						.insertCategory(
								category.getString(DbOpenHelper.TABLE_CATEGORIES_ID),
								category.getString(DbOpenHelper.TABLE_CATEGORIES_NAME),
								category.getBoolean(DbOpenHelper.TABLE_CATEGORIES_IS_DEFAULT) ? 1
										: 0,
								category.getString(DbOpenHelper.TABLE_CATEGORIES_PERMISSION));
			}
		} catch (Exception ex) {
			Log.i(Config.LOG_TAG,
					"insertCategoryToDB exception: " + ex.getMessage());
			sendMessage(new CmdMessage("Message", "Insert categories error."));
			result = false;
		}

		return result;
	}

	private boolean downloadNotes(String sessionId) {
		boolean result = true;

		Cursor cursor = mDbAdapter.fetchAllCategories();
		while (cursor.moveToNext()) {
			String categoryId = cursor.getString(cursor
					.getColumnIndexOrThrow(DbOpenHelper.TABLE_CATEGORIES_ID));

			// -- get notes -----------------------------------------------
			JSONArray notes = null;
			try {
				notes = NoteProxy.getNotesByCategory(mServiceUrl, sessionId,
						categoryId);
			} catch (Exception ex) {
				Log.i(Config.LOG_TAG,
						"getNotesByCategory exception: " + ex.getMessage());
				sendMessage(new CmdMessage("Message", "Download notes error."));
				result = false;
				break;
			}
			if (notes == null) {
				sendMessage(new CmdMessage("Message", "Download notes error."));
				result = false;
				break;
			}

			// -- save notes to local DB ----------------------------------
			try {
				for (int i = 0; i < notes.length(); i++) {
					JSONObject note = notes.getJSONObject(i);
					mDbAdapter
							.insertNote(
									note.getString(DbOpenHelper.TABLE_NOTES_ID),
									note.getString(DbOpenHelper.TABLE_NOTES_CATEGORY_ID),
									note.getString(DbOpenHelper.TABLE_NOTES_NOTE),
									note.getInt(DbOpenHelper.TABLE_NOTES_COLOR),
									note.getInt(DbOpenHelper.TABLE_NOTES_Z_INDEX),
									0,
									note.getString(DbOpenHelper.TABLE_NOTES_LAST_UPDATED));
				}
			} catch (Exception ex) {
				Log.i(Config.LOG_TAG,
						"insertNoteToDB exception: " + ex.getMessage());
				sendMessage(new CmdMessage("Message", "Insert notes error."));
				result = false;
				break;
			}
		}
		cursor.close();

		return result;
	}

	// == implement SimpleCursorAdapter.ViewBinder
	// =============================================================================
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		String note = cursor.getString(columnIndex);
		int color = cursor.getInt(cursor
				.getColumnIndexOrThrow(DbOpenHelper.TABLE_NOTES_COLOR));

		String[] noteValues = note.split("\n");
		for (int i = 0; i < noteValues.length; i++) {
			if (!Utilities.isEmptyOrNull(noteValues[i])) {
				note = noteValues[i];
				break;
			}
		}

		if (note.length() > 30) {
			note = note.substring(0, 28) + "...";
		}

		((TextView) view).setBackgroundColor(Helpers.getColorValue(color));
		((TextView) view).setText(note);

		return true;
	}

	// == populate data
	// ==============================================================================

	private void populateDataBeforeViewFlipping() {
		loadCategoryInfo();

		ListView listView = mViewFlipper.getDisplayedChild() == 0 ? mListView2
				: mListView1;
		Button categoryButton = mViewFlipper.getDisplayedChild() == 0 ? mCategoryButton2
				: mCategoryButton1;

		showNotes(listView);
		showCategoryInfo(categoryButton);
	}

	private void populateDataAfterPageLoaded() {
		loadDefaultCategoryInfo();
		showNotes(mListView1);
		showCategoryInfo(mCategoryButton1);
	}

	private void populateDataAfterSync() {
		loadDefaultCategoryInfo();
		showNotes(mListView1);
		showNotes(mListView2);
		showCategoryInfo(mCategoryButton1);
		showCategoryInfo(mCategoryButton2);
	}

	private void populateDataReturnFromNoteEdit(String categoryId) {
		loadSpecificCategoryInfo(categoryId);

		ListView listView = mViewFlipper.getDisplayedChild() == 0 ? mListView1
				: mListView2;
		Button categoryButton = mViewFlipper.getDisplayedChild() == 0 ? mCategoryButton1
				: mCategoryButton2;

		showNotes(listView);
		showCategoryInfo(categoryButton);
	}

	private void loadDefaultCategoryInfo() {
		Category category = mDbAdapter.fetchDefaultCategory();
		setCategoryInfo(category);
	}

	private void loadCategoryInfo() {
		Category category = mDbAdapter
				.fetchCategoryByIndex(mCurrentCategoryIndex);
		setCategoryInfo(category);
	}

	private void loadSpecificCategoryInfo(String categoryId) {
		Category category = mDbAdapter.fetchCategoryById(categoryId);
		setCategoryInfo(category);
	}

	private void setCategoryInfo(Category category) {
		if (category != null) {
			mCurrentCategoryIndex = category.getIndex();
			mCurrentCategoryId = category.getId();
			mCurrentCategoryName = category.getName();
			mCurrentCategoryPermission = category.getPermission();
		} else {
			mCurrentCategoryIndex = -1;
		}
	}

	// == helpers
	// =============================================================================

	private void sendMessage(CmdMessage msg) {
		Message message = mMainHandler.obtainMessage();
		message.obj = msg;
		mMainHandler.sendMessage(message);
	}

	private void goToAboutActivity() {
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}

	private void goToSettingsActivity() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	private void goToNoteActivity(Long id) {
		Intent intent = new Intent(this, NoteActivity.class);

		if (id != null) {
			intent.putExtra(DbOpenHelper.TABLE_NOTES_ROWID, id);
		}
		intent.putExtra(DbOpenHelper.TABLE_CATEGORIES_ID, mCurrentCategoryId);
		intent.putExtra(DbOpenHelper.TABLE_CATEGORIES_NAME,
				mCurrentCategoryName);
		intent.putExtra(DbOpenHelper.TABLE_CATEGORIES_PERMISSION,
				mCurrentCategoryPermission);

		startActivityForResult(intent, ACTIVITY_NOTE_EDIT);
	}

	private void getSettings() {
		Cursor settingsCursor = mDbAdapter.fetchSettings();
		settingsCursor.moveToFirst();
		startManagingCursor(settingsCursor);
		mServiceUrl = settingsCursor.getString(settingsCursor
				.getColumnIndexOrThrow(DbOpenHelper.TABLE_SETTINGS_SERVICE));
		mUserId = settingsCursor.getString(settingsCursor
				.getColumnIndexOrThrow(DbOpenHelper.TABLE_SETTINGS_USER_ID));
		mUserPwd = settingsCursor.getString(settingsCursor
				.getColumnIndexOrThrow(DbOpenHelper.TABLE_SETTINGS_USER_PWD));
	}

	private void showNotes(ListView listView) {
		if (mCurrentCategoryIndex != -1) {
			Cursor notesCursor = mDbAdapter
					.fetchNotesByCategory(mCurrentCategoryId);
			notesCursor.moveToFirst();
			startManagingCursor(notesCursor);

			String[] from = new String[] { DbOpenHelper.TABLE_NOTES_NOTE };
			int[] to = new int[] { R.id.main_notes_list_item };

			SimpleCursorAdapter notesAdapter = new SimpleCursorAdapter(this,
					R.layout.main_list_row, notesCursor, from, to);

			notesAdapter.setViewBinder(this); // this.setViewValue()

			listView.setAdapter(notesAdapter);
		}
	}

	private void showCategoryInfo(Button categoryButton) {
		if (mCurrentCategoryIndex != -1) {
			String categoryText = mCurrentCategoryName;

			if (mCurrentCategoryPermission.equals("owner")
					|| mCurrentCategoryPermission.equals("rw")) {
				categoryText = categoryText + " [+]";
			}

			categoryButton.setText(categoryText);
		}
	}
}
