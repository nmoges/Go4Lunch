package com.openclassrooms.go4lunch.repositories;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import androidx.annotation.RequiresPermission;
import androidx.lifecycle.LiveData;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.openclassrooms.go4lunch.dao.HoursDao;
import com.openclassrooms.go4lunch.dao.RestaurantAndHoursDao;
import com.openclassrooms.go4lunch.dao.RestaurantDao;
import com.openclassrooms.go4lunch.database.HoursData;
import com.openclassrooms.go4lunch.database.RestaurantAndHoursData;
import com.openclassrooms.go4lunch.model.Restaurant;
import com.openclassrooms.go4lunch.database.RestaurantData;
import com.openclassrooms.go4lunch.service.autocomplete.AutocompleteService;
import com.openclassrooms.go4lunch.service.autocomplete.ServiceAutocompleteCallback;
import com.openclassrooms.go4lunch.service.places.ListRestaurantsService;
import com.openclassrooms.go4lunch.service.places.ServiceDetailsCallback;
import com.openclassrooms.go4lunch.service.places.ServicePlacesCallback;
import com.openclassrooms.go4lunch.service.places.response.details.DetailsResponse;
import com.openclassrooms.go4lunch.service.places.response.places.PlaceResponse;
import com.openclassrooms.go4lunch.service.places.response.places.ResultPlaces;
import com.openclassrooms.go4lunch.ui.activities.MainActivity;
import com.openclassrooms.go4lunch.ui.fragments.map.MapViewFragmentCallback;
import com.openclassrooms.go4lunch.utils.AppInfo;
import com.openclassrooms.go4lunch.utils.DataConverters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class to communicate with the @{@link ListRestaurantsService} service class.
 */
public class PlacesRepository {

    // Services
    private final ListRestaurantsService listRestaurantsServices;
    private final AutocompleteService autocompleteService;

    // Dao
    private final RestaurantDao restaurantDao;
    private final HoursDao hoursDao;
    private final RestaurantAndHoursDao restaurantAndHoursDao;

    // SharedPreferences
    private final SharedPreferences[] sharedPrefNextPageToken;
    private SharedPreferences.Editor editor;

    public PlacesRepository(RestaurantDao restaurantDao,
                            HoursDao hoursDao,
                            RestaurantAndHoursDao restaurantAndHoursDao,
                            Context context,
                            PlacesClient placesClient,
                            FusedLocationProviderClient locationClient) {
        // Initialize services
        this.listRestaurantsServices = new ListRestaurantsService();
        this.autocompleteService = new AutocompleteService(placesClient, locationClient);

        // Initialize DAOs
        this.restaurantDao = restaurantDao;
        this.hoursDao = hoursDao;
        this.restaurantAndHoursDao = restaurantAndHoursDao;

        // Initialize parameters for SharedPreferences
        sharedPrefNextPageToken = new SharedPreferences[2];
        sharedPrefNextPageToken[0] = context.getSharedPreferences(AppInfo.FILE_PREF_NEXT_PAGE_TOKEN,
                                                                              Context.MODE_PRIVATE);
        sharedPrefNextPageToken[1] = context.getSharedPreferences(AppInfo.FILE_PREF_NEXT_PAGE_TOKEN,
                                                                              Context.MODE_PRIVATE);
    }

    // Methods to access ListRestaurantsService
    /**
     * Accesses the findPlacesNearby() method of the @{@link ListRestaurantsService } service class.
     * @param location : Info location of the user
     * @param type : Type of places to search
     * @param callback : Callback interface
     * @throws IOException : Exception thrown by findPlacesNearby() method of the @{@link ListRestaurantsService }
     *                       service class
     */
    public void findPlacesNearby(String location, String type, ServicePlacesCallback callback)
            throws IOException {
        PlaceResponse response = listRestaurantsServices.findPlacesNearby(location, type);
        for (int i = 0; i < response.results.size(); i++) {
            // Initialize new restaurant object
            Restaurant restaurant = initializeRestaurantObject(response.results.get(i));

            // Add restaurant to the list
            listRestaurantsServices.updateListRestaurants(restaurant);

            // Save next page token
            if (response.next_page_token != null) {
                editor = sharedPrefNextPageToken[0].edit();
                editor.putString(AppInfo.PREF_FIRST_NEXT_PAGE_TOKEN_KEY,
                                response.next_page_token)
                            .apply();
            }
        }
        callback.onPlacesAvailable(listRestaurantsServices.getListRestaurants());
    }

    /**
     * Accesses the getNextPlacesNearby() method of the @{@link ListRestaurantsService } service class.
     * @param callback : ServicePlacesCallback callback interface to send back results
     * @param listRestaurants : List of existing restaurants
     * @param numNextPageToken : Token to request next places to load
     * @throws IOException : Exception
     */
    public void getNextPlacesNearby(ServicePlacesCallback callback, List<Restaurant> listRestaurants,
                                    int numNextPageToken) throws IOException {
        String nextPageToken;
        nextPageToken = sharedPrefNextPageToken[numNextPageToken].getString(
                                              AppInfo.PREF_FIRST_NEXT_PAGE_TOKEN_KEY, null);
        PlaceResponse response = listRestaurantsServices.getNextPlacesNearby(nextPageToken);

        for (int i = 0; i < response.getResults().size(); i++) {
            // Initialize new restaurant object
            Restaurant restaurant = initializeRestaurantObject(response.getResults().get(i));

            // Add restaurant to the list
            listRestaurants.add(restaurant);

            // Save next page token
            if (response.getNextPageToken() != null) {
                switch (numNextPageToken) {
                    case 0:
                        editor = sharedPrefNextPageToken[0].edit();
                        editor.putString(AppInfo.PREF_FIRST_NEXT_PAGE_TOKEN_KEY,
                                         response.getNextPageToken()).apply();
                        break;
                    case 1:
                        editor = sharedPrefNextPageToken[1].edit();
                        editor.putString(AppInfo.PREF_SECOND_NEXT_PAGE_TOKEN_KEY,
                                         response.getNextPageToken()).apply();
                        break;
                    default :
                        break;
                }
            }
        }
        callback.onPlacesAvailable(listRestaurants);
    }

    /**
     * Updates the list of restaurants with their details.
     * @param listRestaurant : List of restaurants
     * @param callback : Callback interface
     * @throws IOException : Exception thrown by getPlacesDetails() method of the @{@link ListRestaurantsService }
     *                       service class
     */
    public void getPlacesDetails(List<Restaurant> listRestaurant,
                                 ServiceDetailsCallback callback) throws IOException {
        // Contains each restaurant periods (closing and opening hours of a week) found
        List<HoursData> listHoursData = new ArrayList<>();
        List<List<HoursData>> listOfListHoursData = new ArrayList<>();

        for (int i = 0; i < listRestaurant.size(); i++) {
            DetailsResponse response = listRestaurantsServices.getPlacesDetails(
                                                                listRestaurant.get(i).getPlaceId());
            if (response.getResult().getWebsite() != null)
                 listRestaurant.get(i).setWebsiteUri(Uri.parse(response.getResult().getWebsite()).toString());
            if (response.getResult().getFormattedPhoneNumber() != null)
                 listRestaurant.get(i).setPhoneNumber(response.getResult().getFormattedPhoneNumber());

            if (response.getResult().getOpeningHours() != null) {
                if (response.getResult().getOpeningHours().getPeriods() != null) {
                    for (int j = 0; j < response.getResult().getOpeningHours().getPeriods().size(); j++) {
                        HoursData hoursData = new HoursData(
                                response.getResult().getOpeningHours().getPeriods().get(j).getClose(),
                                response.getResult().getOpeningHours().getPeriods().get(j).getOpen(),
                                listRestaurant.get(i).getPlaceId());
                        listHoursData.add(hoursData);
                    }
                    // Update Restaurant with associated Closing/Opening hours
                    listRestaurant.get(i).setOpeningAndClosingHours(
                          DataConverters.converterHoursDataToOpeningAndClosingHours(listHoursData));
                    // Update list of data (Closing/Opening hours) to send to database
                    ArrayList<HoursData> copy = new ArrayList<>(listHoursData);
                    listOfListHoursData.add(copy);
                }
            }
            listHoursData.clear();
        }
        callback.onPlacesDetailsAvailable(listRestaurant, listOfListHoursData);
    }

    /**
     * Creates a Restaurant object, by extracting data from a ResultPlaces
     * @param results : Results from GET response
     * @return : Restaurant object
     */
    public Restaurant initializeRestaurantObject(ResultPlaces results) {
        Restaurant restaurant = new Restaurant(
                results.getPlace_id(),
                results.getName(),
                results.getVicinity(),
                results.getGeometry().getLocation().getLat(),
                results.getGeometry().getLocation().getLng(),
                results.getRating());

        // Add photo data
        if (results.getPhotos() != null) {
            if (!results.getPhotos().isEmpty()) {
                restaurant.setPhotoReference(results.getPhotos().get(0).getPhoto_reference());
                restaurant.setPhotoHeight(results.getPhotos().get(0).getHeight());
                restaurant.setPhotoWidth(results.getPhotos().get(0).getWidth());
            }
        }
        return restaurant;
    }

    // Methods to access AutocompleteService
    /**
     * Accesses the performAutocompleteRequest() method of the @{@link ListRestaurantsService }
     * service class.
     * @param query : Autocomplete query
     * @param callback : ServiceAutocompleteCallback callback interface to send back results
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void performAutocompleteRequest(String query, ServiceAutocompleteCallback callback) {
        autocompleteService.performAutocompleteRequest(query, callback);
    }


    // Methods to access Database RestaurantDao
    /**
     * Inserts a new RestaurantData item in "restaurant_table" table in database.
     * @param restaurantData : Item to add
     */
    public void insertRestaurantData(RestaurantData restaurantData) {
        restaurantDao.insertRestaurantData(restaurantData);
    }

    /**
     * Deletes all data in restaurant_table from database.
     */
    public void deleteAllRestaurantsData() {
        restaurantDao.deleteAllRestaurantsData();
    }

    // Methods to access Database HoursDataDao
    /**
     * Inserts a new HoursData item in "hours_table" table in database.
     * @param hoursData : Item to add
     */
    public void insertHoursData(HoursData hoursData) {
        hoursDao.insertHoursData(hoursData);
    }

    /**
     * Deletes all data in "hours_table" table from database.
     */
    public void deleteAllHoursData() {
        hoursDao.deleteAllHoursData();
    }

    // Methods to access Database RestaurantAndHoursDao
    /**
     * Retrieves all RestaurantData and associated HourData from both tables in
     * database.
     * @return : List of RestaurantData and HoursData
     */
    public LiveData<List<RestaurantAndHoursData>> loadAllRestaurantsWithHours() {
        return restaurantAndHoursDao.loadAllRestaurantsWithHours();
    }

    // Other method
    /**
     * Checks the current user location and compare with the previous saved value,
     * to determine if a new search request is necessary or if data can be reloading from database.
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void getPlacesFromDatabaseOrRetrofitRequest(MainActivity activity,
                       SharedPreferences sharedPrefLatLon, MapViewFragmentCallback callback) {
        activity.getLocationClient()
                .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    double currentLatUserPosition;
                    double currentLonUserPosition;
                    double savedLatUserPosition;
                    double savedLonUserPosition;

                    // Get current location
                    currentLatUserPosition = location.getLatitude();
                    currentLonUserPosition = location.getLongitude();

                    if (AppInfo.checkIfFirstRunApp(activity.getApplicationContext())) {
                        callback.searchPlacesFromCurrentLocation();
                    }
                    else {
                        // Get previous location
                        savedLatUserPosition = Double.longBitsToDouble(sharedPrefLatLon.getLong(
                                AppInfo.PREF_OLD_LAT_POSITION_KEY,
                                Double.doubleToRawLongBits(currentLatUserPosition)));
                        savedLonUserPosition = Double.longBitsToDouble(sharedPrefLatLon.getLong(
                                AppInfo.PREF_OLD_LON_POSITION_KEY,
                                Double.doubleToRawLongBits(currentLonUserPosition)));
                        // Check distance
                        float[] result = new float[1];
                        Location.distanceBetween(currentLatUserPosition, currentLonUserPosition,
                                                savedLatUserPosition, savedLonUserPosition, result);
                        // Get locations
                        if (result[0] < 800) { // distance < 800m : reload locations from database
                            callback.restoreListFromDatabase();
                        }
                        else { // >= 800m : search places
                            callback.searchPlacesFromCurrentLocation();
                        }
                    }
                });
    }
}
