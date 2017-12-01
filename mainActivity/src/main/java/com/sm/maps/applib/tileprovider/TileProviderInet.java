package com.sm.maps.applib.tileprovider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.views.util.StreamUtils;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;

import com.sm.maps.applib.R;
import com.sm.maps.applib.utils.ICacheProvider;
import com.sm.maps.applib.utils.RException;
import com.sm.maps.applib.utils.SQLiteMapDatabase;
import com.sm.maps.applib.utils.SimpleThreadFactory;
import com.sm.maps.applib.utils.Ut;

public class TileProviderInet extends TileProviderBase {
	private ICacheProvider mCacheProvider = null;
	private ExecutorService mThreadPool = Executors.newFixedThreadPool(5, new SimpleThreadFactory("TileProviderInet"));
    private int threads = 0;
    private final BoolObj mInUse = new BoolObj();
//    private MapTileMemCache mPrevCachedCache = new MapTileMemCache(128);

    private class BoolObj extends Object {
        public boolean flag1 = false;
        public boolean flag2 = false;
    }

	public TileProviderInet(Context ctx, TileURLGeneratorBase gen, final String cacheDatabaseName, MapTileMemCache aTileCache, final Bitmap aLoadingMapTile, final TileSource tileSource) throws SQLiteException, RException {
		this(ctx, gen, cacheDatabaseName, aTileCache, tileSource);
		mLoadingMapTile = aLoadingMapTile;
	}

	public TileProviderInet(Context ctx, TileURLGeneratorBase gen, final String cacheDatabaseName, MapTileMemCache aTileCache, final TileSource tileSource) throws SQLiteException, RException {
        super(ctx, tileSource);
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

        // 1'st level dispatcher - main zoom from Cache
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

                        Ut.i("Downloading Maptile from url: " + xyz.TILEURL);

                        bmp = null;

                        if (mCacheProvider != null) {
                            if (!mReloadTileMode) {

                                data = mCacheProvider.getTile(xyz.TILEURL, xyz.X, xyz.Y, xyz.Z);

                                if (data != null && !isBlank(data)) {

                                    try {
                                        bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    } catch (Throwable e) {
                                        bmp = null;
                                    }

                                    if (bmp == null) {
                                        mCacheProvider.deleteTile(xyz.TILEURL, xyz.X, xyz.Y, xyz.Z);
                                    }
                                }
                            }
                        }

                        if (bmp != null) {
                            if (mTileCache.putTile(xyz.TILEURL, bmp, MapTileMemCache.SRC_CACHE, false, null)) {
                                bmpFlag = true;
                            } else bmp.recycle();
                        }

                        if (bmp == null) {
                            synchronized (mPendCache2Req) {
                                if (!mPendCache2Req.containsKey(xyz.TILEURL)) {
                                    mPendCache2Req.put(xyz.TILEURL, xyz);
                                    mPendCache2Req.notifyAll();
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

        // 2'nd level dispatcher #1 - other zoom's from cache
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
                            if (z > 0 && z <= mTileSource.ZOOM_MAXDNLD) {
                                String prevZurl = mTileURLGenerator.Get(x, y, z);
//                                zbmp = mPrevCachedCache.getMapTile(prevZurl);
//                                if (zbmp == null) {
                                    data = mCacheProvider.getTile(prevZurl, x, y, z);
                                    if (data != null && !isBlank(data))
                                        try {
                                            zbmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                        } catch (Throwable e) {
                                            zbmp = null;
                                        }
//                                    if (zbmp != null) {
//                                        mPrevCachedCache.putTile(prevZurl, zbmp);
//                                    }
//                                }
                            }
                        } while (zbmp == null && z > 0 && az <= 4);

                        if (zbmp != null) {

                            x = (zbmp.getWidth() / (1 << az)) * xm;
                            y = (zbmp.getHeight() / (1 << az)) * ym;
                            tbmp = Bitmap.createBitmap(zbmp, x, y, zbmp.getWidth() / (1 << az), zbmp.getHeight() / (1 << az));
                            if (tbmp != null) {
                                bmp = Bitmap.createScaledBitmap(tbmp, zbmp.getWidth(), zbmp.getHeight(), true);

                                if (bmp != null) {
                                    Bitmap[] arr = new Bitmap[2];
                                    arr[0] = tbmp;
                                    arr[1] = zbmp;
                                    if (mTileCache.putTile(xyz.TILEURL, bmp, MapTileMemCache.SRC_CACHE_SECOND, false, arr)) {
                                        bmpFlag = true;
                                    } else {
                                        bmp.recycle();
                                        if (!tbmp.isRecycled()) tbmp.recycle();
                                        if (!zbmp.isRecycled()) zbmp.recycle();
                                    }
                                } else {
                                    if (!tbmp.isRecycled()) tbmp.recycle();
                                    if (!zbmp.isRecycled()) zbmp.recycle();
                                }
                            } else zbmp.recycle();
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

            if (mXYZ.Z <= mTileSource.ZOOM_MAXDNLD) {
                do {
                    try {
                        data = getSingleTile(mTileURLGenerator.getRealURL(mXYZ.TILEURL));
                    } catch (Exception e) {
                        data = null;
                    }

                    blank = isBlank(data);

                    if (data != null && !blank)
                        try {
                            bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                        } catch (Exception e) {
                            bmp = null;
                        }

                    ii = ii + 1;
                } while (bmp == null && ii < 1); // 1 retries in requsted zoom
            }

            // Add to cache (on SD card)
			if (mCacheProvider != null && (bmp != null || (data != null && blank))) {
				if (mReloadTileMode)
					mCacheProvider.deleteTile(mXYZ.TILEURL, mXYZ.X, mXYZ.Y, mXYZ.Z);
				try {
					mCacheProvider.putTile(mXYZ.TILEURL, mXYZ.X, mXYZ.Y, mXYZ.Z, data);
				} catch (Exception e) {
				}
			}

			if (bmp == null) {
			    // attempt to download from smaller zoom
                Bitmap zbmp = null;
                Bitmap tbmp;
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
                    if (z > 0 && z <= mTileSource.ZOOM_MAXDNLD) {
                        String prevZURL = mTileURLGenerator.Get(x,y,z);

                        data = null;
                        try {
                            data = getSingleTile(mTileURLGenerator.getRealURL(prevZURL));
                        } catch (Exception e) {
                        }

                        blank = isBlank(data);

                        if (data != null && !blank)
                            try {
                                zbmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                            } catch (Throwable e) {
                                zbmp = null;
                            }

                        if (mCacheProvider != null && (zbmp != null || (data != null && blank))) {
                            // Add to cache (on SD card)
                            if (mReloadTileMode)
                                mCacheProvider.deleteTile(prevZURL, x, y, z);
                            try {
                                mCacheProvider.putTile(prevZURL, x, y, z, data);
                            } catch (Exception e) {
                            }
                        }

                    }
                } while (zbmp == null && z > 0 && az <= 4);

                if (zbmp != null) {
                    x = (zbmp.getWidth() / (1 << az)) * xm;
                    y = (zbmp.getHeight() / (1 << az)) * ym;
                    tbmp = Bitmap.createBitmap(zbmp, x, y, zbmp.getWidth() / (1 << az), zbmp.getHeight() / (1 << az));
                    if (tbmp != null) {
                        bmp = Bitmap.createScaledBitmap(tbmp, zbmp.getWidth(), zbmp.getHeight(), true);
                        if (bmp != null) {
                            Bitmap[] arr = new Bitmap[2];
                            arr[0] = tbmp;
                            arr[1] = zbmp;
                            if (!mTileCache.putTile(mXYZ.TILEURL, bmp, MapTileMemCache.SRC_INET_SECOND, false, arr)) {
                                bmp.recycle();
                                if (!tbmp.isRecycled()) tbmp.recycle();
                                if (!zbmp.isRecycled()) zbmp.recycle();
                            }
                        } else {
                            tbmp.recycle();
                            if (!zbmp.isRecycled()) zbmp.recycle();
                        }
                    } else zbmp.recycle();
			    }
            } else {
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

	@Override
	public void setHandler(Handler mTileMapHandler) {
		super.setHandler(mTileMapHandler);
		if(mTileURLGenerator instanceof TileURLGeneratorYANDEXTRAFFIC) {
			((TileURLGeneratorYANDEXTRAFFIC) mTileURLGenerator).setCallbackHandler(mTileMapHandler);
		}
	}

	@Override
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
		Bitmap bmp = null;

		Ut.w("FROM INTERNET " + tileurl);

		try {
		    HttpURLConnection conn = (HttpURLConnection) (new URL(tileurl)).openConnection();
            conn.setRequestProperty("User-Agent", mCtx.getString(R.string.user_agent));
            conn.setRequestProperty("Accept", mCtx.getString(R.string.accept_content));
//		    conn.setRequestProperty("User-Agent", "RMaps/0.10.0");
//            conn.setRequestProperty("Accept", "image/*");
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
		final String tileurl = mTileURLGenerator.Get(x, y, z);

		if(mReloadTileMode)
			mTileCache.removeTile(tileurl);
		else {
			final Bitmap bmp = mTileCache.getMapTile(tileurl);
			if(bmp != null)
				return bmp;
		}

		XYZ xyz = new XYZ(tileurl,x,y,z);

		boolean contains;

		synchronized(mPendCacheReq) {
			contains = mPendCacheReq.containsKey(xyz.TILEURL);
		}
        if (contains)
            return mLoadingMapTile;

		synchronized(mPendCache2Req) {
			contains = mPendCache2Req.containsKey(xyz.TILEURL);
		}

        if (contains)
            return mLoadingMapTile;

        synchronized(mPendTileReq) {
            contains = mPendTileReq.containsKey(xyz.TILEURL);
        }

        if (contains)
            return mLoadingMapTile;

		synchronized(mPendCacheReq) {
			mPendCacheReq.put(xyz.TILEURL, xyz);
			mPendCacheReq.notifyAll();
		}

		return mLoadingMapTile;
	}

	@Override
	public double getTileLength() {
		return mCacheProvider.getTileLenght();
	}

}
