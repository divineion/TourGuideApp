# TourGuide Application

## Overview

TourGuide is a Spring Boot application designed for travelers. It allows users to:

- Locate nearby tourist attractions.
- Earn rewards from visiting attractions.
- Access personalized travel offers from partner services such as hotels or shows.

The application is optimized for performance and scalability, handling up to 100,000 simulated users.


## Technologies

> Java 17  
> Spring Boot 3.X  
> JUnit 5  

---

## External Dependencies

The project relies on the following external libraries, which are **not available in Maven Central**:

- **GpsUtil** – provides geographical coordinates and attraction data.
- **RewardCentral** – calculates reward points for visiting attractions.
- **TripPricer** – generates personalized offers for users based on preferences and reward points.

### How to install these dependencies locally
> Run : 
- mvn install:install-file -Dfile=libs/gpsUtil.jar -DgroupId=gpsUtil -DartifactId=gpsUtil -Dversion=1.0.0 -Dpackaging=jar  
- mvn install:install-file -Dfile=libs/RewardCentral.jar -DgroupId=rewardCentral -DartifactId=rewardCentral -Dversion=1.0.0 -Dpackaging=jar  
- mvn install:install-file -Dfile=libs/TripPricer.jar -DgroupId=tripPricer -DartifactId=tripPricer -Dversion=1.0.0 -Dpackaging=jar
