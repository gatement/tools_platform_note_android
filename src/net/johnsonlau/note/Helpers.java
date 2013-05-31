package net.johnsonlau.note;

public class Helpers {
	public static final int getColorValue(int color) {
		int result = 0xffd5f0fa;

		switch (color) {
		case 1:
			result = 0xffd5f0fa;
			break;
		case 2:
			result = 0xffcdfbc7;
			break;
		case 3:
			result = 0xfff4cff4;
			break;
		case 4:
			result = 0xffdbd7fe;
			break;
		case 5:
			result = 0xfffdfdfd;
			break;
		case 6:
			result = 0xfffdfdc8;
			break;
		}

		return result;
	}
}
