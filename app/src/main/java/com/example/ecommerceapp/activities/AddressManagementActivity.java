package com.example.ecommerceapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.adapters.AddressAdapter;
import com.example.ecommerceapp.models.Address;
import com.example.ecommerceapp.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;

public class AddressManagementActivity extends AppCompatActivity implements AddressAdapter.AddressClickListener {
    private static final String TAG = "AddressManagement";
    
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddAddress;
    private ProgressBar progressBar;
    
    private AddressAdapter adapter;
    private List<Address> addresses;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private User userProfile;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_management);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Manage Addresses");
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        if (currentUser == null) {
            finish();
            return;
        }
        
        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewAddresses);
        fabAddAddress = findViewById(R.id.fabAddAddress);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize address list
        addresses = new ArrayList<>();
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AddressAdapter(this, addresses, this);
        recyclerView.setAdapter(adapter);
        
        // Set up FAB click listener
        fabAddAddress.setOnClickListener(v -> showAddAddressDialog());
        
        // Load addresses
        loadAddresses();
    }
    
    private void loadAddresses() {
        progressBar.setVisibility(View.VISIBLE);
        
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (documentSnapshot.exists()) {
                        userProfile = documentSnapshot.toObject(User.class);
                        userProfile.setId(documentSnapshot.getId());
                        
                        // Get addresses
                        if (userProfile.getAddresses() != null) {
                            addresses.clear();
                            addresses.addAll(userProfile.getAddresses());
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        userProfile = new User();
                        userProfile.setId(currentUser.getUid());
                    }
                    
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading addresses", e);
                    Toast.makeText(this, "Error loading addresses", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void updateEmptyState() {
        if (addresses.isEmpty()) {
            findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            findViewById(R.id.emptyView).setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showAddAddressDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_address, null);
        
        TextInputEditText etAddressName = dialogView.findViewById(R.id.etAddressName);
        TextInputEditText etRecipientName = dialogView.findViewById(R.id.etRecipientName);
        TextInputEditText etStreet = dialogView.findViewById(R.id.etStreet);
        TextInputEditText etCity = dialogView.findViewById(R.id.etCity);
        TextInputEditText etState = dialogView.findViewById(R.id.etState);
        TextInputEditText etZipCode = dialogView.findViewById(R.id.etZipCode);
        TextInputEditText etCountry = dialogView.findViewById(R.id.etCountry);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etPhone);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add New Address")
                .setView(dialogView)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();
        
        dialog.show();
        
        // Override the positive button click to prevent dialog from dismissing on validation error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validate fields
            String addressName = etAddressName.getText().toString().trim();
            String recipientName = etRecipientName.getText().toString().trim();
            String street = etStreet.getText().toString().trim();
            String city = etCity.getText().toString().trim();
            String state = etState.getText().toString().trim();
            String zipCode = etZipCode.getText().toString().trim();
            String country = etCountry.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            
            if (addressName.isEmpty()) {
                etAddressName.setError("Required");
                return;
            }
            
            if (recipientName.isEmpty()) {
                etRecipientName.setError("Required");
                return;
            }
            
            if (street.isEmpty()) {
                etStreet.setError("Required");
                return;
            }
            
            if (city.isEmpty()) {
                etCity.setError("Required");
                return;
            }
            
            if (state.isEmpty()) {
                etState.setError("Required");
                return;
            }
            
            if (zipCode.isEmpty()) {
                etZipCode.setError("Required");
                return;
            }
            
            if (country.isEmpty()) {
                etCountry.setError("Required");
                return;
            }
            
            if (phone.isEmpty()) {
                etPhone.setError("Required");
                return;
            }
            
            // Create and add address
            Address address = new Address(
                    addressName, recipientName, street, city, state, zipCode, country, phone);
            
            // Set as default if this is the first address
            if (addresses.isEmpty()) {
                address.setDefault(true);
            }
            
            addAddress(address);
            dialog.dismiss();
        });
    }
    
    private void addAddress(Address address) {
        if (userProfile == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        userProfile.addAddress(address);
        
        // Update in Firestore
        firestore.collection("users")
                .document(currentUser.getUid())
                .set(userProfile, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    addresses.add(address);
                    adapter.notifyItemInserted(addresses.size() - 1);
                    updateEmptyState();
                    Toast.makeText(this, "Address added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error adding address", e);
                    Toast.makeText(this, "Error adding address", Toast.LENGTH_SHORT).show();
                });
    }
    
    @Override
    public void onAddressEdit(int position) {
        // For simplicity, just show a toast for now
        Toast.makeText(this, "Edit address feature coming soon", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onAddressDelete(int position) {
        if (position >= 0 && position < addresses.size()) {
            Address addressToDelete = addresses.get(position);
            
            new AlertDialog.Builder(this)
                    .setTitle("Delete Address")
                    .setMessage("Are you sure you want to delete this address?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteAddress(addressToDelete, position);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
    
    @Override
    public void onSetDefault(int position) {
        if (position >= 0 && position < addresses.size()) {
            setDefaultAddress(position);
        }
    }
    
    private void setDefaultAddress(int position) {
        if (userProfile == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Update default status for all addresses
        for (int i = 0; i < addresses.size(); i++) {
            addresses.get(i).setDefault(i == position);
        }
        
        userProfile.setAddresses(addresses);
        
        // Update in Firestore
        firestore.collection("users")
                .document(currentUser.getUid())
                .set(userProfile, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Default address updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error updating default address", e);
                    Toast.makeText(this, "Error updating default address", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void deleteAddress(Address address, int position) {
        if (userProfile == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        userProfile.removeAddress(address);
        
        // Update in Firestore
        firestore.collection("users")
                .document(currentUser.getUid())
                .set(userProfile, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    addresses.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateEmptyState();
                    Toast.makeText(this, "Address deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error deleting address", e);
                    Toast.makeText(this, "Error deleting address", Toast.LENGTH_SHORT).show();
                });
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 