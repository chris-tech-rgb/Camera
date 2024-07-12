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
        double[] coe = new double[6];
        switch (selectedItem) {
            case "pH":
                coe[0] = -15.718605037575877;
                coe[1] = -0.03080303103844433;
                coe[2] = 45.64599555321209;
                coe[3] = -0.2009124581020623;
                coe[4] = 0.33180976001457324;
                coe[5] = 0.18380161142124604;
                break;
            case "Glucose":
                coe[0] = 0.9359533720850897;
                coe[1] = 0.1262190476083578;
                coe[2] = -0.014892333900063486;
                coe[3] = 1.210664316525882;
                coe[4] = 0.025640415192638964;
                coe[5] = 1.0536548024608872;
                break;
            case "Lactate":
                coe[0] = 2.2315746305948734;
                coe[1] = 0.8593691098429146;
                coe[2] = -6.597149856350124;
                coe[3] = 0.6850134245147629;
                coe[4] = 48.19166719957681;
                coe[5] = -0.035251607735916055;
                break;
        }
        double r1 = Math.pow(rgb[0], coe[1]);
        double r2 = Math.pow(rgb[1], coe[3]);
        double r3 = Math.pow(rgb[2], coe[5]);
        double res = coe[0] * r1 + coe[2] * r2 + coe[4] * r3;
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
}
