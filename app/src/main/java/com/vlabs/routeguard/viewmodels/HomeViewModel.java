package com.vlabs.routeguard.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.vlabs.routeguard.models.PlaceSuggestion;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.repositories.ReportRepository;
import com.vlabs.routeguard.repositories.SearchRepository;
import com.vlabs.routeguard.utils.NetworkUtils;
import java.util.List;

public class HomeViewModel extends BaseViewModel {

    private final SearchRepository searchRepository;
    private final ReportRepository reportRepository;
    private final MutableLiveData<List<PlaceSuggestion>> suggestionsLiveData;
    private final MutableLiveData<List<RoadReport>> roadReportsLiveData;
    private final MutableLiveData<String> errorLiveData;
    
    public HomeViewModel() {
        this.searchRepository = new SearchRepository();
        this.reportRepository = new ReportRepository();
        this.suggestionsLiveData = new MutableLiveData<>();
        this.roadReportsLiveData = new MutableLiveData<>();
        this.errorLiveData = new MutableLiveData<>();
    }

    public LiveData<List<PlaceSuggestion>> getSuggestions() {
        return suggestionsLiveData;
    }

    public LiveData<List<RoadReport>> getRoadReports() {
        return roadReportsLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void loadRoadReports() {
        reportRepository.fetchRoadReports(new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(List<RoadReport> reports) {
                roadReportsLiveData.postValue(reports);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
            }
        });
    }

    public void startRoadReportsListener() {
        reportRepository.startRoadReportsListener(new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(List<RoadReport> reports) {
                roadReportsLiveData.postValue(reports);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
            }
        });
    }

    public void stopRoadReportsListener() {
        reportRepository.stopRoadReportsListener();
    }

    public void searchPlaces(String query, boolean isNetworkAvailable) {
        if (!isNetworkAvailable) {
            errorLiveData.setValue("No internet connection");
            return;
        }

        if (query == null || query.trim().length() < 2) {
            suggestionsLiveData.setValue(null); // Clear suggestions
            return;
        }

        searchRepository.searchPlaces(query.trim(), new SearchRepository.SearchCallback() {
            @Override
            public void onSuccess(List<PlaceSuggestion> results) {
                suggestionsLiveData.postValue(results);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
            }
        });
    }

    public void clearSuggestions() {
        suggestionsLiveData.setValue(null);
    }
}
