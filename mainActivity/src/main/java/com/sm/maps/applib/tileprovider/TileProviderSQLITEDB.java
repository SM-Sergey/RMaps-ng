package com.sm.maps.applib.tileprovider;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Message;

import com.sm.maps.applib.R;
import com.sm.maps.applib.utils.RException;
import com.sm.maps.applib.utils.SQLiteMapDatabase;
import com.sm.maps.applib.utils.SimpleThreadFactory;
import com.sm.maps.applib.utils.Ut;

public class TileProviderSQLITEDB extends TileProviderFileBase {
	private SQLiteMapDatabase mUserMapDatabase;
	private String mMapID;
	private ProgressDialog mProgressDialog;

	public TileProviderSQLITEDB(Context ctx, final String filename, final String mapid, MapTileMemCache aTileCache, TileSource tileSource) throws SQLiteException, RException {
		super(ctx, tileSource);
		mTileURLGenerator = new TileURLGeneratorBase(filename);
		mTileCache = aTileCache == null ? new MapTileMemCache() : aTileCache;
		mUserMapDatabase = new SQLiteMapDatabase();
		mUserMapDatabase.setFile(filename);
		mMapID = mapid;

		final File file = new File(filename);
		Ut.d("TileProviderSQLITEDB: mapid = " + mapid);
		Ut.d("TileProviderSQLITEDB: filename = " + filename);
		Ut.d("TileProviderSQLITEDB: file.exists = " + file.exists());
		Ut.d("TileProviderSQLITEDB: getRMapsMapsDir = " + Ut.getRMapsMapsDir(ctx));
		if (needIndex(mapid, file.length(), file.lastModified(), false)) {
			mProgressDialog = Ut.ShowWaitDialog(ctx, R.string.message_updateminmax);
			new IndexTask().execute(file.length(), file.lastModified());
		}
	}

	public void updateMapParams(TileSource tileSource) {
		tileSource.ZOOM_MINLEVEL = ZoomMinInCashFile(mMapID);
		tileSource.ZOOM_MAXDNLD = ZoomMaxInCashFile(mMapID);
		tileSource.ZOOM_MAXLEVEL = Math.min(19, tileSource.ZOOM_MAXDNLD + tileSource.mPrevZCached);
	}
	
	private class IndexTask extends AsyncTask<Long, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Long... params) {
			try {
				final long fileLength = params[0];
				Ut.d("IndexTask: fileLength = "+fileLength);
				final long fileModified = params[1];
				Ut.d("IndexTask: fileModified = "+fileModified);
				mUserMapDatabase.updateMinMaxZoom();
				Ut.d("IndexTask: mUserMapDatabase.updateMinMaxZoom = OK");
				final int minzoom = mUserMapDatabase.getMinZoom();
				Ut.d("IndexTask: minzoom = "+minzoom);
				final int maxzoom = mUserMapDatabase.getMaxZoom();
				Ut.d("IndexTask: maxzoom = "+maxzoom);

				CommitIndex(mMapID, fileLength, fileModified, minzoom, maxzoom);
			} catch (Exception e) {
				e.printStackTrace();
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(result && mCallbackHandler != null)
				Message.obtain(mCallbackHandler, MessageHandlerConstants.MAPTILEFSLOADER_INDEXIND_SUCCESS_ID).sendToTarget();
			try {
				if(mProgressDialog != null)
					mProgressDialog.dismiss();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected byte[] getSingleTile(int x, int y, int z) {
		byte [] data = null;
		try {
			data = mUserMapDatabase.getTile(x, y, z);
		} catch (Exception e) {
		}
		return data;
	}

	@Override
	public void Free() {
		super.Free();
		mUserMapDatabase.Free();
	}

	public Bitmap getTile(final int x, final int y, final int z) {
		return getTileFromSource(x, y, z);
	}

	public int[] findTheMap(int zoomLevel) {
		return mUserMapDatabase.findTheMap(zoomLevel);
	}

}
