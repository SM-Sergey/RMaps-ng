package com.sm.maps.applib.tileprovider;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.util.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.sm.maps.applib.MainPreferences;
import com.sm.maps.applib.utils.RException;
import com.sm.maps.applib.utils.Ut;

public class TileSource extends TileSourceBase {
	private TileProviderBase mTileProvider;
	private TileURLGeneratorBase mTileURLGenerator;
	public TileSourceBase mTileSourceBaseOverlay;
	private TileSource mTileSourceForTileOverlay;
	
	public TileSource(Context ctx, String aId) throws SQLiteException, RException {
		this(ctx, aId, true, true);
	}
	
	public TileSource(Context ctx, String aId, boolean aShowOverlay) throws SQLiteException, RException {
		this(ctx, aId, aShowOverlay, true);
	}
	
	public TileSource(Context ctx, String aId, boolean aShowOverlay, boolean aNeedTileProvider) throws SQLiteException, RException {
		super(ctx, aId);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		
		if(MAP_TYPE == MIXMAP_PAIR) {

			final MapTileMemCache tileCache = new MapTileMemCache();
			mTileURLGenerator = initTileURLGenerator(this, pref);
			final TileProviderBase provider = initTileProvider(ctx, this, mTileURLGenerator, null);
			
			if(aShowOverlay) {
				final TileSourceBase tileSourceBase = new TileSourceBase(ctx, OVERLAYID);
				
				if (this.PROJECTION == tileSourceBase.PROJECTION
						&& (int) (1E6 * this.OFFSET_LAT) == (int) (1E6 * tileSourceBase.OFFSET_LAT)
						&& (int) (1E6 * this.OFFSET_LON) == (int) (1E6 * tileSourceBase.OFFSET_LON)
						) {
					mTileSourceBaseOverlay = tileSourceBase;
					final TileURLGeneratorBase layerURLGenerator = initTileURLGenerator(mTileSourceBaseOverlay, pref);
					final TileProviderBase layerProvider = initTileProvider(ctx, mTileSourceBaseOverlay, layerURLGenerator, null);
					
					mTileProvider = new TileProviderDual(ctx, this.ID, provider, layerProvider, tileCache, this);
				} else {
					mTileURLGenerator = initTileURLGenerator(this, pref);
					mTileProvider = initTileProvider(ctx, this, mTileURLGenerator, null);
					mTileSourceBaseOverlay = null;
					
					mTileSourceForTileOverlay = new TileSource(ctx, OVERLAYID);
					mTileSourceForTileOverlay.clearLoadingMapTile();
				}
			} else {
				mTileProvider = aNeedTileProvider ? initTileProvider(ctx, this, mTileURLGenerator, null) : null;
			}

		} else {
			mTileURLGenerator = initTileURLGenerator(this, pref);
			mTileProvider = aNeedTileProvider ? initTileProvider(ctx, this, mTileURLGenerator, null) : null;
			mTileSourceBaseOverlay = null;
		}
		
	}
	
	public TileSource(Context ctx, String aId, String aLayerId) throws SQLiteException, RException {
		super(ctx, aId);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		
		final TileSourceBase tileSourceBase = new TileSourceBase(ctx, aLayerId);
		    
		if (this.PROJECTION == tileSourceBase.PROJECTION
				&& (int) (1E6 * this.OFFSET_LAT) == (int) (1E6 * tileSourceBase.OFFSET_LAT)
				&& (int) (1E6 * this.OFFSET_LON) == (int) (1E6 * tileSourceBase.OFFSET_LON)
				) {
			final MapTileMemCache tileCache = new MapTileMemCache();
			mTileURLGenerator = initTileURLGenerator(this, pref);
			final TileProviderBase provider = initTileProvider(ctx, this, mTileURLGenerator, null);
			
			mTileSourceBaseOverlay = tileSourceBase;
			final TileURLGeneratorBase layerURLGenerator = initTileURLGenerator(mTileSourceBaseOverlay, pref);
			final TileProviderBase layerProvider = initTileProvider(ctx, mTileSourceBaseOverlay, layerURLGenerator, null);

			mTileProvider = new TileProviderDual(ctx, this.ID, provider, layerProvider, tileCache, this);
		} else {
			mTileURLGenerator = initTileURLGenerator(this, pref);
			mTileProvider = initTileProvider(ctx, this, mTileURLGenerator, null);
			mTileSourceBaseOverlay = null;
			
			mTileSourceForTileOverlay = new TileSource(ctx, aLayerId);
			mTileSourceForTileOverlay.clearLoadingMapTile();
		}
		

//		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
//
//		final MapTileMemCache tileCache = new MapTileMemCache();
//		mTileURLGenerator = initTileURLGenerator(this, pref);
//		final TileProviderBase provider = initTileProvider(ctx, this, mTileURLGenerator, null);
//		
//		mTileSourceOverlay = new TileSourceBase(ctx, aLayerId);
//		final TileURLGeneratorBase layerURLGenerator = initTileURLGenerator(mTileSourceOverlay, pref);
//		final TileProviderBase layerProvider = initTileProvider(ctx, mTileSourceOverlay, layerURLGenerator, null);
//		
//		mTileProvider = new TileProviderDual(ctx, this.ID, provider, layerProvider, tileCache);

	}
	
	public TileSource getTileSourceForTileOverlay() {
		final TileSource tileSource = mTileSourceForTileOverlay;
		mTileSourceForTileOverlay = null;
		return tileSource;
	}
	
	public TileSourceBase getTileSourceBaseOverlay() {
		return mTileSourceBaseOverlay;
	}
	
	public String getOverlayName() {
		return mTileSourceBaseOverlay == null ? "" : mTileSourceBaseOverlay.ID;
	}
	
	private TileProviderBase initTileProvider(Context ctx, TileSourceBase tileSource, TileURLGeneratorBase aTileURLGenerator, MapTileMemCache aTileCache) throws SQLiteException, RException {
		TileProviderBase provider = null;
		
		switch(tileSource.TILE_SOURCE_TYPE) {
		case 0:
			if(tileSource.LAYER)
				provider = new TileProviderInet(ctx, aTileURLGenerator, CacheDatabaseName(tileSource), aTileCache, null, this);
			else
				provider = new TileProviderInet(ctx, aTileURLGenerator, CacheDatabaseName(tileSource), aTileCache, this);
			break;
		case 3:
			provider = new TileProviderMNM(ctx, tileSource.BASEURL, tileSource.ID, aTileCache, this);
			provider.updateMapParams(this);
			break;
		case 4:
			provider = new TileProviderTAR(ctx, tileSource.BASEURL, tileSource.ID, aTileCache, this);
			provider.updateMapParams(this);
			break;
		case 5:
			provider = new TileProviderSQLITEDB(ctx, tileSource.BASEURL, tileSource.ID, aTileCache, this);
			provider.updateMapParams(this);
			break;
		case 6:
			provider = new TileProviderMBTiles(ctx, tileSource.BASEURL, tileSource.ID, aTileCache, this);
			provider.updateMapParams(this);
			break;
		default:
			provider = new TileProviderBase(ctx, this, TileProviderBase.SRC_WRAPPER);
		}
			
		return provider;
	}
	
	private TileURLGeneratorBase initTileURLGenerator(TileSourceBase tileSource, SharedPreferences pref) {
		TileURLGeneratorBase generator = null;
		
		if(tileSource.TILE_SOURCE_TYPE == 0) {
			switch(tileSource.URL_BUILDER_TYPE) {
			case 0:
				generator = new TileURLGeneratorOSM(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 1:
				generator = new TileURLGeneratorGOOGLEMAP(tileSource.BASEURL, tileSource.GOOGLE_LANG_CODE, pref.getString(MainPreferences.PREF_PREDEFMAPS_ + tileSource.ID + "_googlescale", "1"));
				break;
			case 2:
				generator = new TileURLGeneratorYANDEX(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 3:
				generator = new TileURLGeneratorYANDEXTRAFFIC(tileSource.BASEURL);
				break;
			case 4:
				generator = new TileURLGeneratorGOOGLESAT(tileSource.BASEURL, tileSource.GOOGLE_LANG_CODE);
				break;
			case 5:
				generator = new TileURLGeneratorOrdnanceSurveyMap(tileSource.BASEURL, tileSource.ZOOM_MINLEVEL);
				break;
			case 6:
				generator = new TileURLGeneratorMS(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 7:
				generator = new TileURLGeneratorDOCELUPL(tileSource.BASEURL);
				break;
			case 8:
				generator = new TileURLGeneratorVFR(tileSource.BASEURL);
				break;
			case 9:
				generator = new TileURLGeneratorAVC(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 10:
				generator = new TileURLGeneratorSovMilMap(tileSource.BASEURL);
				break;
			case 11:
				generator = new TileURLGeneratorVFRCB(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			case 12:
				generator = new TileURLGeneratorCustom(tileSource.BASEURL, this);
				break;
			case 13:
				generator = new TileURLGeneratorEAtlas(tileSource.BASEURL, tileSource.IMAGE_FILENAMEENDING);
				break;
			default:
				generator = null;
				break;
			}
		}
		
		return generator;
	}
	
	private String CacheDatabaseName(TileSourceBase aTileSource) {
		if(!aTileSource.mOnlineMapCacheEnabled && !aTileSource.LAYER) // Cache Enabled?
			return null;
		if(aTileSource.TIMEDEPENDENT)
			return null;
		if(aTileSource.CACHE.trim().equalsIgnoreCase(""))
			return aTileSource.ID;
		else
			return aTileSource.CACHE;
	}
	
	public void setReloadTileMode(boolean reloadTileMode) {
		mTileProvider.setReloadTileMode(reloadTileMode);
	}

	public Bitmap getTile(final int x, final int y, final int z) {
		return mTileProvider.getTile(x, y, z);
	}

	public String getTileURL (final int x, final int y, final int z) {
	   return mTileProvider.mTileURLGenerator.getRealURL(mTileProvider.mTileURLGenerator.Get(x,y,z),x,y);
	}

	public void Free() {
		if(mTileProvider != null) mTileProvider.Free();
	}

	protected void finalize() throws Throwable {
		Ut.d("TileSource finalize");
		super.finalize();
	}

	public int getZOOM_MINLEVEL() {
		return ZOOM_MINLEVEL;
	}

	public int getZOOM_MAXLEVEL() {
		return ZOOM_MAXLEVEL;
	}

	public int getTileSizePx(int mZoom) {
		return MAPTILE_SIZEPX;
	}

	// z2...z18
	private static final int[] baidu_bounds = {1,3,6,12,24,48,77,153,306,612,1224,2446,4892,9784,19568,39136,78272,156544,313086};

	public int getTileUpperBound(final int zoomLevel) {
//		if (this.URL_BUILDER_TYPE == 5) {
//			return OpenSpaceUpperBoundArray[zoomLevel - ZOOM_MINLEVEL];
//		} else
		if (this.PROJECTION != 4){
			return 1 << zoomLevel;
		} else {
			if (zoomLevel > 18)
				return baidu_bounds[18] * (1 << (zoomLevel-18));
			else
				return baidu_bounds[zoomLevel];
		}
	}

	public void setHandler(Handler mTileMapHandler) {
		mTileProvider.setHandler(mTileMapHandler);
		
	}
	
	public TileProviderBase getTileProvider() {
		return mTileProvider;
	}
	
	public TileURLGeneratorBase getTileURLGenerator () {
		return mTileURLGenerator;
	}

	public void postIndex() {
		mTileProvider.updateMapParams(this);
	}

	public GeoPoint findTheMap(int zoomLevel) {

		if(mTileProvider instanceof TileProviderSQLITEDB) {
			final int[] coord = ((TileProviderSQLITEDB) mTileProvider).findTheMap(zoomLevel);
			final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(coord, zoomLevel, PROJECTION);
			return new GeoPoint(bb.getLatSouthE6(), bb.getLonEastE6());
		}

		if(mTileProvider instanceof TileProviderMBTiles) {
			final int[] coord = ((TileProviderMBTiles) mTileProvider).findTheMap(zoomLevel);
			final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(coord, zoomLevel, PROJECTION);
			return new GeoPoint(bb.getLatSouthE6(), bb.getLonEastE6());
		}
		
		return null;
	}


	public void clearLoadingMapTile () {
		mTileProvider.setLoadingMapTile(null);
	}

}
