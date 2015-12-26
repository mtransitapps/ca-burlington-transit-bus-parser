package org.mtransit.parser.ca_burlington_transit_bus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

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
		System.out.printf("\nGenerating Burlington Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Burlington Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	private static final String A = "A";
	private static final String B = "B";
	private static final String X = "X";

	private static final long RID_ENDS_WITH_A = 1000l;
	private static final long RID_ENDS_WITH_B = 2000l;
	private static final long RID_ENDS_WITH_X = 24000l;

	@Override
	public long getRouteId(GRoute gRoute) {
		String routeId = gRoute.getRouteId();
		if (routeId != null && routeId.length() > 0 && Utils.isDigitsOnly(routeId)) {
			return Integer.valueOf(routeId); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(routeId);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			if (routeId.endsWith(A)) {
				return RID_ENDS_WITH_A + digits;
			} else if (routeId.endsWith(B)) {
				return RID_ENDS_WITH_B + digits;
			} else if (routeId.endsWith(X)) {
				return RID_ENDS_WITH_X + digits;
			}
		}
		System.out.printf("\nCan't find route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String AGENCY_COLOR = "006184"; // BLUE

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String APPLEBY_GO = "Appleby GO";
	private static final String APPLEBY_GO_LC = "appleby go";
	private static final String HAMILTON = "Hamilton";
	private static final String HAMILTON_LC = "hamilton";
	private static final String SUTTON = "Sutton";
	private static final String SUTTON_LC = "sutton";
	private static final String _407_CARPOOL = "407 Carpool";
	private static final String _407_CARPOOL_LC = "407 carpool";
	private static final String BURLINGTON_GO = "Burlington GO";
	private static final String BURLINGTON_GO_LC = "burlington go";
	// private static final String BURLINGTON = "Burlington";
	private static final String TIM_DOBBIE_LC = "tim dobbie";
	private static final String TIM_DOBBIE = "Tim Dobbie";
	private static final String ALDERSHOT_GO_LC = "aldershot go";
	private static final String ALDERSHOT_GO = "Aldershot GO";
	private static final String NORTHWEST_LC = "northwest";
	private static final String NORTHWEST = "Northwest";
	private static final String NORTHEAST = "Northeast";
	private static final String NORTHEAST_LC = "northeast";
	private static final String SOUTH = "South";
	private static final String SOUTH_LC = "south";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		String tripHeadsignLC = gTrip.getTripHeadsign().toLowerCase(Locale.ENGLISH);
		if (mRoute.getId() == 12l) {
			if (tripHeadsignLC.endsWith(SUTTON_LC)) {
				mTrip.setHeadsignString(SUTTON, 0);
				return;
			} else if (tripHeadsignLC.endsWith(BURLINGTON_GO_LC)) {
				mTrip.setHeadsignString(BURLINGTON_GO, 1);
				return;
			}
			System.out.printf("\nUnexpected 12 trip %s!\n", gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 12l + RID_ENDS_WITH_X) { // 12X
			if (tripHeadsignLC.endsWith(SUTTON_LC)) {
				mTrip.setHeadsignString(SUTTON, 0);
				return;
			} else if (tripHeadsignLC.endsWith(BURLINGTON_GO_LC)) {
				mTrip.setHeadsignString(BURLINGTON_GO, 1);
				return;
			}
			System.out.println("Unexpected 12X trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 21l) {
			if (tripHeadsignLC.endsWith(APPLEBY_GO_LC)) {
				mTrip.setHeadsignString(APPLEBY_GO, 0);
				return;
			} else if (tripHeadsignLC.endsWith(BURLINGTON_GO_LC)) {
				mTrip.setHeadsignString(BURLINGTON_GO, 1);
				return;
			}
			System.out.println("Unexpected 21 trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 25l) {
			if (tripHeadsignLC.endsWith(_407_CARPOOL_LC)) {
				mTrip.setHeadsignString(_407_CARPOOL, 0);
				return;
			} else if (tripHeadsignLC.endsWith(BURLINGTON_GO_LC)) {
				mTrip.setHeadsignString(BURLINGTON_GO, 1);
				return;
			}
			System.out.println("Unexpected 25 trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 48l) {
			if (tripHeadsignLC.endsWith(SUTTON_LC)) {
				mTrip.setHeadsignString(SUTTON, 0);
				return;
			} else if (tripHeadsignLC.endsWith(TIM_DOBBIE_LC)) {
				mTrip.setHeadsignString(TIM_DOBBIE, 1);
				return;
			}
			System.out.println("Unexpected 48 trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 50l) {
			if (tripHeadsignLC.endsWith(SOUTH_LC)) {
				mTrip.setHeadsignString(SOUTH, 0);
				return;
			}
			System.out.println("Unexpected 50 trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 51l) {
			if (tripHeadsignLC.endsWith(NORTHEAST_LC)) {
				mTrip.setHeadsignString(NORTHEAST, 0);
				return;
			}
			System.out.println("Unexpected 51 trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 52l) {
			if (tripHeadsignLC.endsWith(NORTHWEST_LC)) {
				mTrip.setHeadsignString(NORTHWEST, 0);
				return;
			}
			System.out.println("Unexpected 52 trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 80l) {
			if (tripHeadsignLC.endsWith(APPLEBY_GO_LC)) {
				mTrip.setHeadsignString(APPLEBY_GO, 0);
				return;
			} else if (tripHeadsignLC.endsWith(BURLINGTON_GO_LC)) {
				mTrip.setHeadsignString(BURLINGTON_GO, 1);
				return;
			}
			System.out.println("Unexpected " + mRoute.getId() + " trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 81l) {
			if (tripHeadsignLC.endsWith(APPLEBY_GO_LC)) {
				mTrip.setHeadsignString(APPLEBY_GO, 0);
				return;
			} else if (tripHeadsignLC.endsWith(BURLINGTON_GO_LC)) {
				mTrip.setHeadsignString(BURLINGTON_GO, 1);
				return;
			}
			System.out.println("Unexpected " + mRoute.getId() + " trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 87l) {
			if (tripHeadsignLC.endsWith(ALDERSHOT_GO_LC)) {
				mTrip.setHeadsignString(ALDERSHOT_GO, 0);
				return;
			} else if (tripHeadsignLC.endsWith(BURLINGTON_GO_LC)) {
				mTrip.setHeadsignString(BURLINGTON_GO, 1);
				return;
			}
			System.out.println("Unexpected " + mRoute.getId() + " trip " + gTrip);
			System.exit(-1);
			return;
		} else if (mRoute.getId() == 101l) {
			if (tripHeadsignLC.endsWith(BURLINGTON_GO_LC)) {
				mTrip.setHeadsignString(BURLINGTON_GO, 0);
				return;
			} else if (tripHeadsignLC.endsWith(HAMILTON_LC)) {
				mTrip.setHeadsignString(HAMILTON, 1);
				return;
			}
			System.out.println("Unexpected " + mRoute.getId() + " trip " + gTrip);
			System.exit(-1);
			return;
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (mTrip.getRouteId() == 4l) {
			if (mTrip.getHeadsignId() == 1l) {
				mTrip.setHeadsignString(APPLEBY_GO, mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge %s and %s.\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern RLN_DASH_BOUNDS_TO = Pattern.compile("(^([^\\-]*\\- )+(east |west |north |south )?to )", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = RLN_DASH_BOUNDS_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		String stopId = gStop.getStopId();
		if (stopId != null && stopId.length() > 0 && Utils.isDigitsOnly(stopId)) {
			return Integer.valueOf(stopId);
		}
		Matcher matcher = DIGITS.matcher(stopId);
		matcher.find();
		return Integer.parseInt(matcher.group());
	}
}
