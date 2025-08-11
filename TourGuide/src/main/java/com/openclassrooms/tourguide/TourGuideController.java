package com.openclassrooms.tourguide;

import java.util.List;

import com.openclassrooms.tourguide.dto.NearbyAttractionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) {
    	return tourGuideService.getUserLocation(getUser(userName));
    }

    /**
     * Retrieves the five closest tourist attractions to the specified user.
     * <p>
     * The returned list contains detailed information for each attraction, including:
     * <ul>
     *   <li>The attraction's name,</li>
     *   <li>The attraction's latitude and longitude,</li>
     *   <li>The user's current latitude and longitude,</li>
     *   <li>The distance in miles between the user and the attraction,</li>
     *   <li>The reward points for visiting the attraction.</li>
     * </ul>
     *
     * @param userName the name of the current user
     * @return a list of {@link NearbyAttractionDto}
     */
    @RequestMapping("/getNearbyAttractions") 
    public List<NearbyAttractionDto> getNearbyAttractions(@RequestParam String userName) {
        User user = getUser(userName);
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
        List<Attraction> attractions = tourGuideService.getNearByAttractions(visitedLocation);

        return tourGuideService.getNearByAttractionsInfo(user, attractions);
    }
    
    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}