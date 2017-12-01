package com.sm.maps.applib.kml.constants;

import com.sm.maps.applib.R;

public interface PoiConstants {
	public static final int EMPTY_ID = -777;
	public static final int ZERO = 0;
	public static final int ONE = 1;
	public static final String EMPTY = "";
	public static final String ONE_SPACE = " ";

	public static final String LON = "lon";
	public static final String LAT = "lat";
	public static final String LATLON = "lat,lon";
	public static final String ELE = "ele";
	public static final String NAME = "name";
	public static final String DESCR = "descr";
	public static final String DESC = "desc";
	public static final String DESCRIPTION = "description";
	public static final String ALT = "alt";
	public static final String TYPE = "type";
	public static final String EXTENSIONS = "extensions";
	public static final String CATEGORYID = "categoryid";
	public static final String POINTSOURCEID = "pointsourceid";
	public static final String HIDDEN = "hidden";
	public static final String ICONID = "iconid";
	public static final String MINZOOM = "minzoom";
	public static final String MAXZOOM = "maxzoom";
	public static final String MAXDNLD = "maxdnld";
	public static final String SHOW = "show";
	public static final String TRACKID = "trackid";
	public static final String SPEED = "speed";
	public static final String DATE = "date";
	public static final String STYLE = "style";
	public static final String CNT = "cnt";
	public static final String DISTANCE = "distance";
	public static final String DURATION = "duration";
	public static final String ACTIVITY = "activity";
	public static final String MAPID = "mapid";

	public static final String POINTS = "points";
	public static final String CATEGORY = "category";
	public static final String TRACKS = "tracks";
	public static final String TRACKPOINTS = "trackpoints";
	public static final String DATA = "data";
	public static final String GEODATA_FILENAME = "/geodata.db";
	public static final String TRACK = "Track";
	public static final String PARAMS = "params";
	public static final String MAPS = "maps";

	public static final String UPDATE_POINTS = "pointid = @1";
	public static final String UPDATE_CATEGORY = "categoryid = @1";
	public static final String UPDATE_TRACKS = "trackid = @1";
	public static final String UPDATE_MAPS = "mapid = @1";

	public static final int[] POI_RES_ID =  {
			R.drawable.poi_redinfo, // poi,      // 0 - old RMaps 0x7F02000A
			R.drawable.poi_blueinfo, // blue,    // 1 - old RMaps 0x7F02000C
			R.drawable.poi_greeninfo, // green,  // 2 - old RMaps 0x7F02000D
			R.drawable.poi_grayinfo, // white,   // 3 - old RMaps 0x7F02000E
			R.drawable.poi_yellinfo, // yellow,  // 4 - old RMaps 0x7F02000F
			R.drawable.poi_magentinfo,
			R.drawable.poi_cyaninfo,

			R.drawable.poi_redbigfish,
			R.drawable.poi_bluebigfish,
			R.drawable.poi_greenbigfish,
			R.drawable.poi_graybigfish,
			R.drawable.poi_yellbigfish,
			R.drawable.poi_magentbigfish,
			R.drawable.poi_cyanbigfish,

			R.drawable.poi_redfish,
			R.drawable.poi_bluefish,
			R.drawable.poi_greenfish,
			R.drawable.poi_grayfish,
			R.drawable.poi_yellfish,
			R.drawable.poi_magentfish,
			R.drawable.poi_cyanfish,

			R.drawable.poi_redwhat,
			R.drawable.poi_bluewhat,
			R.drawable.poi_greenwhat,
			R.drawable.poi_graywhat,
			R.drawable.poi_yellwhat,
			R.drawable.poi_magentwhat,
			R.drawable.poi_cyanwhat,

			R.drawable.poi_redwood,
			R.drawable.poi_bluewood,
			R.drawable.poi_greenwood,
			R.drawable.poi_graywood,
			R.drawable.poi_yellwood,
			R.drawable.poi_magentwood,
			R.drawable.poi_cyanwood,

			R.drawable.poi_redgryb,
			R.drawable.poi_bluegryb,
			R.drawable.poi_greengryb,
			R.drawable.poi_graygryb,
			R.drawable.poi_yellgryb,
			R.drawable.poi_magentgryb,
			R.drawable.poi_cyangryb,

			R.drawable.poi_redberry,
			R.drawable.poi_blueberry,
			R.drawable.poi_greenberry,
			R.drawable.poi_grayberry,
			R.drawable.poi_yellberry,
			R.drawable.poi_magentberry,
			R.drawable.poi_cyanberry,

			R.drawable.poi_stop,
			R.drawable.poi_stop2,

			R.drawable.poi_redkar,
			R.drawable.poi_bluekar,
			R.drawable.poi_greenkar,
			R.drawable.poi_graykar,
			R.drawable.poi_yellkar,
			R.drawable.poi_magentkar,
			R.drawable.poi_cyankar,

			R.drawable.poi_redboat,
			R.drawable.poi_blueboat,
			R.drawable.poi_greenboat,
			R.drawable.poi_grayboat,
			R.drawable.poi_yellboat,
			R.drawable.poi_magentboat,
			R.drawable.poi_cyanboat,

			R.drawable.poi_redanchor,
			R.drawable.poi_blueanchor,
			R.drawable.poi_greenanchor,
			R.drawable.poi_grayanchor,
			R.drawable.poi_yellanchor,
			R.drawable.poi_magentanchor,
			R.drawable.poi_cyananchor,

			R.drawable.poi_redkemping,
			R.drawable.poi_bluekemping,
			R.drawable.poi_greenkemping,
			R.drawable.poi_graykemping,
			R.drawable.poi_yellkemping,
			R.drawable.poi_magentkemping,
			R.drawable.poi_cyankemping

	};



	public static final int POI_I_RED 	= 0;
	public static final int POI_I_BLUE 	= 1;
	public static final int POI_I_GREEN = 2;
	public static final int POI_I_WHITE	= 3;
	public static final int POI_I_YELLOW = 4;

	public static final int POI_LAST    = 78;   // count of bitmaps - 1

	public static final String POI_XLAT = " WHEN x'7F02000A' THEN " + POI_I_RED
					    + " WHEN x'7F02000C' THEN " + POI_I_BLUE
					    + " WHEN x'7F02000D' THEN " + POI_I_GREEN
					    + " WHEN x'7F02000E' THEN " + POI_I_WHITE
					    + " WHEN x'7F02000F' THEN " + POI_I_YELLOW
					    + " ELSE ";


	public static final String STAT_GET_POI_LIST = "SELECT lat, lon, points.name, descr, pointid, pointid _id, pointid ID, points.hidden AS hidden, CASE category.iconid"
							+ POI_XLAT +"category.iconid END iconid, category.name as catname FROM points LEFT JOIN category ON category.categoryid = points.categoryid ORDER BY ";

	public static final String STAT_PoiListNotHidden = "SELECT poi.lat, poi.lon, poi.name, poi.descr, poi.pointid, poi.pointid _id, poi.pointid ID, poi.categoryid,"
					+" CASE cat.iconid" + POI_XLAT +"cat.iconid END iconid FROM points poi LEFT JOIN category cat ON cat.categoryid = poi.categoryid WHERE poi.hidden = 0 AND cat.hidden = 0"
		+ " AND cat.minzoom <= @1"
		+ " AND poi.lon BETWEEN @2 AND @3"
		+ " AND poi.lat BETWEEN @4 AND @5"
		+ " ORDER BY lat, lon";

	public static final String STAT_PoiCategoryList = "SELECT name, CASE iconid" + POI_XLAT +"iconid END iconid, categoryid _id, hidden FROM category ORDER BY name";
	public static final String STAT_PoiCategoryListNotHidden = "SELECT name, CASE iconid" + POI_XLAT +"iconid END iconid, categoryid _id, hidden FROM category WHERE hidden = 0 ORDER BY name";

	public static final String STAT_ActivityList = "SELECT name, activityid _id FROM activity ORDER BY activityid";

	public static final String STAT_getPoi = "SELECT poi.lat AS lat, poi.lon AS lon, poi.name AS name, poi.descr AS desct, poi.pointid AS pointid, poi.alt AS alt, poi.hidden AS hidden, poi.categoryid AS categoryid, poi.pointsourceid AS pointsourceid,"
						 + " CASE cat.iconid" + POI_XLAT +"cat.iconid END iconid FROM points poi LEFT JOIN category cat ON cat.categoryid = poi.categoryid WHERE pointid = @1";


	public static final String STAT_deletePoi = "DELETE FROM points WHERE pointid = @1";
	public static final String STAT_deletePoiCategory = "DELETE FROM category WHERE categoryid = @1";

	public static final String STAT_getPoiCategory = "SELECT name, categoryid, hidden,"
							+ " CASE iconid" + POI_XLAT +"iconid END iconid, minzoom FROM category WHERE categoryid = @1";

	public static final String STAT_DeleteAllPoi = "DELETE FROM points";
	public static final String STAT_getTrackList = "SELECT tracks.name, activity.name || ', ' || strftime('%%d/%%m/%%Y %%H:%%M:%%S', date, 'unixepoch', 'localtime') As title2, descr, trackid _id, cnt, TIME('2011-01-01', duration || ' seconds') as duration, round(distance/1000, 2) AS distance0, show, IFNULL(duration, -1) As NeedStatUpdate, '%s' as units, round(distance/1000/1.609344, 2) AS distance1 FROM tracks LEFT JOIN activity ON activity.activityid = tracks.activity ORDER BY ";
	public static final String STAT_getTrackChecked = "SELECT name, descr, show, trackid, cnt, distance, duration, categoryid, activity, date, style FROM tracks WHERE show = 1";
	public static final String STAT_getTrack = "SELECT name, descr, show, cnt, distance, duration, categoryid, activity, date, style FROM tracks WHERE trackid = @1";
	public static final String STAT_getTrackPoints = "SELECT lat, lon, alt, speed, date FROM trackpoints WHERE trackid = @1 ORDER BY id";
	public static final String STAT_setTrackChecked_1 = "UPDATE tracks SET show = 1 - show * 1 WHERE trackid = @1";
	public static final String STAT_setCategoryHidden = "UPDATE category SET hidden = 1 - hidden * 1 WHERE categoryid = @1";
	public static final String STAT_setTrackChecked_2 = "UPDATE tracks SET show = 0 WHERE trackid <> @1";
	public static final String STAT_deleteTrack_1 = "DELETE FROM trackpoints WHERE trackid = @1";
	public static final String STAT_deleteTrack_2 = "DELETE FROM tracks WHERE trackid = @1";
	public static final String STAT_saveTrackFromWriter = "SELECT lat, lon, alt, speed, date FROM trackpoints ORDER BY id;";
	public static final String STAT_CLEAR_TRACKPOINTS = "DELETE FROM 'trackpoints';";
	public static final String STAT_get_maps = "SELECT mapid, name, type, params FROM 'maps';";
	public static final String STAT_get_map = "SELECT mapid, name, type, params FROM 'maps' WHERE mapid = @1;";
	

	public static final String SQL_CREATE_points = "CREATE TABLE 'points' (pointid INTEGER NOT NULL PRIMARY KEY UNIQUE,name VARCHAR,descr VARCHAR,lat FLOAT DEFAULT '0',lon FLOAT DEFAULT '0',alt FLOAT DEFAULT '0',hidden INTEGER DEFAULT '0',categoryid INTEGER,pointsourceid INTEGER,iconid INTEGER DEFAULT NULL);";
	public static final String SQL_CREATE_category = "CREATE TABLE 'category' (categoryid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR, hidden INTEGER DEFAULT '0', iconid INTEGER DEFAULT NULL, minzoom INTEGER DEFAULT '14');";
	public static final String SQL_CREATE_pointsource = "CREATE TABLE IF NOT EXISTS 'pointsource' (pointsourceid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR);";
	public static final String SQL_CREATE_tracks = "CREATE TABLE IF NOT EXISTS 'tracks' (trackid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR, descr VARCHAR, date DATETIME, show INTEGER, cnt INTEGER, duration INTEGER, distance INTEGER, categoryid INTEGER, activity INTEGER, style VARCHAR);";
	public static final String SQL_CREATE_routes = "CREATE TABLE IF NOT EXISTS 'routes' (routeid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR, descr VARCHAR, date DATETIME, show INTEGER, duration INTEGER, distance INTEGER, categoryid INTEGER, style VARCHAR);";
	public static final String SQL_CREATE_trackpoints = "CREATE TABLE IF NOT EXISTS 'trackpoints' (trackid INTEGER NOT NULL, id INTEGER NOT NULL PRIMARY KEY UNIQUE, lat FLOAT, lon FLOAT, alt FLOAT, speed FLOAT, date DATETIME);";
	public static final String SQL_CREATE_activity = "CREATE TABLE 'activity' (activityid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR);";
	public static final String SQL_CREATE_drop_activity = "DROP TABLE IF EXISTS 'activity';";
	public static final String SQL_CREATE_insert_activity = "INSERT INTO 'activity' (activityid, name) VALUES (%d, '%s');";
	public static final String SQL_CREATE_maps = "CREATE TABLE IF NOT EXISTS 'maps' (mapid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR, type INTEGER, params VARCHAR)";

	public static final String SQL_ADD_category = "INSERT INTO 'category' (categoryid, name, hidden, iconid) VALUES (0, 'My POI', 0, "
			+ POI_I_BLUE + ");";

	public static final String SQL_UPDATE_1_1 = "DROP TABLE IF EXISTS 'points_45392250'; ";
	public static final String SQL_UPDATE_1_2 = "CREATE TABLE 'points_45392250' AS SELECT * FROM 'points';";
	public static final String SQL_UPDATE_1_3 = "DROP TABLE 'points';";
	public static final String SQL_UPDATE_1_5 = "INSERT INTO 'points' (pointid, name, descr, lat, lon, alt, hidden, categoryid, pointsourceid, iconid) SELECT pointid, name, descr, lat, lon, alt, hidden, categoryid, pointsourceid, "
			+ POI_I_RED + " FROM 'points_45392250';";
	public static final String SQL_UPDATE_1_6 = "DROP TABLE 'points_45392250';";

	public static final String SQL_UPDATE_1_7 = "DROP TABLE IF EXISTS 'category_46134312'; ";
	public static final String SQL_UPDATE_1_8 = "CREATE TABLE 'category_46134312' AS SELECT * FROM 'category';";
	public static final String SQL_UPDATE_1_9 = "DROP TABLE 'category';";
	public static final String SQL_UPDATE_1_11 = "INSERT INTO 'category' (categoryid, name) SELECT categoryid, name FROM 'category_46134312';";
	public static final String SQL_UPDATE_1_12 = "DROP TABLE 'category_46134312';";

	public static final String SQL_UPDATE_2_7 = "DROP TABLE IF EXISTS 'category_46134313'; ";
	public static final String SQL_UPDATE_2_8 = "CREATE TABLE 'category_46134313' AS SELECT * FROM 'category';";
	public static final String SQL_UPDATE_2_9 = "DROP TABLE 'category';";
	public static final String SQL_UPDATE_2_11 = "INSERT INTO 'category' (categoryid, name, hidden, iconid) SELECT categoryid, name, hidden, iconid FROM 'category_46134313';";
	public static final String SQL_UPDATE_2_12 = "DROP TABLE 'category_46134313';";

	public static final String SQL_UPDATE_6_1 = "DROP TABLE IF EXISTS 'tracks_46134313'; ";
	public static final String SQL_UPDATE_6_2 = "CREATE TABLE 'tracks_46134313' AS SELECT * FROM 'tracks'; ";
	public static final String SQL_UPDATE_6_3 = "DROP TABLE IF EXISTS 'tracks'; ";
	public static final String SQL_UPDATE_6_4 = "INSERT INTO 'tracks' (trackid, name, descr, date, show, cnt, duration, distance, categoryid, activity) SELECT trackid, name, descr, date, show, (SELECT COUNT(*) FROM trackpoints WHERE trackid = tracks_46134313.trackid), null, null, null, 0 FROM 'tracks_46134313';";
	public static final String SQL_UPDATE_6_5 = "DROP TABLE 'tracks_46134313';";
	
	public static final String SQL_UPDATE_20_1 = "INSERT INTO 'tracks' (trackid, name, descr, date, show, cnt, duration, distance, categoryid, activity, style) SELECT trackid, name, descr, date, show, cnt, duration, distance, categoryid, activity, '' FROM 'tracks_46134313';";

	//(trackid INTEGER NOT NULL PRIMARY KEY UNIQUE, name VARCHAR, descr VARCHAR, date DATETIME
	//, show INTEGER, cnt INTEGER, duration INTEGER, distance INTEGER, categoryid INTEGER, activity INTEGER);";
}
