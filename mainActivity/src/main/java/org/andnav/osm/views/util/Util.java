// Created by plusminus on 17:53:07 - 25.09.2008
package org.andnav.osm.views.util;

import org.andnav.osm.util.BoundingBoxE6;
import org.andnav.osm.views.util.constants.OpenStreetMapViewConstants;
import org.andnav.osm.views.util.ChinaCoordTransform;

import com.sm.maps.applib.utils.OSGB36;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class Util implements OpenStreetMapViewConstants{
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public static int[] getMapTileFromCoordinates(final int aLat, final int aLon, final int zoom, final int[] reuse, final int aProjection) {
		return getMapTileFromCoordinates(aLat / 1E6, aLon / 1E6, zoom, reuse, aProjection);
	}

	private static final int[] baidu_bounds = {1,3,6,12,24,48,77,153,306,612,1224,2446,4892,9784,19568,39136,78272,156544,313086};

	private static double baiduBound(int z) {
		if (z > 18)
			return baidu_bounds[18] * (1 << (z-18));
		else
			return baidu_bounds[z];
	}


	public static int[] getMapTileFromCoordinates(final double aLat, final double aLon, final int zoom, final int[] aUseAsReturnValue, final int aProjection) {
		final int[] out = (aUseAsReturnValue != null) ? aUseAsReturnValue : new int[2];

		if (aProjection == 3) {
			final double[] OSRef = OSGB36.LatLon2OSGB(aLat, aLon);
			out[0] = (int) ((1 - OSRef[0] / 1000000) * OpenSpaceUpperBoundArray[zoom - 7]);
			out[1] = (int) ((OSRef[1] / 1000000) * OpenSpaceUpperBoundArray[zoom - 7]);
		} else if (aProjection == 4) {
			double [] baida1 = ChinaCoordTransform.wgs84tobd09(aLon, aLat);
			double [] baida2 = BaiduMercatorProjection.ll2mc(baida1[0], baida1[1], baida1);
			int z = (18-(zoom + 1));
			out[MAPTILE_LONGITUDE_INDEX] = (int)(baida2[0])/256/(1<<z) + baidu_bounds[zoom]/2;
			out[MAPTILE_LATITUDE_INDEX] = (int)(-baida2[1])/256/(1<<z) + baidu_bounds[zoom]/2;
		} else {
			if (aProjection == 1)
				out[MAPTILE_LATITUDE_INDEX] = (int) Math.floor((1 - Math.log(Math.tan(Math.PI/4 + aLat * Math.PI / 360)) / Math.PI) / 2 * (1 << zoom));
			else {
				final double E2 = (double) aLat * Math.PI / 180;
				final long sradiusa = 6378137;
				final long sradiusb = 6356752;
				final double J2 = (double) Math.sqrt(sradiusa * sradiusa
						- sradiusb * sradiusb)
						/ sradiusa;
				final double M2 = (double) Math.log((1 + Math.sin(E2))
						/ (1 - Math.sin(E2)))
						/ 2
						- J2
						* Math.log((1 + J2 * Math.sin(E2))
								/ (1 - J2 * Math.sin(E2))) / 2;
				final double B2 = (double) (1 << zoom);
				out[MAPTILE_LATITUDE_INDEX] = (int) Math.floor(B2 / 2 - M2 * B2
						/ 2 / Math.PI);
			}
			out[MAPTILE_LONGITUDE_INDEX] = (int) Math.floor((aLon + 180) / 360 * (1 << zoom));
		}

		return out;
	}

	// Conversion of a MapTile to a BoundingBox

	public static BoundingBoxE6 getBoundingBoxFromMapTile(final int[] aMapTile, final int zoom, final int aProjection) {
		final int y = aMapTile[MAPTILE_LATITUDE_INDEX];
		final int x = aMapTile[MAPTILE_LONGITUDE_INDEX];

		if(aProjection == 3){
			final double[] LatLon0 = OSGB36.OSGB2LatLon(
					(double)((OpenSpaceUpperBoundArray[zoom - 7] - y - 1) * 1000000
							/ OpenSpaceUpperBoundArray[zoom - 7]), (double)(x * 1000000
							/ OpenSpaceUpperBoundArray[zoom - 7]));
			final double[] LatLon1 = OSGB36.OSGB2LatLon(
					(double)((OpenSpaceUpperBoundArray[zoom - 7] - y - 1 + 1) * 1000000
							/ OpenSpaceUpperBoundArray[zoom - 7]), (double)((x + 1) * 1000000
							/ OpenSpaceUpperBoundArray[zoom - 7]));
			return new BoundingBoxE6(LatLon1[0], LatLon1[1], LatLon0[0], LatLon0[1]);
		} else {
			double[] latLon1 = tile2LatLon(x, y, zoom, aProjection);
			double[] latLon2 = tile2LatLon(x+1, y+1, zoom, aProjection);
			return new BoundingBoxE6(latLon1[0], latLon2[1], latLon2[0], latLon1[1]);
		}
	}

	private static double[] tile2LatLon (int x, int y, int aZoom, int aProjection) {
		double[] rv = new double[2];

		if (aProjection == 4) {
			int z = (18-(aZoom + 1));

			double[] arv = new double[2];
			// lon
			arv[0] = (x - baidu_bounds[aZoom]/2)*256*(1<<z);
			// lat
			arv[1] = -(y - baidu_bounds[aZoom]/2)*256*(1<<z);

			BaiduMercatorProjection.mc2ll(arv[0], arv[1], arv);
			arv = ChinaCoordTransform.bd09towgs84(arv[0], arv[1]);

			rv[1] = arv[0];
			rv[0] = arv[1];
			return rv;
		}


		double sc = (1 << aZoom);

		rv[1] = ((double)x) / sc * 360.0 - 180;

		if (aProjection == 1) {
			final double n = Math.PI - (2.0 * Math.PI * y / sc);
			rv[0] = 180.0 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
		} else {
			final double y1 = 20037508.342789 * (1 - 2 / sc * y);

			final double r_major = 6378137.0; //Equatorial Radius, WGS84
			final double r_minor = 6356752.314245179; //defined as constant

			double ts = Math.exp(-y1 / r_major);
			double phi = Math.PI / 2 - 2 * Math.atan(ts);
			double dphi = 1.0;
			int i;
			for (i = 0; Math.abs(dphi) > 0.000000001 && i < 15; i++) {
				double con = (Math.sqrt(1.0 - (r_minor / r_major * r_minor / r_major))) * Math.sin(phi);
				dphi = Math.PI / 2 - 2 * Math.atan(ts * Math.pow((1.0 - con) / (1.0 + con), (0.5 * (Math.sqrt(1.0 - (r_minor / r_major * r_minor / r_major)))))) - phi;
				phi += dphi;
			}
			rv[0] = phi / (Math.PI / 180.0);
		}

		return rv;
	}
	/*
	private static double tile2lon(int x, int aZoom) {
		return (x / Math.pow(2.0, aZoom) * 360.0) - 180;
	}

	private static double tile2lat(int y, int aZoom, final int aProjection) {

		if (aProjection == 1) {
			final double n = Math.PI - ((2.0 * Math.PI * y) / Math.pow(2.0, aZoom));
			//final double n1 = 180.0 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
			return 180.0 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));

			//return 180 / Math.PI * (2 * Math.atan(Math.exp(a*Math.PI/180)) - Math.PI/2); // http://wiki.openstreetmap.org/wiki/Mercator
		} else if (aProjection == 4) {

		} else {
			final double y1 = 20037508.342789 - 20037508.342789 * 2 / (1 << aZoom) * y;
			
			final double r_major = 6378137.0; //Equatorial Radius, WGS84
			final double r_minor = 6356752.314245179; //defined as constant
			
			double ts = Math.exp ( -y1 / r_major);
	        double phi = Math.PI/2 - 2 * Math.atan(ts);
	        double dphi = 1.0;
	        int i;
	        for (i = 0; Math.abs(dphi) > 0.000000001 && i < 15; i++) {
	                double con = (Math.sqrt(1.0 - (r_minor/r_major * r_minor/r_major))) * Math.sin (phi);
	                dphi = Math.PI/2 - 2 * Math.atan (ts * Math.pow((1.0 - con) / (1.0 + con), (0.5 * (Math.sqrt(1.0 - (r_minor/r_major * r_minor/r_major)))))) - phi;
	                phi += dphi;
	        }
	        return phi / (Math.PI / 180.0);	

		}
	}
*/
	public static int x2lon(int x, int aZoom, final int MAPTILE_SIZEPX) {
		int px = MAPTILE_SIZEPX * (1 << aZoom);
		if (x < 0)
			x = px + x;
		if (x > px)
			x = x - px;
		return (int) (1E6 * (((double)x / px * 360.0) - 180));
	}

	public static double y2lat(int y, int aZoom, final int MAPTILE_SIZEPX) {
//		final int aProjection = 1;

//		if (aProjection == 1) {
			final double n = Math.PI
					- ((2.0 * Math.PI * y) / MAPTILE_SIZEPX * Math.pow(2.0, aZoom));
			return 180.0 / Math.PI
					* Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
//		} else {
//			final double MerkElipsK = 0.0000001;
//			final long sradiusa = 6378137;
//			final long sradiusb = 6356752;
//			final double FExct = (double) Math.sqrt(sradiusa * sradiusa
//					- sradiusb * sradiusb)
//					/ sradiusa;
//			final int TilesAtZoom = 1 << aZoom;
//			double result = (y - TilesAtZoom / 2)
//					/ -(TilesAtZoom / (2 * Math.PI));
//			result = (2 * Math.atan(Math.exp(result)) - Math.PI / 2) * 180
//					/ Math.PI;
//			double Zu = result / (180 / Math.PI);
//			double yy = ((y) - TilesAtZoom / 2);
//
//			double Zum1 = Zu;
//			Zu = Math
//					.asin(1
//							- ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct
//									* Math.sin(Zum1), FExct))
//							/ (Math.exp((2 * yy)
//									/ -(TilesAtZoom / (2 * Math.PI))) * Math
//									.pow(1 + FExct * Math.sin(Zum1), FExct)));
//			while (Math.abs(Zum1 - Zu) >= MerkElipsK) {
//				Zum1 = Zu;
//				Zu = Math
//						.asin(1
//								- ((1 + Math.sin(Zum1)) * Math.pow(1 - FExct
//										* Math.sin(Zum1), FExct))
//								/ (Math.exp((2 * yy)
//										/ -(TilesAtZoom / (2 * Math.PI))) * Math
//										.pow(1 + FExct * Math.sin(Zum1), FExct)));
//			}
//
//			result = Zu * 180 / Math.PI;
//
//			return result;
//		}
	}







	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
