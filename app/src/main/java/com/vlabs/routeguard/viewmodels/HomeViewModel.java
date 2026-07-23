package com.vlabs.routeguard.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.vlabs.routeguard.models.PlaceSuggestion;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.models.ConfirmationCount;
import com.vlabs.routeguard.models.ReportConfirmation;
import com.vlabs.routeguard.repositories.ReportRepository;
import com.vlabs.routeguard.repositories.SearchRepository;
import com.vlabs.routeguard.repositories.ConfirmationRepository;
import com.vlabs.routeguard.repositories.CommentRepository;
import com.vlabs.routeguard.managers.ConfirmationManager;
import com.vlabs.routeguard.managers.SessionManager;
import com.google.firebase.auth.FirebaseUser;
import com.vlabs.routeguard.models.Comment;
import com.vlabs.routeguard.utils.NetworkUtils;
import com.vlabs.routeguard.utils.RadiusConfig;
import org.osmdroid.util.GeoPoint;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeViewModel extends BaseViewModel {

    public enum CommentSubmitState {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private final SearchRepository searchRepository;
    private final ReportRepository reportRepository;
    private final ConfirmationManager confirmationManager;
    private final MutableLiveData<List<PlaceSuggestion>> suggestionsLiveData;
    private final MutableLiveData<List<RoadReport>> roadReportsLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;
    private final MutableLiveData<GeoPoint> currentLocationLiveData;
    private final MutableLiveData<Double> activeRadiusKm;
    private final MutableLiveData<Set<String>> selectedCategories;
    
    private final MutableLiveData<ConfirmationCount> confirmationCountLiveData;
    private final MutableLiveData<ReportConfirmation> userConfirmationLiveData;
    
    private final CommentRepository commentRepository;
    private final MutableLiveData<List<Comment>> commentsLiveData;
    private final MutableLiveData<Boolean> commentsLoadingLiveData;
    private final MutableLiveData<CommentSubmitState> commentSubmitStateLiveData;
    
    private com.google.firebase.firestore.ListenerRegistration confirmationCountListener;
    private com.google.firebase.firestore.ListenerRegistration userConfirmationListener;
    private com.google.firebase.firestore.ListenerRegistration commentsListener;

    public HomeViewModel() {
        this.searchRepository = new SearchRepository();
        this.reportRepository = new ReportRepository();
        this.confirmationManager = new ConfirmationManager();
        this.suggestionsLiveData = new MutableLiveData<>();
        this.roadReportsLiveData = new MutableLiveData<>();
        this.errorLiveData = new MutableLiveData<>();
        this.isLoadingLiveData = new MutableLiveData<>(false);
        this.currentLocationLiveData = new MutableLiveData<>();
        this.activeRadiusKm = new MutableLiveData<>(RadiusConfig.DEFAULT_RADIUS_KM);
        this.selectedCategories = new MutableLiveData<>(new HashSet<>());
        this.confirmationCountLiveData = new MutableLiveData<>();
        this.userConfirmationLiveData = new MutableLiveData<>();
        this.commentRepository = new CommentRepository();
        this.commentsLiveData = new MutableLiveData<>();
        this.commentsLoadingLiveData = new MutableLiveData<>(false);
        this.commentSubmitStateLiveData = new MutableLiveData<>(CommentSubmitState.IDLE);
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

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<GeoPoint> getCurrentLocation() {
        return currentLocationLiveData;
    }

    public LiveData<Set<String>> getSelectedCategories() {
        return selectedCategories;
    }

    public LiveData<Double> getActiveRadiusKm() {
        return activeRadiusKm;
    }

    public LiveData<ConfirmationCount> getConfirmationCount() {
        return confirmationCountLiveData;
    }

    public LiveData<ReportConfirmation> getUserConfirmation() {
        return userConfirmationLiveData;
    }

    public LiveData<List<Comment>> getComments() {
        return commentsLiveData;
    }

    public LiveData<Boolean> getCommentsLoading() {
        return commentsLoadingLiveData;
    }

    public LiveData<CommentSubmitState> getCommentSubmitState() {
        return commentSubmitStateLiveData;
    }

    public void resetCommentSubmitState() {
        commentSubmitStateLiveData.setValue(CommentSubmitState.IDLE);
    }

    public void startConfirmationListeners(String reportId, String userId) {
        stopConfirmationListeners();
        
        confirmationCountListener = confirmationManager.getRepository().startConfirmationsListener(reportId, new ConfirmationRepository.GetConfirmationsCallback() {
            @Override
            public void onSuccess(List<ReportConfirmation> confirmations) {
                ConfirmationCount count = confirmationManager.calculateConfirmationCount(confirmations);
                confirmationCountLiveData.postValue(count);
            }

            @Override
            public void onFailure(Exception e) {
                // Ignore
            }
        });

        if (userId != null && !userId.isEmpty()) {
            userConfirmationListener = confirmationManager.getRepository().startCurrentUserConfirmationListener(reportId, userId, new ConfirmationRepository.GetCurrentUserConfirmationCallback() {
                @Override
                public void onSuccess(ReportConfirmation confirmation) {
                    userConfirmationLiveData.postValue(confirmation);
                }

                @Override
                public void onFailure(Exception e) {
                    // Ignore
                }
            });
        }
    }

    public void stopConfirmationListeners() {
        if (confirmationCountListener != null) {
            confirmationCountListener.remove();
            confirmationCountListener = null;
        }
        if (userConfirmationListener != null) {
            userConfirmationListener.remove();
            userConfirmationListener = null;
        }
        confirmationCountLiveData.setValue(null);
        userConfirmationLiveData.setValue(null);
    }

    public void fetchConfirmationCount(String reportId) {
        confirmationManager.getConfirmationCount(reportId, new ConfirmationManager.GetConfirmationCountCallback() {
            @Override
            public void onSuccess(ConfirmationCount count) {
                confirmationCountLiveData.postValue(count);
            }

            @Override
            public void onFailure(Exception e) {
                // Ignore
            }
        });
    }

    public void fetchUserConfirmation(String reportId, String userId) {
        if (userId != null && !userId.isEmpty()) {
            confirmationManager.getCurrentUserConfirmation(reportId, userId, new ConfirmationRepository.GetCurrentUserConfirmationCallback() {
                @Override
                public void onSuccess(ReportConfirmation confirmation) {
                    userConfirmationLiveData.postValue(confirmation);
                }

                @Override
                public void onFailure(Exception e) {
                    // Ignore
                }
            });
        }
    }

    public void startCommentsListener(String reportId) {
        stopCommentsListener();
        commentsLoadingLiveData.postValue(true);
        
        commentsListener = commentRepository.startCommentsListener(reportId, new CommentRepository.GetCommentsCallback() {
            @Override
            public void onSuccess(List<Comment> comments) {
                commentsLoadingLiveData.postValue(false);
                commentsLiveData.postValue(comments);
            }

            @Override
            public void onFailure(Exception e) {
                commentsLoadingLiveData.postValue(false);
                errorLiveData.postValue("Failed to load comments: " + e.getMessage());
                android.util.Log.e("HomeViewModel", "Failed to load comments", e);
            }
        });
    }

    public void stopCommentsListener() {
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
    }

    public void postComment(String reportId, String text) {
        if (text == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.length() > 200) {
            return;
        }
        
        FirebaseUser currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            errorLiveData.postValue("You must be logged in to comment.");
            return;
        }

        commentSubmitStateLiveData.postValue(CommentSubmitState.LOADING);

        Comment comment = new Comment();
        comment.setUserId(currentUser.getUid());
        comment.setDisplayName(currentUser.getDisplayName());
        if (currentUser.getPhotoUrl() != null) {
            comment.setPhotoUrl(currentUser.getPhotoUrl().toString());
        }
        comment.setText(trimmed);
        
        commentRepository.addComment(reportId, comment, new CommentRepository.AddCommentCallback() {
            @Override
            public void onSuccess() {
                commentSubmitStateLiveData.postValue(CommentSubmitState.SUCCESS);
            }

            @Override
            public void onFailure(Exception e) {
                commentSubmitStateLiveData.postValue(CommentSubmitState.ERROR);
                android.util.Log.e("HomeViewModel", "Failed to post comment", e);
            }
        });
    }

    public void setActiveRadiusKm(double radiusKm) {
        activeRadiusKm.postValue(radiusKm);
    }

    public void setSelectedCategories(Set<String> categories) {
        if (categories != null) {
            selectedCategories.postValue(new HashSet<>(categories));
        } else {
            selectedCategories.postValue(new HashSet<>());
        }
    }

    public void setCurrentLocation(GeoPoint point) {
        currentLocationLiveData.postValue(point);
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
            isLoadingLiveData.postValue(false);
            errorLiveData.setValue("No internet connection");
            return;
        }

        if (query == null || query.trim().length() < 2) {
            isLoadingLiveData.postValue(false);
            suggestionsLiveData.setValue(null); // Clear suggestions
            return;
        }

        isLoadingLiveData.postValue(true);
        searchRepository.searchPlaces(query.trim(), new SearchRepository.SearchCallback() {
            @Override
            public void onSuccess(List<PlaceSuggestion> results) {
                isLoadingLiveData.postValue(false);
                suggestionsLiveData.postValue(results);
            }

            @Override
            public void onError(String error) {
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue(error);
            }
        });
    }

    public void clearSuggestions() {
        suggestionsLiveData.setValue(null);
    }
}
