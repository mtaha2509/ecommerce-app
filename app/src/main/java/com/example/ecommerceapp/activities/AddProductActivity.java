package com.example.ecommerceapp.activities;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;                      // OkHttp classes :contentReference[oaicite:4]{index=4}
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.net.SocketTimeoutException;

public class AddProductActivity extends AppCompatActivity {
    private static final String TAG = "AddProductActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String MODEL_VERSION = "4876f2a8da1c544772dffa32e8889da4a1bab3a1f5c1937bfcfccb99ae347251";
    private static final String REPLICATE_TOKEN = "api_key";

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
        if (!validateForm()) return;

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        String userId = auth.getCurrentUser().getUid();
        List<String> imageUrls = new ArrayList<>();

        // Count and upload images to Firebase Storage first
        final int totalImages = (int) selectedImageUris.stream().filter(uri -> uri != null).count();
        if (totalImages == 0) {
            resetUiWithError("Select at least one image");
            return;
        }

        for (Uri uri : selectedImageUris) {
            if (uri == null) continue;
            String imageName = UUID.randomUUID().toString();
            StorageReference ref = storage.getReference()
                    .child("products")
                    .child(userId)
                    .child(imageName);

            ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        imageUrls.add(downloadUri.toString());
                        if (imageUrls.size() == totalImages) {
                            callTrellisAPI(imageUrls, userId);
                        }
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting download URL", e);
                        resetUiWithError("Error getting image URL");
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading image", e);
                    resetUiWithError("Error uploading image");
                });
        }
    }

    private void callTrellisAPI(List<String> imageUrls, String userId) {
        try {
            JSONArray imagesArray = new JSONArray(imageUrls);

            JSONObject input = new JSONObject()
                    .put("images",              imagesArray)
                    .put("texture_size",        2048)
                    .put("mesh_simplify",       0.9)
                    .put("ss_sampling_steps",   38)
                    .put("generate_model",      false) // ✅ no GLB
                    .put("generate_color",      true)  // ✅ request MP4 video
                    .put("generate_normal",     false); // ✅ skip normals

            JSONObject payload = new JSONObject()
                    .put("version", MODEL_VERSION)
                    .put("input",   input);

            Log.d(TAG, "API Payload (video only): " + payload);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
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

            makeApiCall(client, request, imageUrls, userId);

        } catch (JSONException e) {
            Log.e(TAG, "JSON build error", e);
            resetUiWithError("Error building API request");
        }
    }

    private void makeApiCall(OkHttpClient client, Request request, List<String> imageUrls, String userId) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed", e);
                runOnUiThread(() -> {
                    if (e instanceof SocketTimeoutException) {
                        resetUiWithError("Request timed out. Please try again later.");
                    } else {
                        resetUiWithError("Network error: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null
                            ? response.body().string()
                            : "No response body";
                    Log.e(TAG, "HTTP error: " + response.code() + " — " + errorBody);
                    runOnUiThread(() -> resetUiWithError("API error: " + response.code()));
                    return;
                }

                try {
                    // 1. Parse the full prediction object (sync mode returns this) :contentReference[oaicite:2]{index=2}
                    String resp = response.body().string();
                    JSONObject prediction = new JSONObject(resp);

                    // 2. 'output' is a JSONObject, not a JSONArray :contentReference[oaicite:3]{index=3}
                    JSONObject outputObj = prediction.getJSONObject("output");

                    // 3. Safely extract your video URL (combined_video or fallback to color_video)
                    String videoUrl;
                    if (outputObj.has("combined_video") &&
                            !outputObj.isNull("combined_video")) {
                        videoUrl = outputObj.getString("combined_video");
                    } else {
                        videoUrl = outputObj.optString("color_video", null);
                    }

                    if (videoUrl == null) {
                        Log.e(TAG, "No video URL in output object");
                        runOnUiThread(() -> resetUiWithError("No video URL received from API"));
                        return;
                    }

                    final String finalVideoUrl = videoUrl;

                    // 4. On the UI thread, build and save your Product with only the MP4 :contentReference[oaicite:4]{index=4}
                    runOnUiThread(() -> {
                        String title    = titleEditText.getText().toString().trim();
                        String desc     = descriptionEditText.getText().toString().trim();
                        String category = categoryEditText.getText().toString().trim();
                        String priceStr = priceEditText.getText().toString().trim();

                        double price;
                        try {
                            price = Double.parseDouble(priceStr);
                        } catch (NumberFormatException ex) {
                            resetUiWithError("Invalid price format");
                            return;
                        }

                        // No GLB produced: pass null for model_file :contentReference[oaicite:5]{index=5}
                        Product product = new Product(
                                title, desc, category, price,
                                imageUrls,
                                null,               // model_file = null
                                finalVideoUrl,      // MP4 URL
                                userId
                        );

                        firestore.collection("products")
                                .add(product)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(
                                            AddProductActivity.this,
                                            "Product and video added successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    finish();
                                })
                                .addOnFailureListener(err -> {
                                    Log.e(TAG, "Error saving product", err);
                                    resetUiWithError("Error saving product");
                                });
                    });

                } catch (JSONException je) {
                    // Catch when keys are missing or types mismatch :contentReference[oaicite:6]{index=6}
                    Log.e(TAG, "JSON parse error", je);
                    runOnUiThread(() -> resetUiWithError("Error parsing API response"));
                }
            }

        });
    }

    private void resetUiWithError(String message) {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
