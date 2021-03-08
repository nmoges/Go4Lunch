package com.openclassrooms.go4lunch.service.response.places;

import java.util.List;

/**
 * Data retrieved from a JSON object, as a response of a Search Nearby API request.
 * Contains all information from the root element of the JSON object :
 *      - "result"
 *      - "next_page_token"
 *      - "status"
 **/
public class PlaceResponse {
    public List<ResultPlaces> results;
    public String next_page_token;
    public String status;
}
