package com.sm.maps.applib.tileprovider;

public class TileURLGeneratorCustom extends TileURLGeneratorBase {
	private final static String X = "{x}";
	private final static String Y = "{y}";
	private final static String XD = "{xd}";
	private final static String YD = "{yd}";
	private final static String BX = "{bx}";
	private final static String BY = "{by}";
	private final static String RY = "{ry}";
	private final static String Z = "{z}";
	private final static String ZP = "{z+1}";
	private final static String ZM = "{z-1}";
	private final static String ZR = "{17-z}";
	private final static String strGalileo = "Galileo";
	private final static String GALILEO = "{galileo}";

	public TileURLGeneratorCustom(String baseurl) {
		super(baseurl);
	}


	private int mod (int a, int b){
		int r = a%b;
		return r < 0 ? r+b : r;
	}

	@Override
	public String Get(int x, int y, int z) {

		int bx = mod(x - (1 << (z-1)), (1 << z));
		int by = (1 << z) - 1 - mod(y - (1 << (z-1)), (1 << z));

		int xxd = x / 1024;
		int yyd = y / 1024;

		return getBase()
				.replace(X, Integer.toString(x))
				.replace(Y, Integer.toString(y))
				.replace(XD, Integer.toString(xxd))
				.replace(YD, Integer.toString(yyd))
				.replace(BX, Integer.toString(bx))
				.replace(BY, Integer.toString(by))
				.replace(RY, Integer.toString((1<<z)-1-y))
				.replace(Z, Integer.toString(z))
				.replace(ZP, Integer.toString(z+1))
				.replace(ZM, Integer.toString(z-1))
				.replace(ZR, Integer.toString(17-z))
				.replace(GALILEO, strGalileo.substring(0, (x*3+y)% 8))
				;
	}

}
