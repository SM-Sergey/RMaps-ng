package com.sm.maps.applib.kml;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.ImageView;

import com.sm.maps.applib.utils.Ut;

import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;

import com.sm.maps.applib.R;

public class PoiCategorySelectListActivity extends ListActivity {
	private PoiManager mPoiManager;
	private Cursor mCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AppCompatCallback callback = new AppCompatCallback() {
			@Override
			public void onSupportActionModeStarted(ActionMode actionMode) {
			}

			@Override
			public void onSupportActionModeFinished(ActionMode actionMode) {
			}
		};

		AppCompatDelegate delegate = AppCompatDelegate.create(this,callback);

		delegate.onCreate(savedInstanceState);
		delegate.setContentView(R.layout.poicategory_list);

        mPoiManager = new PoiManager(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPoiManager.FreeDatabases();
	}

	@Override
	protected void onResume() {
		FillData();
		super.onResume();
	}

	private void FillData() {
		mCursor = mPoiManager.getGeoDatabase().getPoiCategoryListCursor();
        startManagingCursor(mCursor);

        ListAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.poicategoryselectlist_item, mCursor,
                        new String[] { "name", "iconid"}, 
                        new int[] { R.id.title1, R.id.pic });
        ((SimpleCursorAdapter) adapter).setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (view.getId() == R.id.pic) {
					ImageView v = (ImageView) view;
					int id = cursor.getInt(columnIndex);
					v.setImageResource(Ut.IconResId(id));
					return true;
				}
				return false;
			}
		});
        setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		mCursor.moveToPosition(position);
		int catid = mCursor.getInt(mCursor.getColumnIndex("_id"));
		int iconId = mCursor.getInt(mCursor.getColumnIndex("iconid"));
		int pointid = getIntent().getIntExtra("pointid",0);

		setResult(RESULT_OK, (new Intent()).putExtra("iconid", iconId)
				                           .putExtra("pointid", pointid)
				                           .putExtra("catid", catid));
		finish();

	}


}
