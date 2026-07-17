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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.managers.VotingManager;
import com.vlabs.routeguard.managers.SessionManager;
import com.google.firebase.auth.FirebaseUser;
import com.vlabs.routeguard.models.VoteCount;
import com.vlabs.routeguard.models.ReportVote;
import com.vlabs.routeguard.models.VoteResult;

public class ReportDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_REPORT_ID = "arg_report_id";
    private static final String ARG_REPORTER_ID = "arg_reporter_id";
    private static final String ARG_TYPE = "arg_type";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_TIMESTAMP = "arg_timestamp";

    private VotingManager votingManager;

    public static ReportDetailsBottomSheet newInstance(String reportId, String reporterId, ReportType type, String description, long timestamp) {
        ReportDetailsBottomSheet fragment = newInstance(type, description, timestamp);
        fragment.getArguments().putString(ARG_REPORT_ID, reportId);
        fragment.getArguments().putString(ARG_REPORTER_ID, reporterId);
        return fragment;
    }

    public static ReportDetailsBottomSheet newInstance(ReportType type, String description, long timestamp) {
        ReportDetailsBottomSheet fragment = new ReportDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type.name());
        args.putString(ARG_DESCRIPTION, description);
        args.putLong(ARG_TIMESTAMP, timestamp);
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

        ImageView ivIcon = view.findViewById(R.id.ivReportIcon);
        TextView tvType = view.findViewById(R.id.tvReportType);
        TextView tvTime = view.findViewById(R.id.tvReportTime);
        TextView tvDesc = view.findViewById(R.id.tvReportDescription);

        if (getArguments() != null) {
            String typeStr = getArguments().getString(ARG_TYPE, ReportType.OTHER.name());
            ReportType type = ReportType.valueOf(typeStr);
            String description = getArguments().getString(ARG_DESCRIPTION, "");
            long timestamp = getArguments().getLong(ARG_TIMESTAMP, System.currentTimeMillis());

            tvDesc.setText(description);
            tvTime.setText(DateUtils.getRelativeTimeSpanString(timestamp));

            switch (type) {
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

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        ImageButton btnUpvote = view.findViewById(R.id.btnUpvote);
        ImageButton btnDownvote = view.findViewById(R.id.btnDownvote);
        String reportId = getArguments() != null ? getArguments().getString(ARG_REPORT_ID, "unknown_report_id") : "unknown_report_id";
        String reporterId = getArguments() != null ? getArguments().getString(ARG_REPORTER_ID, "unknown_reporter_id") : "unknown_reporter_id";

        if (btnUpvote != null) {
            btnUpvote.setOnClickListener(v -> submitVote(reportId, VotingManager.VoteType.UPVOTE));
        }

        if (btnDownvote != null) {
            btnDownvote.setOnClickListener(v -> submitVote(reportId, VotingManager.VoteType.DOWNVOTE));
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
        TextView tvVoteCount = getView().findViewById(R.id.tvVoteCount);

        votingManager.getCurrentUserVote(reportId, user.getUid(), new VotingManager.GetCurrentUserVoteCallback() {
            @Override
            public void onSuccess(ReportVote vote) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> updateVoteButtons(btnUpvote, btnDownvote, vote != null ? vote.getVoteType() : null));
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> updateVoteButtons(btnUpvote, btnDownvote, null));
            }
        });

        votingManager.getVoteCount(reportId, new VotingManager.GetVoteCountCallback() {
            @Override
            public void onSuccess(VoteCount voteCount) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (tvVoteCount != null) {
                        tvVoteCount.setText(String.valueOf(voteCount.getUpvoteCount()));
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                // Leave the default value "0"
            }
        });
    }

    private void submitVote(String reportId, VotingManager.VoteType voteType) {
        FirebaseUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return; // Handle missing user safely without crashing
        }

        VoteResult result = votingManager.vote(reportId, voteType, user.getUid());
        if (result != null && result.isSuccess()) {
            fetchVoteData();
        }
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
}
