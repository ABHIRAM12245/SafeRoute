package com.vlabs.routeguard.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> comments = new ArrayList<>();

    public void setComments(List<Comment> newComments) {
        comments.clear();
        if (newComments != null) {
            comments.addAll(newComments);
        }
        notifyDataSetChanged();
    }
    
    public int getCommentsCount() {
        return comments.size();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;
        TextView tvTime;
        TextView tvText;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvText = itemView.findViewById(R.id.tvText);
        }

        public void bind(Comment comment) {
            tvName.setText(comment.getDisplayName() != null ? comment.getDisplayName() : "Anonymous");
            tvText.setText(comment.getText());
            
            if (comment.getTimestamp() != null) {
                long timeMs = comment.getTimestamp().toDate().getTime();
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(timeMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                tvTime.setText(relativeTime);
            } else {
                tvTime.setText("Just now");
            }
            
            // TODO: Implement Glide to load user avatar from comment.getPhotoUrl()
            // Set a default placeholder for now
            ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }
}
