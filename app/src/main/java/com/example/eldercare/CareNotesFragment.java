package com.example.eldercare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.eldercare.databinding.FragmentCareNotesBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class CareNotesFragment extends Fragment {

    private FragmentCareNotesBinding binding;
    private CareNoteAdapter adapter;
    private List<CareNoteAdapter.CareNote> notesList;
    private FirebaseFirestore db;
    private String elderUid;
    private ListenerRegistration notesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCareNotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        elderUid = UserSessionManager.getInstance().getLinkedElderId();

        notesList = new ArrayList<>();
        adapter = new CareNoteAdapter(notesList);
        binding.rvCareNotes.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvCareNotes.setAdapter(adapter);

        binding.fabAddNote.setOnClickListener(v -> {
            AddCareNoteBottomSheet bottomSheet = new AddCareNoteBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "AddCareNote");
        });

        if (elderUid != null) {
            fetchCareNotes();
        }
    }

    private void fetchCareNotes() {
        if (notesListener != null) {
            notesListener.remove();
            ListenerRegistry.INSTANCE.unregister("care_notes_" + elderUid);
        }

        notesListener = db.collection("users").document(elderUid).collection("careNotes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || binding == null || !isAdded()) return;

                    notesList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String text = doc.getString("text");
                        String author = doc.getString("caregiverName");
                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                        long time = ts != null ? ts.toDate().getTime() : System.currentTimeMillis();
                        
                        notesList.add(new CareNoteAdapter.CareNote(text, "", author, time));
                    }
                    adapter.notifyDataSetChanged();
                });
        
        ListenerRegistry.INSTANCE.register("care_notes_" + elderUid, notesListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notesListener != null) {
            notesListener.remove();
            ListenerRegistry.INSTANCE.unregister("care_notes_" + elderUid);
        }
        binding = null;
    }
}
