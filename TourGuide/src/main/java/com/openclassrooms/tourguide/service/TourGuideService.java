package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.NearbyAttractionDto;
import com.openclassrooms.tourguide.helper.Constants;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	private final List<Attraction> allAttractions = AttractionsService.allAttractions;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
        return (!user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation()
                : trackUserLocation(user);
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Retrieves the location of a user, adds it to their visited locations,
	 * and updates their accessible rewards list.
	 *
	 * @param user {@link User} to track location for
	 */
	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);

		rewardsService.calculateRewards(user).join();

		return visitedLocation;
	}

	/**
	 * Retrieves the location of each user from a list, adds it to the user's visited locations,
	 * and updates the list of the user's accessible rewards.
	 * @param users a list of {@link User} to track locations for
	 */
	public void trackUsersLocations(List<User> users) {
        users.parallelStream().forEach(this::trackUserLocation);
    }

	/**
	 * Returns the nearest tourist attractions to the specified user location sorted by ascending distance
	 * from the given location.
	 *
	 * @param visitedLocation the user's last known location
	 * @return a list of the nearest {@link Attraction}
	 */
	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		return allAttractions.stream()
				.sorted(Comparator.comparingDouble(attraction ->
						rewardsService.getDistance(visitedLocation.location,
													new Location(attraction.latitude, attraction.longitude))))
				.limit(Constants.NB_OF_NEARBY_ATTRACTIONS)
				.toList();
	}

	/**
	 * Builds detailed information about the given attractions for a specified user.
	 * <p>
	 * For each attraction, this method calculates:
	 * <ul>
	 *   <li>The attraction's location and name,</li>
	 *   <li>the distance from the user's current location,</li>
	 *   <li>and the number of reward points the user can earn,</li>
	 * </ul>
	 * then creates a {@link NearbyAttractionDto} object for each attraction and
	 * returns the complete list.
	 *
	 * @param user the user for whom distances and reward points are calculated
	 * @param attractions the list of attractions to include
	 * @return a list of {@link NearbyAttractionDto} objects containing location,
	 *         distance from the user, and reward points for each attraction
	 */
	public List<NearbyAttractionDto> getNearByAttractionsInfo(User user, List<Attraction> attractions) {
		Location userLocation = getUserLocation(user).location;

		return attractions.stream().map(attraction -> {
			Location attractionLocation = new Location(attraction.latitude, attraction.longitude);
			double distance = rewardsService.getDistance(userLocation, attractionLocation);
			int rewardPoints = rewardsService.getRewardPoints(attraction, user);

			return new NearbyAttractionDto(
				attraction.attractionName,
				attractionLocation,
				userLocation,
				distance,
				rewardPoints
			);
		}).toList();
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
}
