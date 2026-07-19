package com.vlabs.routeguard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.PlaceSuggestion;
import java.util.ArrayList;
import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {

    private final List<PlaceSuggestion> suggestions = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(PlaceSuggestion suggestion);
    }

    public PlacesAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<PlaceSuggestion> newList) {
        suggestions.clear();
        if (newList != null) {
            suggestions.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place_suggestion, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        PlaceSuggestion suggestion = suggestions.get(position);
        
        String fullName = suggestion.getDisplayName();
        if (fullName != null) {
            int firstComma = fullName.indexOf(',');
            if (firstComma != -1) {
                holder.tvPlaceName.setText(fullName.substring(0, firstComma).trim());
                holder.tvPlaceAddress.setText(fullName.substring(firstComma + 1).trim());
                holder.tvPlaceAddress.setVisibility(View.VISIBLE);
            } else {
                holder.tvPlaceName.setText(fullName.trim());
                holder.tvPlaceAddress.setVisibility(View.GONE);
            }
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(suggestion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaceName;
        TextView tvPlaceAddress;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
            tvPlaceAddress = itemView.findViewById(R.id.tvPlaceAddress);
        }
    }
}
