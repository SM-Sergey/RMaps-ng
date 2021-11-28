package com.sm.maps.applib.overlays;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.andnav.osm.util.GeoPoint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;

import com.sm.maps.applib.MainActivity;
import com.sm.maps.applib.kml.PoiManager;
import com.sm.maps.applib.kml.Track;
import com.sm.maps.applib.utils.SimpleThreadFactory;
import com.sm.maps.applib.utils.Ut;
import com.sm.maps.applib.view.TileView;
import com.sm.maps.applib.view.TileViewOverlay;

public class TrackOverlay extends TileViewOverlay {
	private Paint[] mPaints;
	private int mLastZoom;
	private Path[] mPaths;
	private Track[] mTracks;
	private Point mBaseCoords;
	private GeoPoint mBaseLocation;
	private PoiManager mPoiManager;
	private TrackThread mThread;
	private boolean mThreadRunned = false;
	private TileView mOsmv;
	private Handler mMainMapActivityCallbackHandler;
	private boolean mStopDraw = false;
	private com.sm.maps.applib.view.TileView.OpenStreetMapViewProjection mProjection;
	private double mLastTS;

	protected ExecutorService mThreadExecutor = Executors.newSingleThreadExecutor(new SimpleThreadFactory("TrackOverlay"));

	private class TrackThread extends Thread {

		@Override
		public void run() {
			Ut.d("run TrackThread");

			mPaths = null;

			if(mTracks == null){
				mTracks = mPoiManager.getTrackChecked(false);
				if(mTracks == null){
					Ut.d("Track is null. Stoped??");
					mThreadRunned = false;
					mStopDraw = true;
					return;
				}
				Ut.d("Track loaded");
			}

			try {
				mPaths = new Path[mTracks.length];
				mPaints = new Paint[mTracks.length];
				
				for(int i = 0; i < mTracks.length; i++) {
					if (mTracks[i] != null) {
						try {
							mPaths[i] = mProjection.toPixelsTrackPoints(mPoiManager.getGeoDatabase().getTrackPoints(mTracks[i].getId()), mBaseCoords, mBaseLocation);
							mPaints[i] = new Paint();
							mPaints[i].setAntiAlias(true);
							mPaints[i].setStyle(Paint.Style.STROKE);
							mPaints[i].setStrokeCap(Paint.Cap.ROUND);
							mPaints[i].setColor(mTracks[i].Color);
							mPaints[i].setStrokeWidth(mTracks[i].Width);
							mPaints[i].setAlpha(Color.alpha(mTracks[i].ColorShadow));
							mPaints[i].setShadowLayer((float) mTracks[i].ShadowRadius, 0, 0, mTracks[i].ColorShadow);

							Message.obtain(mMainMapActivityCallbackHandler, Ut.MAPTILEFSLOADER_SUCCESS_ID).sendToTarget();
						} catch (Exception e) {
							mPaths[i] = null;
						}
					} else
						mPaths[i] = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}


			mThreadRunned = false;
		}
	}

	public TrackOverlay(MainActivity mainActivity, PoiManager poiManager, Handler aHandler) {
		mMainMapActivityCallbackHandler = aHandler;
		mTracks = null;
		mPoiManager = poiManager;
		mBaseCoords = new Point();
		mBaseLocation = new GeoPoint(0, 0);
		mLastZoom = -1;
		mLastTS = 1.0;
		mThread = new TrackThread();
		mThread.setName("Track thread");
	}

	@Override
	public void Free() {
		if(mPoiManager != null)
			mPoiManager.StopProcessing();
		if(mProjection != null)
			mProjection.StopProcessing();
		mThreadExecutor.shutdown();
		super.Free();
	}

	public void setStopDraw(boolean stopdraw){
		mStopDraw = stopdraw;
	}

	@Override
	protected void onDraw(Canvas c, TileView osmv) {
		if(mStopDraw) return;

		if (!mThreadRunned && (mTracks == null || mLastZoom != osmv.getZoomLevel()) || mLastTS != osmv.getTouchScale() ) {
			mPaths = null;
			mLastZoom = osmv.getZoomLevel();
			mLastTS = osmv.getTouchScale();
			mOsmv = osmv;
			mProjection = mOsmv.getProjection();
			mThreadRunned = true;
			mThreadExecutor.execute(mThread);
			return;
		}

		if(mPaths == null)
			return;

		final com.sm.maps.applib.view.TileView.OpenStreetMapViewProjection pj = osmv.getProjection();
		final Point screenCoords = new Point();

		pj.toPixels(mBaseLocation, screenCoords);

		c.save();
		if(screenCoords.x != mBaseCoords.x && screenCoords.y != mBaseCoords.y){
			c.translate(screenCoords.x - mBaseCoords.x, screenCoords.y - mBaseCoords.y);
		};
		for(int i = 0; i < mPaths.length; i++)
			if(mPaths[i] != null && mPaints[i] != null)
				c.drawPath(mPaths[i], mPaints[i]);
		c.restore();
	}

	@Override
	protected void onDrawFinished(Canvas c, TileView osmv) {
	}

	public void clearTrack(){
		mTracks = null;
	}

}
