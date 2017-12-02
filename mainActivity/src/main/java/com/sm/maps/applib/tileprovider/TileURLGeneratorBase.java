package com.sm.maps.applib.tileprovider;

public class TileURLGeneratorBase {
	protected final String mName;
	protected static final String DELIMITER = "_";
	protected static final String COMMA = ",";
	protected static final String SLASH = "/";
	protected static final char[][] M_TSQR = {{'q','t'},{'r','s'}};

	public TileURLGeneratorBase(String mName) {
		this.mName = mName;
	}

	public String getBase(){
		return mName;
	}

	public String Get(final int x, final int y, final int z) {
		return new StringBuilder(getBase())
		.append(DELIMITER)
		.append(x)
		.append(DELIMITER)
		.append(y)
		.append(DELIMITER)
		.append(z)
		.toString();
	}
	
	protected String getQRTS(int x, int y, int zoomLevel){
		int i;
		int mask;

		String result = "t";
		mask = 1 << zoomLevel;
		x = x % mask;
		if (x < 0) x += mask;
		for (i = 2; i <= zoomLevel+1; i++){
			mask = mask >> 1;
		    result += M_TSQR[((x & mask) > 0)? 1 : 0][((y & mask) > 0)? 1 : 0];
		}
		return result;
	}

	final private static String R03 = "{r03}";
	final private static String R12 = "{r12}";
	final private static String R14 = "{r14}";
	final private static String R02 = "{r02}";
	final private static String R13 = "{r13}";
	final private static String RAD = "{rad}";
	final private static String RAB = "{rab}";
	final private static String RAC = "{rac}";
	final private static String RXY = "{rxy}";
	final private static String[] abcd = {"a","b","c","d"};

	public String getRealURL(String url, int x, int y){
		final int rn = (int)Math.floor((Math.random() * 4.0f));
		final int r3 = (int)Math.floor((Math.random() * 3.0f));
		final int rb = (int)Math.floor((Math.random() * 2.0f));
		final int xy = x%4 + (y%4)*4;

		return url.replace(R03, Integer.toString(rn))
				.replace(R14, Integer.toString(rn+1))
				.replace(R12, Integer.toString(rb+1))
				.replace(R02, Integer.toString(r3))
				.replace(R13, Integer.toString(r3+1))
				.replace(RAD, abcd[rn])
				.replace(RAB, abcd[rb])
				.replace(RAC, abcd[r3])
				.replace(RXY, Integer.toString(xy))

				;

	}

	public void Free() {
		
	}

}
