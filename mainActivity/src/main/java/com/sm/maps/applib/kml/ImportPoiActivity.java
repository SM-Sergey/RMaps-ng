package com.sm.maps.applib.kml;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.openintents.filemanager.FileManagerActivity;
import org.openintents.filemanager.intents.FileManagerIntents;
import org.openintents.filemanager.util.FileUtils;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import com.sm.maps.applib.R;
import com.sm.maps.applib.kml.XMLparser.GpxPoiParser;
import com.sm.maps.applib.kml.XMLparser.KmlPoiParser;
import com.sm.maps.applib.utils.SimpleThreadFactory;
import com.sm.maps.applib.utils.Ut;

public class ImportPoiActivity extends AppCompatActivity {
	EditText mFileName;
	Spinner mSpinner;
	private PoiManager mPoiManager;

	private ProgressDialog dlgWait;
	protected ExecutorService mThreadPool = Executors.newSingleThreadExecutor(new SimpleThreadFactory("ImportPoi"));

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
		this.setContentView(R.layout.importpoi);

		if (mPoiManager == null)
			mPoiManager = new PoiManager(this);

		mFileName = (EditText) findViewById(R.id.FileName);
		mFileName.setText(settings.getString("IMPORT_POI_FILENAME", Ut.getRMapsImportDir(this).getAbsolutePath()));

		mSpinner = (Spinner) findViewById(R.id.spinnerCategory);
		Cursor c = mPoiManager.getGeoDatabase().getPoiCategoryListCursor();
		startManagingCursor(c);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, c,
				new String[] { "name" }, new int[] { android.R.id.text1 });
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);

		((Button) findViewById(R.id.SelectFileBtn))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doSelectFile();
			}
		});
		((Button) findViewById(R.id.ImportBtn))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doImportPOI();
			}
		});
		((Button) findViewById(R.id.discardButton))
		.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ImportPoiActivity.this.finish();
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == R.id.dialog_wait) {
			dlgWait = new ProgressDialog(this);
			dlgWait.setMessage("Please wait while loading...");
			dlgWait.setIndeterminate(true);
			dlgWait.setCancelable(false);
			return dlgWait;
		}
		return null;
	}

	protected void doSelectFile() {
		Intent intent = new Intent(this, FileManagerActivity.class);
		intent.setAction(FileManagerIntents.ACTION_PICK_FILE);
		intent.setData(Uri.parse(mFileName.getText().toString()));
		startActivityForResult(intent, R.id.ImportBtn & 0xFFFF);
/*
//		Intent intent = new Intent("org.openintents.action.PICK_FILE");
//		startActivityForResult(intent, 1);
//
		String fileName = mFileName.getText().toString();

		Intent intent = new Intent("org.openintents.action.PICK_FILE");

		// Construct URI from file name.
		intent.setData(Uri.parse("file://" + fileName));

		// Set fancy title and button (optional)
//		intent.putExtra(FileManagerIntents.EXTRA_TITLE, getString(R.string.open_title));
//		intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getString(R.string.open_button));

		try {
			startActivityForResult(intent, R.id.ImportBtn);
		} catch (ActivityNotFoundException e) {
			// No compatible file manager was found.
			Toast.makeText(this, "No compatible file manager found",
					Toast.LENGTH_SHORT).show();
		}
*/
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == (R.id.ImportBtn & 0xFFFF)) {
			if (resultCode == RESULT_OK && data != null) {
				// obtain the filename
				String filename = Uri.decode(data.getDataString());
				if (filename != null) {
					// Get rid of URI prefix:
					if (filename.startsWith("file://")) {
						filename = filename.substring(7);
					}

					mFileName.setText(filename);
				}

			}
		}
	}

	private void doImportPOI() {
		File file = new File(mFileName.getText().toString());

		if(!file.exists()){
			Toast.makeText(this, "No such file", Toast.LENGTH_LONG).show();
			return;
		}

		showDialog(R.id.dialog_wait);

		this.mThreadPool.execute(new Runnable() {
			public void run() {
				int CategoryId = (int)mSpinner.getSelectedItemId();
				File file = new File(mFileName.getText().toString());

				SAXParserFactory fac = SAXParserFactory.newInstance();
				SAXParser parser = null;
				try {
					parser = fac.newSAXParser();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if(parser != null){
					mPoiManager.beginTransaction();
					Ut.d("Start parsing file " + file.getName());
					try {
						if(FileUtils.getExtension(file.getName()).equalsIgnoreCase(".kml"))
							parser.parse(file, new KmlPoiParser(mPoiManager, CategoryId));
						else if(FileUtils.getExtension(file.getName()).equalsIgnoreCase(".gpx"))
							parser.parse(file, new GpxPoiParser(mPoiManager, CategoryId));

						mPoiManager.commitTransaction();
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						mPoiManager.rollbackTransaction();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						mPoiManager.rollbackTransaction();
					} catch (IllegalStateException e) {
					} catch (OutOfMemoryError e) {
						Ut.w("OutOfMemoryError");
						mPoiManager.rollbackTransaction();
					}
					Ut.d("Pois commited");
				}



				dlgWait.dismiss();
				ImportPoiActivity.this.finish();
			};
		});

	}


	@Override
	protected void onDestroy() {
		mThreadPool.shutdown();
		super.onDestroy();
		mPoiManager.FreeDatabases();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("IMPORT_POI_FILENAME", mFileName.toString());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		SharedPreferences uiState = getPreferences(0);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString("IMPORT_POI_FILENAME", mFileName.getText().toString());
		editor.commit();
		super.onPause();
	}

}
