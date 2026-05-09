package com.example.eldercare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eldercare.databinding.BottomSheetAddCareNoteBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddCareNoteBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetAddCareNoteBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAddCareNoteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSaveNote.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String text = binding.etNoteText.getText().toString().trim();
        if (text.isEmpty()) {
            binding.etNoteText.setError("Note cannot be empty");
            return;
        }

        String elderUid = UserSessionManager.getInstance().getLinkedElderId();
        String caregiverName = UserSessionManager.getInstance().getFullName();

        if (elderUid == null) return;

        Map<String, Object> note = new HashMap<>();
        note.put("text", text);
        note.put("caregiverName", !caregiverName.isEmpty() ? caregiverName : "Caregiver");
        note.put("timestamp", FieldValue.serverTimestamp());

        binding.btnSaveNote.setEnabled(false);

        FirebaseFirestore.getInstance().collection("users").document(elderUid)
                .collection("careNotes").add(note)
                .addOnSuccessListener(documentReference -> {
                    if (getActivity() != null) {
                        Snackbar.make(requireActivity().findViewById(android.R.id.content), "Care note saved", Snackbar.LENGTH_SHORT).show();
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        binding.btnSaveNote.setEnabled(true);
                        Snackbar.make(binding.getRoot(), "Failed to save note.", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
