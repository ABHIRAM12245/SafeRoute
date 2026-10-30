package com.vlabs.routeguard.api;

import com.vlabs.routeguard.models.PlaceSuggestion;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * OpenStreetMap Nominatim Geocoding REST API Service.
 *
 * Retrofit interface defining the HTTP GET contract for OpenStreetMap's Nominatim
 * geocoding and search endpoint.
 *
 * Endpoint Details:
 * - Base URL: {@code https://nominatim.openstreetmap.org/}
 * - Endpoint: {@code search}
 * - Response: JSON array mapped to {@link PlaceSuggestion} domain models.
 *
 * Usage:
 * - Invoked by {@link com.vlabs.routeguard.repositories.SearchRepository#searchPlaces(String, SearchRepository.SearchCallback)}
 *   when users enter text queries into the map search bar in {@link com.vlabs.routeguard.activities.HomeActivity}.
 */
public interface NominatimService {

    /**
     * Executes a forward geocoding query against the Nominatim API.
     *
     * @param query          The search string (e.g. city, landmark, or street name).
     * @param format         Expected serialization format ("json" or "jsonv2").
     * @param addressDetails Flag indicating whether to return fine-grained address components (1 for true, 0 for false).
     * @param limit          Maximum number of suggestions to return (typically capped at 5).
     * @return Retrofit {@link Call} delivering a list of {@link PlaceSuggestion} items.
     */
    @GET("search")
    Call<List<PlaceSuggestion>> searchPlaces(
            @Query("q") String query,
            @Query("format") String format,
            @Query("addressdetails") int addressDetails,
            @Query("limit") int limit
    );
}
