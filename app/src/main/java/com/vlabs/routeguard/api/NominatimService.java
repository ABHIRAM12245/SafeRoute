package com.vlabs.routeguard.api;

import com.vlabs.routeguard.models.PlaceSuggestion;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NominatimService {

    @GET("search")
    Call<List<PlaceSuggestion>> searchPlaces(
            @Query("q") String query,
            @Query("format") String format,
            @Query("addressdetails") int addressDetails,
            @Query("limit") int limit
    );
}
