package com.sm.maps.applib.tileprovider;

import com.sm.maps.applib.tileprovider.TileSource;

public class TileURLGeneratorCustom extends TileURLGeneratorBase {
	private final static String X = "{x}";
	private final static String Y = "{y}";
	private final static String BX = "{bx}";
	private final static String BY = "{by}";
	private final static String RY = "{ry}";
	private final static String Z = "{z}";
	private final static String ZP = "{z+1}";
	private final static String ZM = "{z-1}";
	private final static String ZR = "{17-z}";
	private final static String strGalileo = "Galileo";
	private final static String GALILEO = "{galileo}";

	private TileSource mTileSource;

	public TileURLGeneratorCustom(String baseurl, TileSource aTileSource) {
		super(baseurl);
		mTileSource = aTileSource;
	}

	@Override
	public String Get(int x, int y, int z) {



		int bx = x - mTileSource.getTileUpperBound(z)/2;
		int by = mTileSource.getTileUpperBound(z)/2 - y;

		return getBase()
				.replace(X, Integer.toString(x))
				.replace(Y, Integer.toString(y))
				.replace(BX, (bx < 0) ? ("M" + Integer.toString(-bx)) : Integer.toString(bx))
				.replace(BY, (by < 0) ? ("M" + Integer.toString(-by)) : Integer.toString(by))
				.replace(RY, Integer.toString((1<<z)-1-y))
				.replace(Z, Integer.toString(z))
				.replace(ZP, Integer.toString(z+1))
				.replace(ZM, Integer.toString(z-1))
				.replace(ZR, Integer.toString(17-z))
				.replace(GALILEO, strGalileo.substring(0, (x*3+y)% 8))
				;
	}

}
