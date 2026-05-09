package com.example.eldercare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CareNoteAdapter extends RecyclerView.Adapter<CareNoteAdapter.ViewHolder> {

    private List<CareNote> notes;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public CareNoteAdapter(List<CareNote> notes) {
        this.notes = notes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_care_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CareNote note = notes.get(position);
        holder.tvAuthor.setText(note.authorName);
        holder.tvNote.setText(note.note);
        holder.tvTimestamp.setText(dateFormat.format(new Date(note.timestamp)));
    }

    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    public void updateData(List<CareNote> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    public static class CareNote {
        public String note;
        public String authorUid;
        public String authorName;
        public long timestamp;

        public CareNote() {}

        public CareNote(String note, String authorUid, String authorName, long timestamp) {
            this.note = note;
            this.authorUid = authorUid;
            this.authorName = authorName;
            this.timestamp = timestamp;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvTimestamp, tvNote;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvNote = itemView.findViewById(R.id.tvNote);
        }
    }
}
