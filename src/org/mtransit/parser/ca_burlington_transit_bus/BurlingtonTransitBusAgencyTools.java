package org.mtransit.parser.ca_burlington_transit_bus;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www.burlington.ca/en/services-for-you/Open-Data-Catalogue.asp
// http://www.burlington.ca/en/services-for-you/resources/Ongoing_Projects/Open_Data/Catalogue/GTFS_Data.zip
public class BurlingtonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-burlington-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new BurlingtonTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("Generating Burlington Transit bus data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating Burlington Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public long getRouteId(GRoute gRoute) {
		String routeId = gRoute.route_id;
		if (routeId != null && routeId.length() > 0 && Utils.isDigitsOnly(routeId)) {
			return Integer.valueOf(routeId); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(routeId);
		matcher.find();
		int digits = Integer.parseInt(matcher.group());
		if (routeId.endsWith("A")) {
			return 1000 + digits;
		} else if (routeId.endsWith("B")) {
			return 2000 + digits;
		} else if (routeId.endsWith("X")) {
			return 24000 + digits;
		} else {
			System.out.println("Can't find route ID for " + gRoute);
			System.exit(-1);
			return -1;
		}
	}

	private static final String AGENCY_COLOR = "006184"; // BLUE

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String APPLEBY_GO = "Appleby Go";
	private static final String HAMILTON = "Hamilton";
	private static final String SUTTON = "Sutton";
	private static final String _407_CARPOOL = "407 Carpool";
	private static final String BURLINGTON_GO = "Burlington Go";
	private static final String BURLINGTON = "Burlington";

	@Override
	public void setTripHeadsign(MRoute route, MTrip mTrip, GTrip gTrip) {
		int directionId = gTrip.direction_id;
		String stationName = cleanTripHeadsign(gTrip.trip_headsign);
		if (route.id == 4l) {
			if (directionId == 1) {
				stationName = APPLEBY_GO;
			}
		} else if (route.id == 12l) {
			if (SUTTON.equalsIgnoreCase(stationName)) {
				directionId = 0;
			} else {
				directionId = 1;
			}
		} else if (route.id == 24012l) { // 12X
			if (SUTTON.equalsIgnoreCase(stationName)) {
				directionId = 0;
			} else {
				directionId = 1;
			}
		} else if (route.id == 21l) {
			if (APPLEBY_GO.equalsIgnoreCase(stationName)) {
				directionId = 0;
			} else {
				directionId = 1;
			}
		} else if (route.id == 25l) {
			if (_407_CARPOOL.equalsIgnoreCase(stationName)) {
				directionId = 0;
			} else {
				directionId = 1;
			}
		} else if (route.id == 48l) {
			if (SUTTON.equalsIgnoreCase(stationName)) {
				directionId = 0;
			} else {
				directionId = 1;
			}
		} else if (route.id == 50l) {
			if (stationName.startsWith(BURLINGTON)) {
				stationName = stationName.substring(BURLINGTON.length() + 1);
			}
		} else if (route.id == 51l) {
			if (stationName.startsWith(BURLINGTON)) {
				stationName = stationName.substring(BURLINGTON.length() + 1);
			}
		} else if (route.id == 52l) {
			if (stationName.startsWith(BURLINGTON)) {
				stationName = stationName.substring(BURLINGTON.length() + 1);
			}
		} else if (route.id == 80l) {
			if (APPLEBY_GO.equalsIgnoreCase(stationName)) {
				directionId = 0;
			} else {
				directionId = 1;
			}
		} else if (route.id == 81l) {
			if (APPLEBY_GO.equalsIgnoreCase(stationName)) {
				directionId = 0;
			} else {
				directionId = 1;
			}
		} else if (route.id == 87l) {
			if (BURLINGTON_GO.equalsIgnoreCase(stationName)) {
				directionId = 1;
			} else {
				directionId = 0;
			}
		} else if (route.id == 101l) {
			if (HAMILTON.equalsIgnoreCase(stationName)) {
				directionId = 1;
			} else {
				directionId = 0;
			}
		}
		mTrip.setHeadsignString(stationName, directionId);
	}

	private static final String DASH_TO = " - to ";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		int indexOfTO = tripHeadsign.toLowerCase(Locale.ENGLISH).indexOf(DASH_TO);
		if (indexOfTO >= 0) {
			tripHeadsign = tripHeadsign.substring(indexOfTO + DASH_TO.length());
		}
		return MSpec.cleanLabel(tripHeadsign);
	}

	private static final Pattern FIRST = Pattern.compile("(first)", Pattern.CASE_INSENSITIVE);
	private static final String FIRST_REPLACEMENT = "1st";
	private static final Pattern SECOND = Pattern.compile("(second)", Pattern.CASE_INSENSITIVE);
	private static final String SECOND_REPLACEMENT = "2nd";
	private static final Pattern THIRD = Pattern.compile("(third)", Pattern.CASE_INSENSITIVE);
	private static final String THIRD_REPLACEMENT = "3rd";
	private static final Pattern FOURTH = Pattern.compile("(fourth)", Pattern.CASE_INSENSITIVE);
	private static final String FOURTH_REPLACEMENT = "4th";
	private static final Pattern FIFTH = Pattern.compile("(fifth)", Pattern.CASE_INSENSITIVE);
	private static final String FIFTH_REPLACEMENT = "5th";
	private static final Pattern SIXTH = Pattern.compile("(sixth)", Pattern.CASE_INSENSITIVE);
	private static final String SIXTH_REPLACEMENT = "6th";
	private static final Pattern SEVENTH = Pattern.compile("(seventh)", Pattern.CASE_INSENSITIVE);
	private static final String SEVENTH_REPLACEMENT = "7th";
	private static final Pattern EIGHTH = Pattern.compile("(eighth)", Pattern.CASE_INSENSITIVE);
	private static final String EIGHTH_REPLACEMENT = "8th";
	private static final Pattern NINTH = Pattern.compile("(ninth)", Pattern.CASE_INSENSITIVE);
	private static final String NINTH_REPLACEMENT = "9th";

	private static final Pattern AT = Pattern.compile("( at )", Pattern.CASE_INSENSITIVE);
	private static final String AT_REPLACEMENT = " / ";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = AT.matcher(gStopName).replaceAll(AT_REPLACEMENT);
		gStopName = FIRST.matcher(gStopName).replaceAll(FIRST_REPLACEMENT);
		gStopName = SECOND.matcher(gStopName).replaceAll(SECOND_REPLACEMENT);
		gStopName = THIRD.matcher(gStopName).replaceAll(THIRD_REPLACEMENT);
		gStopName = FOURTH.matcher(gStopName).replaceAll(FOURTH_REPLACEMENT);
		gStopName = FIFTH.matcher(gStopName).replaceAll(FIFTH_REPLACEMENT);
		gStopName = SIXTH.matcher(gStopName).replaceAll(SIXTH_REPLACEMENT);
		gStopName = SEVENTH.matcher(gStopName).replaceAll(SEVENTH_REPLACEMENT);
		gStopName = EIGHTH.matcher(gStopName).replaceAll(EIGHTH_REPLACEMENT);
		gStopName = NINTH.matcher(gStopName).replaceAll(NINTH_REPLACEMENT);
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		return MSpec.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		String stopId = gStop.stop_id;
		if (stopId != null && stopId.length() > 0 && Utils.isDigitsOnly(stopId)) {
			return Integer.valueOf(stopId);
		}
		Matcher matcher = DIGITS.matcher(stopId);
		matcher.find();
		return Integer.parseInt(matcher.group());
	}
}
