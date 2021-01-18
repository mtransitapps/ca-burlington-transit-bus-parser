package org.mtransit.parser.ca_burlington_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://navburl-burlington.opendata.arcgis.com/pages/data
// https://navburl-burlington.opendata.arcgis.com/search?tags=Transportation
// https://navburl-burlington.opendata.arcgis.com/datasets/transit-schedule-data-gtfs
// http://opendata.burlington.ca/gtfs-rt/
// http://opendata.burlington.ca/gtfs-rt/GTFS_Data.zip
public class BurlingtonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-burlington-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new BurlingtonTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Burlington Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Burlington Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String A = "A";
	private static final String B = "B";
	private static final String X = "X";

	private static final long RID_ENDS_WITH_A = 1000L;
	private static final long RID_ENDS_WITH_B = 2000L;
	private static final long RID_ENDS_WITH_X = 24000L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		String routeId = gRoute.getRouteShortName();
		if (routeId.length() > 0 && Utils.isDigitsOnly(routeId)) {
			return Integer.parseInt(routeId); // using stop code as stop ID
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
		throw new MTLog.Fatal("Can't find route ID for %s!", gRoute);
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		return cleanRouteLongName(super.getRouteLongName(gRoute));
	}

	private String cleanRouteLongName(String routeLongName) {
		if (Utils.isUppercaseOnly(routeLongName, true, true)) {
			routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		}
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "006184"; // BLUE

	@NotNull
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

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(6L, new RouteTripSpec(6L, //
				0, MTrip.HEADSIGN_TYPE_STRING, BURLINGTON_GO, //
				1, MTrip.HEADSIGN_TYPE_STRING, _407_CARPOOL) //
				.addTripSort(0, //
						Arrays.asList(//
								"615", // GO 407 CARPOOL
								"85" // BURLINGTON GO STATION
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"85", // BURLINGTON GO STATION
								"615" // GO 407 CARPOOL
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(12L, new RouteTripSpec(12L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "North Smart Ctr", //
				1, MTrip.HEADSIGN_TYPE_STRING, BURLINGTON_GO) //
				.addTripSort(0, //
						Arrays.asList(//
								"85", // BURLINGTON GO STATION
								"298", // ++
								"965" // BURLINGTON NORTH SMARTCENTRE
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"965", // BURLINGTON NORTH SMARTCENTRE
								"346", // ++
								"85" // BURLINGTON GO STATION
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(15L, new RouteTripSpec(15L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "A Appleby - Walkers", // AM
				1, MTrip.HEADSIGN_TYPE_STRING, "B Walkers - Appleby") // PM
				.addTripSort(0, //
						Arrays.asList(//
								"535", // <> APPLEBY GO STATION
								"505", // <>
								"473", // <>
								"499", // !=
								"923", // APPLEBY AT UPPER MIDDLE
								"457", // !=
								"474", // <>
								"507", // <>
								"535" // <> APPLEBY GO STATION
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"535", // <> APPLEBY GO STATION
								"505", // <>
								"473", // <>
								"439", // !=
								"510", // !=
								"474", // <>
								"507", // <>
								"535" // <> APPLEBY GO STATION
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(40L, new RouteTripSpec(40L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "B Pinedale - Hampton Heath", // PM
				1, MTrip.HEADSIGN_TYPE_STRING, "A Hampton Heath - Pinedale") // AM
				.addTripSort(0, //
						Arrays.asList(//
								"535", // APPLEBY GO STATION
								"505", // <>
								"438", // <>
								"421", // !=
								"376", // !=
								"395", // <>
								"507", // <>
								"535" // APPLEBY GO STATION
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"535", // APPLEBY GO STATION
								"505", // <>
								"438", // <>
								"392", // !=
								"397", // !=
								"395", // <>
								"507", // <>
								"535" // APPLEBY GO STATION
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(50L, new RouteTripSpec(50L, //
				0, MTrip.HEADSIGN_TYPE_STRING, SOUTH, // LAKESHORE
				1, MTrip.HEADSIGN_TYPE_STRING, BURLINGTON_GO) //
				.addTripSort(0, //
						Arrays.asList(//
								"85", // BURLINGTON GO STATION
								"996", // NEW AT SHANE
								"447" // BURLOAK AT LAKESHORE
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"447", // BURLOAK AT LAKESHORE
								"498", // ++
								"85" // BURLINGTON GO STATION
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(51L, new RouteTripSpec(51L, //
				0, MTrip.HEADSIGN_TYPE_STRING, NORTHEAST, // SUTTON
				1, MTrip.HEADSIGN_TYPE_STRING, BURLINGTON_GO) //
				.addTripSort(0, //
						Arrays.asList(//
								"85", // BURLINGTON GO STATION
								"411", // ++
								"872" // SUTTON AT DUNDAS
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"872", // SUTTON AT DUNDAS
								"538", // UPPER MIDDLE AT WALKERS
								"85" // BURLINGTON GO STATION
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(52L, new RouteTripSpec(52L, //
				0, MTrip.HEADSIGN_TYPE_STRING, NORTHWEST, //
				1, MTrip.HEADSIGN_TYPE_STRING, BURLINGTON_GO) //
				.addTripSort(0, //
						Arrays.asList(//
								"85", // BURLINGTON GO STATION
								"446", // ++
								"299" // GUELPH AT UPPER MIDDLE
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"299", // GUELPH AT UPPER MIDDLE
								"32", // ++
								"85" // BURLINGTON GO STATION
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(300L, new RouteTripSpec(300L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SENIORS_CTR, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LA_SALLE_TOWERS) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(//
								"1052", // LA SALLE TOWERS
								"748", // ++
								"1051" // SENIORS CENTRE
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(//
								"1051", // SENIORS CENTRE
								"749", // ++
								"1052" // LA SALLE TOWERS
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(301L, new RouteTripSpec(301L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, LAKESHORE_PL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SENIORS_CTR) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(//
								"1051", // SENIORS CENTRE
								"329", // ++
								"1057" // LAKESHORE PLACE
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(//
								"1057", // LAKESHORE PLACE
								"338", // ++
								"1051" // SENIORS CENTRE
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(302L, new RouteTripSpec(302L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, TANSLEY_WOODS_CC, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SENIORS_CTR) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(//
								"1051", // SENIORS CENTRE
								"228", // ++
								"1055" // TANSLEY WOODS COMMUNITY CENTRE
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(//
								"1055", // TANSLEY WOODS COMMUNITY CENTRE
								"272", // ++
								"1051" // SENIORS CENTRE
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		if (MTrip.mergeEmpty(mTrip, mTripToMerge)) {
			return true;
		}
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					BURLINGTON_GO, // <>
					"Appleby Go" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Appleby Go", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					BURLINGTON_GO, // <>
					"Hamilton" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Hamilton", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 2L) {
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
		throw new MTLog.Fatal("Unexpected trips to merge %s and %s.", mTrip, mTripToMerge);
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, getIgnoredWords());
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private String[] getIgnoredWords() {
		return new String[]{"GO"};
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredWords());
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) { // used for GTFS-RT
		return super.getStopId(gStop);
	}
}
