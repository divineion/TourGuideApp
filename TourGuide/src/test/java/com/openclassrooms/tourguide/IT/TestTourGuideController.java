package com.openclassrooms.tourguide.IT;

import com.openclassrooms.tourguide.service.TourGuideService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
@SpringBootTest
public class TestTourGuideController {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourGuideService service;

    @Test
    void testGetNearbyAttractionsInfo_shouldReturnDto() throws Exception {
        String username = service.getAllUsers().get(0).getUserName();
        mockMvc.perform(get("/getNearbyAttractions?userName="+username))
                .andExpectAll(
                        jsonPath("$").isArray(),
                        jsonPath("$.[0].attractionName").isString(),
                        jsonPath("$.[0].attractionLocation.longitude").isNumber(),
                        jsonPath("$.[0].attractionLocation.latitude").isNumber(),
                        jsonPath("$.[0].userLocation.longitude").isNumber(),
                        jsonPath("$.[0].userLocation.latitude").isNumber(),
                        jsonPath("$.[0].distance").isNumber(),
                        jsonPath("$.[0].rewardPoints").isNumber()
                );
    }

    //Todo handle exceptions
    @Disabled
    @Test
    void testGetNearbyAttractions_shouldReturnException() throws Exception {
        String username = "anyUnknownUsername";
        mockMvc.perform(get("/getNearbyAttractions?userName="+username))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserLocation() throws Exception {
        String username = service.getAllUsers().get(0).getUserName();

        mockMvc.perform(get("/getLocation?userName="+username))
                .andExpectAll(
                        jsonPath("$.userId").isNotEmpty(),
                        jsonPath("$.location.longitude").isNumber(),
                        jsonPath("$.location.latitude").isNumber()
                );
    }
}
