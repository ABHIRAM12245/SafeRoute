package com.vlabs.routeguard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.ReportType;

public class CategorySelectionBottomSheet extends BottomSheetDialogFragment {

    public interface OnCategorySelectedListener {
        void onCategorySelected(ReportType category);
    }

    private OnCategorySelectedListener listener;

    public static CategorySelectionBottomSheet newInstance() {
        return new CategorySelectionBottomSheet();
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_category_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup cgCategories = view.findViewById(R.id.cgCategories);
        if (cgCategories != null) {
            for (int i = 0; i < cgCategories.getChildCount(); i++) {
                View child = cgCategories.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    chip.setOnClickListener(v -> {
                        Object tag = chip.getTag();
                        if (tag != null) {
                            try {
                                ReportType category = ReportType.valueOf(tag.toString());
                                if (listener != null) {
                                    listener.onCategorySelected(category);
                                }
                            } catch (Exception ignored) {}
                        }
                        dismiss();
                    });
                }
            }
        }
    }
}
