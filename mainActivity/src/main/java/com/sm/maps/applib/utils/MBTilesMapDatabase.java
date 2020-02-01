package com.sm.maps.applib.utils;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.sm.maps.applib.tileprovider.TileSource;

import org.json.JSONObject;

import java.io.File;

public class MBTilesMapDatabase implements ICacheProvider {
	private static final String SQL_SELECT_PARAMS = "SELECT * FROM metadata";
	private static final String SQL_SELECT_IMAGE = "SELECT tile_data as ret FROM tiles WHERE tile_column = ? AND tile_row = ? AND zoom_level = ?";
	private static final String SQL_FINDTHEMAP = "SELECT tile_column AS x, tile_row AS y FROM tiles WHERE zoom_level = ? LIMIT 1";
	private static final String SQL_tiles_count = "SELECT COUNT(*) cnt FROM tiles";
	private static final String SQL_GET_MINZOOM = "SELECT DISTINCT zoom_level AS z FROM tiles ORDER BY zoom_level ASC LIMIT 1;";
	private static final String SQL_GET_MAXZOOM = "SELECT DISTINCT zoom_level AS z FROM tiles ORDER BY zoom_level DESC LIMIT 1;";
	
	private static final String RET = "ret";
	private static final String TILES = "tiles";
	private static final String PARAMS = "metadata";

	private SQLiteDatabase mDatabase = null;
	private String mBaseFileName;
	private int[] mMinMaxZoom = null;
	
	public String getID(String pref) {
		return Ut.FileName2ID(pref+mBaseFileName);
	}

	private void initDatabaseFiles(final String aFileName, final boolean aCreateNewDatabaseFile) throws RException {

		if (mDatabase != null)
			mDatabase.close();

		mBaseFileName = aFileName;

		try {
			mDatabase = SQLiteDatabase.openDatabase(aFileName, null, 0);
		} catch (Exception e) {
		}

	}
	
	public synchronized void setFile(final String aFileName) throws SQLiteException, RException {
		initDatabaseFiles(aFileName, false);
	}

	public synchronized void setFile(final File aFile) throws SQLiteException, RException {
		setFile(aFile.getAbsolutePath());
	}

	
	public void updateMapParams(TileSource tileSource) {
		tileSource.ZOOM_MINLEVEL = getMinZoom();
		tileSource.ZOOM_MAXLEVEL = getMaxZoom();
		tileSource.ZOOM_MAXDNLD = tileSource.ZOOM_MAXLEVEL;
	}
	
	public synchronized void updateMinMaxZoom() {
		if(mMinMaxZoom == null)
			mMinMaxZoom = new int[2];
		mMinMaxZoom[0] = 22; //min
		mMinMaxZoom[1] = 0; //max
		int zoom;
		
		try {
			zoom = (int) this.mDatabase.compileStatement(SQL_GET_MINZOOM).simpleQueryForLong();
			if(zoom < mMinMaxZoom[0])
				mMinMaxZoom[0] = zoom;
		} catch (SQLException e) {
		}
		try {
			zoom = (int) this.mDatabase.compileStatement(SQL_GET_MAXZOOM).simpleQueryForLong();
			if(zoom > mMinMaxZoom[1])
				mMinMaxZoom[1] = zoom;
		} catch (SQLException e) {
		}
	}

	public synchronized int getMaxZoom() {
		if(mMinMaxZoom == null)
			updateMinMaxZoom();
		
		return mMinMaxZoom[1];
	}

	public synchronized int getMinZoom() {
		if(mMinMaxZoom == null)
			updateMinMaxZoom();
		
		return mMinMaxZoom[0];
	}

	public synchronized void putTile(final int aX, final int aY, final int aZ, final byte[] aData) throws RException {
	}
	
	public synchronized byte[] getTile(final int aX, final int aY, final int aZ) {
		byte[] ret = null;
		if (mDatabase != null && mDatabase.isOpen() && !mDatabase.isDbLockedByOtherThreads()) {
			final String[] args = {""+aX, ""+((1 << aZ)-1-aY), ""+aZ};
			try {
				final Cursor c = this.mDatabase.rawQuery(SQL_SELECT_IMAGE, args);
				if (c != null) {
					if (c.moveToFirst())
						ret = c.getBlob(c.getColumnIndexOrThrow(RET));
					c.close();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
	
	@Override
	public synchronized void deleteTile(String aURLstring, int aX, int aY, int aZ) {
	}

	public synchronized boolean existsTile(final int aX, final int aY, final int aZ) {
		final String[] args = {""+aX, ""+((1<<aZ)-1-aY), ""+aZ};
		boolean ret = false;
		if(mDatabase != null) {
			final Cursor c = this.mDatabase.rawQuery(SQL_SELECT_IMAGE, args);
			if(c != null) {
				if(c.moveToFirst())
					ret = true;
				c.close();
			}
		}
		return ret;
	}

	@Override
	protected void finalize() throws Throwable {
		if(mDatabase != null)
			if (mDatabase.isOpen()) {
				mDatabase.close();
			}

		super.finalize();
	}

	public synchronized void freeDatabases() {
		if (mDatabase != null)
			if (mDatabase.isOpen()) {
				mDatabase.close();
			}
	}

	public synchronized byte[] getTile(String aURLstring, int aX, int aY, int aZ) {
		return getTile(aX, aY, aZ);
	}

	public synchronized void putTile(String aURLstring, int aX, int aY, int aZ, byte[] aData) throws RException {
		putTile(aX, aY, aZ, aData);
	}

	public synchronized void Free() {
		freeDatabases();
	}

	public void clearTiles() {
	}

	public double getTileLenght() {
		double ret = 0L;
		if(mDatabase != null) {
			final long cnt = mDatabase.compileStatement(SQL_tiles_count).simpleQueryForLong();
			if(cnt > 0) {
				final File file = new File(mDatabase.getPath());
				ret = file.length()/cnt;
			};
		}
		return ret;
	}
	
	public JSONObject getParams() {
		JSONObject json = null;

		if (json == null)
			json = new JSONObject();

		return json;
	}

	public void setParams(String mapID, String mapName, int[] coordArr, int[] zoomArr, int zoom) {
	}

	public int[] findTheMap(int zoomLevel) {
		int[] coord = new int[2];
		coord[0]=0;
		coord[1]=0;
		final String[] args = {""+zoomLevel};
		if(mDatabase != null) {
			final Cursor c = this.mDatabase.rawQuery(SQL_FINDTHEMAP, args);
			if(c != null) {
				if(c.moveToFirst()) {
					coord[0] = c.getInt(1);
					coord[1] = c.getInt(0);
				}
				c.close();
			}
		}
		return coord;
	}
}
