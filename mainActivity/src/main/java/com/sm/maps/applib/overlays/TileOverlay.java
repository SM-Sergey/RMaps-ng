package com.sm.maps.applib.overlays;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.util.MyMath;
import org.andnav.osm.util.constants.OpenStreetMapConstants;
import org.andnav.osm.views.util.Util;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.content.Context;

import com.sm.maps.applib.tileprovider.MessageHandlerConstants;
import com.sm.maps.applib.tileprovider.TileSource;
import com.sm.maps.applib.utils.Ut;
import com.sm.maps.applib.view.IMoveListener;
import com.sm.maps.applib.view.TileView;
import com.sm.maps.applib.view.TileViewOverlay;

public class TileOverlay extends TileViewOverlay implements OpenStreetMapConstants, OpenStreetMapViewConstants {
	private TileSource mTileSource;
	private double mOffsetLat, mOffsetLon;
	final Matrix mMatrixBearing = new Matrix();
	final Rect mRectDraw = new Rect();
	final Paint mPaint = new Paint();
	private TileMapHandler mTileMapHandler = new TileMapHandler();
	private TileView mTileView;
	private boolean mAsOverlay;
	private IMoveListener mMoveListener;
	private int mCacheMult;
	private int mLastTiles = 0;
	private int mLastRX = 3;
	private int mLastRY = 3;


	public TileOverlay(TileView tileView, boolean asOverlay) {
		super();
		mPaint.setFilterBitmap(true);
		mPaint.setAntiAlias(true);
		
		mTileView = tileView;
		mAsOverlay = asOverlay;
		mMoveListener = null;

		InitCacheMult(false);

		final SharedPreferences prefs = mTileView.getContext().getSharedPreferences("settings", 0);
		final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				if (key.equals("pref_memcache")) {
					InitCacheMult(true);
				}
			}
		};
		prefs.registerOnSharedPreferenceChangeListener(listener);

	}

	public void InitCacheMult(boolean fResize) {
		Context ctx = mTileView.getContext();
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		mCacheMult = Integer.parseInt(pref.getString("pref_memcache", "2"));
		if (fResize && mTileSource!=null && mLastTiles != 0)
			mTileSource.getTileProvider().ResizeCashe(mLastTiles*mCacheMult);
	}

	public void setTileSource(TileSource tileSource) {
		if(mTileSource != null)
			mTileSource.Free();
		
		mTileSource = tileSource;
		mTileSource.setHandler(mTileMapHandler);
		mOffsetLat = mTileSource.OFFSET_LAT;
		mOffsetLon = mTileSource.OFFSET_LON;
	}
	
	public TileSource getTileSource() {
		return mTileSource;
	}
	
	public void setMoveListener(IMoveListener moveListener) {
		mMoveListener = moveListener;
	}
	
	public void setOffset(double lat, double lon) {
		mOffsetLat = lat; 
		mOffsetLon = lon;
	}

	@Override
	public void Free() {
		if(mTileSource != null)
			mTileSource.Free();
		super.Free();
	}

	@Override
	protected void onDraw(Canvas c, TileView tileView) {
		if (mTileSource != null) {

			final int tileSizePxNotScale = mTileSource.getTileSizePx(tileView.getZoomLevel());
			final int tileSizePx = (int) (tileSizePxNotScale * tileView.getTouchScale());
			final int[] centerMapTileCoords = Util.getMapTileFromCoordinates(tileView.mLatitudeE6 + (int)(1E6 * mOffsetLat), tileView.mLongitudeE6 + (int)(1E6 * mOffsetLon), tileView.getZoomLevel(), null, mTileSource.PROJECTION);

			final Point upperLeftCornerOfCenterMapTile = getUpperLeftCornerOfCenterMapTileInScreen(tileView,
					centerMapTileCoords, tileSizePx, mOffsetLat, mOffsetLon, null);
			final int centerMapTileScreenLeft = upperLeftCornerOfCenterMapTile.x;
			final int centerMapTileScreenTop = upperLeftCornerOfCenterMapTile.y;

			final int mapTileUpperBound = mTileSource.getTileUpperBound(tileView.getZoomLevel());
			final int[] mapTileCoords = new int[] {
					centerMapTileCoords[LATITUDE],
					centerMapTileCoords[LONGITUDE] };

			int x, y, r = 0, tilecnt = 0;
			mMatrixBearing.reset();
			mMatrixBearing.setRotate(360 - tileView.getBearing(), tileView.getWidth() / 2, tileView.getHeight() / 2);

			int RX = ( (tileView.getWidth()+1)/2 + tileSizePx - 1) / tileSizePx;
			int RY = ( (tileView.getHeight()+1)/2 + tileSizePx - 1) / tileSizePx;

			if (tileView.mInZoom) {
				if (RX > mLastRX) RX = mLastRX;
				if (RY > mLastRY) RY = mLastRY;
			} else {
				mLastRX = RX;
				mLastRY = RY;
			}

			while (r <= RX || r <= RY) {
				
				for(x = -r; x <= r; x++) {
					for(y = -r; y <= r; y++) {

						if(x != -r && x != r && y != -r && y != r) continue;

						if (x < -RX || x > RX || y < -RY || y > RY ) continue;

						mapTileCoords[LATITUDE] = MyMath.mod(centerMapTileCoords[LATITUDE] + y, mapTileUpperBound);
						mapTileCoords[LONGITUDE] = MyMath.mod(centerMapTileCoords[LONGITUDE] + x, mapTileUpperBound);

						final int tileLeft = centerMapTileScreenLeft + (x * tileSizePx);
						final int tileTop = centerMapTileScreenTop + (y * tileSizePx);
						mRectDraw.set(tileLeft, tileTop, tileLeft + tileSizePx, tileTop + tileSizePx);

						float arr[] = {mRectDraw.left, mRectDraw.top, mRectDraw.right, mRectDraw.top, mRectDraw.right, mRectDraw.bottom, mRectDraw.left, mRectDraw.bottom, mRectDraw.left, mRectDraw.top};
						mMatrixBearing.mapPoints(arr);

						tilecnt++;

						final Bitmap currentMapTile = this.mTileSource.getTile(mapTileCoords[LONGITUDE], mapTileCoords[LATITUDE], tileView.getZoomLevel());
						if (currentMapTile != null) {
							if (!currentMapTile.isRecycled())
								c.drawBitmap(currentMapTile, null, mRectDraw, mPaint);

							if (tileView.mDrawTileGrid || OpenStreetMapViewConstants.DEBUGMODE) {
								c.drawLine(tileLeft, tileTop, tileLeft + tileSizePx, tileTop, mPaint);
								c.drawLine(tileLeft, tileTop, tileLeft, tileTop + tileSizePx, mPaint);
								c.drawText("y x = " + mapTileCoords[LATITUDE] + " " + mapTileCoords[LONGITUDE] + " zoom " + tileView.getZoomLevel() + " ", tileLeft + 5,
										tileTop + 15, mPaint);
								c.drawText(this.mTileSource.getTileURL(mapTileCoords[LONGITUDE], mapTileCoords[LATITUDE], tileView.getZoomLevel()),
										 tileLeft + 5, tileTop + 40, mPaint);
							}

						}
					}
				}
				
				r++;

			}

			mLastTiles = tilecnt;
			mTileSource.getTileProvider().ResizeCashe(mLastTiles*mCacheMult);
			mTileSource.setReloadTileMode(false);
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, TileView tileView) {
	}

	// TODO След процедуры под вопросом о переделке
	private static final int LATITUDE = 0;
	private static final int LONGITUDE = 1;

	public Point getUpperLeftCornerOfCenterMapTileInScreen(TileView tileView, final int[] centerMapTileCoords, final int tileSizePx, final Point reuse) {
		return getUpperLeftCornerOfCenterMapTileInScreen(tileView, centerMapTileCoords, tileSizePx, 0, 0, reuse);
	}

	public Point getUpperLeftCornerOfCenterMapTileInScreen(TileView tileView, final int[] centerMapTileCoords, final int tileSizePx, final double offsetLat, final double offsetLon, final Point reuse) {
		final Point out = (reuse != null) ? reuse : new Point();

		final int viewWidth = tileView.getWidth();
		final int viewWidth_2 = viewWidth / 2;
		final int viewHeight = tileView.getHeight();
		final int viewHeight_2 = viewHeight / 2;

		final BoundingBoxE6 bb = Util.getBoundingBoxFromMapTile(centerMapTileCoords,
				tileView.getZoomLevel(), mTileSource.PROJECTION);
		final float[] relativePositionInCenterMapTile = bb
				.getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(
						tileView.mLatitudeE6 + (int)(1E6 * offsetLat), tileView.mLongitudeE6 + (int)(1E6 * offsetLon), null);

		final int centerMapTileScreenLeft = viewWidth_2
				- (int) (0.5f + (relativePositionInCenterMapTile[LONGITUDE] * tileSizePx));
		final int centerMapTileScreenTop = viewHeight_2
				- (int) (0.5f + (relativePositionInCenterMapTile[LATITUDE] * tileSizePx));

		out.set(centerMapTileScreenLeft, centerMapTileScreenTop);
		return out;
	}



	private class TileMapHandler extends Handler {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MessageHandlerConstants.MAPTILEFSLOADER_SUCCESS_ID:
				mTileView.postQueuedInvalidate();
				break;
			case MessageHandlerConstants.MAPTILEFSLOADER_INDEXIND_SUCCESS_ID:
				mTileSource.postIndex();
				
				if(!mAsOverlay) {
					mTileView.setZoomLevel(mTileView.getZoomLevel(), true);
					if(mMoveListener != null)
						mMoveListener.onZoomDetected();
				}
				break;
			}
		}
	}

}
