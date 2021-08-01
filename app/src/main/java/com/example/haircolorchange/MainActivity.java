package com.example.haircolorchange;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.obsez.android.lib.filechooser.ChooserDialog;
import com.permissionx.guolindev.PermissionX;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ai.fritz.core.Fritz;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionModels;
import ai.fritz.vision.ModelVariant;
import ai.fritz.vision.imagesegmentation.BlendMode;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentationPredictor;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentationResult;
import ai.fritz.vision.imagesegmentation.MaskClass;
import ai.fritz.vision.imagesegmentation.SegmentationOnDeviceModel;
import top.defaults.colorpicker.ColorPickerPopup;

public class MainActivity extends AppCompatActivity {

    private Button btn0,btn1,btn2;
    private TextView tpath;
    private int selcolor = Color.BLUE;
    private ImageView imv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionX.init(this)
                .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .request((allGranted, grantedList, deniedList) -> {
                    if (!allGranted) {
                        Toast.makeText(MainActivity.this, "Not all permissions are granted!", Toast.LENGTH_LONG).show();
                        MainActivity.this.finish();
                        System.exit(0);
                    }
                });
        setContentView(R.layout.activity_main);
        Fritz.configure(this, "a4e85980765c4b0880545930411334cd");
        imv = findViewById(R.id.jorge);
        btn0 = findViewById(R.id.eglise);
        tpath = findViewById(R.id.sanmarcos);
        btn0.setOnClickListener(view -> new ChooserDialog(MainActivity.this)
                .withStartFile(Environment.getExternalStorageDirectory().getAbsolutePath())
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        MainActivity.this.tpath.setText(path);
                    }
                }).build().show());
        btn1 = findViewById(R.id.guardar);
        btn1.setOnClickListener(v -> {
            String fp = MainActivity.this.tpath.getText().toString();
            String fnuevo = Environment.getExternalStorageDirectory() + File.separator + "Pictures" + File.separator + (new File(fp)).getName() + "_nuevo.jpg";
            if(!fp.trim().isEmpty() && (new File(fp)).exists()) {
                Bitmap rtn = startfromImageBitmap(fp);
                imv.setImageBitmap(rtn);
                try (FileOutputStream fout = new FileOutputStream(fnuevo)) {
                    rtn.compress(Bitmap.CompressFormat.JPEG, 100, fout); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        btn2 = findViewById(R.id.dyecolor);
        btn2.setOnClickListener(view -> new ColorPickerPopup.Builder(MainActivity.this)
                .initialColor(Color.RED) // Set initial color
                .enableBrightness(true) // Enable brightness slider or not
                .enableAlpha(true) // Enable alpha slider or not
                .okTitle("Choose")
                .cancelTitle("Cancel")
                .showIndicator(true)
                .showValue(true)
                .build()
                .show(view, new ColorPickerPopup.ColorPickerObserver() {
                    @Override
                    public void onColorPicked(int color) {
                        selcolor = color;
                    }
                }));
    }

    private Bitmap startfromImageBitmap(String filepath) {
        // load a bitmap from drawable
        Bitmap bitmap = BitmapFactory.decodeFile(filepath);
        // Initialize the model included with the app
        SegmentationOnDeviceModel onDeviceModel = FritzVisionModels.getHairSegmentationOnDeviceModel(ModelVariant.FAST);
        // Create the predictor with the Hair Segmentation model.
        FritzVisionSegmentationPredictor predictor = FritzVision.ImageSegmentation.getPredictor(onDeviceModel);
        // Create a FritzVisionImage object from BitMap object
        FritzVisionImage visionImage = FritzVisionImage.fromBitmap(bitmap);
        // Run the image through the model to identify pixels representing hair.
        FritzVisionSegmentationResult segmentationResult = predictor.predict(visionImage);
        // Color Blend
        BlendMode colorBlend = BlendMode.COLOR;
        // Extract the mask for which we detected hair in the image
        Bitmap maskBitmap = segmentationResult.buildSingleClassMask(MaskClass.HAIR, 180, .5f, .5f, selcolor);
        // blend maskBitmap with the original image.
        Bitmap blendedBitmap = visionImage.blend(maskBitmap, colorBlend);
        return blendedBitmap;
    }
}