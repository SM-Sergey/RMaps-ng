package com.sm.maps.applib.tileprovider;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import android.graphics.Bitmap;

public class MapTileMemCache {
	private static final int CACHE_MAPTILECOUNT_DEFAULT = 16;
	private int mSize = CACHE_MAPTILECOUNT_DEFAULT;

	final static int SRC_INET = 0;
	final static int SRC_INET_SECOND = 1;
	final static int SRC_CACHE = 2;
	final static int SRC_CACHE_SECOND = 3;

	private class LRUBitmapCache<K, V> extends LinkedHashMap<K, V> {

		private int mSize;

		public LRUBitmapCache(int cacheSize){
			super(cacheSize, 1, true);
			mSize = cacheSize;
		}

		@Override
		protected boolean removeEldestEntry(Entry<K, V> eldest) {
			if (size() > mSize) {
				CacheItem ci = (CacheItem) eldest.getValue();
				if (!ci.norecycle) {
					if (ci.bmp != null && !ci.bmp.isRecycled())
						ci.bmp.recycle();
					if (ci.mRelated != null)
						for (Bitmap b : ci.mRelated) {
							if (b != null && !b.isRecycled())
								b.recycle();
						}
				}
				return true;
			}
			return false;
		}

		public void resize(int size) {
			int sz = size + (size >> 2);
			if (sz > mSize)
				mSize = sz;
		}

	}

	protected class CacheItem {
		public Bitmap bmp;
		public int source;
		public boolean norecycle;
		public Bitmap[] mRelated;

		public CacheItem(Bitmap bmp, int source, boolean norecycle, Bitmap[] related){
			this.norecycle = norecycle;
			this.bmp = bmp;
			this.source = source;
			this.mRelated = related;
		}

	}

	protected LRUBitmapCache<String, CacheItem> mHardCachedTiles;

	public MapTileMemCache(){
		this(CACHE_MAPTILECOUNT_DEFAULT);
	}

	public MapTileMemCache(final int aMaximumCacheSize){
		mHardCachedTiles = new LRUBitmapCache<String, CacheItem>(aMaximumCacheSize);
		mSize = aMaximumCacheSize;
	}

	public synchronized Bitmap getMapTile(final String aTileURLString) {
		if(aTileURLString != null) {
			Bitmap bmpHard = null;
			CacheItem ci = this.mHardCachedTiles.get(aTileURLString);
			if (ci != null)
				bmpHard = ci.bmp;
			if(bmpHard != null && !bmpHard.isRecycled())
				return bmpHard;
		}
		return null;
	}

	public synchronized void putTile(final String aTileURLString, final Bitmap aTile) {
			mHardCachedTiles.put(aTileURLString, new CacheItem(aTile, SRC_INET, false, null));
	}

	public synchronized boolean putTile(final String aTileURLString, final Bitmap aTile, final int source, final boolean norecycle, final Bitmap[] related) {
		CacheItem ci;
		boolean applied = false;
			ci = mHardCachedTiles.get(aTileURLString);
			if (ci != null) {
				if (source <= ci.source) {
					removeTile(aTileURLString);
					mHardCachedTiles.put(aTileURLString, new CacheItem(aTile, source, norecycle, related));
					applied = true;
				}
			} else {
				mHardCachedTiles.put(aTileURLString, new CacheItem(aTile, source, norecycle, related));
				applied = true;
			}

		return applied;
	}

	public synchronized void removeTile(final String aTileURLString) {
			if (mHardCachedTiles.containsKey(aTileURLString)) {
				final CacheItem ci = mHardCachedTiles.remove(aTileURLString);
				if (!ci.norecycle) {
					if (ci.bmp != null && !ci.bmp.isRecycled())
						ci.bmp.recycle();
					if (ci.mRelated != null)
						for (Bitmap b : ci.mRelated) {
							if (b != null && !b.isRecycled())
								b.recycle();
						}
				}
			}
	}

	public synchronized void Commit() {
	}
	
	public synchronized void Resize(final int size) {
		if(size > mSize){
			mSize = size;
			mHardCachedTiles.resize(size);
		}
	}

	public synchronized void Free() {
			Iterator<Entry<String, CacheItem>> it = mHardCachedTiles.entrySet().iterator();
			while (it.hasNext()) {
				final CacheItem ci = it.next().getValue();
				if (!ci.norecycle) {
					if (ci.bmp != null && !ci.bmp.isRecycled())
						ci.bmp.recycle();
					if (ci.mRelated != null)
						for (Bitmap b : ci.mRelated) {
							if (b != null && !b.isRecycled())
								b.recycle();
						}
				}
			}
		mHardCachedTiles.clear();
	}

}
