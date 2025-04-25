package com.example.ecommerceapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.Product;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddProductActivity extends AppCompatActivity {
    private static final String TAG = "AddProductActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private EditText titleEditText, descriptionEditText, categoryEditText, priceEditText;
    private ImageView[] imageViews;
    private Button[] imageButtons;
    private Button submitButton;
    private ProgressBar progressBar;
    private List<Uri> selectedImageUris;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private int currentButtonIndex = -1;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Image picker result received");
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        Uri imageUri = result.getData().getData();
                        Log.d(TAG, "Selected image URI: " + imageUri);
                        if (imageUri != null && currentButtonIndex >= 0 && currentButtonIndex < imageViews.length) {
                            imageViews[currentButtonIndex].setImageURI(imageUri);
                            selectedImageUris.set(currentButtonIndex, imageUri);
                            Log.d(TAG, "Image set successfully for index: " + currentButtonIndex);
                        } else {
                            Log.e(TAG, "Invalid image URI or button index");
                            Toast.makeText(this, "Error selecting image", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling image picker result", e);
                        Toast.makeText(this, "Error selecting image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "Image picker cancelled or failed");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        setContentView(R.layout.activity_add_product);

        try {
            // Initialize Firebase instances
            Log.d(TAG, "Initializing Firebase instances");
            storage = FirebaseStorage.getInstance();
            firestore = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();

            if (auth.getCurrentUser() == null) {
                Log.e(TAG, "User not logged in");
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "User is logged in: " + auth.getCurrentUser().getUid());
            initializeViews();
            setupClickListeners();
            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing activity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");
        try {
            titleEditText = findViewById(R.id.et_title);
            descriptionEditText = findViewById(R.id.et_description);
            categoryEditText = findViewById(R.id.et_category);
            priceEditText = findViewById(R.id.et_price);
            submitButton = findViewById(R.id.btn_submit);
            progressBar = findViewById(R.id.progress_bar);

            if (titleEditText == null || descriptionEditText == null || 
                categoryEditText == null || priceEditText == null || 
                submitButton == null || progressBar == null) {
                throw new IllegalStateException("One or more views not found in layout");
            }

            // Initialize image views and buttons
            imageViews = new ImageView[4];
            imageButtons = new Button[4];
            selectedImageUris = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                selectedImageUris.add(null);
            }

            int[] imageViewIds = {R.id.iv_image1, R.id.iv_image2, R.id.iv_image3, R.id.iv_image4};
            int[] buttonIds = {R.id.btn_image1, R.id.btn_image2, R.id.btn_image3, R.id.btn_image4};

            for (int i = 0; i < 4; i++) {
                imageViews[i] = findViewById(imageViewIds[i]);
                imageButtons[i] = findViewById(buttonIds[i]);
                if (imageViews[i] == null || imageButtons[i] == null) {
                    throw new IllegalStateException("Image view or button not found for index: " + i);
                }
            }
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        try {
            for (int i = 0; i < 4; i++) {
                final int index = i;
                imageButtons[i].setOnClickListener(v -> {
                    Log.d(TAG, "Image button clicked: " + index);
                    currentButtonIndex = index;
                    if (checkAndRequestPermissions()) {
                        openImagePicker();
                    }
                });
            }

            submitButton.setOnClickListener(v -> {
                Log.d(TAG, "Submit button clicked");
                uploadProduct();
            });
            Log.d(TAG, "Click listeners setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
            Toast.makeText(this, "Error setting up buttons: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Requesting READ_MEDIA_IMAGES permission");
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                            PERMISSION_REQUEST_CODE);
                    return false;
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Requesting READ_EXTERNAL_STORAGE permission");
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_CODE);
                    return false;
                }
            }
            Log.d(TAG, "Permissions already granted");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions", e);
            return false;
        }
    }

    private void openImagePicker() {
        Log.d(TAG, "Opening image picker for button: " + currentButtonIndex);
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
            Log.d(TAG, "Image picker launched");
        } catch (Exception e) {
            Log.e(TAG, "Error opening image picker", e);
            Toast.makeText(this, "Error opening image picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProduct() {
        Log.d(TAG, "Starting product upload");
        try {
            if (!validateForm()) {
                Log.d(TAG, "Form validation failed");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            submitButton.setEnabled(false);

            String userId = auth.getCurrentUser().getUid();
            List<String> imageUrls = new ArrayList<>();
            
            // Count how many images were selected
            int totalImages = 0;
            for (Uri uri : selectedImageUris) {
                if (uri != null) totalImages++;
            }
            Log.d(TAG, "Total images to upload: " + totalImages);

            if (totalImages == 0) {
                Log.d(TAG, "No images selected");
                Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                submitButton.setEnabled(true);
                return;
            }

            final int finalTotalImages = totalImages;

            // Upload images first
            for (int i = 0; i < selectedImageUris.size(); i++) {
                Uri imageUri = selectedImageUris.get(i);
                if (imageUri != null) {
                    Log.d(TAG, "Uploading image " + (i + 1) + " of " + totalImages);
                    String imageName = UUID.randomUUID().toString();
                    StorageReference imageRef = storage.getReference()
                            .child("products")
                            .child(userId)
                            .child(imageName);

                    imageRef.putFile(imageUri)
                            .addOnSuccessListener(taskSnapshot -> {
                                Log.d(TAG, "Image upload successful: " + imageName);
                                // Get the download URL
                                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                    Log.d(TAG, "Got download URL: " + uri);
                                    imageUrls.add(uri.toString());
                                    
                                    // If this was the last image, create the product
                                    if (imageUrls.size() == finalTotalImages) {
                                        Log.d(TAG, "All images uploaded, creating product");
                                        createProductInFirestore(imageUrls);
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "Error getting download URL", e);
                                    Toast.makeText(AddProductActivity.this,
                                            "Error getting image URL: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                    submitButton.setEnabled(true);
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error uploading image", e);
                                Toast.makeText(AddProductActivity.this,
                                        "Failed to upload image: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                submitButton.setEnabled(true);
                            });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in uploadProduct", e);
            Toast.makeText(this, "Error uploading product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            submitButton.setEnabled(true);
        }
    }

    private void createProductInFirestore(List<String> imageUrls) {
        Log.d(TAG, "Creating product in Firestore");
        try {
            String title = titleEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();
            String category = categoryEditText.getText().toString().trim();
            double price = Double.parseDouble(priceEditText.getText().toString().trim());
            String userId = auth.getCurrentUser().getUid();

            Log.d(TAG, "Product details - Title: " + title + ", Category: " + category + ", Price: " + price);
            Log.d(TAG, "Number of images: " + imageUrls.size());

            Product product = new Product(title, description, category, price, imageUrls, userId);

            firestore.collection("products")
                    .add(product)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Product added successfully with ID: " + documentReference.getId());
                        Toast.makeText(AddProductActivity.this,
                                "Product added successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding product to Firestore", e);
                        Toast.makeText(AddProductActivity.this,
                                "Failed to add product: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in createProductInFirestore", e);
            Toast.makeText(this, "Error creating product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            submitButton.setEnabled(true);
        }
    }

    private boolean validateForm() {
        Log.d(TAG, "Validating form");
        try {
            boolean valid = true;

            if (TextUtils.isEmpty(titleEditText.getText())) {
                Log.d(TAG, "Title is empty");
                titleEditText.setError("Required");
                valid = false;
            }

            if (TextUtils.isEmpty(descriptionEditText.getText())) {
                Log.d(TAG, "Description is empty");
                descriptionEditText.setError("Required");
                valid = false;
            }

            if (TextUtils.isEmpty(categoryEditText.getText())) {
                Log.d(TAG, "Category is empty");
                categoryEditText.setError("Required");
                valid = false;
            }

            if (TextUtils.isEmpty(priceEditText.getText())) {
                Log.d(TAG, "Price is empty");
                priceEditText.setError("Required");
                valid = false;
            } else {
                try {
                    Double.parseDouble(priceEditText.getText().toString());
                } catch (NumberFormatException e) {
                    Log.d(TAG, "Invalid price format");
                    priceEditText.setError("Invalid price");
                    valid = false;
                }
            }

            Log.d(TAG, "Form validation result: " + valid);
            return valid;
        } catch (Exception e) {
            Log.e(TAG, "Error in validateForm", e);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "Permission result received");
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted");
                openImagePicker();
            } else {
                Log.d(TAG, "Permission denied");
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 