package com.sm.maps.applib.tileprovider;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;

import com.sm.maps.applib.R;
import com.sm.maps.applib.utils.ICacheProvider;
import com.sm.maps.applib.utils.SimpleThreadFactory;
import com.sm.maps.applib.utils.Ut;

public class TileProviderBase {
	protected Bitmap mLoadingMapTile;
	protected Bitmap mNoMapTile;
	protected Bitmap mNoMapTile1;
	protected Bitmap mNoMapTile2;
	protected TileURLGeneratorBase mTileURLGenerator;

	private final Map<String,XYZ> mPendCacheReq = Collections.synchronizedMap(new HashMap<String,XYZ>());
	private final Map<String,XYZ> mPendCache2Req = Collections.synchronizedMap(new HashMap<String,XYZ>());
	private final Map<String,XYZ> mPendTileReq = Collections.synchronizedMap(new HashMap<String,XYZ>());

	protected MapTileMemCache mTileCache;
	protected Handler mCallbackHandler;
	protected boolean mReloadTileMode = false;

	protected TileSource mTileSource;
	protected Context mCtx;
	private MessageDigest mDg = null;

	protected ExecutorService mThreadPool = Executors.newFixedThreadPool(10, new SimpleThreadFactory("TileProvider"));
	private int threads = 0;
	private final BoolObj mInUse = new BoolObj();
//    private MapTileMemCache mPrevCachedCache = new MapTileMemCache(128);

	public static final int SRC_WRAPPER = 0;
	public static final int SRC_ONLINE = 1;
	public static final int SRC_OFFLINE = 2;

	protected int mSourceType = SRC_WRAPPER;

	protected ICacheProvider mCacheProvider = null;

	private class BoolObj extends Object {
		public boolean flag1 = false;
		public boolean flag2 = false;
	}

	public TileProviderBase(final Context ctx, TileSource tileSource, int src) {
		super();
		mSourceType = src;
		mCtx = ctx;
		mLoadingMapTile = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_loading);
//		mNoMapTile = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_notile);
//		mNoMapTile1 = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_notile1);
//		mNoMapTile2 = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.maptile_notile2);
		mTileSource = tileSource;

		try {
			mDg = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			mDg = null;
		}

		// 1'st level dispatcher - main zoom from Cache or local source
		if (mSourceType != SRC_WRAPPER) {
			mThreadPool.execute(new Runnable() {
				public void run() {
					XYZ xyz;
					Collection<XYZ> col;
					Iterator<XYZ> it;
					byte[] data = null;
					Bitmap bmp = null;
					boolean bmpFlag = false;
					boolean fGC = true;
					boolean testGC = false;
					boolean fSend = false;

					while (!mThreadPool.isShutdown()) {

						xyz = null;

						synchronized (mPendCacheReq) {
							while (mPendCacheReq.isEmpty() && !mThreadPool.isShutdown() && !testGC) {
								try {
									mPendCacheReq.wait(150);
								} catch (InterruptedException e) {
								}
								if (mPendCacheReq.isEmpty() && !fGC) {
									testGC = true;
								}
							}
							if (!mThreadPool.isShutdown() && !mPendCacheReq.isEmpty()) {
								col = mPendCacheReq.values();
								it = col.iterator();
								xyz = it.next();
							}
						}

						synchronized (mInUse) {
							if (mInUse.flag1 || mInUse.flag2 || xyz != null) testGC = false;
						}

						if (testGC) {
							if (fSend) {
								SendMessageSuccess();
								fSend = false;
							}
							System.gc();
							testGC = false;
							fGC = true;
						}

						if (xyz != null && !mThreadPool.isShutdown()) {

							fGC = false;
							bmp = null;

							Ut.i("Starting for url: " + xyz.TILEURL);

							if (mSourceType == SRC_OFFLINE) {

								// Attempt to get tile from current, then previous zooms
								Bitmap zbmp = null;
								Bitmap tbmp;
								int z;
								int x;
								int y;
								int xm;
								int ym;
								int az = -1;

								do {
									az += 1;
									z = xyz.Z - az;
									x = xyz.X / (1 << az);
									y = xyz.Y / (1 << az);
									xm = xyz.X & ((1 << az) - 1);
									ym = xyz.Y & ((1 << az) - 1);
									if (z > 0 && z <= getTileSource().ZOOM_MAXDNLD) {
//                                    zbmp = mPrevCachedCache.getMapTile(prevZurl);
//                                    if (zbmp == null) {
										data = getSingleTile(x, y, z);
										if (data != null && !isBlank(data))
											try {
												zbmp = BitmapFactory.decodeByteArray(data, 0, data.length);
											} catch (Throwable e) {
												zbmp = null;
											}
//                                        if (zbmp != null) {
//                                            mPrevCachedCache.putTile(prevZurl, zbmp);
//                                        }
//                                    }
									}
								}
								while (zbmp == null && z > 0 && az < getTileSource().mPrevZCached);

								if (zbmp != null) {
									if (az == 0) {
										if (mTileCache.putTile(xyz.TILEURL, zbmp, MapTileMemCache.SRC_INET, false, null))
											bmpFlag = true;
										else
											zbmp.recycle();
									} else {
										bmp = scalePartOfBitmap(zbmp, (zbmp.getWidth() / (1 << az)) * xm, (zbmp.getHeight() / (1 << az)) * ym,
												zbmp.getWidth() / (1 << az), zbmp.getHeight() / (1 << az),
												zbmp.getWidth(), zbmp.getHeight());
										if ( mTileCache.putTile(xyz.TILEURL, bmp, MapTileMemCache.SRC_INET, false, null))
											bmpFlag = true;
										else
											bmp.recycle();
									}
								}

							} else if (mCacheProvider != null && getTileSource().mOnlineMapCacheEnabled) {

								data = mCacheProvider.getTile(xyz.TILEURL, xyz.X, xyz.Y, xyz.Z);

								if (data != null && !isBlank(data)) {

									try {
										bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
									} catch (Throwable e) {
										bmp = null;
									}

									if (bmp == null) {
										mCacheProvider.deleteTile(xyz.TILEURL, xyz.X, xyz.Y, xyz.Z);
									} else {
										if (mTileCache.putTile(xyz.TILEURL, bmp, MapTileMemCache.SRC_CACHE, false, null)) {
											bmpFlag = true;
										} else bmp.recycle();

									}
								}
							}

							if ((bmp == null || xyz.mReload) && (mSourceType == SRC_ONLINE)) {
								if (getTileSource().mPrevZCached != 0 && getTileSource().mOnlineMapCacheEnabled && mCacheProvider != null) {
									synchronized (mPendCache2Req) {
										if (!mPendCache2Req.containsKey(xyz.TILEURL)) {
											mPendCache2Req.put(xyz.TILEURL, xyz);
											mPendCache2Req.notifyAll();
										}
									}
								}
								synchronized (mPendTileReq) {
									if (!mPendTileReq.containsKey(xyz.TILEURL)) {
										mPendTileReq.put(xyz.TILEURL, xyz);
										mPendTileReq.notifyAll();
									}
								}
							}

							synchronized (mPendCacheReq) {
								mPendCacheReq.remove(xyz.TILEURL);
								fSend = mPendCacheReq.isEmpty() && bmpFlag;
							}

							if (fSend) {
								SendMessageSuccess();
								bmpFlag = false;
							}

						}
					}
				}
			});
		}

		// 2'nd level dispatcher #1 - other zoom's from cache
		if (mSourceType == SRC_ONLINE) {
			mThreadPool.execute(new Runnable() {
				public void run() {
					XYZ xyz;
					Collection<XYZ> col;
					Iterator<XYZ> it;
					boolean bmpFlag = false;
					boolean fLast;
					boolean clearUse = false;
					boolean wasRun = false;

					while (!mThreadPool.isShutdown()) {

						xyz = null;

						synchronized (mPendCache2Req) {
							while (mPendCache2Req.isEmpty() && !mThreadPool.isShutdown() && !clearUse) {
								try {
									mPendCache2Req.wait(160);
								} catch (InterruptedException e) {
								}
								clearUse = wasRun && mPendCache2Req.isEmpty();
							}
							if (!mThreadPool.isShutdown() && !mPendCache2Req.isEmpty()) {
								col = mPendCache2Req.values();
								it = col.iterator();
								xyz = it.next();
							}
						}

						synchronized (mInUse) {
							if (clearUse) {
								mInUse.flag1 = false;
								clearUse = false;
								wasRun = false;
							} else {
								mInUse.flag1 = true;
								wasRun = true;
							}
						}

						if (xyz != null && !mThreadPool.isShutdown()) {
							// Attempt to get tile from previous zooms
							Bitmap zbmp = null;
							Bitmap tbmp;
							Bitmap bmp;
							int z;
							int x;
							int y;
							int xm;
							int ym;
							int az = 0;
							byte[] data;

							do {
								az += 1;
								z = xyz.Z - az;
								x = xyz.X / (1 << az);
								y = xyz.Y / (1 << az);
								xm = xyz.X & ((1 << az) - 1);
								ym = xyz.Y & ((1 << az) - 1);
								if (z > 0 && z <= getTileSource().ZOOM_MAXDNLD) {
									String prevZurl = mTileURLGenerator.Get(x, y, z);
//                                    zbmp = mPrevCachedCache.getMapTile(prevZurl);
//                                    if (zbmp == null) {
									data = mCacheProvider.getTile(prevZurl, x, y, z);
									if (data != null && !isBlank(data))
										try {
											zbmp = BitmapFactory.decodeByteArray(data, 0, data.length);
										} catch (Throwable e) {
											zbmp = null;
										}
//                                        if (zbmp != null) {
//                                             mPrevCachedCache.putTile(prevZurl, zbmp);
//                                        }
//                                    }
								}
							} while (zbmp == null && z > 0 && az < getTileSource().mPrevZCached);

							if (zbmp != null) {
								bmp = scalePartOfBitmap(zbmp, (zbmp.getWidth() / (1 << az)) * xm, (zbmp.getHeight() / (1 << az)) * ym,
										zbmp.getWidth() / (1 << az), zbmp.getHeight() / (1 << az),
										zbmp.getWidth(), zbmp.getHeight());

								if (mTileCache.putTile(xyz.TILEURL, bmp, MapTileMemCache.SRC_CACHE_SECOND, false, null))
									bmpFlag = true;
								else
									bmp.recycle();
							}

							synchronized (mPendCache2Req) {
								mPendCache2Req.remove(xyz.TILEURL);
								fLast = mPendCache2Req.isEmpty();
							}

							if (fLast && bmpFlag) {
								SendMessageSuccess();
								bmpFlag = false;
							}

						}
					}
				}
			});

			// 2'nd level dispatcher #2 - download from internet
			mThreadPool.execute(new Runnable() {
				public void run() {
					XYZ xyz;
					Collection<XYZ> col;
					Iterator<XYZ> it;
					boolean clearUse = false;
					boolean wasRun = false;

					while (!mThreadPool.isShutdown()) {

						synchronized (mPendTileReq) {
							do {
								xyz = null;

								if (threads < 4 && !mThreadPool.isShutdown() && !mPendTileReq.isEmpty()) {
									col = mPendTileReq.values();
									it = col.iterator();
									do {
										xyz = it.next();
									} while (xyz.waiting && it.hasNext());
									if (xyz.waiting)
										xyz = null;
								}

								if (xyz == null)
									try {
										mPendTileReq.wait(170);
									} catch (InterruptedException e) {
									}

								if (mPendTileReq.isEmpty() && wasRun)
									clearUse = true;

							} while (xyz == null && !mThreadPool.isShutdown() && !clearUse);
						}

						synchronized (mInUse) {
							if (clearUse) {
								mInUse.flag2 = false;
								clearUse = false;
								wasRun = false;
							} else {
								mInUse.flag2 = true;
								wasRun = true;
							}
						}

						if (!mThreadPool.isShutdown() && xyz != null) {
							try {
								mThreadPool.execute(new DownloadThread(xyz));
								synchronized (mPendTileReq) {
									threads = threads + 1;
									xyz.waiting = true;
								}
							} catch (Exception e) {
							}
						}

					}
				}
			});
		}
	}

	private class DownloadThread implements Runnable {

		private XYZ mXYZ;

		public DownloadThread(XYZ xyz) {
			mXYZ = xyz;
		}

		public void run() {

			byte[] data = null;
			Bitmap bmp = null;
			int ii=0;
			boolean blank = true;

			if (mXYZ.Z <= getTileSource().ZOOM_MAXDNLD) {
				do {
					try {
						data = getSingleTile(mTileURLGenerator.getRealURL(mXYZ.TILEURL, mXYZ.X, mXYZ.Y));
					} catch (Exception e) {
						data = null;
					}

					blank = isBlank(data);
                    bmp = null;
					if (data != null && !blank)
						try {
							bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
						} catch (Exception e) {
						}

					ii = ii + 1;
				} while (bmp == null && ii < 1); // 1 retries in requsted zoom
			}

			// Add to cache (on SD card)
			if (mCacheProvider != null && getTileSource().mOnlineMapCacheEnabled && bmp != null) {
				if (mXYZ.mReload)
					mCacheProvider.deleteTile(mXYZ.TILEURL, mXYZ.X, mXYZ.Y, mXYZ.Z);
				try {
					mCacheProvider.putTile(mXYZ.TILEURL, mXYZ.X, mXYZ.Y, mXYZ.Z, data);
				} catch (Exception e) {
				}
			}

			if (bmp == null && getTileSource().mPrevZInet != 0) {
				// attempt to download from smaller zoom
				Bitmap zbmp = null;
				int z;
				int x;
				int y;
				int xm;
				int ym;
				int az = 0;

				do {
					az += 1;
					z = mXYZ.Z - az;
					x = mXYZ.X / (1 << az);
					y = mXYZ.Y / (1 << az);
					xm = mXYZ.X & ((1 << az) - 1);
					ym = mXYZ.Y & ((1 << az) - 1);
					if (z > 0 && z <= getTileSource().ZOOM_MAXDNLD) {
						String prevZURL = mTileURLGenerator.Get(x,y,z);

						data = null;
						try {
							data = getSingleTile(mTileURLGenerator.getRealURL(prevZURL,x,y));
						} catch (Exception e) {
						}

						blank = isBlank(data);

						zbmp = null;
						if (data != null && !blank)
							try {
								zbmp = BitmapFactory.decodeByteArray(data, 0, data.length);
							} catch (Throwable e) {
							}

						if (mCacheProvider != null && getTileSource().mOnlineMapCacheEnabled && zbmp != null) {
							// Add to cache (on SD card)
							if (mXYZ.mReload)
								mCacheProvider.deleteTile(prevZURL, x, y, z);
							try {
								mCacheProvider.putTile(prevZURL, x, y, z, data);
							} catch (Exception e) {
							}
						}

					}
				} while (zbmp == null && z > 0 && az < getTileSource().mPrevZInet);

				if (zbmp != null) {
					bmp = scalePartOfBitmap(zbmp, (zbmp.getWidth() / (1 << az)) * xm, (zbmp.getHeight() / (1 << az)) * ym,
							zbmp.getWidth() / (1 << az), zbmp.getHeight() / (1 << az),
							zbmp.getWidth(), zbmp.getHeight());
					if (!mTileCache.putTile(mXYZ.TILEURL, bmp, MapTileMemCache.SRC_INET_SECOND, false, null))
						bmp.recycle();
				}
			} else if (bmp != null) {
				if (!mTileCache.putTile(mXYZ.TILEURL, bmp, MapTileMemCache.SRC_INET, false, null))
					bmp.recycle();
			}

			synchronized(mPendTileReq) {
				mPendTileReq.remove(mXYZ.TILEURL);
				threads = threads - 1;
				mPendTileReq.notifyAll();
			}

			if (bmp != null)
				SendMessageSuccess();
		}
	}

	public void Free() {

		mThreadPool.shutdown();

		synchronized (mPendCacheReq) {
			mPendCacheReq.notifyAll();
		}
		synchronized (mPendCache2Req) {
			mPendCache2Req.notifyAll();
		}
		synchronized (mPendTileReq) {
			mPendTileReq.notifyAll();
		}
		if(mCacheProvider != null)
			mCacheProvider.Free();

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

    public byte[] getSingleTile(String tileurl, String logFilename) { return null; }
	public byte[] getSingleTile(String tileurl) { return getSingleTile(tileurl, null); }
	protected byte[] getSingleTile(int x, int y, int z) { return null; }

	protected class XYZ {
		public String TILEURL;
		public int X;
		public int Y;
		public int Z;
		public boolean waiting;
		public boolean mReload;

		public XYZ(final String tileurl, final int x, final int y, final int z) {
			TILEURL = tileurl;
			X = x;
			Y = y;
			Z = z;
			waiting = false;
			mReload = mReloadTileMode;
		}
	}

	protected synchronized boolean isBlank(byte[] data) {

		if (data == null)
			return true;

		if (mDg == null)
			return false;

		TileSourceBase tileSrc = getTileSource();

		if (tileSrc == null || tileSrc.blankTiles == null || tileSrc.blankTiles.length == 0) {
			return false;
		}

		byte[] di = mDg.digest(data);
		BigInteger bi = new BigInteger(1,di);
		String hash = String.format("%032x", bi);
		for (String md5 : tileSrc.blankTiles) {
			if (hash.equals(md5))
				return true;
		}

		return false;
	}

	protected TileSourceBase getTileSource() {
		TileSourceBase tileSrc = mLoadingMapTile == null ? mTileSource.mTileSourceBaseOverlay : mTileSource;
		if (tileSrc == null) tileSrc = mTileSource;
		return tileSrc;
	}

	public void setReloadTileMode(boolean mode) {
		mReloadTileMode = mode;
	}

	protected boolean isPending(String tileURL) {

		boolean contains;

		synchronized(mPendCacheReq) {
			contains = mPendCacheReq.containsKey(tileURL);
		}

		if (contains)
			return true;

		synchronized(mPendCache2Req) {
			contains = mPendCache2Req.containsKey(tileURL);
		}

		if (contains)
			return true;

		synchronized(mPendTileReq) {
			contains = mPendTileReq.containsKey(tileURL);
		}

		return contains;
	}

	protected void requestTile (XYZ xyz) {

		synchronized(mPendCacheReq) {
			mPendCacheReq.put(xyz.TILEURL, xyz);
			mPendCacheReq.notifyAll();
		}

	}

	protected Bitmap getTileFromSource(final int x, final int y, final int z) {
		final String tileurl = mTileURLGenerator.Get(x, y, z);

		if(mReloadTileMode)
			mTileCache.removeTile(tileurl);
		else {
			final Bitmap bmp = mTileCache.getMapTile(tileurl);
			if(bmp != null)
				return bmp;
		}

		if (isPending(tileurl))
			return mLoadingMapTile;

		XYZ xyz = new XYZ(tileurl,x,y,z);

		requestTile(xyz);

		return mLoadingMapTile;
	}

	private Bitmap scalePartOfBitmap(Bitmap bmp, int xSrc, int ySrc, int wSrc, int hSrc, int wDst, int hDst) {
		Bitmap bmpRet = Bitmap.createBitmap(wDst, hDst, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bmpRet);
		Rect src = new Rect(xSrc, ySrc, xSrc + wSrc, ySrc + hSrc);
		Rect dst = new Rect(0,0, wDst, hDst);
		Paint paint = new Paint();
		c.drawBitmap(bmp, src, dst, paint);
		bmp.recycle();
		return bmpRet;
	}


}
