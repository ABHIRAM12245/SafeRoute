package com.vlabs.routeguard.fragments;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FiltersBottomSheet extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFiltersApplied(Set<String> selectedCategories);
    }

    private FilterListener listener;
    private Set<String> initialFilters = new HashSet<>();

    public static FiltersBottomSheet newInstance(Set<String> selectedCategories) {
        FiltersBottomSheet fragment = new FiltersBottomSheet();
        Bundle args = new Bundle();
        args.putStringArrayList("filters", new ArrayList<>(selectedCategories));
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
        }

        // Pre-check chips based on initial filters
        for (int i = 0; i < cgCategories.getChildCount(); i++) {
            View child = cgCategories.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                Object tag = chip.getTag();
                if (tag != null && initialFilters.contains(tag.toString())) {
                    chip.setChecked(true);
                }
            }
        }

        btnReset.setOnClickListener(v -> {
            cgCategories.clearCheck();
        });

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
}
