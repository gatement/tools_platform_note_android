package net.johnsonlau.note.activity;

import net.johnsonlau.note.Config;
import net.johnsonlau.note.Helpers;
import net.johnsonlau.note.R;
import net.johnsonlau.note.db.DbAdapter;
import net.johnsonlau.note.db.DbOpenHelper;
import net.johnsonlau.tool.Utilities;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class NoteActivity extends Activity {

	private static final int MENU_ID_EDIT_SAVE = Menu.FIRST;
	private static final int MENU_ID_DELETE = Menu.FIRST + 1;

	private DbAdapter mDbAdapter;
	private MenuItem mEditSaveMenuItem;

	private Long mRowId = null;
	private String mCategoryId = null;
	private String mCategoryName = "Note";
	private String mCategoryPermission = "r";

	private EditText mNoteContent;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.note);

		getInputData(savedInstanceState);

		initMembers();

		setTitle();

		populateData();

		if (isCreating()) {
			setReadOnlyMode(false);
		} else {
			setReadOnlyMode(true);
		}
	}

	// == initialization methods
	// ===============================================================

	private void initMembers() {
		mDbAdapter = new DbAdapter(this).open();

		mNoteContent = (EditText) findViewById(R.id.note_content);
	}

	private void populateData() {
		if (!isCreating()) {
			try {
				Cursor noteCursor = mDbAdapter.fetchNote(mRowId);
				noteCursor.moveToFirst();

				String noteContent = noteCursor.getString(noteCursor
						.getColumnIndexOrThrow(DbOpenHelper.TABLE_NOTES_NOTE));
				int color = noteCursor.getInt(noteCursor
						.getColumnIndexOrThrow(DbOpenHelper.TABLE_NOTES_COLOR));

				noteCursor.close();

				mNoteContent.setText(noteContent);
				mNoteContent.setBackgroundColor(Helpers.getColorValue(color));
			} catch (SQLException ex) {
				Log.e(Config.LOG_TAG, "Load note error: " + ex.toString());
			}
		} else {
			mNoteContent.setBackgroundColor(Helpers
					.getColorValue(Config.DEFAULT_NOTE_COLOR));
		}
	}

	// == override methods
	// =====================================================================

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		if (mCategoryPermission.equals("owner")
				|| mCategoryPermission.equals("rw")) {
			int editSaveMenuItemId = R.string.note_menu_edit;
			if (isCreating()) {
				editSaveMenuItemId = R.string.note_menu_save;
			}
			mEditSaveMenuItem = menu.add(0, MENU_ID_EDIT_SAVE, 1,
					editSaveMenuItemId);

			menu.add(0, MENU_ID_DELETE, 2, R.string.note_menu_delete);
		}

		return true;
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		switch (item.getItemId()) {

		case MENU_ID_EDIT_SAVE:
			editSaveMenuItemClicked(item);
			return true;

		case MENU_ID_DELETE:
			deleteNote();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	protected void onPause() {
		super.onPause();

		saveNote();
	}

	protected void onDestroy() {
		super.onDestroy();

		mDbAdapter.close();
	}

	// == helpers
	// ============================================================================

	private void getInputData(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mRowId = (Long) savedInstanceState
					.getSerializable(DbOpenHelper.TABLE_NOTES_ROWID);
			mCategoryId = (String) savedInstanceState
					.getSerializable(DbOpenHelper.TABLE_CATEGORIES_ID);
			mCategoryName = (String) savedInstanceState
					.getSerializable(DbOpenHelper.TABLE_CATEGORIES_NAME);
			mCategoryPermission = (String) savedInstanceState
					.getSerializable(DbOpenHelper.TABLE_CATEGORIES_PERMISSION);
		} else {
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
				mRowId = extras.getLong(DbOpenHelper.TABLE_NOTES_ROWID);
				mCategoryId = extras
						.getString(DbOpenHelper.TABLE_CATEGORIES_ID);
				mCategoryName = extras
						.getString(DbOpenHelper.TABLE_CATEGORIES_NAME);
				mCategoryPermission = extras
						.getString(DbOpenHelper.TABLE_CATEGORIES_PERMISSION);
			}
		}
	}

	private void setTitle() {
		this.setTitle(mCategoryName);
	}

	private void setReadOnlyMode(boolean readOnlyMode) {
		if (readOnlyMode) {
			mNoteContent.setInputType(InputType.TYPE_NULL);
		} else {
			mNoteContent.setInputType(InputType.TYPE_CLASS_TEXT);
		}

		mNoteContent.setSingleLine(false);
	}

	private void goToMainActivity() {
		Intent resultData = new Intent();
		resultData.putExtra(DbOpenHelper.TABLE_CATEGORIES_ID, mCategoryId);
		setResult(Activity.RESULT_OK, resultData);
		finish();
	}

	private void editSaveMenuItemClicked(MenuItem item) {
		Resources resources = NoteActivity.this.getResources();
		if (item.getTitle()
				.equals(resources.getString(R.string.note_menu_edit))) {

			mEditSaveMenuItem.setTitle(resources
					.getString(R.string.note_menu_save));

			setReadOnlyMode(false);
		} else {
			saveNote();
			mEditSaveMenuItem.setTitle(resources
					.getString(R.string.note_menu_edit));

			setReadOnlyMode(true);
		}
	}

	private void saveNote() {
		String noteContent = mNoteContent.getText().toString().trim();
		if (!Utilities.isEmptyOrNull(noteContent)) {
			if (isCreating()) {
				long id = mDbAdapter.createNote(mCategoryId, noteContent);
				if (id > 0) {
					mRowId = id;
				}
			}
			// update
			else {
				mDbAdapter.updateNote(mRowId, noteContent);
			}
		}
	}

	private void deleteNote() {
		new AlertDialog.Builder(NoteActivity.this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage("Are you sure you want to delete this note?")
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								deleteNoteInner();
							}

						}).setNegativeButton("No", null).show();
	}

	private void deleteNoteInner() {
		if (!isCreating()) {
			mDbAdapter.insertDeletedNote(mRowId);
			mDbAdapter.deleteNote(mRowId);

			mNoteContent.setText("");
			mRowId = null;
		}

		goToMainActivity();
	}

	private boolean isCreating() {
		return mRowId == null || mRowId == 0;
	}
}