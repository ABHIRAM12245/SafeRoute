package com.vlabs.routeguard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
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

        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCategories.setAdapter(new CategoryAdapter(ReportType.values()));
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

        private final ReportType[] categories;

        public CategoryAdapter(ReportType[] categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            ReportType category = categories[position];
            holder.tvCategoryName.setText(category.getDisplayName());
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategorySelected(category);
                }
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return categories.length;
        }

        class CategoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategoryName;

            public CategoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            }
        }
    }
}
