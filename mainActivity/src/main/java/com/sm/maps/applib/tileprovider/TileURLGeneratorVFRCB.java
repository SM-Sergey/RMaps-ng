package com.sm.maps.applib.tileprovider;

public class TileURLGeneratorVFRCB extends TileURLGeneratorBase {
	private String IMAGE_FILENAMEENDING;
	
	public TileURLGeneratorVFRCB(String mName, String imagefilename) {
		super(mName);
		IMAGE_FILENAMEENDING = imagefilename;
	}

	@Override
	public String Get(int x, int y, int z) {
		final int tilecount = (int) Math.pow(2, z);
		return new StringBuilder().append(getBase())
		.append(SLASH)
		.append(tilecount - y - 1)
		.append(this.IMAGE_FILENAMEENDING)
		.toString();
	}
	
}
