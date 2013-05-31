package net.johnsonlau.note;

public class Config {
	public static final String LOG_TAG = "johnsonnote";	

	public static final String SESSION_COOKIE_NAME = "usr_sid";
	
	public static final String URL_SESSION_CREATE = "/user/session";	

	public static final String URL_NOTE_CATEGORY_LIST = "/note/category/list";	
	public static final String URL_NOTE_LIST = "/note/note/list";	
	public static final String URL_NOTE_UPDATE_NOTE = "/note/update";
	public static final String URL_NOTE_DELETE_NOTES = "/note/deleteNotes";

	public static final int DEFAULT_NOTE_COLOR = 6;	
	public static final int DEFAULT_NOTE_Z_INDEX = 1000;	
}
