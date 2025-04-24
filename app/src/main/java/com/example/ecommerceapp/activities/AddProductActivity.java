package com.example.ecommerceapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ecommerceapp.R;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class AddProductActivity extends AppCompatActivity {

    private static final int MAX_IMAGES = 4;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private List<Uri> selectedImages = new ArrayList<>();
    private ImageView[] imageViews;
    private Button[] addImageButtons;
    private EditText productName, productDescription, productPrice;
    private AutoCompleteTextView productCategory;
    private Button submitButton;
    private int currentImageIndex = 0;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null && selectedImages.size() < MAX_IMAGES) {
                        selectedImages.add(imageUri);
                        updateImageViews();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        initializeViews();
        setupCategoryDropdown();
        setupImageButtons();
        setupSubmitButton();
    }

    private void initializeViews() {
        imageViews = new ImageView[]{
                findViewById(R.id.img_product_1),
                findViewById(R.id.img_product_2),
                findViewById(R.id.img_product_3),
                findViewById(R.id.img_product_4)
        };

        addImageButtons = new Button[]{
                findViewById(R.id.btn_add_image_1),
                findViewById(R.id.btn_add_image_2),
                findViewById(R.id.btn_add_image_3),
                findViewById(R.id.btn_add_image_4)
        };

        productName = findViewById(R.id.et_product_name);
        productDescription = findViewById(R.id.et_product_description);
        productPrice = findViewById(R.id.et_product_price);
        productCategory = findViewById(R.id.actv_product_category);
        submitButton = findViewById(R.id.btn_submit_product);
    }

    private void setupCategoryDropdown() {
        String[] categories = {"Electronics", "Clothing", "Home & Kitchen", "Books", "Sports"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, categories);
        productCategory.setAdapter(adapter);
    }

    private void setupImageButtons() {
        for (int i = 0; i < MAX_IMAGES; i++) {
            final int index = i;
            addImageButtons[i].setOnClickListener(v -> {
                currentImageIndex = index;
                checkAndRequestPermissions();
            });
        }
    }

    private void setupSubmitButton() {
        submitButton.setOnClickListener(v -> {
            if (validateForm()) {
                // TODO: Implement product submission logic
                Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied. Cannot access images.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void updateImageViews() {
        for (int i = 0; i < MAX_IMAGES; i++) {
            if (i < selectedImages.size()) {
                imageViews[i].setImageURI(selectedImages.get(i));
                imageViews[i].setVisibility(View.VISIBLE);
                addImageButtons[i].setVisibility(View.GONE);
            } else {
                imageViews[i].setVisibility(View.GONE);
                addImageButtons[i].setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean validateForm() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Please add at least one product image", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (productName.getText().toString().trim().isEmpty()) {
            productName.setError("Product name is required");
            return false;
        }

        if (productDescription.getText().toString().trim().isEmpty()) {
            productDescription.setError("Product description is required");
            return false;
        }

        if (productPrice.getText().toString().trim().isEmpty()) {
            productPrice.setError("Product price is required");
            return false;
        }

        if (productCategory.getText().toString().trim().isEmpty()) {
            productCategory.setError("Product category is required");
            return false;
        }

        return true;
    }
} 