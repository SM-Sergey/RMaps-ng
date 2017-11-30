package com.sm.maps.applib.kml;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;
import com.sm.maps.applib.kml.constants.PoiConstants;

import android.support.v7.app.AppCompatActivity;

import com.sm.maps.applib.R;

public class PoiIconSetActivity extends AppCompatActivity implements PoiConstants {
	private GridView mGridInt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.poiiconset);

		mGridInt = (GridView) findViewById(R.id.GridInt);
		mGridInt.setAdapter(new AppsAdapter());
		
		mGridInt.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				//Toast.makeText(PoiIconSetActivity.this, "sel="+arg3, Toast.LENGTH_SHORT).show();
				setResult(RESULT_OK, (new Intent()).putExtra("iconid", arg2));
				finish();
			}
		});
	}

    public class AppsAdapter extends BaseAdapter {
        public AppsAdapter() {
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i;

            int dpi = getResources().getDisplayMetrics().densityDpi;
            int grid =  dpi <= DisplayMetrics.DENSITY_MEDIUM ? 53 : dpi / 3;

            if (convertView == null) {
                i = new ImageView(PoiIconSetActivity.this);
                i.setScaleType(ImageView.ScaleType.FIT_CENTER);
                i.setLayoutParams(new GridView.LayoutParams(grid, grid));
            } else {
                i = (ImageView) convertView;
            }

            i.setImageResource(POI_RES_ID[position]);

            return i;
        }


        public final int getCount() {
            return POI_LAST + 1;
        }

        public final Object getItem(int position) {
            return null;
        }

        public final long getItemId(int position) {
            return POI_RES_ID[position];
        }
    }
	
}
