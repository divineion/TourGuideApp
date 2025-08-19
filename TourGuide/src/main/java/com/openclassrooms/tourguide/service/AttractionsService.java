package com.openclassrooms.tourguide.service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;

import java.util.List;

public class AttractionsService extends GpsUtil {
    private final static GpsUtil gpsUtil = new GpsUtil();

    /**
     * Immutable cached list of attractions, loaded once at application startup.
     * This list wraps the result of a single call to {@link GpsUtil#getAttractions()}
     */
    public final static List<Attraction> allAttractions = List.copyOf(gpsUtil.getAttractions());
}
