package com.sm.maps.applib.trackwriter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.sm.maps.applib.kml.constants.PoiConstants;
import com.sm.maps.applib.utils.RSQLiteOpenHelper;

public class DatabaseHelper extends RSQLiteOpenHelper {

	public DatabaseHelper(Context context, String name) {
		super(context, name, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(PoiConstants.SQL_CREATE_trackpoints);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

}
