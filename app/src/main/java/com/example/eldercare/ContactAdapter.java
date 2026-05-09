package com.example.eldercare;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eldercare.databinding.ItemContactBinding;

import java.util.List;
import java.util.Map;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private final List<Map<String, String>> contacts;

    public ContactAdapter(List<Map<String, String>> contacts) {
        this.contacts = contacts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> contact = contacts.get(position);
        String name = contact.get("name");
        String phone = contact.get("phone");
        String relation = contact.get("relationship");

        holder.binding.tvContactName.setText(name);
        holder.binding.tvContactRelation.setText(relation);

        holder.binding.btnCallContact.setOnClickListener(v -> {
            if (phone != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts != null ? contacts.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemContactBinding binding;
        public ViewHolder(ItemContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
