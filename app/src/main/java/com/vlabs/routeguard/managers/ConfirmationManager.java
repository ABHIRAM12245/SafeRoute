package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.ConfirmationCount;
import com.vlabs.routeguard.models.ReportConfirmation;
import com.vlabs.routeguard.repositories.ConfirmationRepository;

import java.util.List;

public class ConfirmationManager {

    private final ConfirmationRepository confirmationRepository;

    public ConfirmationManager() {
        this.confirmationRepository = new ConfirmationRepository();
    }

    public ConfirmationRepository getRepository() {
        return confirmationRepository;
    }

    public ConfirmationCount calculateConfirmationCount(List<ReportConfirmation> confirmations) {
        int active = 0;
        int gone = 0;
        long latestTimestamp = 0;

        if (confirmations != null) {
            for (ReportConfirmation conf : confirmations) {
                if (conf.getStatus() == ReportConfirmation.Status.ACTIVE) {
                    active++;
                } else if (conf.getStatus() == ReportConfirmation.Status.GONE) {
                    gone++;
                }
                if (conf.getTimestamp() > latestTimestamp) {
                    latestTimestamp = conf.getTimestamp();
                }
            }
        }
        return new ConfirmationCount(active, gone, latestTimestamp);
    }

    public static class ConfirmResult {
        public final boolean success;
        public final String error;
        public ConfirmResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }

    public void confirmReport(String reportId, String userId, ReportConfirmation.Status status, ReportConfirmation previousConfirmation, ConfirmationRepository.SaveCallback callback) {
        if (previousConfirmation != null) {
            long timeSinceLast = System.currentTimeMillis() - previousConfirmation.getTimestamp();
            if (timeSinceLast < 5 * 60 * 1000) {
                callback.onComplete(false, "You can only change your confirmation once every 5 minutes.");
                return;
            }
            if (previousConfirmation.getStatus() == status) {
                callback.onComplete(false, "You have already confirmed this status.");
                return;
            }
        }

        ReportConfirmation newConf = new ReportConfirmation(reportId, userId, status, System.currentTimeMillis());
        confirmationRepository.saveConfirmation(newConf, callback);
    }

    public interface GetConfirmationCountCallback {
        void onSuccess(ConfirmationCount count);
        void onFailure(Exception e);
    }

    public void getConfirmationCount(String reportId, GetConfirmationCountCallback callback) {
        if (reportId == null || reportId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Report ID cannot be empty"));
            return;
        }

        confirmationRepository.getConfirmations(reportId, new ConfirmationRepository.GetConfirmationsCallback() {
            @Override
            public void onSuccess(List<ReportConfirmation> confirmations) {
                ConfirmationCount count = calculateConfirmationCount(confirmations);
                callback.onSuccess(count);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void getCurrentUserConfirmation(String reportId, String userId, ConfirmationRepository.GetCurrentUserConfirmationCallback callback) {
        if (reportId == null || reportId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Report ID and User ID cannot be empty"));
            return;
        }
        
        confirmationRepository.getCurrentUserConfirmation(reportId, userId, callback);
    }
}
