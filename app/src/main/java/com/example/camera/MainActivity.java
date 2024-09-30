package com.example.camera;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 22;
    private Button extractButton, analyzeButton;
    private ImageView imageView;
    private CropImageView cropImageView;
    private Bitmap scaledBitmap;
    private Spinner spinnerOptions;
    private String selectedItem;
    private TextView result;

    @SuppressLint("QueryPermissionsNeeded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonPicture = findViewById(R.id.camera_button);
        extractButton = findViewById(R.id.extract_button);
        analyzeButton = findViewById(R.id.analyze_button);
        imageView = findViewById(R.id.action_image);
        cropImageView = findViewById(R.id.cropImageView);
        result = findViewById(R.id.result);

        cropImageView.setVisibility(View.GONE);

        buttonPicture.setOnClickListener(v -> {
            extractButton.setVisibility(View.VISIBLE);
            analyzeButton.setVisibility(View.GONE);
            result.setVisibility(View.GONE);
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, REQUEST_CODE);
            } else {
                Toast.makeText(MainActivity.this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        });

        extractButton.setOnClickListener(v -> {
            try {
                extractBitmapPart();
                cropImageView.clearSelection(); // Clear selection in CropImageView
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error analyzing picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        analyzeButton.setOnClickListener(v -> {
            try {
                analyzingPicture();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error extracting bitmap part: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        spinnerOptions = findViewById(R.id.spinner_options);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.options_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOptions.setAdapter(adapter);
        spinnerOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedItem = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap photoBitmap = (Bitmap) extras.get("data");
                if (photoBitmap != null) {
                    // Scale and set bitmap
                    scaledBitmap = Bitmap.createScaledBitmap(photoBitmap, 720, 970, false);
                    imageView.setImageBitmap(scaledBitmap);
                    imageView.setVisibility(View.VISIBLE); // Show the ImageView with captured image
                    cropImageView.setVisibility(View.VISIBLE); // Show the CropImageView for selection
                } else {
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractBitmapPart() {
        if (scaledBitmap == null) {
            Toast.makeText(this, "No photo to crop", Toast.LENGTH_SHORT).show();
            return;
        }

        Rect selectedRect = cropImageView.getSelectedRect();

        // Validate and adjust the rectangle to ensure it's within bounds
        int left = selectedRect.left;
        int top = selectedRect.top - 256;
        int right = selectedRect.right;
        int bottom = selectedRect.bottom  - 256;

        // Calculate width and height
        int width = right - left;
        int height = bottom - top;

        try {
            // Check if selected area is valid
            if (width > 0 && height > 0) {
                Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, left, top, width, height);
                // Display the cropped bitmap in imageView
                imageView.setImageBitmap(croppedBitmap);
                imageView.setVisibility(View.VISIBLE);
                cropImageView.clearSelection(); // Clear selection in CropImageView
                cropImageView.setVisibility(View.GONE); // Hide CropImageView after extraction
                spinnerOptions.setVisibility(View.VISIBLE);
                extractButton.setVisibility(View.GONE);
                analyzeButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Please select a valid area", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Error creating cropped bitmap: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void analyzingPicture() {
        result.setVisibility(View.VISIBLE);
        int[] rgb = getAverageRGB(scaledBitmap);
        double luminance = getLuminance(rgb);
        double[] coef = new double[2];
        switch (selectedItem) {
            case "pH":
                coef[0] = -0.33567;
                coef[1] = 6.25133;
                break;
            case "Glucose":
                coef[0] = -0.46851;
                coef[1] = 5.02739;
                break;
            case "Lactate":
                coef[0] = -0.06374;
                coef[1] = 5.06541;
        }
        double res = (luminance - coef[1]) / coef[0];
        String resultText = "R: "+ rgb[0] + "\nG: "+ rgb[1] + "\nB: "+ rgb[2] + "\nResult: " + String.format(Locale.US, "%.2f", res);
        if (!selectedItem.equals("pH") && !String.format(Locale.US, "%.2f", res).equals("NaN")) {
            resultText += " mM";
        }
        result.setText(resultText);
    }

    /** @noinspection ReassignedVariable*/
    private int[] getAverageRGB(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;

        long redSum = 0;
        long greenSum = 0;
        long blueSum = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                redSum += Color.red(pixel);
                greenSum += Color.green(pixel);
                blueSum += Color.blue(pixel);
            }
        }

        int redAverage = (int) (redSum / size);
        int greenAverage = (int) (greenSum / size);
        int blueAverage = (int) (blueSum / size);

        return new int[]{redAverage, greenAverage, blueAverage};
    }

    private double getLuminance(int[] rgb) {
        return Math.log1p(0.299*rgb[0] + 0.587*rgb[1] + 0.114*rgb[2]);
    }
}
