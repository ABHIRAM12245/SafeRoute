package com.vlabs.routeguard.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.vlabs.routeguard.managers.PhotoAttachmentManager;
import com.vlabs.routeguard.models.PhotoAttachment;
import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.models.RoadReportDraft;
import org.osmdroid.util.GeoPoint;
import android.location.Location;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.repositories.ReportRepository;
import com.vlabs.routeguard.repositories.ReportCreationRepository;
import java.util.List;

public class ReportCreationViewModel extends ViewModel {

    private final ReportCreationRepository repository;
    private final ReportRepository reportRepository;
    private final RoadReportDraft currentDraft;
    private final PhotoAttachmentManager photoAttachmentManager;
    
    private final MutableLiveData<Boolean> isSubmitEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<SubmissionState> submissionState = new MutableLiveData<>(SubmissionState.IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private RoadReport duplicateReport = null;
    private float duplicateDistanceMeters = 0f;

    public ReportCreationViewModel() {
        this.repository = new ReportCreationRepository();
        this.reportRepository = new ReportRepository();
        this.currentDraft = new RoadReportDraft();
        this.photoAttachmentManager = new PhotoAttachmentManager();
    }

    public LiveData<Boolean> getIsSubmitEnabled() {
        return isSubmitEnabled;
    }

    public LiveData<SubmissionState> getSubmissionState() {
        return submissionState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public RoadReport getDuplicateReport() {
        return duplicateReport;
    }

    public float getDuplicateDistanceMeters() {
        return duplicateDistanceMeters;
    }

    public void setLocation(GeoPoint location) {
        currentDraft.setLocation(location);
        validateDraft();
    }

    public void setCategory(ReportType category) {
        currentDraft.setCategory(category);
        validateDraft();
    }

    public void setDescription(String description) {
        currentDraft.setDescription(description);
        // Description is optional, no validation logic needed
    }

    public void setAttachment(PhotoAttachment attachment) {
        photoAttachmentManager.setAttachment(attachment);
        currentDraft.setPhotoAttachment(attachment);
    }

    public PhotoAttachment getAttachment() {
        return photoAttachmentManager.getAttachment();
    }

    public void clearAttachment() {
        photoAttachmentManager.clear();
        currentDraft.setPhotoAttachment(null);
    }

    public boolean hasAttachment() {
        return photoAttachmentManager.hasAttachment();
    }

    private void validateDraft() {
        boolean valid = currentDraft.getLocation() != null && currentDraft.getCategory() != null;
        isSubmitEnabled.setValue(valid);
    }

    public void submitReport(String reporterId) {
        submitReportInternal(reporterId, false);
    }

    public void submitReportInternal(String reporterId, boolean forceBypassDuplicateCheck) {
        if (Boolean.TRUE.equals(isSubmitEnabled.getValue()) && submissionState.getValue() != SubmissionState.SUBMITTING) {
            submissionState.setValue(SubmissionState.VALIDATING);
            
            if (reporterId == null || reporterId.isEmpty()) {
                errorMessage.setValue("Validation failed: Invalid session.");
                submissionState.setValue(SubmissionState.ERROR);
                return;
            }

            if (!forceBypassDuplicateCheck) {
                // Perform Duplicate Check
                reportRepository.fetchRoadReports(new ReportRepository.ReportCallback() {
                    @Override
                    public void onSuccess(List<RoadReport> reports) {
                        RoadReport closestDuplicate = null;
                        float minDistance = Float.MAX_VALUE;

                        for (RoadReport r : reports) {
                            if (r.getType() == currentDraft.getCategory() && r.getConfidenceScore() >= 20) {
                                float[] results = new float[1];
                                Location.distanceBetween(
                                    currentDraft.getLocation().getLatitude(), currentDraft.getLocation().getLongitude(),
                                    r.getLatitude(), r.getLongitude(), results);
                                
                                float distance = results[0];
                                if (distance <= 100 && distance < minDistance) {
                                    minDistance = distance;
                                    closestDuplicate = r;
                                }
                            }
                        }

                        if (closestDuplicate != null) {
                            duplicateReport = closestDuplicate;
                            duplicateDistanceMeters = minDistance;
                            submissionState.postValue(SubmissionState.DUPLICATE_FOUND);
                        } else {
                            executeSubmission(reporterId);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // On error fetching reports, just proceed with creation
                        executeSubmission(reporterId);
                    }
                });
            } else {
                executeSubmission(reporterId);
            }
        }
    }

    private void executeSubmission(String reporterId) {
        submissionState.postValue(SubmissionState.SUBMITTING);
        repository.submitReport(currentDraft, reporterId, new ReportCreationRepository.ReportSubmissionCallback() {
            @Override
            public void onSuccess() {
                submissionState.postValue(SubmissionState.SUCCESS);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
                submissionState.postValue(SubmissionState.ERROR);
            }
        });
    }
}
