package com.adro.firedrive;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import android.content.Context;
import android.net.Uri;

import com.bumptech.glide.Glide;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private final List<FileModel> fileList;
    private final List<Integer> selectedItems;
    private final OnItemLongClickListener longClickListener;
    private final OnItemClickListener clickListener;
    private final Context context;
    public String allURL;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public FileAdapter(List<FileModel> fileList, List<Integer> selectedItems,
                       OnItemLongClickListener longClickListener, OnItemClickListener clickListener, Context context) {
        this.fileList = fileList;
        this.selectedItems = selectedItems;
        this.longClickListener = longClickListener;
        this.clickListener = clickListener;
        this.context = context;
        this.allURL = "";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileModel file = fileList.get(position);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        holder.tvFileName.setText(file.getFileName());
        if (file.getSize() >= 1024) {
            String details = String.format("%.2f MB  |  %s  |  %s", file.getSize() / 1024, file.getDate(), file.getTime());
            holder.tvFileDetails.setText(details);
        } else {
            String details = String.format("%.2f KB  |  %s  |  %s", file.getSize(), file.getDate(), file.getTime());
            holder.tvFileDetails.setText(details);
        }
        String fileName = file.getFileName().toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".webp")) {
            if (file.getSize() < 512) {
                Uri fileUri = Uri.parse(file.getFileUrl());
                Glide.with(context)
                        .load(fileUri)
                        .placeholder(R.drawable.image)
                        .error(R.drawable.error)
                        .into(holder.thumbnail);
            } else {
                holder.thumbnail.setImageResource(R.drawable.image);
            }
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") ||
                fileName.endsWith(".mov") || fileName.endsWith(".mkv")) {
            holder.thumbnail.setImageResource(R.drawable.video);

        } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
            holder.thumbnail.setImageResource(R.drawable.audio);

        } else if (fileName.endsWith(".zip") || fileName.endsWith(".rar") ||
                fileName.endsWith(".tar") || fileName.endsWith(".7z")) {
            holder.thumbnail.setImageResource(R.drawable.zip);

        } else if (fileName.endsWith(".txt") || fileName.endsWith(".xml") ||
                fileName.endsWith(".json")) {
            holder.thumbnail.setImageResource(R.drawable.text);

        } else if (fileName.endsWith(".pdf")) {
            holder.thumbnail.setImageResource(R.drawable.pdf);

        } else {
            holder.thumbnail.setImageResource(R.drawable.file);
        }

        if (selectedItems.contains(position)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.yellow));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
        }
        holder.itemView.setOnClickListener(v -> {
            if (selectedItems.contains(position)) {
                allURL = allURL.replace("\n" + file.getFileUrl(), "").replace(file.getFileUrl(), "");
            } else {
                allURL += (allURL.isEmpty() ? "" : "\n") + file.getFileUrl();
            }
            ClipData clip = ClipData.newPlainText("File URL", allURL);
            clipboard.setPrimaryClip(clip);
            clickListener.onItemClick(position);
        });

        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onItemLongClick(position);
            return true;
        });
    }

    public void resetAllURLs() {
        allURL = "";
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvFileDetails;
        ImageView thumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileDetails = itemView.findViewById(R.id.tvFileDetails);
            thumbnail = itemView.findViewById(R.id.thumbnail);
        }
    }
}


