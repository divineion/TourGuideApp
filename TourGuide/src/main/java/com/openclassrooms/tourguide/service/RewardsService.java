package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

/**
 * Service responsible for calculating user rewards based on his visited locations
 * and proximity to attractions.
 */
@Service
public class RewardsService {
    private final List<Attraction> allAttractions = AttractionsService.allAttractions;

    /**
     * Conversion factor from nautical miles to statute miles.
     */
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    /**
     * Default distance in miles to consider a user as being near an attraction.
     * This value is used to initialize the proximityBuffer and can be modified at runtime
     * via {@link #setProximityBuffer(int)}.
     */
    private int defaultProximityBuffer = 10;

    /**
     * Current distance in miles used to determine if a user is near an attraction.
     * <p>Initialized with {@link #defaultProximityBuffer} but
     * can be updated via {@link #setProximityBuffer(int)}.</p>
     */
    private int proximityBuffer = defaultProximityBuffer;

    /**
     * Maximum distance in miles to consider an attraction as nearby.
     */
    private int attractionProximityRange = 200;

    private final RewardCentral rewardsCentral;

    private final ExecutorService executorService = Executors.newFixedThreadPool(500);

    /**
     * Constructs a RewardsService with the specified RewardCentral instance.
     *
     * @param rewardCentral external service to fetch reward points for attractions.
     */
    public RewardsService(RewardCentral rewardCentral) {
		this.rewardsCentral = rewardCentral;
	}

    /**
     * Sets a custom proximity buffer for determining if a user is near an attraction.
     *
     * @param proximityBuffer The new proximity buffer in miles.
     */
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

    /**
     * Asynchronously calculates rewards for a user based on his visited locations.
     * Adds new rewards if the user is near an attraction he hasn't visited before.
     *
     * @param user The user to calculate rewards for.
     * @return A CompletableFuture that completes when the calculation is done.
     */
    public CompletableFuture<Void> calculateRewards(User user) {
        return CompletableFuture.runAsync(() -> {
            synchronized (user) {
                List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());

                for (VisitedLocation visitedLocation : userLocations) {
                    for (Attraction attraction : allAttractions) {
                        if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
                            if (nearAttraction(visitedLocation, attraction)) {
                                user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
                            }
                        }
                    }
                }
            }
        }, executorService);
    }
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) <= attractionProximityRange;
	}

    /**
     * Checks if a user's visited location is near an attraction, using the current proximity buffer.
     *
     * @param visitedLocation The user's visited location.
     * @param attraction The attraction to check proximity to.
     * @return true if the user is near the attraction, false otherwise.
     */
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
	}
	
	protected int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}
}
