package com.vlabs.routeguard.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.vlabs.routeguard.managers.PhotoAttachmentManager;
import com.vlabs.routeguard.models.PhotoAttachment;
import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.models.RoadReportDraft;
import com.vlabs.routeguard.repositories.ReportCreationRepository;

import org.osmdroid.util.GeoPoint;

public class ReportCreationViewModel extends ViewModel {

    private final ReportCreationRepository repository;
    private final RoadReportDraft currentDraft;
    private final PhotoAttachmentManager photoAttachmentManager;
    
    private final MutableLiveData<Boolean> isSubmitEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<SubmissionState> submissionState = new MutableLiveData<>(SubmissionState.IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ReportCreationViewModel() {
        this.repository = new ReportCreationRepository();
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
        if (Boolean.TRUE.equals(isSubmitEnabled.getValue()) && submissionState.getValue() != SubmissionState.SUBMITTING) {
            submissionState.setValue(SubmissionState.VALIDATING);
            
            if (reporterId == null || reporterId.isEmpty()) {
                errorMessage.setValue("Validation failed: Invalid session.");
                submissionState.setValue(SubmissionState.ERROR);
                return;
            }

            submissionState.setValue(SubmissionState.SUBMITTING);
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
}
