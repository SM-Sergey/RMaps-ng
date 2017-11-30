package com.sm.maps.applib.mapselector;

import android.os.Bundle;
import android.widget.ScrollView;

import android.support.v7.app.AppCompatActivity;

import com.sm.maps.applib.R;

public class MapSelectorActivity extends AppCompatActivity {
	private ScrollView mScrollView; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapselector);
		//mScrollView = (ScrollView) findViewById(R.id.GridInt);
		
	}
	
}
