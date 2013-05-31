package net.johnsonlau.note.model;

public class Category {
	private int mIndex;
	private String mId;
	private String mName;
	private String mPermission;

	public Category(int index, String id, String name, String permission) {
		mIndex = index;
		mId = id;
		mName = name;
		mPermission = permission;
	}

	public int getIndex() {
		return mIndex;
	}

	public String getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public String getPermission() {
		return mPermission;
	}
}
