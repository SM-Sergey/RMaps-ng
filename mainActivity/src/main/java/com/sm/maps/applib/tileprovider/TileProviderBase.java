package com.sm.maps.applib.tileprovider;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import com.sm.maps.applib.R;

public class TileProviderBase {
	protected Bitmap mLoadingMapTile;
	protected Bitmap mNoMapTile;
	protected Bitmap mNoMapTile1;
	protected Bitmap mNoMapTile2;
	protected TileURLGeneratorBase mTileURLGenerator;
	protected final Set<String> mPending = Collections.synchronizedSet(new HashSet<String>());
	protected final Map<String,XYZ> mPendXYZ = Collections.synchronizedMap(new HashMap<String,XYZ>());

	protected final Map<String,XYZ> mPendCacheReq = Collections.synchronizedMap(new HashMap<String,XYZ>());
	protected final Map<String,XYZ> mPendCache2Req = Collections.synchronizedMap(new HashMap<String,XYZ>());
	protected final Map<String,XYZ> mPendTileReq = Collections.synchronizedMap(new HashMap<String,XYZ>());

	protected MapTileMemCache mTileCache;
	protected Handler mCallbackHandler;
	protected boolean mReloadTileMode = false;

	protected TileSource mTileSource;
	private Context mCtx;
	private MessageDigest mDg = null;

	public TileProviderBase(Context ctx, TileSource tileSource) {
		super();
		mCtx = ctx;
		mLoadingMapTile = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_loading);
		mNoMapTile = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_notile);
		mNoMapTile1 = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_notile1);
		mNoMapTile2 = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_notile2);
		mTileSource = tileSource;

		try {
			mDg = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			mDg = null;
		}

	}
	
	public void Free() {
		mPending.clear();
		if(mTileURLGenerator != null)
			mTileURLGenerator.Free();
		if(mLoadingMapTile != null)
			mLoadingMapTile.recycle();
		mCallbackHandler = null;
		if(mTileCache != null)
			mTileCache.Free();
	}

	protected void finalize() throws Throwable {
		super.finalize();
	}

	public Bitmap getTile(final int x, final int y, final int z) {
		return mLoadingMapTile;
	}
	
	public void removeTile(final String aTileURLString) {
		if(mTileCache != null)
			mTileCache.removeTile(aTileURLString);
	}
	
	public void removeTileFromCache(final int x, final int y, final int z) {
	}
	
	protected void SendMessageSuccess() {
		if(mCallbackHandler != null)
			Message.obtain(mCallbackHandler, MessageHandlerConstants.MAPTILEFSLOADER_SUCCESS_ID).sendToTarget();
	}
	
	protected void SendMessageFail() {
		if(mCallbackHandler != null)
			Message.obtain(mCallbackHandler, MessageHandlerConstants.MAPTILEFSLOADER_FAIL_ID).sendToTarget();
	}

	public void setHandler(Handler mTileMapHandler) {
		mCallbackHandler = mTileMapHandler;
	}
	
	public void ResizeCashe(final int size) {
		if(mTileCache != null)
			mTileCache.Resize(size);
	}
	
	public void CommitCashe() {
		if(mTileCache != null)
			mTileCache.Commit();
	}

	public void updateMapParams(TileSource tileSource) {
	}
	
	public boolean needIndex(final String aCashTableName, final long aSizeFile, final long aLastModifiedFile, final boolean aBlockIndexing) {
		return false;
	}
	
	public void Index() {
		
	}
	
	public void setLoadingMapTile(Bitmap aLoadingMapTile) {
		if(mLoadingMapTile != null)
			mLoadingMapTile.recycle();
		mLoadingMapTile = aLoadingMapTile;
	}
	
	public double getTileLength() {
		return 0;
	}

	protected byte[] getSingleTile(String tileurl) { return null; }

	protected class XYZ {
		public String TILEURL;
		public int X;
		public int Y;
		public int Z;
		public boolean waiting;

		public XYZ(final String tileurl, final int x, final int y, final int z) {
			TILEURL = tileurl;
			X = x;
			Y = y;
			Z = z;
			waiting = false;
		}
	}

	final private static String R0 = "{r0}";
	final private static String R1 = "{r1}";
	final private static String R3 = "{r3}";
	final private static String RA = "{ra}";
	final private static String RB = "{rb}";
	final private static String[] abcd = {"a","b","c","d"};

	String getRealURL(String url){
		final int rn = (int)Math.floor((Math.random() * 4.0f));
		final int r3 = (int)Math.floor((Math.random() * 3.0f));
		final int rb = (int)Math.floor((Math.random() * 2.0f));

		return url.replace(R0, Integer.toString(rn))
				.replace(R1, Integer.toString(rn+1))
				.replace(R3, Integer.toString(r3))
				.replace(RA, abcd[rn])
				.replace(RB, abcd[rb]);

	}

	protected synchronized boolean isBlank(byte[] data) {

		if (data == null)
			return true;

		if (mDg == null)
			return false;

		TileSourceBase tileSrc = mLoadingMapTile == null ? mTileSource.mTileSourceBaseOverlay : mTileSource;

//		String mLogFileName = Ut.getRMapsMainDir(mCtx, "")+"/log.txt";

//		if (mTileSource.BLANKTILE != null)
//		Ut.appendLog(mLogFileName, "BT:"+mTileSource.BLANKTILE);
//		else
//		Ut.appendLog(mLogFileName, "No blank tiles");

		if (tileSrc == null || tileSrc.blankTiles == null || tileSrc.blankTiles.length == 0) {
			return false;
		}

//		Ut.appendLog(mLogFileName, "BTA:" + mTileSource.blankTiles[0]);

		byte[] di = mDg.digest(data);
		BigInteger bi = new BigInteger(1,di);
		String hash = String.format("%032x", bi);
		for (String md5 : tileSrc.blankTiles) {
			if (hash.equals(md5))
				return true;
		}

		return false;
	}

}
