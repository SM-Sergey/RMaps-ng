package com.sm.maps.applib.tileprovider;


public class TileURLGeneratorVFR extends TileURLGeneratorBase {
	
	public TileURLGeneratorVFR(String mName) {
		super(mName);
	}

	@Override
	public String Get(int x, int y, int z) {
		return new StringBuilder().append(getBase())
		.append("x=")
		.append(x)
		.append("&y=")
		.append(y)
		.append("&z=")
		.append(18-z-1)
		.toString();
	}
	
}
