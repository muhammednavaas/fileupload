package com.e.uploadapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.e.uploadapp.utility.FileUploadService;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static final int REQUEST_TAKE_PHOTO = 101;
    static final int REQUEST_GALLERY = 102;
    File mPhotoFile;
    ImageView ivDisplayImage;
    Button buttonUpload;
    TextView tvSelectedFilePath;
    ImageView ivSelectImage;
    TextView txvResult;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivDisplayImage = findViewById(R.id.ivDisplayImage);
        buttonUpload = findViewById(R.id.buttonUpload);
        tvSelectedFilePath = findViewById(R.id.tvSelectedFilePath);
        ivSelectImage = findViewById(R.id.imageView2);
        txvResult = findViewById(R.id.txvResult);
        progressBar=findViewById(R.id.progressBar);
        buttonUpload.setOnClickListener(this);
        ivSelectImage.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonUpload:
                if (tvSelectedFilePath.getText().toString().isEmpty()) {
                    Toast.makeText(this, "Select file first", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(this, FileUploadService.class);
                intent.putExtra("mFilePath", tvSelectedFilePath.getText().toString());
                FileUploadService.enqueueWork(this, intent);
                break;
            case R.id.imageView2:
                selectImage();
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    requestStoragePermission(true);
                } else if (items[item].equals("Choose from Gallery")) {
                    requestStoragePermission(false);
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    /**
     * Requesting multiple permissions (storage and camera) at once
     * This uses multiple permission model from dexter
     * On permanent denial opens settings dialog
     */
    private void requestStoragePermission(final boolean isCamera) {
        Dexter.withActivity(this).withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            if (isCamera) {
                                startCamera();
                            } else {
                                chooseGallery();
                            }
                        }
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            chooseGallery();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }

                }).withErrorListener(new PermissionRequestErrorListener() {
            @Override
            public void onError(DexterError error) {
                Toast.makeText(MainActivity.this.getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show();
            }
        })
                .onSameThread()
                .check();

    }

    private void chooseGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhoto.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(pickPhoto, REQUEST_GALLERY);
    }

    public void startCamera() {
        mPhotoFile = newFile();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            if (mPhotoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.e.uploadapp.fileprovider", mPhotoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    private File newFile() {
        Calendar cal = Calendar.getInstance();
        long timeMillis = cal.getTimeInMillis();
        String mFileName = timeMillis + ".mp4";
        File mFilePathe = getFilePath();
        try {
            File newFile = new File(mFilePathe.getAbsoluteFile(), mFileName);
            newFile.createNewFile();
            return newFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private File getFilePath() {
        return getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                tvSelectedFilePath.setText(mPhotoFile.getAbsolutePath());
                Glide.with(this).load(mPhotoFile).apply(new RequestOptions().centerCrop().circleCrop()).into(ivDisplayImage);
            } else if (requestCode == REQUEST_GALLERY) {
                Uri selectedImage = data.getData();
                tvSelectedFilePath.setText(getRealPathFromUri(selectedImage));
                Glide.with(this).load(getRealPathFromUri(selectedImage)).apply(new RequestOptions().centerCrop().circleCrop()).into(ivDisplayImage);

            }
        }
    }

    private String getRealPathFromUri(Uri selectedImage) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(selectedImage, proj, null, null, null);
            assert cursor != null;
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter("my.own.broadcast");
        LocalBroadcastManager.getInstance(this).registerReceiver(myLocalBroadcastReceiver, intentFilter);
    }

    private BroadcastReceiver myLocalBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("result");
            txvResult.setText(result);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myLocalBroadcastReceiver);
    }
}
