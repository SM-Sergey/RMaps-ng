package com.sm.maps.applib.kml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.CheckBox;

import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;

import com.sm.maps.applib.R;
import com.sm.maps.applib.kml.XMLparser.SimpleXML;
import com.sm.maps.applib.kml.constants.PoiConstants;
import com.sm.maps.applib.utils.CoordFormatter;
import com.sm.maps.applib.utils.Ut;

public class PoiListActivity extends ListActivity {
	private PoiManager mPoiManager;
	private ProgressDialog dlgWait;
	private String mSortOrder;
	private SimpleCursorAdapter.ViewBinder mViewBinder;

	private SparseBooleanArray mArr = new SparseBooleanArray();
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

		AppCompatDelegate delegate = AppCompatDelegate.create(this, callback);

		delegate.onCreate(savedInstanceState);
		delegate.setContentView(R.layout.poi_list);

		setTitle(R.string.menu_poilist);

		registerForContextMenu(getListView());
		mPoiManager = new PoiManager(this);
		mSortOrder = "lat asc, lon asc";
		mViewBinder = new PoiViewBinder();
	}

	@Override
	protected void onDestroy() {
		mPoiManager.FreeDatabases();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString("sortOrder", mSortOrder);
		editor.commit();
		super.onPause();
	}

	@Override
	protected void onResume() {
		final SharedPreferences uiState = getPreferences(Activity.MODE_PRIVATE);
		mSortOrder = uiState.getString("sortOrder", mSortOrder);

		FillData();
		super.onResume();
	}

	private void FillData() {
		mCursor = mPoiManager.getGeoDatabase().getPoiListCursor(mSortOrder);
		startManagingCursor(mCursor);

		ListAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.poilist_item, mCursor,
				new String[]{"name", "iconid", "catname", "descr", "lat"},
				new int[]{R.id.title1, R.id.pic, R.id.title2, R.id.descr, R.id.checkbox});
		((SimpleCursorAdapter) adapter).setViewBinder(mViewBinder);
		setListAdapter(adapter);
	}

	private class PoiViewBinder implements SimpleCursorAdapter.ViewBinder {
		private static final String CATNAME = "catname";
		private static final String LAT = "lat";
		private static final String LON = "lon";
		private CoordFormatter mCf = new CoordFormatter(PoiListActivity.this.getApplicationContext());

		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (cursor.getColumnName(columnIndex).equalsIgnoreCase(CATNAME)) {
				((TextView) view.findViewById(R.id.title2)).setText(cursor.getString(cursor.getColumnIndex(CATNAME))
						+ ", " + mCf.convertLat(cursor.getDouble(cursor.getColumnIndex(LAT)))
						+ ", " + mCf.convertLon(cursor.getDouble(cursor.getColumnIndex(LON)))
						+ ((cursor.getInt(cursor.getColumnIndex("hidden")) == 1) ? ", X" : "")
				);
				return true;
			}

			if (view.getId() == R.id.pic) {
				ImageView v = (ImageView) view;
				int id = cursor.getInt(columnIndex);
				v.setImageResource(Ut.IconResId(id));
				return true;
			}

			if (view.getId() == R.id.checkbox) {
				((CheckBox) view).setChecked(mArr.get(cursor.getPosition()));
				return true;
			}

			return false;
		}

	}


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		final ViewGroup row = (ViewGroup) v;
		final CheckBox ch = (CheckBox) row.findViewById(R.id.checkbox);
		ch.toggle();

		mArr.put(position, ch.isChecked());

	}

	private boolean isSelected() {
		int sz = mArr.size();
		int i;
		for (i = 0; i < sz; i++) {
			if (mArr.valueAt(i))
				return true;
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.poilist_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		if (item.getItemId() == R.id.menu_addpoi) {
			final Intent PoiIntent = new Intent(this, PoiActivity.class);
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
				PoiIntent.putExtra("lat", extras.getDouble("lat")).putExtra("lon", extras.getDouble("lon")).putExtra("title", extras.getString("title"));
			}
			startActivity(PoiIntent);
			return true;
		} else if (item.getItemId() == R.id.menu_categorylist) {
			startActivity((new Intent(this, PoiCategoryListActivity.class).putExtra("usage","edit")));
			return true;
		} else if (item.getItemId() == R.id.menu_importpoi) {
			startActivity((new Intent(this, ImportPoiActivity.class)));
			return true;
		} else if (item.getItemId() == R.id.menu_delete) {
			showDialog(R.id.menu_delete);
			return true;
		} else if (item.getItemId() == R.id.menu_exportgpx) {
			DoExportGpx();
			return true;
		} else if (item.getItemId() == R.id.menu_exportkml) {
			DoExportKml();

		} else if (item.getItemId() == R.id.menu_sort_name) {
			if (mSortOrder.contains("points.name")) {
				if (mSortOrder.contains("asc"))
					mSortOrder = "points.name desc";
				else
					mSortOrder = "points.name asc";
			} else {
				mSortOrder = "points.name asc";
			}
			((SimpleCursorAdapter) getListAdapter()).changeCursor(mPoiManager.getGeoDatabase().getPoiListCursor(mSortOrder));

		} else if (item.getItemId() == R.id.menu_sort_category) {
			if (mSortOrder.contains("category.name")) {
				if (mSortOrder.contains("asc"))
					mSortOrder = "category.name desc";
				else
					mSortOrder = "category.name asc";
			} else {
				mSortOrder = "category.name asc";
			}
			((SimpleCursorAdapter) getListAdapter()).changeCursor(mPoiManager.getGeoDatabase().getPoiListCursor(mSortOrder));

		} else if (item.getItemId() == R.id.menu_sort_coord) {
			if (mSortOrder.contains("lat")) {
				if (mSortOrder.contains("asc"))
					mSortOrder = "lat desc, lon desc";
				else
					mSortOrder = "lat asc, lon asc";
			} else {
				mSortOrder = "lat, lon asc";
			}
			((SimpleCursorAdapter) getListAdapter()).changeCursor(mPoiManager.getGeoDatabase().getPoiListCursor(mSortOrder));
		}

		return true;
	}

	private void DoExportKml() {
		dlgWait = Ut.ShowWaitDialog(this, 0);

		new ExportKmlTask().execute();
	}

	class ExportKmlTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			SimpleXML xml = new SimpleXML("kml");
			xml.setAttr("xmlns:gx", "http://www.google.com/kml/ext/2.2");
			xml.setAttr("xmlns", "http://www.opengis.net/kml/2.2");
			SimpleXML fold = xml.createChild("Folder");

			Cursor c = mPoiManager.getGeoDatabase().getPoiListCursor();
			PoiPoint poi = null;

			if (c != null) {
				if (c.moveToFirst()) {
					do {
						poi = mPoiManager.getPoiPoint(c.getInt(4));

						SimpleXML wpt = fold.createChild("Placemark");
						wpt.createChild(PoiConstants.NAME).setText(poi.Title);
						wpt.createChild(PoiConstants.DESCRIPTION).setText(poi.Descr);
						SimpleXML point = wpt.createChild("Point");
						point.createChild("coordinates").setText(new StringBuilder().append(poi.GeoPoint.getLongitude()).append(",").append(poi.GeoPoint.getLatitude()).toString());
						SimpleXML ext = wpt.createChild("ExtendedData");
						SimpleXML category = ext.createChild(PoiConstants.CATEGORYID);
						final PoiCategory poiCat = mPoiManager.getPoiCategory(poi.CategoryId);
						category.setAttr(PoiConstants.CATEGORYID, Integer.toString(poiCat.getId()));
						category.setAttr(PoiConstants.NAME, poiCat.Title);
						category.setAttr(PoiConstants.ICONID, Integer.toString(poiCat.IconId));

					} while (c.moveToNext());
				}
				;
				c.close();
			}

			File folder = Ut.getRMapsExportDir(PoiListActivity.this);
			String filename = folder.getAbsolutePath() + "/poilist.kml";
			File file = new File(filename);
			FileOutputStream out;
			try {
				file.createNewFile();
				out = new FileOutputStream(file);
				OutputStreamWriter wr = new OutputStreamWriter(out);
				wr.write(SimpleXML.saveXml(xml));
				wr.close();
				return PoiListActivity.this.getResources().getString(R.string.message_poiexported, filename);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return PoiListActivity.this.getResources().getString(R.string.message_error, e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				return PoiListActivity.this.getResources().getString(R.string.message_error, e.getMessage());
			}

		}

		@Override
		protected void onPostExecute(String result) {
			dlgWait.dismiss();
			Toast.makeText(PoiListActivity.this, result, Toast.LENGTH_LONG).show();
			super.onPostExecute(result);
		}

	}

	private void DoExportGpx() {
		dlgWait = Ut.ShowWaitDialog(this, 0);

		new ExportGpxTask().execute();
	}

	class ExportGpxTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			//SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			SimpleXML xml = new SimpleXML("gpx");
			xml.setAttr("xsi:schemaLocation", "http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd");
			xml.setAttr("xmlns", "http://www.topografix.com/GPX/1/0");
			xml.setAttr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			xml.setAttr("creator", "RMaps-ng");
			xml.setAttr("version", "1.0");

			Cursor c = mPoiManager.getGeoDatabase().getPoiListCursor();
			PoiPoint poi = null;

			if (c != null) {
				if (c.moveToFirst()) {
					do {
						poi = mPoiManager.getPoiPoint(c.getInt(4));

						SimpleXML wpt = xml.createChild("wpt");
						wpt.setAttr(PoiConstants.LAT, Double.toString(poi.GeoPoint.getLatitude()));
						wpt.setAttr(PoiConstants.LON, Double.toString(poi.GeoPoint.getLongitude()));
						wpt.createChild(PoiConstants.ELE).setText(Double.toString(poi.Alt));
						wpt.createChild(PoiConstants.NAME).setText(poi.Title);
						wpt.createChild(PoiConstants.DESC).setText(poi.Descr);
						wpt.createChild(PoiConstants.TYPE).setText(mPoiManager.getPoiCategory(poi.CategoryId).Title);
						SimpleXML ext = wpt.createChild("extensions");
						SimpleXML category = ext.createChild(PoiConstants.CATEGORYID);
						final PoiCategory poiCat = mPoiManager.getPoiCategory(poi.CategoryId);
						category.setAttr(PoiConstants.CATEGORYID, Integer.toString(poiCat.getId()));
						category.setAttr(PoiConstants.NAME, poiCat.Title);
						category.setAttr(PoiConstants.ICONID, Integer.toString(poiCat.IconId));

					} while (c.moveToNext());
				}
				;
				c.close();
			}

			File folder = Ut.getRMapsExportDir(PoiListActivity.this);
			String filename = folder.getAbsolutePath() + "/poilist.gpx";
			File file = new File(filename);
			FileOutputStream out;
			try {
				file.createNewFile();
				out = new FileOutputStream(file);
				OutputStreamWriter wr = new OutputStreamWriter(out);
				wr.write(SimpleXML.saveXml(xml));
				wr.close();
				return PoiListActivity.this.getResources().getString(R.string.message_poiexported, filename);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return PoiListActivity.this.getResources().getString(R.string.message_error, e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				return PoiListActivity.this.getResources().getString(R.string.message_error, e.getMessage());
			}
		}

		@Override
		protected void onPostExecute(String result) {
			dlgWait.dismiss();
			Toast.makeText(PoiListActivity.this, result, Toast.LENGTH_LONG).show();
			super.onPostExecute(result);
		}

	}

	;

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == R.id.menu_delete) {
			return new AlertDialog.Builder(this)
					//.setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.warning_delete_all_poi)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									mPoiManager.DeleteAllPoi();
									((SimpleCursorAdapter) getListAdapter()).getCursor().requery();
								}
							}).setNegativeButton(android.R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

									/* User clicked Cancel so do some stuff */
								}
							}).create();
		}
		;

		return super.onCreateDialog(id);
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
									ContextMenuInfo menuInfo) {
		int pointid = (int) ((AdapterView.AdapterContextMenuInfo) menuInfo).id;
		PoiPoint poi = mPoiManager.getPoiPoint(pointid);

		final boolean fSel = isSelected();

		if (!fSel) {
			menu.add(0, R.id.menu_gotopoi, 0, getText(R.string.menu_goto));
			menu.add(0, R.id.menu_editpoi, 0, getText(R.string.menu_edit));
			if (poi.Hidden)
				menu.add(0, R.id.menu_show, 0, getText(R.string.menu_show));
			else
				menu.add(0, R.id.menu_hide, 0, getText(R.string.menu_hide));
		}
		menu.add(0, R.id.menu_setcat, 0, getText(R.string.menu_setcat));
		menu.add(0, R.id.menu_deletepoi, 0, getText(R.string.menu_delete));
		if (!fSel) {
			menu.add(0, R.id.menu_share, 0, getText(R.string.menu_share));
			menu.add(0, R.id.menu_toradar, 0, getText(R.string.menu_toradar));
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int pointid = (int) ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;
		PoiPoint poi = mPoiManager.getPoiPoint(pointid);

		if (item.getItemId() == R.id.menu_setcat) {
			startActivityForResult(new Intent(this, PoiCategoryListActivity.class)
					.putExtra("pointid", pointid)
					.putExtra("usage", "select"), 1);
		} else if (item.getItemId() == R.id.menu_editpoi) {
			startActivity((new Intent(this, PoiActivity.class)).putExtra("pointid", pointid));
		} else if (item.getItemId() == R.id.menu_gotopoi) {
			setResult(RESULT_OK, (new Intent()).putExtra("pointid", pointid));
			finish();
		} else if (item.getItemId() == R.id.menu_deletepoi) {
			new AlertDialog.Builder(this)
					.setTitle(R.string.app_name)
					.setMessage(getResources().getString(R.string.question_delete, getText(R.string.poi)))
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if (!isSelected()) {
								mPoiManager.deletePoi(pointid);
							} else {
								int sz = mArr.size();
								int i;
								int _pointid;
								for (i = 0; i < sz; i++) {
									if (mArr.valueAt(i)) {
										mCursor.moveToPosition(mArr.keyAt(i));
										mPoiManager.deletePoi(mCursor.getInt(mCursor.getColumnIndex("_id")));
									}
								}

							}
							mArr.clear();
							((SimpleCursorAdapter) getListAdapter()).getCursor().requery();
						}
					}).setNegativeButton(R.string.no, null).create().show();

		} else if (item.getItemId() == R.id.menu_hide) {
			poi.Hidden = true;
			mPoiManager.updatePoi(poi);
			((SimpleCursorAdapter) getListAdapter()).getCursor().requery();
		} else if (item.getItemId() == R.id.menu_show) {
			poi.Hidden = false;
			mPoiManager.updatePoi(poi);
			((SimpleCursorAdapter) getListAdapter()).getCursor().requery();
		} else if (item.getItemId() == R.id.menu_share) {
			try {
				Intent intent1 = new Intent(Intent.ACTION_SEND);
				intent1.setType("text/plain");
				intent1.putExtra(Intent.EXTRA_TEXT, new StringBuilder()
						.append(poi.Title)
						.append("\nhttp://maps.google.com/?q=")
						.append(poi.GeoPoint.toDoubleString())
						.toString());
				startActivity(Intent.createChooser(intent1, getText(R.string.menu_share)));
			} catch (Exception e) {
			}
		} else if (item.getItemId() == R.id.menu_toradar) {
			try {
				Intent i = new Intent("com.google.android.radar.SHOW_RADAR");
				i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				i.putExtra("name", poi.Title);
				i.putExtra("latitude", (float) (poi.GeoPoint.getLatitudeE6() / 1000000f));
				i.putExtra("longitude", (float) (poi.GeoPoint.getLongitudeE6() / 1000000f));
				startActivity(i);
			} catch (Exception e) {
				Toast.makeText(this, R.string.message_noradar, Toast.LENGTH_LONG).show();
			}
		}

		return super.onContextItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && resultCode == RESULT_OK) {
			int catid = data.getIntExtra("catid", 0);
			int iconid = data.getIntExtra("iconid", 0);
			int pointid =  data.getIntExtra("pointid", 0);
			if (!isSelected()) {
				PoiPoint poi = mPoiManager.getPoiPoint(pointid);
				poi.IconId = iconid;
				poi.CategoryId = catid;
				mPoiManager.updatePoi(poi);
			} else {
				int sz = mArr.size();
				int i;
				for (i = 0; i < sz; i++) {
					if (mArr.valueAt(i)) {
						mCursor.moveToPosition(mArr.keyAt(i));
						PoiPoint poi = mPoiManager.getPoiPoint(mCursor.getInt(mCursor.getColumnIndex("_id")));
						poi.IconId = iconid;
						poi.CategoryId = catid;
						mPoiManager.updatePoi(poi);
					}
				}

			}
			mArr.clear();
			((SimpleCursorAdapter) getListAdapter()).getCursor().requery();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}