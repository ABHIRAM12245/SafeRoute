package com.vlabs.routeguard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.vlabs.routeguard.R;

public class FiltersBottomSheet extends BottomSheetDialogFragment {

    public static FiltersBottomSheet newInstance() {
        return new FiltersBottomSheet();
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

        btnReset.setOnClickListener(v -> {
            cgCategories.clearCheck();
        });

        btnApply.setOnClickListener(v -> {
            dismiss();
        });
    }
}
