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

public class ReportDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_REPORT_ID = "arg_report_id";
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
    
    // Comments UI
    private CommentAdapter commentAdapter;
    private RecyclerView rvComments;
    private ProgressBar pbCommentsLoading;
    private TextView tvCommentsEmptyState;
    private EditText etCommentInput;
    private TextView tvCharCounter;
    private ImageButton btnSendComment;

    public static ReportDetailsBottomSheet newInstance(String reportId, double userLat, double userLon) {
        ReportDetailsBottomSheet fragment = new ReportDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_REPORT_ID, reportId);
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

        String reportId = getArguments() != null ? getArguments().getString(ARG_REPORT_ID) : null;
        if (reportId != null) {
            reportRepository.fetchReportById(reportId, new ReportRepository.SingleReportCallback() {
                @Override
                public void onSuccess(RoadReport report) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        reportTimestampMs = report.getTimestamp();
                        updateUI(view, report);
                        setupVoting(view, report);
                        setupConfirmations(view, report);
                        setupComments(view, reportId);
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> dismiss());
                }
            });
        } else {
            dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
        }
    }

    private void updateUI(View view, RoadReport report) {
        ImageView ivIcon = view.findViewById(R.id.ivReportIcon);
        TextView tvType = view.findViewById(R.id.tvReportType);
        TextView tvTime = view.findViewById(R.id.tvReportTime);
        TextView tvDesc = view.findViewById(R.id.tvReportDescription);
        TextView tvReporterName = view.findViewById(R.id.tvReporterName);
        TextView tvDistance = view.findViewById(R.id.tvDistance);
        TextView tvExpiryStatus = view.findViewById(R.id.tvExpiryStatus);

        tvDesc.setText(report.getDescription());
        tvTime.setText(DateUtils.getRelativeTimeSpanString(report.getTimestamp()));

        // Expiry Status
        long timeRemaining = report.getExpiresAt() - System.currentTimeMillis();
        if (timeRemaining <= 0) {
            tvExpiryStatus.setText("Expired");
            tvExpiryStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        } else if (timeRemaining < 5 * 60 * 1000) {
            tvExpiryStatus.setText("Expiring Soon");
            tvExpiryStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
        } else {
            long mins = timeRemaining / (60 * 1000);
            tvExpiryStatus.setText("Expires in " + mins + " min");
            tvExpiryStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
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
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
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
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> tvReporterName.setText("Anonymous"));
                    }
                });
            } else {
                tvReporterName.setText("Anonymous");
            }
        }

        switch (report.getType()) {
            case POLICE_CHECKING:
                tvType.setText(getString(R.string.police_checking));
                ivIcon.setImageResource(R.drawable.ic_police);
                ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
                break;
            case ACCIDENT:
                tvType.setText(getString(R.string.accident));
                ivIcon.setImageResource(R.drawable.ic_accident);
                ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                break;
            case HEAVY_TRAFFIC:
                tvType.setText(getString(R.string.heavy_traffic));
                ivIcon.setImageResource(R.drawable.ic_traffic);
                ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
                break;
            case ROAD_CONSTRUCTION:
                tvType.setText(getString(R.string.road_construction));
                ivIcon.setImageResource(R.drawable.ic_construction);
                ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light));
                break;
            case ANIMAL_ON_ROAD:
                tvType.setText(getString(R.string.animal_on_road));
                ivIcon.setImageResource(R.drawable.ic_animal);
                ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                break;
            case WATER_LOGGING:
                tvType.setText(getString(R.string.water_logging));
                ivIcon.setImageResource(R.drawable.ic_water_logging);
                ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light));
                break;
            case OTHER:
            default:
                tvType.setText(getString(R.string.other));
                ivIcon.setImageResource(R.drawable.ic_other);
                ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                break;
        }
    }

    private void setupVoting(View view, RoadReport report) {
        ImageButton btnUpvote = view.findViewById(R.id.btnUpvote);
        ImageButton btnDownvote = view.findViewById(R.id.btnDownvote);
        String reportId = report.getReportId();

        String currentUserId = SessionManager.getInstance().getCurrentUser() != null ? SessionManager.getInstance().getCurrentUser().getUid() : "";
        boolean isOwnReport = report.getReporterId() != null && report.getReporterId().equals(currentUserId);

        if (btnUpvote != null) {
            if (isOwnReport) {
                btnUpvote.setEnabled(false);
                btnUpvote.setAlpha(0.5f);
            } else {
                btnUpvote.setOnClickListener(v -> submitVote(reportId, VotingManager.VoteType.UPVOTE, btnUpvote, btnDownvote));
            }
        }

        if (btnDownvote != null) {
            if (isOwnReport) {
                btnDownvote.setEnabled(false);
                btnDownvote.setAlpha(0.5f);
            } else {
                btnDownvote.setOnClickListener(v -> submitVote(reportId, VotingManager.VoteType.DOWNVOTE, btnUpvote, btnDownvote));
            }
        }

        fetchVoteData();
    }

    private void fetchVoteData() {
        if (getView() == null || getArguments() == null) return;
        
        String reportId = getArguments().getString(ARG_REPORT_ID, "unknown_report_id");
        if (reportId == null || reportId.equals("unknown_report_id")) return;

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
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    lastKnownUserVoteType = vote != null ? vote.getVoteType() : null;
                    if (!isVoting) {
                        updateVoteButtons(btnUpvote, btnDownvote, lastKnownUserVoteType);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
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
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
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

    private void submitVote(String reportId, VotingManager.VoteType voteType, ImageButton btnUpvote, ImageButton btnDownvote) {
        FirebaseUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null || isVoting) {
            return;
        }

        // Optimistic UI
        isVoting = true;
        btnUpvote.setEnabled(false);
        btnDownvote.setEnabled(false);
        updateVoteButtons(btnUpvote, btnDownvote, voteType);

        new Thread(() -> {
            VoteResult result = votingManager.vote(reportId, voteType, user.getUid());
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                isVoting = false;
                btnUpvote.setEnabled(true);
                btnDownvote.setEnabled(true);
                
                if (result == null || !result.isSuccess()) {
                    // Revert to last known good state
                    updateVoteButtons(btnUpvote, btnDownvote, lastKnownUserVoteType);
                    android.widget.Toast.makeText(requireContext(), "Failed to vote. Check your connection.", android.widget.Toast.LENGTH_SHORT).show();
                }
                // If successful, the Firestore realtime listener will handle the permanent state update
            });
        }).start();
    }

    private void updateVoteButtons(ImageButton btnUpvote, ImageButton btnDownvote, VotingManager.VoteType userVoteType) {
        if (btnUpvote == null || btnDownvote == null) return;

        int defaultColor = Color.parseColor("#757575");
        int highlightColor = ContextCompat.getColor(requireContext(), R.color.black);

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
        if (homeViewModel == null) return;
        
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
            
            homeViewModel.getConfirmationCount().observe(this, count -> {
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
            
            homeViewModel.getUserConfirmation().observe(this, conf -> {
                lastKnownUserConfirmation = conf;
                if (!isConfirming) {
                    updateConfirmationButtons(btnConfirmActive, btnConfirmGone, lastKnownUserConfirmation != null ? lastKnownUserConfirmation.getStatus() : null);
                }
            });
        }
    }
    
    private void submitConfirmation(String reportId, String userId, ReportConfirmation.Status status, com.google.android.material.button.MaterialButton btnActive, com.google.android.material.button.MaterialButton btnGone) {
        if (userId == null || userId.isEmpty() || isConfirming) return;
        
        isConfirming = true;
        btnActive.setEnabled(false);
        btnGone.setEnabled(false);
        updateConfirmationButtons(btnActive, btnGone, status);
        
        confirmationManager.confirmReport(reportId, userId, status, lastKnownUserConfirmation, (success, error) -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                isConfirming = false;
                btnActive.setEnabled(true);
                btnGone.setEnabled(true);
                if (!success) {
                    updateConfirmationButtons(btnActive, btnGone, lastKnownUserConfirmation != null ? lastKnownUserConfirmation.getStatus() : null);
                    if (error != null) {
                        android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }
    
    private void updateConfirmationButtons(com.google.android.material.button.MaterialButton btnActive, com.google.android.material.button.MaterialButton btnGone, ReportConfirmation.Status status) {
        if (btnActive == null || btnGone == null) return;
        int defaultColor = ContextCompat.getColor(requireContext(), android.R.color.transparent);
        int highlightColor = ContextCompat.getColor(requireContext(), R.color.black);
        int defaultTextColor = ContextCompat.getColor(requireContext(), R.color.black);
        int highlightTextColor = ContextCompat.getColor(requireContext(), android.R.color.white);
        
        if (status == ReportConfirmation.Status.ACTIVE) {
            btnActive.setBackgroundColor(highlightColor);
            btnActive.setTextColor(highlightTextColor);
            btnGone.setBackgroundColor(defaultColor);
            btnGone.setTextColor(defaultTextColor);
        } else if (status == ReportConfirmation.Status.GONE) {
            btnGone.setBackgroundColor(highlightColor);
            btnGone.setTextColor(highlightTextColor);
            btnActive.setBackgroundColor(defaultColor);
            btnActive.setTextColor(defaultTextColor);
        } else {
            btnActive.setBackgroundColor(defaultColor);
            btnActive.setTextColor(defaultTextColor);
            btnGone.setBackgroundColor(defaultColor);
            btnGone.setTextColor(defaultTextColor);
        }
    }

    private void setupComments(View view, String reportId) {
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
        etCommentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s != null ? s.length() : 0;
                tvCharCounter.setText(length + "/200");
                
                String trimmed = s != null ? s.toString().trim() : "";
                HomeViewModel.CommentSubmitState currentState = homeViewModel.getCommentSubmitState().getValue();
                if (currentState != HomeViewModel.CommentSubmitState.LOADING) {
                    boolean enabled = !trimmed.isEmpty() && trimmed.length() <= 200;
                    btnSendComment.setEnabled(enabled);
                    btnSendComment.setAlpha(enabled ? 1.0f : 0.5f);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Send Button
        btnSendComment.setOnClickListener(v -> {
            String text = etCommentInput.getText().toString().trim();
            if (text.isEmpty() || text.length() > 200) return;

            homeViewModel.postComment(reportId, text);
        });

        // Observers
        homeViewModel.getCommentsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                pbCommentsLoading.setVisibility(View.VISIBLE);
                rvComments.setVisibility(View.GONE);
                tvCommentsEmptyState.setVisibility(View.GONE);
            } else {
                pbCommentsLoading.setVisibility(View.GONE);
            }
        });

        homeViewModel.getComments().observe(getViewLifecycleOwner(), comments -> {
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

        homeViewModel.getCommentSubmitState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            
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
                    Toast.makeText(requireContext(), "Failed to send comment", Toast.LENGTH_SHORT).show();
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
