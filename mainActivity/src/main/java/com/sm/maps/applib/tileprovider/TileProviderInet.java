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
	protected byte[] getSingleTile(String tileurl) {

		InputStream in = null;
		OutputStream out = null;
		byte[] data;

		Ut.w("FROM INTERNET " + tileurl);

		try {
		    HttpURLConnection conn = (HttpURLConnection) (new URL(tileurl)).openConnection();
            conn.setRequestProperty("User-Agent", mCtx.getString(R.string.user_agent));
            conn.setRequestProperty("Accept", mCtx.getString(R.string.accept_content));
            conn.setRequestMethod("GET");
			in = new BufferedInputStream(conn.getInputStream(), StreamUtils.IO_BUFFER_SIZE);
			final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
			out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
			StreamUtils.copy(in, out);
			out.flush();
			data = dataStream.toByteArray();
		} catch (Exception e) {
			data = null;
		} finally {
			if (in != null)
				StreamUtils.closeStream(in);
			if (out != null)
				StreamUtils.closeStream(out);
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
