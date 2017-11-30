package com.sm.maps.applib.view;

import java.util.List;

import org.andnav.osm.util.GeoPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.sm.maps.applib.R;
import com.sm.maps.applib.tileprovider.TileSource;
import com.sm.maps.applib.utils.ScaleBarDrawable;

public class MapView extends RelativeLayout {
	public static final int ZOOM_CONTROL_HIDE = 0;
	public static final int ZOOM_CONTROL_TOP = 1;
	public static final int ZOOM_CONTROL_BOTTOM = 2;
	public static final String MAPNAME = "MapName";
	
	private final TileView mTileView;
	private final MapController mController;
	private IMoveListener mMoveListener;
	private boolean mStopBearing = false;
	private boolean mUseVolumeControl;
	private ScaleBarDrawable mScaleBarDrawable;
	private ImageView ivScaleBar = null;

	public MapView(Context context, int sideInOutButtons, int scaleBarVisible) {
		super(context);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		mUseVolumeControl = pref.getBoolean("pref_use_volume_controls", true);
		mController = new MapController();
		mTileView = new TileView(context);
		mMoveListener = null;
		addView(mTileView, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		final LinearLayout ll = new LinearLayout(context);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		ll.setId(R.id.dashboard_area);
		final RelativeLayout.LayoutParams dashboardParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		dashboardParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		addView(ll, dashboardParams);
		
		final RelativeLayout rl = new RelativeLayout(context);
		rl.setId(R.id.right_area);
		final RelativeLayout.LayoutParams rigthAreaParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		rigthAreaParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rigthAreaParams.addRule(RelativeLayout.BELOW, R.id.dashboard_area);
		addView(rl, rigthAreaParams);
		
		displayZoomControls(sideInOutButtons);

		if (scaleBarVisible == 1) {
	                ivScaleBar = new ImageView(getContext());
			mScaleBarDrawable = new ScaleBarDrawable(context, this, Integer.parseInt(pref.getString("pref_units","0")));
			ivScaleBar.setImageDrawable(mScaleBarDrawable);
			ivScaleBar.setId(R.id.scale_bar);
			final RelativeLayout.LayoutParams scaleParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			if(sideInOutButtons == 1)
				scaleParams.addRule(RelativeLayout.RIGHT_OF, R.id.whatsnew);
			else
				scaleParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			scaleParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			addView(ivScaleBar, scaleParams);
		}

		setFocusable(true);
		setFocusableInTouchMode(true);
	}

	public MapView(Context context) {
		this(context, 1, 1);

	}

	public MapView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MapView);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		mController = new MapController();
		mTileView = new TileView(context);
		addView(mTileView, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		final LinearLayout ll = new LinearLayout(context);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		ll.setId(R.id.dashboard_area);
		final RelativeLayout.LayoutParams dashboardParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		dashboardParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		addView(ll, dashboardParams);
		
		final RelativeLayout rl = new RelativeLayout(context);
		rl.setId(R.id.right_area);
		final RelativeLayout.LayoutParams rigthAreaParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		rigthAreaParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rigthAreaParams.addRule(RelativeLayout.BELOW, R.id.dashboard_area);
		addView(rl, rigthAreaParams);
		
		final int sideBottom = a.getInt(R.styleable.MapView_SideInOutButtons, 0);
		displayZoomControls(sideBottom);

		if (a.getInt(R.styleable.MapView_SideInOutButtons, 0) == 1) {
	        final ImageView ivScaleBar = new ImageView(getContext());
			mScaleBarDrawable = new ScaleBarDrawable(context, this, Integer.parseInt(pref.getString("pref_units","0")));
			ivScaleBar.setImageDrawable(mScaleBarDrawable);
			final RelativeLayout.LayoutParams scaleParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			scaleParams.addRule(RelativeLayout.RIGHT_OF, R.id.whatsnew);
			scaleParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			addView(ivScaleBar, scaleParams);
		}
		if(a.getBoolean(R.styleable.MapView_DisableControl, false))
			mTileView.setDisableControl(true);
		

        a.recycle();
	}
	
	public TileView getTileView() {
		return mTileView;
	}

	public class MapController {
		public void setCenter(GeoPoint point) {
			mTileView.setMapCenter(point);
		}
		
		public void setZoom(int zoom) {
			mTileView.setZoomLevel(zoom, true);
		}
		
		public void zoomOut() {
			mTileView.zoomOut();
		}

		public void zoomIn() {
			mTileView.zoomIn();
		}
}
	
	public MapController getController() {
		return mController;
	}
	
	public void setTileSource(TileSource tilesource) {
		mTileView.setTileSource(tilesource);
		if(mScaleBarDrawable != null && tilesource != null)
			mScaleBarDrawable.correctScale(tilesource.MAPTILE_SIZE_FACTOR, tilesource.GOOGLESCALE_SIZE_FACTOR);
	}
	
	public TileSource getTileSource() {
		return mTileView.getTileSource();
	}
	
	public void displayZoomControls(final boolean takeFocus) {
		displayZoomControls(1);
	}
	
	public void displayZoomControls(final int SideInOutButtons) {
		RelativeLayout rigthArea = (RelativeLayout) findViewById(R.id.right_area);
		
		final LinearLayout ll = new LinearLayout(getContext());
		ll.setId(R.id.right_panel);
		ll.setOrientation(LinearLayout.VERTICAL);
        final RelativeLayout.LayoutParams llParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        llParams.addRule(RelativeLayout.CENTER_VERTICAL);
        llParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rigthArea.addView(ll, llParams);
        final int pad = getResources().getDimensionPixelSize(R.dimen.zoom_ctrl_padding);
		
		if(SideInOutButtons == 0) return;
		
        final ImageView ivZoomIn = new ImageView(getContext());
        ivZoomIn.setImageResource(R.drawable.zoom_in);

        if(SideInOutButtons == 3) {
        	ivZoomIn.setPadding(0, pad, 0, pad);
        	ll.addView(ivZoomIn);
        } else {
	        final RelativeLayout.LayoutParams zoominParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        zoominParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	        if(SideInOutButtons != 2) {
		        zoominParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	        } else {
		        zoominParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	        }
	        rigthArea.addView(ivZoomIn, zoominParams);
        }
        
        ivZoomIn.setOnClickListener(new OnClickListener(){
			// @Override
			public void onClick(View v) {
				mTileView.zoomIn();
				if(mMoveListener != null)
					mMoveListener.onZoomDetected();
			}
        });
        ivZoomIn.setOnLongClickListener(new OnLongClickListener(){
			// @Override
			public boolean onLongClick(View v) {
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
				final int zoom = Integer.parseInt(pref.getString("pref_zoommaxlevel", "17"));
				if (zoom > 0) {
					mTileView.setZoomLevel(zoom - 1, true);
					if(mMoveListener != null)
						mMoveListener.onZoomDetected();
				}
				return true;
			}
        });

        final ImageView ivZoomOut = new ImageView(getContext());
        ivZoomOut.setId(R.id.whatsnew);
        ivZoomOut.setImageResource(R.drawable.zoom_out);
        
        if(SideInOutButtons == 3) {
	        ivZoomOut.setPadding(0, pad, 0, pad);
        	ll.addView(ivZoomOut);
        } else {
	        final RelativeLayout.LayoutParams zoomoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        zoomoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	        if(SideInOutButtons != 2) {
	        	zoomoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	        } else {
	        	zoomoutParams.addRule(RelativeLayout.BELOW, R.id.dashboard_area);
	        }
	        addView(ivZoomOut, zoomoutParams);
        }
        
        ivZoomOut.setOnClickListener(new OnClickListener(){
			// @Override
			public void onClick(View v) {
				mTileView.zoomOut();
				if(mMoveListener != null)
					mMoveListener.onZoomDetected();
			}
        });
        ivZoomOut.setOnLongClickListener(new OnLongClickListener(){
			// @Override
			public boolean onLongClick(View v) {
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
				final int zoom = Integer.parseInt(pref.getString("pref_zoomminlevel", "10"));
				if (zoom > 0) {
					mTileView.setZoomLevel(zoom - 1, true);
					if(mMoveListener != null)
						mMoveListener.onZoomDetected();
				}
				return true;
			}
        });
	}
	
	public void InvalidateScaleBar() {
		if (ivScaleBar != null)
			ivScaleBar.invalidate();
	}

	public int getZoomLevel() {
		return mTileView.getZoomLevel();
	}
	public double getTouchScale() {
		return mTileView.getTouchScale();
	}

	public GeoPoint getMapCenter() {
		return mTileView.getMapCenter();
	}
	
	public List<TileViewOverlay> getOverlays() {
		return mTileView.getOverlays();
	}

	@Override
	protected ContextMenuInfo getContextMenuInfo() {
		return mTileView.mPoiMenuInfo;
	}

	public void setBearing(float bearing) {
		if(!mStopBearing)
			mTileView.setBearing(bearing);
	}

	public void setMoveListener(IMoveListener moveListener) {
		mMoveListener = moveListener;
		mTileView.setMoveListener(moveListener);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean stopPropagation = false;
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if(mUseVolumeControl) {
					stopPropagation = true;
					getController().zoomOut();
				}
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				if(mUseVolumeControl) {
					stopPropagation = true;
					getController().zoomIn();
				}
				break;
			case KeyEvent.KEYCODE_DPAD_UP:
				stopPropagation = true;
				getController().zoomOut();
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				stopPropagation = true;
				getController().zoomIn();
				break;
			case KeyEvent.KEYCODE_DPAD_CENTER:
				if(mStopBearing) 
					mStopBearing = false;
				else {
					setBearing(0);
					mStopBearing = true;
				}
				stopPropagation = true;
				break;
			default:
				break;
		}
		
	    if(stopPropagation) return true;
	    
	    return super.onKeyDown(keyCode, event);
	}

	@Override
	public void invalidate() {
		mTileView.postQueuedInvalidate();
		super.invalidate();
	}
}
