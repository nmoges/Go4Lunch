package com.openclassrooms.go4lunch.viewmodels;

import android.Manifest;
import android.content.Context;
import androidx.annotation.RequiresPermission;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.openclassrooms.go4lunch.database.RestaurantAndHoursData;
import com.openclassrooms.go4lunch.di.DI;
import com.openclassrooms.go4lunch.database.HoursData;
import com.openclassrooms.go4lunch.model.Restaurant;
import com.openclassrooms.go4lunch.database.RestaurantData;
import com.openclassrooms.go4lunch.repositories.PlacesRepository;
import com.openclassrooms.go4lunch.utils.AppInfo;
import com.openclassrooms.go4lunch.utils.DataConverters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * ViewModel class used to store a list of detected restaurant in a MutableLiveData object.
 */
public class PlacesViewModel extends ViewModel {

    // Repository to access a service
    private PlacesRepository placesRepository;

    // Executor to launch all repository accesses
    private final Executor executor = DI.provideExecutor();

    // To store the list of restaurant
    private final MutableLiveData<List<Restaurant>> listRestaurants = new MutableLiveData<>();


    // To store the list of autocomplete results
    private final MutableLiveData<List<String>> listRestaurantsIdAutocomplete = new MutableLiveData<>();


    public PlacesViewModel() { /* Empty constructor */ }

    public MutableLiveData<List<Restaurant>> getListRestaurants() {
        return listRestaurants;
    }

    public MutableLiveData<List<String>> getListRestaurantsAutocomplete() {
        return listRestaurantsIdAutocomplete;
    }

    // Getter/Setter
    public void setRepository(PlacesRepository placesRepository) {
        this.placesRepository = placesRepository;
    }

    public PlacesRepository getPlacesRepository() {
        return placesRepository;
    }

    // Methods to access PlacesRepository -> ListRestaurantsService methods
    /**
     * Accesses the findPlacesNearby() method of the @{@link PlacesRepository } repository class.
     * @param location : Info location of the user
     * @param type : Type of places to search
     */
    public void findPlacesNearby(String location, String type) {
        executor.execute(() -> {
                    try {
                        placesRepository.findPlacesNearby(location, type, newListRestaurants -> {
                            listRestaurants.postValue(newListRestaurants);
                            getPlacesDetails(newListRestaurants, false);
                        });
                    } catch (IOException exception) { exception.printStackTrace(); }
                }
        );
    }

    /**
     * Gets next places available nearby user location
     * @param listRestaurants : List of existing restaurants
     * @param numNextPageToken : Number of the next page of data requested
     */
    public void getNextPlacesNearby(List<Restaurant> listRestaurants, int numNextPageToken) {
        executor.execute(() -> {
            try {
                placesRepository.getNextPlacesNearby(listRestaurant ->
                        getPlacesDetails(listRestaurant, true),
                        listRestaurants,
                        numNextPageToken);
            } catch (IOException exception) { exception.printStackTrace(); }
        });
    }

    /**
     * Accesses the getPlacesDetails() method of the @{@link PlacesRepository } repository class.
     * @param list : List of restaurant to update with details for each place
     */
    public void getPlacesDetails(List<Restaurant> list, boolean nextPageTokenResults) {
        executor.execute(() -> {
            try {
                placesRepository.getPlacesDetails(list,
                        (newListRestaurants, listOfListHoursData) -> {
                    listRestaurants.postValue(newListRestaurants);
                    if (!nextPageTokenResults) {
                        // Store list of periods in database
                        updateDatabaseHoursDataTable(listOfListHoursData);
                        // Store list of restaurants in database
                        updateDatabaseRestaurantTable(newListRestaurants);
                    }
                });
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
    }

    // Methods to access PlacesRepository -> AutocompleteService methods
    /**
     * Performs an autocomplete request using a String "query".
     * @param query : Query for autocomplete request
     * @param context : Context
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void performAutocompleteRequest(String query, Context context) {
        executor.execute(() -> {
            if (AppInfo.checkIfLocationPermissionIsGranted(context)) {
                placesRepository.performAutocompleteRequest(query,
                                                          listRestaurantsIdAutocomplete::postValue);
            }
        });
    }

    // Methods to access PlacesRepository -> RestaurantDao methods
    /**
     * Handles the insertion operation of a new RestaurantData object in restaurant_table.
     * @param restaurantData : Data to insert
     */
    public void insertRestaurantData(RestaurantData restaurantData) {
        executor.execute(() -> placesRepository.insertRestaurantData(restaurantData));
    }

    /**
     * Handles the deletion of all RestaurantData objects stored in restaurant_table.
     */
    public void deleteAllRestaurantsData() {
        executor.execute(placesRepository::deleteAllRestaurantsData);
    }

    /**
     * Handles the update of restaurant_table with a new list of Restaurant (result of a search request).
     * @param list : Data to store in restaurant_table
     */
    private void updateDatabaseRestaurantTable(List<Restaurant> list) {
        deleteAllRestaurantsData();
        for (int i = 0; i < list.size(); i++) {
            RestaurantData restaurantData = new RestaurantData(list.get(i).getPlaceId(),
                    list.get(i).getName(), list.get(i).getAddress(),
                    list.get(i).getLatitude(), list.get(i).getLongitude(), list.get(i).getRating(),
                    list.get(i).getPhoneNumber(), list.get(i).getWebsiteUri(),
                    list.get(i).getPhotoReference(), list.get(i).getPhotoHeight(),
                    list.get(i).getPhotoWidth());
            insertRestaurantData(restaurantData);
        }
    }

    // Methods to access placeRepository -> HoursDao methods
    /**
     * Handles the insertion of a new HoursData object in hours_table.
     * @param hoursData : Data to insert
     */
    public void insertHoursData(HoursData hoursData) {
        executor.execute(() -> placesRepository.insertHoursData(hoursData));
    }

    /**
     * Handles the deletion of all HoursData objects stored in hours_table .
     */
    public void deleteAllHoursData() {
        executor.execute(() -> placesRepository.deleteAllHoursData());
    }

    /**
     * Handles the update of hours_table with a list of several lists containing the closing/opening
     * hours information, each one associated with a restaurant.
     * @param listOfListHoursData : Data to store in hours_table
     */
    private void updateDatabaseHoursDataTable(List<List<HoursData>> listOfListHoursData) {
        deleteAllHoursData();
        for (int i = 0; i < listOfListHoursData.size(); i++) {
           for (int j = 0; j < listOfListHoursData.get(i).size(); j++) {
               insertHoursData(listOfListHoursData.get(i).get(j));
           }
        }
    }

    // Other methods
    /**
     * Handles the restoration of all Restaurant data and OpeningAndClosingHours data, from
     * a list of RestaurantAndHoursData retrieved from a RestaurantAndHoursDao request.
     * @param restaurantAndHoursData : Data from a RestaurantAndHoursDao request
     */
    public void restoreData(List<RestaurantAndHoursData> restaurantAndHoursData) {
        List<Restaurant> oldListRestaurants = new ArrayList<>();

        for (int i = 0; i < restaurantAndHoursData.size(); i++) {
            RestaurantData restaurantData = restaurantAndHoursData.get(i).restaurantData;
            Restaurant restaurant = new Restaurant(restaurantData.getPlaceId(),
                    restaurantData.getName(), restaurantData.getAddress(),
                    restaurantData.getLatitude(), restaurantData.getLongitude(),
                    restaurantData.getRating());

                    restaurant.setPhotoReference(restaurantData.getPhotoReference());
                    restaurant.setPhotoWidth(restaurantData.getPhotoWidth());
                    restaurant.setPhotoHeight(restaurantData.getPhotoHeight());
                    restaurant.setWebsiteUri(restaurantData.getWebsiteUri());
                    restaurant.setPhoneNumber(restaurantData.getPhoneNumber());

            List<HoursData> hoursData = restaurantAndHoursData.get(i).hoursData;
            restaurant.setOpeningAndClosingHours(
                              DataConverters.converterHoursDataToOpeningAndClosingHours(hoursData));
            oldListRestaurants.add(restaurant);
        }
        listRestaurants.postValue(oldListRestaurants);
    }
}
