package com.vlabs.routeguard.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.adapters.MyReportsAdapter;
import com.vlabs.routeguard.fragments.ReportDetailsBottomSheet;
import com.vlabs.routeguard.managers.CascadeCleanupManager;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.repositories.ReportRepository;
import com.vlabs.routeguard.utils.IntentConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Personal Hazard Reports Management Activity ("My Reports").
 *
 * Displays a list of all hazard reports authored by the currently authenticated user,
 * offering status filtering (All, Active, Expired) and report deletion with cascading subcollection cleanup.
 *
 * Core Workflows & Data Sourcing:
 * 1. Hybrid Fetching: Loads reports created by {@code currentUserId} from Cloud Firestore
 *    ({@link ReportRepository#fetchUserReports(String, ReportRepository.ReportCallback)}) and merges
 *    locally stored drafts/reports from {@link com.vlabs.routeguard.managers.LocalReportManager}.
 * 2. Status Categorization:
 *    - Active: Reports whose expiration timestamp ({@code expiresAt}) is in the future.
 *    - Expired: Reports whose validity period has elapsed.
 * 3. Cascade Deletion:
 *    - When the user deletes a report, invokes {@link CascadeCleanupManager} to purge the parent document
 *      at {@code reports/{reportId}} along with all child subcollections (votes, confirmations, comments).
 * 4. Inspection:
 *    - Tapping an item opens the full details view in {@link ReportDetailsBottomSheet}.
 */
public class MyReportsActivity extends BaseActivity implements MyReportsAdapter.OnReportActionListener {

    private ReportRepository reportRepository;
    private CascadeCleanupManager cascadeCleanupManager;
    private MyReportsAdapter adapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvMyReports;
    private View llEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private MaterialButton btnCreateFirstReport;
    private ProgressBar progressLoading;
    private ChipGroup chipGroupFilters;

    private final List<RoadReport> allReports = new ArrayList<>();
    private String currentFilter = "ALL"; // ALL, ACTIVE, EXPIRED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        reportRepository = new ReportRepository();
        cascadeCleanupManager = new CascadeCleanupManager();

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> finish());
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvMyReports = findViewById(R.id.rvMyReports);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnCreateFirstReport = findViewById(R.id.btnCreateFirstReport);
        progressLoading = findViewById(R.id.progressLoading);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);

        if (btnCreateFirstReport != null) {
            btnCreateFirstReport.setOnClickListener(v -> {
                Intent intent = new Intent(MyReportsActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        setupRecyclerView();
        setupFilters();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.rg_primary);
            swipeRefreshLayout.setOnRefreshListener(this::loadUserReports);
        }

        loadUserReports();
    }

    private void setupRecyclerView() {
        adapter = new MyReportsAdapter(this, this);
        rvMyReports.setLayoutManager(new LinearLayoutManager(this));
        rvMyReports.setAdapter(adapter);
    }

    private void setupFilters() {
        if (chipGroupFilters != null) {
            chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chipFilterActive) {
                    currentFilter = "ACTIVE";
                } else if (checkedId == R.id.chipFilterExpired) {
                    currentFilter = "EXPIRED";
                } else {
                    currentFilter = "ALL";
                }
                applyFilter();
            });
        }
    }

    private void loadUserReports() {
        String currentUid = com.vlabs.routeguard.managers.SessionManager.getInstance().getCurrentUserId();
        if (currentUid == null || currentUid.isEmpty()) {
            Toast.makeText(this, "Please log in to view your reports.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        reportRepository.fetchUserReports(currentUid, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(List<RoadReport> reports) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                    allReports.clear();
                    if (reports != null) {
                        allReports.addAll(reports);
                    }
                    applyFilter();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                    Toast.makeText(MyReportsActivity.this, error, Toast.LENGTH_SHORT).show();
                    applyFilter();
                });
            }
        });
    }

    private void applyFilter() {
        List<RoadReport> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (RoadReport report : allReports) {
            boolean isExpired = report.getExpiresAt() > 0 && (now >= report.getExpiresAt());
            if ("ACTIVE".equals(currentFilter) && !isExpired) {
                filtered.add(report);
            } else if ("EXPIRED".equals(currentFilter) && isExpired) {
                filtered.add(report);
            } else if ("ALL".equals(currentFilter)) {
                filtered.add(report);
            }
        }

        adapter.setReports(filtered);

        if (filtered.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvMyReports.setVisibility(View.GONE);

            if (allReports.isEmpty()) {
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No reports yet");
                if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Help your community by creating your first report.");
                if (btnCreateFirstReport != null) btnCreateFirstReport.setVisibility(View.VISIBLE);
            } else {
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No " + currentFilter.toLowerCase() + " reports");
                if (tvEmptySubtitle != null) tvEmptySubtitle.setText("You have no " + currentFilter.toLowerCase() + " reports in your history.");
                if (btnCreateFirstReport != null) btnCreateFirstReport.setVisibility(View.GONE);
            }
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvMyReports.setVisibility(View.VISIBLE);
        }
    }

    // --- OnReportActionListener Implementation ---

    @Override
    public void onReportClick(RoadReport report) {
        if (isFinishing() || isDestroyed() || report == null || report.getReportId() == null) return;
        ArrayList<String> reportIds = new ArrayList<>();
        reportIds.add(report.getReportId());

        ReportDetailsBottomSheet bottomSheet = ReportDetailsBottomSheet.newInstance(reportIds, Double.NaN, Double.NaN);
        bottomSheet.show(getSupportFragmentManager(), "ReportDetailsBottomSheet");
    }

    @Override
    public void onShareClick(RoadReport report) {
        if (isFinishing() || isDestroyed() || report == null) return;

        String categoryName = report.getType() != null ? report.getType().name() : "Road Report";
        String shareText = "SafeRoute Road Alert: " + categoryName + "\n" +
                (report.getDescription() != null && !report.getDescription().isEmpty() ? report.getDescription() + "\n" : "") +
                "Location: https://maps.google.com/?q=" + report.getLatitude() + "," + report.getLongitude();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Report"));
    }

    @Override
    public void onViewOnMapClick(RoadReport report) {
        if (isFinishing() || isDestroyed() || report == null || report.getReportId() == null) return;

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(IntentConstants.EXTRA_REPORT_ID, report.getReportId());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDeleteClick(RoadReport report) {
        if (isFinishing() || isDestroyed() || report == null || report.getReportId() == null) return;

        // Check ownership
        String currentUid = com.vlabs.routeguard.managers.SessionManager.getInstance().getCurrentUserId();
        if (currentUid.isEmpty() || report.getReporterId() == null || !report.getReporterId().equals(currentUid)) {
            com.vlabs.routeguard.utils.SnackbarUtils.showError(findViewById(android.R.id.content), "You can only delete your own reports.");
            return;
        }

        com.vlabs.routeguard.utils.DialogUtils.showDeleteConfirmation(this, "Delete Report", "Are you sure you want to delete this report? This action cannot be undone.", () -> deleteReportInternal(report));
    }

    private void deleteReportInternal(RoadReport report) {
        if (progressLoading != null) progressLoading.setVisibility(View.VISIBLE);

        // Delete from local storage immediately
        com.vlabs.routeguard.managers.LocalReportManager.getInstance().deleteReport(report.getReportId());

        cascadeCleanupManager.cleanupExpiredReport(report, new CascadeCleanupManager.CleanupCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                    com.vlabs.routeguard.utils.SnackbarUtils.showSuccess(findViewById(android.R.id.content), "Report deleted");

                    allReports.removeIf(r -> r != null && r.getReportId() != null && r.getReportId().equals(report.getReportId()));
                    applyFilter();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                    // Still remove from UI since local copy is deleted
                    allReports.removeIf(r -> r != null && r.getReportId() != null && r.getReportId().equals(report.getReportId()));
                    applyFilter();
                    com.vlabs.routeguard.utils.SnackbarUtils.showSuccess(findViewById(android.R.id.content), "Report deleted");
                });
            }
        });
    }
}
