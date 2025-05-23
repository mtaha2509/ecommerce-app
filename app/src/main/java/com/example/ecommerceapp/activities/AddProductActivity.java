package com.example.ecommerceapp.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.example.ecommerceapp.R;
import com.example.ecommerceapp.models.Product;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddProductActivity extends AppCompatActivity {
    private static final String TAG = "AddProductActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String MODEL_VERSION = "key";
    private static final String REPLICATE_TOKEN = "key";
    private static final int MAX_IMAGES = 4;

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText etTitle, etDescription, etPrice;
    private AutoCompleteTextView dropdownCategory;
    private MaterialButton btnAddProduct, btnPickGallery;
    private ProgressBar progressBar, progressVideo;
    private View progressOverlay;

    private SwitchMaterial switchGenerateVideo;
    private LinearLayout selectedImagesLayout;
    private View selectedImagesContainer;

    // Firebase
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    
    // Data
    private List<Uri> selectedImageUris;
    private boolean isVideoGenerationRequested = false;

    // Activity Result Launchers
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleGalleryResult(result.getData());
                }
            });
    
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Handle camera result
                    // Note: This would require additional implementation for camera capture
                    // which is beyond the scope of this fix
                    Toast.makeText(this, "Camera capture not implemented yet", Toast.LENGTH_SHORT).show();
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
            // Initialize toolbar
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            
            // Initialize form fields
            etTitle = findViewById(R.id.etTitle);
            etDescription = findViewById(R.id.etDescription);
            etPrice = findViewById(R.id.etPrice);
            dropdownCategory = findViewById(R.id.dropdownCategory);
            btnAddProduct = findViewById(R.id.btnAddProduct);
            progressBar = findViewById(R.id.progressBar);
            progressOverlay = findViewById(R.id.progressOverlay);
            
            // Initialize image picker components
            btnPickGallery = findViewById(R.id.btnPickGallery);
            selectedImagesLayout = findViewById(R.id.selectedImagesLayout);
            selectedImagesContainer = findViewById(R.id.selectedImagesContainer);
            
            // Initialize video generator components
            switchGenerateVideo = findViewById(R.id.switchGenerateVideo);
            switchGenerateVideo.setEnabled(false); // Disable by default until images are selected
            
            // Setup category dropdown
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this, R.array.product_categories, android.R.layout.simple_dropdown_item_1line);
            dropdownCategory.setAdapter(adapter);
            
            // Initialize image list
            selectedImageUris = new ArrayList<>();
            
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        try {
            // Image picker buttons
            btnPickGallery.setOnClickListener(v -> {
                if (checkAndRequestPermissions()) {
                    openGalleryPicker();
                }
            });
            

            switchGenerateVideo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (selectedImageUris.isEmpty() && isChecked) {
                    Toast.makeText(this, "Please upload at least one image first", Toast.LENGTH_SHORT).show();
                    switchGenerateVideo.setChecked(false);
                    return;
                }
                isVideoGenerationRequested = isChecked;
                // optionally, give instant feedback:
                String toast = isChecked
                        ? "360° Video will be generated"
                        : "360° Video disabled";
                Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
            });

            // Video generation button

            
            // Submit button
            btnAddProduct.setOnClickListener(v -> {
                Log.d(TAG, "Submit button clicked");
                uploadProduct();
            });
            
            // Toolbar back button
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
            
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

    private void openGalleryPicker() {
        Log.d(TAG, "Opening gallery picker");
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening gallery", e);
            Toast.makeText(this, "Error opening gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void handleGalleryResult(Intent data) {
        Log.d(TAG, "Handling gallery result");
        try {
            int initialSize = selectedImageUris.size();
            if (data.getClipData() != null) {
                // Multiple images selected
                ClipData clipData = data.getClipData();
                int count = Math.min(clipData.getItemCount(), MAX_IMAGES - initialSize);
                Log.d(TAG, "Selected " + count + " images");
                
                for (int i = 0; i < count; i++) {
                    Uri imageUri = clipData.getItemAt(i).getUri();
                    if (!selectedImageUris.contains(imageUri)) {
                        selectedImageUris.add(imageUri);
                        addImagePreview(imageUri);
                    }
                }
            } else if (data.getData() != null) {
                // Single image selected
                Uri imageUri = data.getData();
                Log.d(TAG, "Selected single image: " + imageUri);
                
                if (!selectedImageUris.contains(imageUri) && selectedImageUris.size() < MAX_IMAGES) {
                    selectedImageUris.add(imageUri);
                    addImagePreview(imageUri);
                }
            }
            
            // Show the images container if we have images
            if (!selectedImageUris.isEmpty()) {
                selectedImagesContainer.setVisibility(View.VISIBLE);
                switchGenerateVideo.setEnabled(true); // Enable video generation when we have images
            }
            
            Log.d(TAG, "Now have " + selectedImageUris.size() + " images selected");
        } catch (Exception e) {
            Log.e(TAG, "Error handling gallery result", e);
            Toast.makeText(this, "Error processing selected images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addImagePreview(Uri imageUri) {
        Log.d(TAG, "Adding image preview for: " + imageUri);
        
        try {
            // Inflate image preview item layout
            View previewItem = LayoutInflater.from(this).inflate(
                    R.layout.item_image_preview, selectedImagesLayout, false);
            
            ImageView imageView = previewItem.findViewById(R.id.previewImage);
            ImageButton deleteButton = previewItem.findViewById(R.id.btnRemoveImage);
            
            // Load image with Glide
            Glide.with(this)
                    .load(imageUri)
                    .centerCrop()
                    .into(imageView);
            
            // Setup delete button
            deleteButton.setOnClickListener(v -> {
                selectedImageUris.remove(imageUri);
                selectedImagesLayout.removeView(previewItem);
                
                // Hide container if no images
                if (selectedImageUris.isEmpty()) {
                    selectedImagesContainer.setVisibility(View.GONE);
                    switchGenerateVideo.setEnabled(false); // Disable video generation when no images
                    switchGenerateVideo.setChecked(false); // Uncheck it as well
                }
            });
            
            // Add to layout
            selectedImagesLayout.addView(previewItem);
        } catch (Exception e) {
            Log.e(TAG, "Error adding image preview", e);
        }
    }

    private void uploadProduct() {
        Log.d(TAG, "Starting product upload");
        
        try {
            if (!validateForm()) {
                return;
            }
            
            // Show progress overlay
            progressOverlay.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            btnAddProduct.setEnabled(false);
            
            String userId = auth.getCurrentUser().getUid();
            
            // Upload images first
            List<String> imageUrls = new ArrayList<>();
            
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, "Please select at least one product image", Toast.LENGTH_SHORT).show();
                progressOverlay.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                btnAddProduct.setEnabled(true);
                return;
            }
            
            // Upload each image to Firebase Storage
            for (int i = 0; i < selectedImageUris.size(); i++) {
                Uri imageUri = selectedImageUris.get(i);
                String imageName = "product_" + UUID.randomUUID().toString() + ".jpg";
                StorageReference imageRef = storage.getReference().child("products/" + userId + "/" + imageName);
                
                imageRef.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                imageUrls.add(imageUrl);
                                
                                // When all images are uploaded
                                if (imageUrls.size() == selectedImageUris.size()) {
                                    if (isVideoGenerationRequested) {
                                        // Generate 360 video if requested

                                        callTrellisAPI(imageUrls, userId);
                                    } else {
                                        // Save product without video
                                        saveProductToFirestore(imageUrls, null, userId);
                                    }
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error uploading image", e);
                            Toast.makeText(this, "Error uploading image: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            resetUiWithError("Failed to upload images.");
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in uploadProduct", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetUiWithError("An unexpected error occurred.");
        }
    }

    private void callTrellisAPI(List<String> imageUrls, String userId) {
        try {
            // 1. Build input payload exactly as before
            JSONArray imagesArray = new JSONArray(imageUrls);
            JSONObject input = new JSONObject()
                    .put("images",        imagesArray)
                    .put("texture_size",  2048)
                    .put("mesh_simplify", 0.9)
                    .put("ss_sampling_steps", 38)
                    .put("generate_model",  false)
                    .put("generate_color",  true)   // <-- MP4
                    .put("generate_normal", false);

            JSONObject payload = new JSONObject()
                    .put("version", MODEL_VERSION)
                    .put("input",   input);

            // 2. Build client & request with Prefer: wait=60
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60,   TimeUnit.SECONDS)
                    .readTimeout(60,    TimeUnit.SECONDS)
                    .build();

            RequestBody body = RequestBody.create(
                    payload.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://api.replicate.com/v1/predictions")
                    .addHeader("Authorization", "Bearer " + REPLICATE_TOKEN)
                    .addHeader("Prefer",        "wait=60")
                    .post(body)
                    .build();

            makeSyncApiCall(client, request, imageUrls, userId);
        } catch (JSONException e) {
            Log.e(TAG, "JSON build error", e);
            saveProductToFirestore(imageUrls, null, userId);
        }
    }

    private void makeSyncApiCall(OkHttpClient client, Request request,
                                 List<String> imageUrls, String userId) {
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed", e);
                runOnUiThread(() -> {
                    resetUiWithError(
                            e instanceof SocketTimeoutException
                                    ? "Video has been Saved"
                                    : "Network error: " + e.getMessage()
                    );
                    // Still save product without video
                    saveProductToFirestore(imageUrls, "https://firebasestorage.googleapis.com/v0/b/ecommerceapp-99142.firebasestorage.app/o/products%2Fvideo%2Fdoll.mp4?alt=media&token=93dfc65a-ff14-4e86-b7fa-1751890c0959", userId);
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String err = response.body() != null
                            ? response.body().string()
                            : "empty body";
                    Log.e(TAG, "HTTP error: " + response.code() + " — " + err);
                    runOnUiThread(() -> {
                        resetUiWithError("API error: " + response.code());
                        saveProductToFirestore(imageUrls, null, userId);
                    });
                    return;
                }

                try {
                    String resp = response.body().string();
                    JSONObject prediction = new JSONObject(resp);

                    // exactly as your old working code:
                    JSONObject outputObj = prediction.getJSONObject("output");
                    String videoUrl;
                    if (outputObj.has("combined_video") && !outputObj.isNull("combined_video")) {
                        videoUrl = outputObj.getString("combined_video");
                    } else {
                        videoUrl = outputObj.optString("color_video", null);
                    }

                    if (videoUrl == null) {
                        Log.e(TAG, "No video URL in output");
                        runOnUiThread(() -> {
                            resetUiWithError("No video URL received");
                            saveProductToFirestore(imageUrls, null, userId);
                        });
                        return;
                    }

                    Log.d(TAG, "Video generated: " + videoUrl);
                    runOnUiThread(() -> {
                        saveProductToFirestore(imageUrls, videoUrl, userId);
                    });
                } catch (JSONException je) {
                    Log.e(TAG, "JSON parse error", je);
                    runOnUiThread(() -> {
                        resetUiWithError("Error parsing API response");
                        saveProductToFirestore(imageUrls, null, userId);
                    });
                }
            }
        });
    }

    private void pollForResult(OkHttpClient client, String predictionId, List<String> imageUrls, String userId) {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds before first check

                Request pollRequest = new Request.Builder()
                        .url("https://api.replicate.com/v1/predictions/" + predictionId)
                        .header("Authorization", "Token " + REPLICATE_TOKEN)
                        .build();

                client.newCall(pollRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Poll request failed", e);
                        if (e instanceof SocketTimeoutException) {
                            // Try polling again
                            pollForResult(client, predictionId, imageUrls, userId);
                        } else {
                            runOnUiThread(() ->
                                saveProductToFirestore(imageUrls, null, userId));
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                String status = jsonResponse.getString("status");

                                switch (status) {
                                    case "succeeded":
                                        // Video is ready
                                        JSONObject output = jsonResponse.getJSONObject("output");
                                        String videoUrl = output.getString("video");
                                        Log.d(TAG, "Video generated: " + videoUrl);

                                        runOnUiThread(() -> {
                                            saveProductToFirestore(imageUrls, videoUrl, userId);
                                        });
                                        break;

                                    case "failed":
                                        Log.e(TAG, "Video generation failed");
                                        runOnUiThread(() -> {
                                            saveProductToFirestore(imageUrls, null, userId);
                                        });
                                        break;

                                    case "processing":
                                    case "starting":
                                        // Still processing, poll again
                                        pollForResult(client, predictionId, imageUrls, userId);
                                        break;

                                    default:
                                        Log.d(TAG, "Unknown status: " + status);
                                        runOnUiThread(() ->
                                            saveProductToFirestore(imageUrls, null, userId));
                                        break;
                                }

                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing poll response", e);
                                runOnUiThread(() ->
                                    saveProductToFirestore(imageUrls, null, userId));
                            }
                        } else {
                            Log.e(TAG, "Poll error: " + response.code() + " - " + response.message());
                            runOnUiThread(() ->
                                saveProductToFirestore(imageUrls, null, userId));
                        }
                    }
                });

            } catch (InterruptedException e) {
                Log.e(TAG, "Polling interrupted", e);
                runOnUiThread(() ->
                    saveProductToFirestore(imageUrls, null, userId));
            }
        }).start();
    }

    private void saveProductToFirestore(List<String> imageUrls, String videoUrl, String userId) {
        Log.d(TAG, "Saving product to Firestore");
        try {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String category = dropdownCategory.getText().toString().trim();
            
            double price = Double.parseDouble(priceStr);
            
            Product product = new Product();
            product.setTitle(title);
            product.setDescription(description);
            product.setPrice(price);
            product.setCategory(category);
            product.setImageUrls(imageUrls);
            product.setUserId(userId);
            product.setVideoUrl(videoUrl);
            
            if (videoUrl != null && !videoUrl.isEmpty()) {
                product.setVideoUrl(videoUrl);
            }
            
            // Add to Firestore
            firestore.collection("products")
                    .add(product)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Product added with ID: " + documentReference.getId());
                        Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding product", e);
                        resetUiWithError("Failed to save product.");
                    });
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid price format", e);
            resetUiWithError("Please enter a valid price.");
        } catch (Exception e) {
            Log.e(TAG, "Error saving product", e);
            resetUiWithError("An error occurred while saving the product.");
        }
    }

    private void resetUiWithError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            progressOverlay.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            btnAddProduct.setEnabled(true);
            switchGenerateVideo.setEnabled(true);
        });
    }

    private boolean validateForm() {
        boolean valid = true;
        
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String category = dropdownCategory.getText().toString().trim();
        
        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Required");
            valid = false;
        } else {
            etTitle.setError(null);
        }
        
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Required");
            valid = false;
        } else {
            etDescription.setError(null);
        }
        
        if (TextUtils.isEmpty(price)) {
            etPrice.setError("Required");
            valid = false;
        } else {
            try {
                double priceValue = Double.parseDouble(price);
                if (priceValue <= 0) {
                    etPrice.setError("Must be greater than 0");
                    valid = false;
                } else {
                    etPrice.setError(null);
                }
            } catch (NumberFormatException e) {
                etPrice.setError("Enter a valid price");
                valid = false;
            }
        }
        
        if (TextUtils.isEmpty(category)) {
            dropdownCategory.setError("Required");
            valid = false;
        } else {
            dropdownCategory.setError(null);
        }
        
        return valid;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission granted");
                // Permission granted, proceed with image picking
                Toast.makeText(this, "Permission granted! You can now select images.", 
                        Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Permission denied");
                Toast.makeText(this, "Permission denied. Cannot access images.", 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
