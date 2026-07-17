package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.managers.BehaviorEngine;
import com.vlabs.routeguard.models.BehaviorMetrics;
import com.vlabs.routeguard.models.BehaviorResult;

/**
 * Repository layer that orchestrates the Behavior Engine.
 * Acts as the single entry point for behavior analysis.
 */
public class BehaviorRepository {

    public interface GetBehaviorResultCallback {
        void onSuccess(BehaviorResult result);
        void onFailure(Exception e);
    }

    public interface ReadBehaviorResultCallback {
        void onSuccess(BehaviorResult result);
        void onFailure(Exception e);
    }

    public interface SaveBehaviorResultCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    private final BehaviorEngine behaviorEngine;
    private final BehaviorFirestoreRepository behaviorFirestoreRepository;

    public BehaviorRepository() {
        this.behaviorEngine = new BehaviorEngine();
        this.behaviorFirestoreRepository = new BehaviorFirestoreRepository();
    }

    /**
     * Analyzes behavior for a given user asynchronously.
     *
     * @param userId The ID of the user.
     * @param callback The callback to receive the BehaviorResult or an error.
     */
    public void analyzeBehavior(String userId, GetBehaviorResultCallback callback) {
        if (callback == null) return;

        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        behaviorFirestoreRepository.getBehaviorMetrics(userId, new BehaviorFirestoreRepository.GetBehaviorMetricsCallback() {
            @Override
            public void onSuccess(BehaviorMetrics metrics) {
                BehaviorResult result = behaviorEngine.analyze(metrics);
                saveBehaviorResult(userId, result, new SaveBehaviorResultCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onSuccess(result);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void readBehaviorResult(String userId, ReadBehaviorResultCallback callback) {
        behaviorFirestoreRepository.readBehaviorResult(userId, new BehaviorFirestoreRepository.ReadBehaviorResultCallback() {
            @Override
            public void onSuccess(BehaviorResult result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void saveBehaviorResult(String userId, BehaviorResult result, SaveBehaviorResultCallback callback) {
        behaviorFirestoreRepository.saveBehaviorResult(userId, result, new BehaviorFirestoreRepository.SaveBehaviorResultCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                if (callback != null) callback.onFailure(e);
            }
        });
    }
}
