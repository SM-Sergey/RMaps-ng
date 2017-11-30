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

	final private static String R0 = "{r0}";
	final private static String R1 = "{r1}";
	final private static String R3 = "{r3}";
	final private static String RA = "{ra}";
	final private static String RB = "{rb}";
	final private static String[] abcd = {"a","b","c","d"};

	public String getRealURL(String url){
		final int rn = (int)Math.floor((Math.random() * 4.0f));
		final int r3 = (int)Math.floor((Math.random() * 3.0f));
		final int rb = (int)Math.floor((Math.random() * 2.0f));

		return url.replace(R0, Integer.toString(rn))
				.replace(R1, Integer.toString(rn+1))
				.replace(R3, Integer.toString(r3))
				.replace(RA, abcd[rn])
				.replace(RB, abcd[rb]);

	}

	public void Free() {
		
	}

}
