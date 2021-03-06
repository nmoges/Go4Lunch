package com.openclassrooms.go4lunch.service.places;

import com.openclassrooms.go4lunch.database.HoursData;
import com.openclassrooms.go4lunch.model.Restaurant;
import java.util.List;

/**
 * Callback interface to get an updated list of restaurants with details for each place
 */
public interface ServiceDetailsCallback {
    void onPlacesDetailsAvailable(List<Restaurant> listRestaurant,
                                  List<List<HoursData>> listOfListHoursData);
}
