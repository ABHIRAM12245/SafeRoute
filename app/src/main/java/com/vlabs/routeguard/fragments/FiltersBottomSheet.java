package com.vlabs.routeguard.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.ReportType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FiltersBottomSheet extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFiltersApplied(Set<String> selectedCategories);
    }

    private FilterListener listener;
    private Set<String> initialFilters = new HashSet<>();
    private Map<String, Integer> categoryCounts = new HashMap<>();

    public static FiltersBottomSheet newInstance(Set<String> selectedCategories) {
        return newInstance(selectedCategories, null);
    }

    public static FiltersBottomSheet newInstance(Set<String> selectedCategories, Map<String, Integer> counts) {
        FiltersBottomSheet fragment = new FiltersBottomSheet();
        Bundle args = new Bundle();
        args.putStringArrayList("filters", new ArrayList<>(selectedCategories));
        if (counts != null) {
            args.putSerializable("counts", new HashMap<>(counts));
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup cgCategories = view.findViewById(R.id.cgCategories);
        MaterialButton btnReset = view.findViewById(R.id.btnResetFilters);
        MaterialButton btnApply = view.findViewById(R.id.btnApplyFilters);

        if (getArguments() != null) {
            ArrayList<String> list = getArguments().getStringArrayList("filters");
            if (list != null) {
                initialFilters = new HashSet<>(list);
            }
            java.io.Serializable mapObj = getArguments().getSerializable("counts");
            if (mapObj instanceof Map) {
                //noinspection unchecked
                categoryCounts = (Map<String, Integer>) mapObj;
            }
        }

        // Pre-check chips based on initial filters & apply enhanced purple design system
        for (int i = 0; i < cgCategories.getChildCount(); i++) {
            View child = cgCategories.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                Object tag = chip.getTag();
                if (tag != null) {
                    String categoryKey = tag.toString();
                    boolean isChecked = initialFilters.contains(categoryKey);
                    chip.setChecked(isChecked);
                    updateChipStyle(chip, isChecked, false);

                    chip.setOnCheckedChangeListener((buttonView, checked) -> {
                        updateChipStyle(chip, checked, true);
                    });
                }
            }
        }

        btnReset.setOnClickListener(v -> cgCategories.clearCheck());

        btnApply.setOnClickListener(v -> {
            Set<String> selected = new HashSet<>();
            for (int i = 0; i < cgCategories.getChildCount(); i++) {
                View child = cgCategories.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (chip.isChecked() && chip.getTag() != null) {
                        selected.add(chip.getTag().toString());
                    }
                }
            }
            if (listener != null) {
                listener.onFiltersApplied(selected);
            }
            dismiss();
        });
    }

    private void updateChipStyle(Chip chip, boolean isChecked, boolean animate) {
        Object tag = chip.getTag();
        if (tag == null) return;

        String categoryKey = tag.toString();
        String displayName = getCategoryDisplayName(categoryKey);

        int primaryPurple = Color.parseColor("#7C3AED");
        int unselectedPurple = Color.parseColor("#A78BFA");
        int darkBg = Color.parseColor("#1E1B4B");
        int white = Color.WHITE;

        if (isChecked) {
            chip.setText("✓ " + displayName);
            chip.setTextColor(white);
            chip.setChipBackgroundColor(ColorStateList.valueOf(primaryPurple));
            chip.setChipIconTint(ColorStateList.valueOf(white));
            chip.setChipStrokeColor(ColorStateList.valueOf(primaryPurple));
            chip.setChipStrokeWidth(0f);
            chip.setElevation(8f);
        } else {
            chip.setText(displayName);
            chip.setTextColor(unselectedPurple);
            chip.setChipBackgroundColor(ColorStateList.valueOf(darkBg));
            chip.setChipIconTint(ColorStateList.valueOf(unselectedPurple));
            chip.setChipStrokeColor(ColorStateList.valueOf(primaryPurple));
            chip.setChipStrokeWidth(3f);
            chip.setElevation(0f);
        }

        if (animate) {
            chip.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(75)
                .withEndAction(() -> chip.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(75)
                    .start())
                .start();
        }
    }

    private String getCategoryDisplayName(String key) {
        try {
            ReportType type = ReportType.valueOf(key);
            return type.getDisplayName();
        } catch (Exception e) {
            return key;
        }
    }
}
