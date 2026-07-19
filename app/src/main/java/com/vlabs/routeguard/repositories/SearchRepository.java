package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.api.NominatimService;
import com.vlabs.routeguard.api.RetrofitClient;
import com.vlabs.routeguard.models.PlaceSuggestion;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchRepository extends BaseRepository {

    private final NominatimService nominatimService;
    private Call<List<PlaceSuggestion>> currentCall;

    public interface SearchCallback {
        void onSuccess(List<PlaceSuggestion> results);
        void onError(String error);
    }

    public SearchRepository() {
        nominatimService = RetrofitClient.getClient().create(NominatimService.class);
    }

    public void searchPlaces(String query, SearchCallback callback) {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }

        currentCall = nominatimService.searchPlaces(query, "json", 1, 10); // Max 10 results
        currentCall.enqueue(new Callback<List<PlaceSuggestion>>() {
            @Override
            public void onResponse(Call<List<PlaceSuggestion>> call, Response<List<PlaceSuggestion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PlaceSuggestion> results = response.body();
                    if (results.isEmpty()) {
                        callback.onError("No results found");
                    } else {
                        callback.onSuccess(results);
                    }
                } else {
                    callback.onError("Request failed");
                }
            }

            @Override
            public void onFailure(Call<List<PlaceSuggestion>> call, Throwable t) {
                if (!call.isCanceled()) {
                    callback.onError("Request failed: " + t.getMessage());
                }
            }
        });
    }
}
