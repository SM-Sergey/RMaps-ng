package com.sm.maps.applib.tileprovider;

/* FROM SAS Planet "GetUrlScript" */

public class TileURLGeneratorEAtlas extends TileURLGeneratorBase {
	private String IMAGE_FILENAMEENDING;

	public TileURLGeneratorEAtlas(String baseurl, String imagefilename) {
		super(baseurl);
		IMAGE_FILENAMEENDING = imagefilename;
	}

	private static final char[] ABC = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I','J',
			                             'K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z' };

	private static final int[] MAX = {15,15,15,15,15,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16};


	@Override
	public String Get(int x, int y, int z) {

	int	x1=0,x2=0,x3=0,
		y1=0,y2=0,y3=0,
		px1=0,px2=0,px3=0,
		py1=0,py2=0,py3=0,
		ppx1=0,ppx2=0,ppx3=0,
		ppy1=0,ppy2=0,ppy3=0,
		dx3=0,dx2=0,dx1=0,
		dy3=0,dy2=0,dy1=0;

		String sxy = "";
		String strx3, stry3, strx2, stry2, strx1, stry1;

		// dummy URL for caching
		if (z > 17)
			return new StringBuilder().append(getBase())
				.append("x")
				.append(x)
				.append("y")
				.append(y)
				.append("z")
				.append(z).toString();


		int zp = (1<<(z-1));

		if (y >= zp && x >= zp) sxy="/A";
		if (y >= zp && x <  zp) sxy="/B";
		if (y <  zp && x <  zp) sxy="/C";
		if (y <  zp && x >= zp) sxy="/D";

		dx3 = Math.abs(zp - x);
		dy3 = Math.abs(zp - y);

		if (z > 2) {
			dx2 = dx3/MAX[z];
			dy2 = dy3/MAX[z];
			dx1 = dx2/MAX[z];
			dy1 = dy2/MAX[z];
		}

		px3=dx3 / 35;
		py3=dy3 / 35;
		ppx3=px3 / 35;
		ppy3=py3 / 35;
		x3=dx3 % 35;
		y3=dy3 % 35;
		px3=px3-ppx3*35;
		py3=py3-ppy3*35;

		px2=dx2 / 35;
		py2=dy2 / 35;
		ppx2=px2 / 35;
		ppy2=py2 / 35;
		x2=dx2 % 35;
		y2=dy2 % 35;
		px2=px2-ppx2*35;
		py2=py2-ppy2*35;

		px1=dx1 / 35;
		py1=dy1 / 35;
		ppx1=px1 / 35;
		ppy1=py1 / 35;
		x1=dx1 % 35;
		y1=dy1 % 35;
		px1=px1-ppx1*35;
		py1=py1-ppy1*35;

		strx3=""+ABC[x3];
		stry3=""+ABC[y3];
		if (px3>0 || ppx3>0)  strx3=ABC[px3]+strx3;
		if (py3>0 || ppy3>0) stry3=ABC[py3]+stry3;
		if (ppx3>0) strx3=ABC[ppx3]+strx3;
		if (ppy3>0) stry3=ABC[ppy3]+stry3;

		strx2=""+ABC[x2];
		stry2=""+ABC[y2];
		if (px2>0)  strx2=ABC[px2]+strx2;
		if (py2>0)  stry2=ABC[py2]+stry2;
		if (ppx2>0)  strx2=ABC[ppx2]+strx2;
		if (ppy2>0)  stry2=ABC[ppy2]+stry2;

		strx1=""+ABC[x1];
		stry1=""+ABC[y1];
		if (px1>0) strx1=ABC[px1]+strx1;
		if (py1>0) stry1=ABC[py1]+stry1;
		if (ppx1>0)  strx1=ABC[ppx1]+strx1;
		if (ppy1>0)  stry1=ABC[ppy1]+stry1;

		return new StringBuilder().append(getBase())
		.append("Z")
		.append(18-z)
		.append(sxy)
		.append("/L1")
		.append(strx1)
		.append("Z")
		.append(stry1)
        .append("/L2")
		.append(strx2)
		.append("Z")
		.append(stry2)
		.append("/")
		.append(strx3)
		.append("Z")
		.append(stry3)
 		.append(this.IMAGE_FILENAMEENDING)
		.toString();
	}


}
