package org.mtransit.parser.ca_burlington_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://www.burlington.ca/en/services-for-you/Open-Data.asp
// http://cob.burlington.opendata.arcgis.com/
// http://opendata.burlington.ca/gtfs-rt/GTFS_Data.zip
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
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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
		String routeId = gRoute.getRouteShortName();
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

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return cleanRouteLongName(super.getRouteLongName(gRoute));
	}

	private String cleanRouteLongName(String routeLongName) {
		if (Utils.isUppercaseOnly(routeLongName, true, true)) {
			routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		}
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "006184"; // BLUE

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String CAVENDISH = "Cavendish";
	private static final String _407_CARPOOL = "407 Carpool";
	private static final String BURLINGTON_GO = "Burlington Go";
	private static final String LAKESHORE_PL = "Lakeshore Pl";
	private static final String TANSLEY_WOODS_CC = "Tansley Woods CC";
	private static final String SENIORS_CTR = "Seniors Ctr";
	private static final String LA_SALLE_TOWERS = "LaSalle Towers";
	private static final String NORTHWEST = "Northwest";
	private static final String NORTHEAST = "Northeast";
	private static final String SOUTH = "South";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(12L, new RouteTripSpec(12L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "North Smart Ctr", //
				1, MTrip.HEADSIGN_TYPE_STRING, BURLINGTON_GO) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"85", // BURLINGTON GO STATION
								"298", // ++
								"965", // BURLINGTON NORTH SMARTCENTRE
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"965", // BURLINGTON NORTH SMARTCENTRE
								"346", // ++
								"85", // BURLINGTON GO STATION
						})) //
				.compileBothTripSort());
		map2.put(15L, new RouteTripSpec(15L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "A Appleby - Walkers", // AM
				1, MTrip.HEADSIGN_TYPE_STRING, "B Walkers - Appleby") // PM
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"535", // <> APPLEBY GO STATION
								"505", // <>
								"473", // <>
								"499", // !=
								"923", // APPLEBY AT UPPER MIDDLE
								"457", // !=
								"474", // <>
								"507", // <>
								"535", // <> APPLEBY GO STATION
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"535", // <> APPLEBY GO STATION
								"505", // <>
								"473", // <>
								"439", // !=
								"510", // !=
								"474", // <>
								"507", // <>
								"535", // <> APPLEBY GO STATION
						})) //
				.compileBothTripSort());
		map2.put(40L, new RouteTripSpec(40L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "B Pinedale - Hampton Heath", // PM
				1, MTrip.HEADSIGN_TYPE_STRING, "A Hampton Heath - Pinedale") // AM
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"535", // APPLEBY GO STATION
								"505", // <>
								"438", // <>
								"421", // !=
								"376", // !=
								"395", // <>
								"507", // <>
								"535", // APPLEBY GO STATION
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"535", // APPLEBY GO STATION
								"505", // <>
								"438", // <>
								"392", // !=
								"397", // !=
								"395", // <>
								"507", // <>
								"535", // APPLEBY GO STATION
						})) //
				.compileBothTripSort());
		map2.put(50L, new RouteTripSpec(50L, //
				0, MTrip.HEADSIGN_TYPE_STRING, SOUTH, // LAKESHORE
				1, MTrip.HEADSIGN_TYPE_STRING, BURLINGTON_GO) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"85", // BURLINGTON GO STATION
								"996", // NEW AT SHANE
								"447", // BURLOAK AT LAKESHORE
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"447", // BURLOAK AT LAKESHORE
								"498", // ++
								"85", // BURLINGTON GO STATION
						})) //
				.compileBothTripSort());
		map2.put(51L, new RouteTripSpec(51L, //
				0, MTrip.HEADSIGN_TYPE_STRING, NORTHEAST, // SUTTON
				1, MTrip.HEADSIGN_TYPE_STRING, BURLINGTON_GO) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"85", // BURLINGTON GO STATION
								"411", // ++
								"872", // SUTTON AT DUNDAS
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"872", // SUTTON AT DUNDAS
								"538", // UPPER MIDDLE AT WALKERS
								"85", // BURLINGTON GO STATION
						})) //
				.compileBothTripSort());
		map2.put(52L, new RouteTripSpec(52L, //
				0, MTrip.HEADSIGN_TYPE_STRING, NORTHWEST, //
				1, MTrip.HEADSIGN_TYPE_STRING, BURLINGTON_GO) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"85", // BURLINGTON GO STATION
								"446", // ++
								"299", // GUELPH AT UPPER MIDDLE
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"299", // GUELPH AT UPPER MIDDLE
								"32", // ++
								"85", // BURLINGTON GO STATION
						})) //
				.compileBothTripSort());
		map2.put(300l, new RouteTripSpec(300l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SENIORS_CTR, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_SALLE_TOWERS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1052", // LA SALLE TOWERS
								"748", // ++
								"1051", // SENIORS CENTRE
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1051", // SENIORS CENTRE
								"749", // ++
								"1052", // LA SALLE TOWERS
						})) //
				.compileBothTripSort());
		map2.put(301L, new RouteTripSpec(301L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAKESHORE_PL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SENIORS_CTR) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1051", // SENIORS CENTRE
								"329", // ++
								"1057", // LAKESHORE PLACE
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1057", // LAKESHORE PLACE
								"338", // ++
								"1051", // SENIORS CENTRE
						})) //
				.compileBothTripSort());
		map2.put(302l, new RouteTripSpec(302l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TANSLEY_WOODS_CC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SENIORS_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"1051", // SENIORS CENTRE
								"228", // ++
								"1055", // TANSLEY WOODS COMMUNITY CENTRE
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1055", // TANSLEY WOODS COMMUNITY CENTRE
								"272", // ++
								"1051", // SENIORS CENTRE
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
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
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (MTrip.mergeEmpty(mTrip, mTripToMerge)) {
			return true;
		}
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					CAVENDISH, //
					_407_CARPOOL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(_407_CARPOOL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 3L) {
			if (Arrays.asList( //
					CAVENDISH, //
					_407_CARPOOL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(_407_CARPOOL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 11L) {
			if (Arrays.asList( //
					"Tim Dobbie", //
					_407_CARPOOL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(_407_CARPOOL, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 48L) {
			if (Arrays.asList( //
					"Upper Middle", //
					"Sutton" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Sutton", mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge %s and %s.\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern RLN_DASH_BOUNDS_TO = Pattern.compile("(^([^\\-]*\\- )+(east |west |north |south )?)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_TO = Pattern.compile("(^to )", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = RLN_DASH_BOUNDS_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		if (Utils.isUppercaseOnly(gStopName, true, true)) {
			gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		}
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		String stopId = gStop.getStopId();
		if (stopId != null && stopId.length() > 0 && Utils.isDigitsOnly(stopId)) {
			return Integer.valueOf(stopId);
		}
		Matcher matcher = DIGITS.matcher(stopId);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group());
		}
		System.out.printf("\nUnexpected stop ID for %s!", gStop);
		System.exit(-1);
		return -1;
	}
}
