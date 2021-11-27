package com.sm.maps.applib.tileprovider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.andnav.osm.views.util.StreamUtils;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.os.Handler;

import com.sm.maps.applib.R;
import com.sm.maps.applib.utils.RException;
import com.sm.maps.applib.utils.SQLiteMapDatabase;
import com.sm.maps.applib.utils.Ut;

public class TileProviderInet extends TileProviderBase {

	public TileProviderInet(Context ctx, TileURLGeneratorBase gen, final String cacheDatabaseName, MapTileMemCache aTileCache, final Bitmap aLoadingMapTile, final TileSource tileSource) throws SQLiteException, RException {
		this(ctx, gen, cacheDatabaseName, aTileCache, tileSource);
		mLoadingMapTile = aLoadingMapTile;
	}

	public TileProviderInet(Context ctx, TileURLGeneratorBase gen, final String cacheDatabaseName, MapTileMemCache aTileCache, final TileSource tileSource) throws SQLiteException, RException {
        super(ctx, tileSource, SRC_ONLINE);
        mTileURLGenerator = gen;
        if (aTileCache == null)
            mTileCache = new MapTileMemCache();
        else
            mTileCache = aTileCache;
        if (cacheDatabaseName != null) {
            final SQLiteMapDatabase cacheDatabase = new SQLiteMapDatabase();
            final File folder = Ut.getRMapsMainDir(ctx, "cache");
            cacheDatabase.setFile(folder.getAbsolutePath() + "/" + cacheDatabaseName + ".sqlitedb");
            mCacheProvider = cacheDatabase;
        } else {
            final File folder = Ut.getRMapsCacheTilesDir(ctx);
            mCacheProvider = new FSCacheProvider(folder);
        }

    }

	@Override
	public void setHandler(Handler mTileMapHandler) {
		super.setHandler(mTileMapHandler);
		if(mTileURLGenerator instanceof TileURLGeneratorYANDEXTRAFFIC) {
			((TileURLGeneratorYANDEXTRAFFIC) mTileURLGenerator).setCallbackHandler(mTileMapHandler);
		}
	}

	@Override
	public void Free() {
		super.Free();
	}

	@Override
	public void removeTileFromCache(int x, int y, int z) {
		final String tileurl = mTileURLGenerator.Get(x, y, z);
		
		mCacheProvider.deleteTile(tileurl, x, y, z);
		removeTile(tileurl);
		
		super.removeTileFromCache(x, y, z);
	}

	@Override
	public byte[] getSingleTile(String tileurl, String logFilename) {

		InputStream in = null;
		OutputStream out = null;
		HttpURLConnection conn;
		byte[] data = null;
		int rc, retry;

		Ut.w("FROM INTERNET " + tileurl);

		try {
			conn = (HttpURLConnection) (new URL(tileurl)).openConnection();
			conn.setRequestProperty("User-Agent", mCtx.getString(R.string.user_agent));
			conn.setRequestProperty("Accept", mCtx.getString(R.string.accept_content));
			conn.setRequestMethod("GET");
		} catch (Exception e) {
			conn = null;
		}
		if (conn != null) {
			retry = 0;
			do {
				rc = -1; // if exception occurs before getResponseCode - NO RETRY!
				try {
					conn.connect();
					rc = conn.getResponseCode();
					if (rc != 200 && logFilename != null)
						Ut.appendLog(logFilename, String.format("%tc %s: Resp: %d %s", System.currentTimeMillis(), tileurl, rc, conn.getResponseMessage()));
					if (rc < 400) {
						in = new BufferedInputStream(conn.getInputStream(), StreamUtils.IO_BUFFER_SIZE);
						final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
						out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
						StreamUtils.copy(in, out);
						out.flush();
						data = dataStream.toByteArray();
					} else
						data = null;
				} catch (Exception e) {
					if (logFilename != null)
						Ut.appendLog(logFilename, String.format("%tc %s: Xcpt: %s; %s", System.currentTimeMillis(), tileurl, e.toString(), e.getMessage()));
					data = null;
				} catch (OutOfMemoryError e) {
					if (logFilename != null)
						Ut.appendLog(logFilename, String.format("%tc %s: OutOfMem: %s; %s", System.currentTimeMillis(), tileurl, e.toString(), e.getMessage()));
					data = null;
					rc = -1;
				} finally {
					if (in != null)
						StreamUtils.closeStream(in);
					if (out != null)
						StreamUtils.closeStream(out);
					in = null;
					out = null;
					if (conn != null)
						conn.disconnect();
				}
				if (rc == 404 || rc == 403 || rc < 0)
					retry = 999; // no retries for 404, 403 and out of memory
				else {
					retry = retry + 1;
				}
			} while (data == null && retry <= 4);
		}
		return data;
	}

	@Override
	public Bitmap getTile(final int x, final int y, final int z) {
		return getTileFromSource(x, y, z);
	}

	@Override
	public double getTileLength() {
		return mCacheProvider.getTileLenght();
	}

}
