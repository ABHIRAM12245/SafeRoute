package com.vlabs.routeguard.fragments;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.managers.VotingManager;
import com.vlabs.routeguard.managers.SessionManager;
import com.google.firebase.auth.FirebaseUser;
import com.vlabs.routeguard.models.VoteCount;
import com.vlabs.routeguard.models.ReportVote;
import com.vlabs.routeguard.models.VoteResult;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.models.ConfirmationCount;
import com.vlabs.routeguard.models.ReportConfirmation;
import com.vlabs.routeguard.repositories.ReportRepository;
import com.vlabs.routeguard.repositories.UserRepository;
import com.vlabs.routeguard.managers.ConfirmationManager;
import com.vlabs.routeguard.viewmodels.HomeViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.vlabs.routeguard.models.User;
import com.vlabs.routeguard.models.Comment;
import com.vlabs.routeguard.adapters.CommentAdapter;
import android.location.Location;

/**
 * Road Hazard Details Bottom Sheet Dialog.
 *
 * Displays full metadata, media attachments, real-time community verification metrics,
 * and discussion threads for a selected road hazard report.
 *
 * Architectural & Community Interactions:
 * 1. Report Hydration & Cluster Navigation:
 *    - Loads report data by ID via {@link ReportRepository#getReport(String, ReportRepository.ReportCallback)}.
 *    - If multiple co-located reports were grouped under a cluster marker, enables pagination controls
 *      (Previous / Next) to cycle through all reports in the group.
 * 2. Community Voting Subsystem:
 *    - Interfaces with {@link VotingManager} to fetch aggregated upvotes/downvotes and observe the active
 *      user's vote record at {@code reports/{reportId}/votes/{userId}}.
 *    - Enforces anti-abuse rules via {@link com.vlabs.routeguard.managers.VoteBehaviorAnalyzer}.
 * 3. Existence Confirmations:
 *    - Coordinates with {@link ConfirmationManager} to record "Still Exists" (+confirm) or "Cleared / Not Found" (-confirm)
 *      validations at {@code reports/{reportId}/confirmations/{userId}}.
 * 4. Dynamic Trust & Reliability Scoring:
 *    - Evaluates community signals using {@link com.vlabs.routeguard.managers.ConfidenceEngine} to determine
 *      the live reliability tier (Confirmed, High, Moderate, Low, Unreliable).
 * 5. Interactive Discussion Thread:
 *    - Real-time comment submission and rendering backed by {@link com.vlabs.routeguard.repositories.CommentRepository}
 *      and {@link CommentAdapter} under {@code reports/{reportId}/comments}.
 */
public class ReportDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_REPORT_IDS = "arg_report_ids";
    private static final String ARG_USER_LAT = "arg_user_lat";
    private static final String ARG_USER_LON = "arg_user_lon";

    private VotingManager votingManager;
    private ReportRepository reportRepository;
    private UserRepository userRepository;
    private ConfirmationManager confirmationManager;
    private HomeViewModel homeViewModel;
    
    private com.google.firebase.firestore.ListenerRegistration voteCountListener;
    private com.google.firebase.firestore.ListenerRegistration userVoteListener;
    
    private VotingManager.VoteType lastKnownUserVoteType = null;
    private ReportConfirmation lastKnownUserConfirmation = null;
    
    private boolean isVoting = false;
    private boolean isConfirming = false;
    
    private VoteCount currentVoteCount = null;
    private ConfirmationCount currentConfirmCount = null;
    private long reportTimestampMs = 0;
    
    private java.util.ArrayList<String> reportIds = new java.util.ArrayList<>();
    private int currentIndex = 0;
    
    // Comments UI
    private CommentAdapter commentAdapter;
    private RecyclerView rvComments;
    private ProgressBar pbCommentsLoading;
    private TextView tvCommentsEmptyState;
    private EditText etCommentInput;
    private TextView tvCharCounter;
    private ImageButton btnSendComment;
    private TextWatcher commentTextWatcher;

    public static ReportDetailsBottomSheet newInstance(java.util.ArrayList<String> reportIds, double userLat, double userLon) {
        ReportDetailsBottomSheet fragment = new ReportDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_REPORT_IDS, reportIds);
        args.putDouble(ARG_USER_LAT, userLat);
        args.putDouble(ARG_USER_LON, userLon);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_report_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        votingManager = new VotingManager();
        reportRepository = new ReportRepository();
        userRepository = new UserRepository();
        confirmationManager = new ConfirmationManager();
        if (getActivity() != null) {
            homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        }

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        if (getArguments() != null && getArguments().getStringArrayList(ARG_REPORT_IDS) != null) {
            reportIds = getArguments().getStringArrayList(ARG_REPORT_IDS);
        }

        setupClusterNavigation(view);

        if (reportIds != null && !reportIds.isEmpty()) {
            loadReportAtIndex(currentIndex, view);
        } else {
            dismiss();
        }
    }

    private void setupClusterNavigation(View view) {
        View llClusterNavigation = view.findViewById(R.id.llClusterNavigation);
        if (reportIds == null || reportIds.size() <= 1) {
            if (llClusterNavigation != null) llClusterNavigation.setVisibility(View.GONE);
            return;
        }

        if (llClusterNavigation != null) llClusterNavigation.setVisibility(View.VISIBLE);
        
        ImageButton btnPrev = view.findViewById(R.id.btnPrevReport);
        ImageButton btnNext = view.findViewById(R.id.btnNextReport);
        
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (currentIndex > 0) {
                    currentIndex--;
                    loadReportAtIndex(currentIndex, view);
                }
            });
        }
        
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (currentIndex < reportIds.size() - 1) {
                    currentIndex++;
                    loadReportAtIndex(currentIndex, view);
                }
            });
        }
    }

    private void cleanupListeners() {
        if (voteCountListener != null) {
            voteCountListener.remove();
            voteCountListener = null;
        }
        if (userVoteListener != null) {
            userVoteListener.remove();
            userVoteListener = null;
        }
        if (homeViewModel != null) {
            homeViewModel.stopConfirmationListeners();
            homeViewModel.stopCommentsListener();
            try {
                homeViewModel.getConfirmationCount().removeObservers(getViewLifecycleOwner());
                homeViewModel.getUserConfirmation().removeObservers(getViewLifecycleOwner());
                homeViewModel.getCommentsLoading().removeObservers(getViewLifecycleOwner());
                homeViewModel.getComments().removeObservers(getViewLifecycleOwner());
                homeViewModel.getCommentSubmitState().removeObservers(getViewLifecycleOwner());
            } catch (Exception ignored) {}
        }
        if (etCommentInput != null && commentTextWatcher != null) {
            etCommentInput.removeTextChangedListener(commentTextWatcher);
            commentTextWatcher = null;
        }
        // Reset states
        currentVoteCount = null;
        currentConfirmCount = null;
        lastKnownUserVoteType = null;
        lastKnownUserConfirmation = null;
        isVoting = false;
        isConfirming = false;
    }

    private void loadReportAtIndex(int index, View view) {
        if (reportIds == null || index < 0 || index >= reportIds.size()) return;
        
        cleanupListeners();
        
        String reportId = reportIds.get(index);
        
        TextView tvProgress = view.findViewById(R.id.tvClusterProgress);
        if (tvProgress != null && reportIds.size() > 1) {
            tvProgress.setText("Report " + (index + 1) + " of " + reportIds.size());
        }
        
        ImageButton btnPrev = view.findViewById(R.id.btnPrevReport);
        ImageButton btnNext = view.findViewById(R.id.btnNextReport);
        if (btnPrev != null) btnPrev.setEnabled(index > 0);
        if (btnNext != null) btnNext.setEnabled(index < reportIds.size() - 1);
        if (btnPrev != null) btnPrev.setAlpha(index > 0 ? 1.0f : 0.3f);
        if (btnNext != null) btnNext.setAlpha(index < reportIds.size() - 1 ? 1.0f : 0.3f);

        reportRepository.fetchReportById(reportId, new ReportRepository.SingleReportCallback() {
            @Override
            public void onSuccess(RoadReport report) {
                if (getActivity() == null || !isAdded() || getView() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (getContext() == null || !isAdded() || getView() == null) return;
                    reportTimestampMs = report.getTimestamp();
                    updateUI(view, report);
                    setupVoting(view, report);
                    setupConfirmations(view, report);
                    setupComments(view, reportId);
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null || !isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "This report has expired.", Toast.LENGTH_SHORT).show();
                    }
                    dismissAllowingStateLoss();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanupListeners();
    }

    private void updateUI(View view, RoadReport report) {
        if (getContext() == null || !isAdded()) return;
        Context ctx = getContext();

        ImageView ivIcon = view.findViewById(R.id.ivReportIcon);
        TextView tvType = view.findViewById(R.id.tvReportType);
        TextView tvTime = view.findViewById(R.id.tvReportTime);
        TextView tvDesc = view.findViewById(R.id.tvReportDescription);
        TextView tvReporterName = view.findViewById(R.id.tvReporterName);
        TextView tvDistance = view.findViewById(R.id.tvDistance);
        TextView tvExpiryStatus = view.findViewById(R.id.tvExpiryStatus);

        tvDesc.setText(report.getDescription());
        tvTime.setText(DateUtils.getRelativeTimeSpanString(report.getTimestamp()));

        View cvReportPhoto = view.findViewById(R.id.cvReportPhoto);
        ImageView ivReportPhoto = view.findViewById(R.id.ivReportPhoto);
        
        String photoUrl = report.getImageUrl();
        if (photoUrl != null && !photoUrl.isEmpty() && isAdded()) {
            cvReportPhoto.setVisibility(View.VISIBLE);
            try {
                com.bumptech.glide.Glide.with(this)
                        .load(photoUrl)
                        .into(ivReportPhoto);
            } catch (Exception e) {
                cvReportPhoto.setVisibility(View.GONE);
            }
        } else {
            cvReportPhoto.setVisibility(View.GONE);
        }

        // Expiry Status
        long timeRemaining = report.getExpiresAt() - System.currentTimeMillis();
        if (timeRemaining <= 0) {
            tvExpiryStatus.setText("Expired");
            tvExpiryStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_dark));
        } else {
            tvExpiryStatus.setText(formatExpiryStatus(timeRemaining));
            if (timeRemaining < 15 * 60 * 1000L) {
                tvExpiryStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_orange_dark));
            } else {
                tvExpiryStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.darker_gray));
            }
        }

        // Distance
        double userLat = getArguments() != null ? getArguments().getDouble(ARG_USER_LAT, Double.NaN) : Double.NaN;
        double userLon = getArguments() != null ? getArguments().getDouble(ARG_USER_LON, Double.NaN) : Double.NaN;
        
        if (!Double.isNaN(userLat) && !Double.isNaN(userLon) && tvDistance != null) {
            float[] results = new float[1];
            Location.distanceBetween(userLat, userLon, report.getLatitude(), report.getLongitude(), results);
            float distanceMeters = results[0];
            if (distanceMeters < 1000) {
                tvDistance.setText(String.format(java.util.Locale.getDefault(), "%.0f m", distanceMeters));
            } else {
                tvDistance.setText(String.format(java.util.Locale.getDefault(), "%.1f km", distanceMeters / 1000f));
            }
            tvDistance.setVisibility(View.VISIBLE);
        } else if (tvDistance != null) {
            tvDistance.setVisibility(View.GONE);
        }

        // Reporter Name
        if (tvReporterName != null) {
            String currentUserId = SessionManager.getInstance().getCurrentUser() != null ? SessionManager.getInstance().getCurrentUser().getUid() : "";
            if (report.getReporterId() != null && report.getReporterId().equals(currentUserId)) {
                tvReporterName.setText("Reported by You");
            } else if (report.getReporterId() != null && !report.getReporterId().isEmpty()) {
                userRepository.getUser(report.getReporterId(), new UserRepository.GetUserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        if (getActivity() == null || !isAdded()) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded() || getView() == null) return;
                            String name = user != null ? user.getDisplayName() : null;
                            if (name == null || name.trim().isEmpty()) {
                                tvReporterName.setText("Anonymous");
                            } else {
                                tvReporterName.setText(name);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (getActivity() == null || !isAdded()) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded() || getView() == null) return;
                            tvReporterName.setText("Anonymous");
                        });
                    }
                });
            } else {
                tvReporterName.setText("Anonymous");
            }
        }

        // Verification Status Badge (Requirement 9 & Refinement #2)
        TextView tvVerificationBadge = view.findViewById(R.id.tvVerificationBadge);
        if (tvVerificationBadge != null) {
            com.vlabs.routeguard.models.VerificationStatus vStatus = report.getVerificationStatus();
            if (vStatus == null) vStatus = com.vlabs.routeguard.models.VerificationStatus.UNVERIFIED;
            tvVerificationBadge.setText(vStatus.name());
            switch (vStatus) {
                case VERIFIED:
                    tvVerificationBadge.setBackgroundResource(R.drawable.bg_chip_verified);
                    tvVerificationBadge.setTextColor(ContextCompat.getColor(ctx, R.color.rg_on_primary_container));
                    break;
                case DISPUTED:
                    tvVerificationBadge.setBackgroundResource(R.drawable.bg_chip_disputed);
                    tvVerificationBadge.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_dark));
                    break;
                case EXPIRED:
                    tvVerificationBadge.setBackgroundResource(R.drawable.bg_chip_expired);
                    tvVerificationBadge.setTextColor(ContextCompat.getColor(ctx, android.R.color.darker_gray));
                    break;
                case UNVERIFIED:
                default:
                    tvVerificationBadge.setBackgroundResource(R.drawable.bg_chip_unverified);
                    tvVerificationBadge.setTextColor(ContextCompat.getColor(ctx, R.color.rg_on_tertiary_container));
                    break;
            }
        }

        switch (report.getType()) {
            case POLICE_CHECKING:
                tvType.setText(getString(R.string.police_checking));
                ivIcon.setImageResource(R.drawable.ic_police);
                ivIcon.setColorFilter(ContextCompat.getColor(ctx, android.R.color.holo_blue_dark));
                break;
            case ACCIDENT:
                tvType.setText(getString(R.string.accident));
                ivIcon.setImageResource(R.drawable.ic_accident);
                ivIcon.setColorFilter(ContextCompat.getColor(ctx, android.R.color.holo_red_dark));
                break;
            case HEAVY_TRAFFIC:
                tvType.setText(getString(R.string.heavy_traffic));
                ivIcon.setImageResource(R.drawable.ic_traffic);
                ivIcon.setColorFilter(ContextCompat.getColor(ctx, android.R.color.holo_orange_dark));
                break;
            case ROAD_CONSTRUCTION:
                tvType.setText(getString(R.string.road_construction));
                ivIcon.setImageResource(R.drawable.ic_construction);
                ivIcon.setColorFilter(ContextCompat.getColor(ctx, android.R.color.holo_orange_light));
                break;
            case ANIMAL_ON_ROAD:
                tvType.setText(getString(R.string.animal_on_road));
                ivIcon.setImageResource(R.drawable.ic_animal);
                ivIcon.setColorFilter(ContextCompat.getColor(ctx, android.R.color.darker_gray));
                break;
            case WATER_LOGGING:
                tvType.setText(getString(R.string.water_logging));
                ivIcon.setImageResource(R.drawable.ic_water_logging);
                ivIcon.setColorFilter(ContextCompat.getColor(ctx, android.R.color.holo_blue_light));
                break;
            case OTHER:
            default:
                tvType.setText(getString(R.string.other));
                ivIcon.setImageResource(R.drawable.ic_other);
                ivIcon.setColorFilter(ContextCompat.getColor(ctx, android.R.color.darker_gray));
                break;
        }
    }

    private void setupVoting(View view, RoadReport report) {
        ImageButton btnUpvote = view.findViewById(R.id.btnUpvote);
        ImageButton btnDownvote = view.findViewById(R.id.btnDownvote);
        String reportId = report.getReportId();

        String currentUserId = SessionManager.getInstance().getCurrentUser() != null ? SessionManager.getInstance().getCurrentUser().getUid() : "";
        boolean canVote = new com.vlabs.routeguard.managers.TrustManager().canUserVote(currentUserId, report.getReporterId());

        if (btnUpvote != null) {
            if (!canVote) {
                btnUpvote.setEnabled(false);
                btnUpvote.setAlpha(0.5f);
            } else {
                btnUpvote.setOnClickListener(v -> submitVote(report, VotingManager.VoteType.UPVOTE, btnUpvote, btnDownvote));
            }
        }

        if (btnDownvote != null) {
            if (!canVote) {
                btnDownvote.setEnabled(false);
                btnDownvote.setAlpha(0.5f);
            } else {
                btnDownvote.setOnClickListener(v -> submitVote(report, VotingManager.VoteType.DOWNVOTE, btnUpvote, btnDownvote));
            }
        }

        fetchVoteData(reportId);
    }

    private void fetchVoteData(String reportId) {
        if (getView() == null || !isAdded()) return;
        
        if (reportId == null || reportId.isEmpty()) return;

        FirebaseUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        ImageButton btnUpvote = getView().findViewById(R.id.btnUpvote);
        ImageButton btnDownvote = getView().findViewById(R.id.btnDownvote);
        TextView tvUpvoteCount = getView().findViewById(R.id.tvUpvoteCount);
        TextView tvDownvoteCount = getView().findViewById(R.id.tvDownvoteCount);
        TextView tvTrustScore = getView().findViewById(R.id.tvTrustScore);

        if (userVoteListener != null) { userVoteListener.remove(); }
        userVoteListener = votingManager.startCurrentUserVoteListener(reportId, user.getUid(), new VotingManager.GetCurrentUserVoteCallback() {
            @Override
            public void onSuccess(ReportVote vote) {
                if (getActivity() == null || !isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;
                    lastKnownUserVoteType = vote != null ? vote.getVoteType() : null;
                    if (!isVoting) {
                        updateVoteButtons(btnUpvote, btnDownvote, lastKnownUserVoteType);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null || !isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;
                    lastKnownUserVoteType = null;
                    if (!isVoting) {
                        updateVoteButtons(btnUpvote, btnDownvote, null);
                    }
                });
            }
        });

        if (voteCountListener != null) { voteCountListener.remove(); }
        voteCountListener = votingManager.startVoteCountListener(reportId, new VotingManager.GetVoteCountCallback() {
            @Override
            public void onSuccess(VoteCount voteCount) {
                if (getActivity() == null || !isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;
                    currentVoteCount = voteCount;
                    if (tvUpvoteCount != null) {
                        tvUpvoteCount.setText(String.valueOf(voteCount.getUpvoteCount()));
                    }
                    if (tvDownvoteCount != null) {
                        tvDownvoteCount.setText(String.valueOf(voteCount.getDownvoteCount()));
                    }
                    updateTrustScoreUI(tvTrustScore);
                });
            }

            @Override
            public void onFailure(Exception e) {
                // Leave defaults
            }
        });
    }

    private void updateTrustScoreUI(TextView tvTrustScore) {
        if (tvTrustScore != null) {
            int trustScore = votingManager.calculateTrustScore(currentVoteCount, currentConfirmCount, reportTimestampMs);
            tvTrustScore.setText("Trust Score: " + trustScore);
        }
    }

    private void submitVote(RoadReport report, VotingManager.VoteType voteType, ImageButton btnUpvote, ImageButton btnDownvote) {
        if (report == null || report.getReportId() == null || !isAdded()) return;
        FirebaseUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null || isVoting) {
            return;
        }

        // Refinement #4: Self-voting protection check
        if (!new com.vlabs.routeguard.managers.TrustManager().canUserVote(user.getUid(), report.getReporterId())) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "You cannot vote on your own report.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Optimistic UI
        isVoting = true;
        btnUpvote.setEnabled(false);
        btnDownvote.setEnabled(false);
        updateVoteButtons(btnUpvote, btnDownvote, voteType);

        String reportId = report.getReportId();

        new Thread(() -> {
            VoteResult result = votingManager.vote(reportId, report.getReporterId(), voteType, user.getUid());
            if (getActivity() == null || !isAdded()) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || getView() == null) return;
                isVoting = false;
                btnUpvote.setEnabled(true);
                btnDownvote.setEnabled(true);
                
                if (result == null || !result.isSuccess()) {
                    // Revert to last known good state
                    updateVoteButtons(btnUpvote, btnDownvote, lastKnownUserVoteType);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to vote. Check your connection.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Refinement #3: Invoke TrustRepository to recalculate report verification and creator trust
                    boolean isUp = voteType == VotingManager.VoteType.UPVOTE;
                    new com.vlabs.routeguard.repositories.TrustRepository().processReportVote(report, user.getUid(), isUp, new com.vlabs.routeguard.repositories.TrustRepository.VoteSubmissionCallback() {
                        @Override
                        public void onSuccess(com.vlabs.routeguard.models.VerificationStatus newVerificationStatus) {
                            if (getActivity() == null || !isAdded()) return;
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded() || getView() == null) return;
                                TextView tvBadge = getView().findViewById(R.id.tvVerificationBadge);
                                if (tvBadge != null && newVerificationStatus != null) {
                                    tvBadge.setText(newVerificationStatus.name());
                                }
                            });
                        }

                        @Override
                        public void onError(String errorMsg) {
                            if (getActivity() == null || !isAdded()) return;
                            getActivity().runOnUiThread(() -> {
                                if (errorMsg != null && getContext() != null) {
                                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            });
        }).start();
    }

    private void updateVoteButtons(ImageButton btnUpvote, ImageButton btnDownvote, VotingManager.VoteType userVoteType) {
        if (btnUpvote == null || btnDownvote == null || getContext() == null) return;

        int defaultColor = Color.parseColor("#757575");
        int highlightColor = ContextCompat.getColor(getContext(), R.color.black);

        if (userVoteType == VotingManager.VoteType.UPVOTE) {
            btnUpvote.setColorFilter(highlightColor);
            btnDownvote.setColorFilter(defaultColor);
        } else if (userVoteType == VotingManager.VoteType.DOWNVOTE) {
            btnUpvote.setColorFilter(defaultColor);
            btnDownvote.setColorFilter(highlightColor);
        } else {
            btnUpvote.setColorFilter(defaultColor);
            btnDownvote.setColorFilter(defaultColor);
        }
    }

    private void setupConfirmations(View view, RoadReport report) {
        if (homeViewModel == null || !isAdded()) return;
        
        String reportId = report.getReportId();
        FirebaseUser user = SessionManager.getInstance().getCurrentUser();
        String userId = user != null ? user.getUid() : "";

        View llConfirmationSection = view.findViewById(R.id.llConfirmationSection);
        com.google.android.material.button.MaterialButton btnConfirmActive = view.findViewById(R.id.btnConfirmActive);
        com.google.android.material.button.MaterialButton btnConfirmGone = view.findViewById(R.id.btnConfirmGone);
        TextView tvConfirmationStatus = view.findViewById(R.id.tvConfirmationStatus);
        TextView tvTrustScore = view.findViewById(R.id.tvTrustScore);

        double userLat = getArguments() != null ? getArguments().getDouble(ARG_USER_LAT, Double.NaN) : Double.NaN;
        double userLon = getArguments() != null ? getArguments().getDouble(ARG_USER_LON, Double.NaN) : Double.NaN;
        
        float distanceMeters = Float.MAX_VALUE;
        if (!Double.isNaN(userLat) && !Double.isNaN(userLon)) {
            float[] results = new float[1];
            Location.distanceBetween(userLat, userLon, report.getLatitude(), report.getLongitude(), results);
            distanceMeters = results[0];
        }

        if (distanceMeters > 500) {
            llConfirmationSection.setVisibility(View.GONE);
        } else {
            llConfirmationSection.setVisibility(View.VISIBLE);
            
            btnConfirmActive.setOnClickListener(v -> submitConfirmation(reportId, userId, ReportConfirmation.Status.ACTIVE, btnConfirmActive, btnConfirmGone));
            btnConfirmGone.setOnClickListener(v -> submitConfirmation(reportId, userId, ReportConfirmation.Status.GONE, btnConfirmActive, btnConfirmGone));
            
            homeViewModel.fetchConfirmationCount(reportId);
            homeViewModel.fetchUserConfirmation(reportId, userId);
            
            homeViewModel.startConfirmationListeners(reportId, userId);

            homeViewModel.getConfirmationCount().removeObservers(getViewLifecycleOwner());
            homeViewModel.getConfirmationCount().observe(getViewLifecycleOwner(), count -> {
                if (!isAdded() || getView() == null) return;
                if (count != null) {
                    currentConfirmCount = count;
                    updateTrustScoreUI(tvTrustScore);
                    
                    if (count.getActiveCount() + count.getGoneCount() > 0) {
                        tvConfirmationStatus.setText("Confirmed by " + (count.getActiveCount() + count.getGoneCount()) + " users");
                    } else {
                        tvConfirmationStatus.setText("No confirmations yet");
                    }
                }
            });
            
            homeViewModel.getUserConfirmation().removeObservers(getViewLifecycleOwner());
            homeViewModel.getUserConfirmation().observe(getViewLifecycleOwner(), conf -> {
                if (!isAdded() || getView() == null) return;
                lastKnownUserConfirmation = conf;
                if (!isConfirming) {
                    updateConfirmationButtons(btnConfirmActive, btnConfirmGone, lastKnownUserConfirmation != null ? lastKnownUserConfirmation.getStatus() : null);
                }
            });
        }
    }
    
    private void submitConfirmation(String reportId, String userId, ReportConfirmation.Status status, com.google.android.material.button.MaterialButton btnActive, com.google.android.material.button.MaterialButton btnGone) {
        if (userId == null || userId.isEmpty() || isConfirming || !isAdded()) return;
        
        isConfirming = true;
        btnActive.setEnabled(false);
        btnGone.setEnabled(false);
        updateConfirmationButtons(btnActive, btnGone, status);
        
        confirmationManager.confirmReport(reportId, userId, status, lastKnownUserConfirmation, (success, error) -> {
            if (getActivity() == null || !isAdded()) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || getView() == null) return;
                isConfirming = false;
                btnActive.setEnabled(true);
                btnGone.setEnabled(true);
                if (!success) {
                    updateConfirmationButtons(btnActive, btnGone, lastKnownUserConfirmation != null ? lastKnownUserConfirmation.getStatus() : null);
                    if (error != null && getContext() != null) {
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }
    
    private String formatExpiryStatus(long timeRemainingMs) {
        if (timeRemainingMs <= 0) {
            return "Expired";
        }
        long minutes = timeRemainingMs / (60 * 1000L);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            long remainingHours = hours % 24;
            if (remainingHours > 0) {
                return "Expires in " + days + (days == 1 ? " day " : " days ") + remainingHours + (remainingHours == 1 ? " hr" : " hrs");
            } else {
                return "Expires in " + days + (days == 1 ? " day" : " days");
            }
        } else if (hours > 0) {
            long remainingMins = minutes % 60;
            if (remainingMins > 0) {
                return "Expires in " + hours + (hours == 1 ? " hr " : " hrs ") + remainingMins + " min";
            } else {
                return "Expires in " + hours + (hours == 1 ? " hr" : " hrs");
            }
        } else {
            return "Expires in " + Math.max(1, minutes) + " min";
        }
    }

    private void updateConfirmationButtons(com.google.android.material.button.MaterialButton btnActive, com.google.android.material.button.MaterialButton btnGone, ReportConfirmation.Status status) {
        if (btnActive == null || btnGone == null || getView() == null || getContext() == null) return;
        Context ctx = getContext();
        int defaultColor = ContextCompat.getColor(ctx, android.R.color.transparent);
        int activeColor = ContextCompat.getColor(ctx, R.color.rg_primary);
        int goneColor = ContextCompat.getColor(ctx, R.color.rg_tertiary);
        int defaultTextColor = com.google.android.material.color.MaterialColors.getColor(
            getView(),
            com.google.android.material.R.attr.colorOnSurface
        );
        int highlightTextColor = ContextCompat.getColor(ctx, android.R.color.white);
        
        if (status == ReportConfirmation.Status.ACTIVE) {
            btnActive.setBackgroundColor(activeColor);
            btnActive.setTextColor(highlightTextColor);
            btnActive.setText("✓ Still Exists");
            btnGone.setBackgroundColor(defaultColor);
            btnGone.setTextColor(defaultTextColor);
            btnGone.setText("👎 Not Found");
        } else if (status == ReportConfirmation.Status.GONE) {
            btnGone.setBackgroundColor(goneColor);
            btnGone.setTextColor(highlightTextColor);
            btnGone.setText("✓ Not Found");
            btnActive.setBackgroundColor(defaultColor);
            btnActive.setTextColor(defaultTextColor);
            btnActive.setText("👍 Still Exists");
        } else {
            btnActive.setBackgroundColor(defaultColor);
            btnActive.setTextColor(defaultTextColor);
            btnActive.setText("👍 Still Exists");
            btnGone.setBackgroundColor(defaultColor);
            btnGone.setTextColor(defaultTextColor);
            btnGone.setText("👎 Not Found");
        }
    }

    private void setupComments(View view, String reportId) {
        if (getContext() == null || !isAdded()) return;

        rvComments = view.findViewById(R.id.rvComments);
        pbCommentsLoading = view.findViewById(R.id.pbCommentsLoading);
        tvCommentsEmptyState = view.findViewById(R.id.tvCommentsEmptyState);
        etCommentInput = view.findViewById(R.id.etCommentInput);
        tvCharCounter = view.findViewById(R.id.tvCharCounter);
        btnSendComment = view.findViewById(R.id.btnSendComment);

        commentAdapter = new CommentAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvComments.setLayoutManager(layoutManager);
        rvComments.setAdapter(commentAdapter);

        // Character Counter & Validation
        if (commentTextWatcher != null) {
            etCommentInput.removeTextChangedListener(commentTextWatcher);
        }
        commentTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s != null ? s.length() : 0;
                if (tvCharCounter != null) tvCharCounter.setText(length + "/200");
                
                String trimmed = s != null ? s.toString().trim() : "";
                if (homeViewModel != null) {
                    HomeViewModel.CommentSubmitState currentState = homeViewModel.getCommentSubmitState().getValue();
                    if (currentState != HomeViewModel.CommentSubmitState.LOADING && btnSendComment != null) {
                        boolean enabled = !trimmed.isEmpty() && trimmed.length() <= 200;
                        btnSendComment.setEnabled(enabled);
                        btnSendComment.setAlpha(enabled ? 1.0f : 0.5f);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etCommentInput.addTextChangedListener(commentTextWatcher);

        // Send Button
        btnSendComment.setOnClickListener(v -> {
            String text = etCommentInput.getText().toString().trim();
            if (text.isEmpty() || text.length() > 200) return;

            if (homeViewModel != null) {
                homeViewModel.postComment(reportId, text);
            }
        });

        // Observers
        homeViewModel.getCommentsLoading().removeObservers(getViewLifecycleOwner());
        homeViewModel.getCommentsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (!isAdded() || getView() == null) return;
            if (isLoading != null && isLoading) {
                pbCommentsLoading.setVisibility(View.VISIBLE);
                rvComments.setVisibility(View.GONE);
                tvCommentsEmptyState.setVisibility(View.GONE);
            } else {
                pbCommentsLoading.setVisibility(View.GONE);
            }
        });

        homeViewModel.getComments().removeObservers(getViewLifecycleOwner());
        homeViewModel.getComments().observe(getViewLifecycleOwner(), comments -> {
            if (!isAdded() || getView() == null) return;
            if (comments != null) {
                int oldSize = commentAdapter.getCommentsCount();
                commentAdapter.setComments(comments);
                
                if (comments.isEmpty()) {
                    tvCommentsEmptyState.setVisibility(View.VISIBLE);
                    rvComments.setVisibility(View.GONE);
                } else {
                    tvCommentsEmptyState.setVisibility(View.GONE);
                    rvComments.setVisibility(View.VISIBLE);
                    
                    // Smart Auto Scroll
                    if (layoutManager != null) {
                        int lastVisible = layoutManager.findLastCompletelyVisibleItemPosition();
                        boolean isAtBottom = (lastVisible == oldSize - 1) || (lastVisible == -1);
                        boolean isFirstLoad = (oldSize == 0);
                        
                        if (isFirstLoad || isAtBottom) {
                            rvComments.scrollToPosition(comments.size() - 1);
                        }
                    }
                }
            }
        });

        homeViewModel.getCommentSubmitState().removeObservers(getViewLifecycleOwner());
        homeViewModel.getCommentSubmitState().observe(getViewLifecycleOwner(), state -> {
            if (!isAdded() || getView() == null || state == null) return;
            
            switch (state) {
                case LOADING:
                    btnSendComment.setEnabled(false);
                    btnSendComment.setAlpha(0.5f);
                    break;
                case SUCCESS:
                    etCommentInput.setText("");
                    hideKeyboard();
                    homeViewModel.resetCommentSubmitState();
                    break;
                case ERROR:
                    String currentText = etCommentInput.getText().toString().trim();
                    boolean enabled = !currentText.isEmpty() && currentText.length() <= 200;
                    btnSendComment.setEnabled(enabled);
                    btnSendComment.setAlpha(enabled ? 1.0f : 0.5f);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to send comment", Toast.LENGTH_SHORT).show();
                    }
                    homeViewModel.resetCommentSubmitState();
                    break;
                case IDLE:
                default:
                    break;
            }
        });

        // Start listening
        homeViewModel.startCommentsListener(reportId);
    }

    private void hideKeyboard() {
        if (getView() != null && getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }
}
