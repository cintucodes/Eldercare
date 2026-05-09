package com.example.eldercare;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ElderDashboardActivity extends AppCompatActivity {

    private MaterialButton btnGenerateCode;
    private RecyclerView rvCaregivers;
    private CaregiverAdapter adapter;
    private List<String> caregiverNames;
    private FirebaseFirestore db;
    private LinkingCodeManager codeManager;
    private String elderId;
    private ListenerRegistration elderListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elder_dashboard);

        db = FirebaseFirestore.getInstance();
        codeManager = new LinkingCodeManager();
        elderId = FirebaseAuth.getInstance().getUid();

        btnGenerateCode = findViewById(R.id.btnGenerateCode);
        rvCaregivers = findViewById(R.id.rvCaregivers);

        caregiverNames = new ArrayList<>();
        adapter = new CaregiverAdapter(caregiverNames);
        rvCaregivers.setLayoutManager(new LinearLayoutManager(this));
        rvCaregivers.setAdapter(adapter);

        btnGenerateCode.setOnClickListener(v -> generateNewLinkingCode());

        listenForLinkedCaregivers();
    }

    private void generateNewLinkingCode() {
        btnGenerateCode.setEnabled(false);
        codeManager.generateCode(elderId, new LinkingCodeManager.Callback<String>() {
            @Override
            public void onSuccess(String code) {
                btnGenerateCode.setEnabled(true);
                showCodeDialog(code);
            }

            @Override
            public void onFailure(Exception e) {
                btnGenerateCode.setEnabled(true);
                Toast.makeText(ElderDashboardActivity.this, "Failed to generate code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCodeDialog(String code) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_linking_code, null);
        TextView tvCode = dialogView.findViewById(R.id.tvCode);
        MaterialButton btnCopy = dialogView.findViewById(R.id.btnCopy);

        tvCode.setText(code);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Linking Code")
                .setMessage("Share this code with your caregiver. It expires in 48 hours.")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Linking Code", code);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void listenForLinkedCaregivers() {
        if (elderId == null) return;

        // Detach existing if any
        if (elderListener != null) {
            elderListener.remove();
            ListenerRegistry.INSTANCE.unregister("elder_dashboard_caregivers_" + elderId);
        }

        elderListener = db.collection("users").document(elderId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    List<String> caregiverIds = (List<String>) snapshot.get("linkedCaregivers");
                    if (caregiverIds != null) {
                        resolveCaregiverNames(caregiverIds);
                    }
                });
        
        // Fix 1: Register in centralized registry
        ListenerRegistry.INSTANCE.register("elder_dashboard_caregivers_" + elderId, elderListener);
    }

    private void resolveCaregiverNames(List<String> caregiverIds) {
        if (caregiverIds.isEmpty()) {
            caregiverNames.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        List<String> names = new ArrayList<>();
        for (String id : caregiverIds) {
            db.collection("users").document(id).get().addOnSuccessListener(doc -> {
                String name = doc.getString("firstName");
                if (name == null) name = doc.getString("name");
                if (name != null) {
                    if (!names.contains(name)) {
                        names.add(name);
                        caregiverNames.clear();
                        caregiverNames.addAll(names);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (elderListener != null) {
            elderListener.remove();
            if (elderId != null) ListenerRegistry.INSTANCE.unregister("elder_dashboard_caregivers_" + elderId);
        }
    }

    private static class CaregiverAdapter extends RecyclerView.Adapter<CaregiverAdapter.ViewHolder> {
        private final List<String> names;

        CaregiverAdapter(List<String> names) { this.names = names; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(names.get(position));
        }

        @Override
        public int getItemCount() { return names.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View view) { super(view); textView = view.findViewById(android.R.id.text1); }
        }
    }
}
