package krunal.com.example.cameraapp;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";
    private static final int MY_SOCKET_TIMEOUT_MS = 10000;
    Dialog progressdialog;

    private AppExecutor mAppExcutor;

    private ImageView mImageView;

    private Button mStartCamera;
    private Button mStartGallery;

    private String mTempPhotoPath;
int actualimage,compressimage;
    private Bitmap mResultsBitmap,actualimagebitmap;
    String image, imagename;
    private FloatingActionButton mClear, mSave, mShare;
    private int SELECT_FILE = 2;
    Dialog dialogmobilenumber;
    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAppExcutor = new AppExecutor();

        mImageView = findViewById(R.id.imageView);
        mClear = findViewById(R.id.clear);
        mSave = findViewById(R.id.Save);
        mShare = findViewById(R.id.Share);
        mStartCamera = findViewById(R.id.startCamera);
        mStartGallery = findViewById(R.id.start_gallery);
        mImageView.setVisibility(View.GONE);
        mShare.setVisibility(View.GONE);
        mSave.setVisibility(View.GONE);
        mClear.setVisibility(View.GONE);


        mStartCamera.setOnClickListener(v -> {
            // Check for the external storage permission
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // If you do not have permission, request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                // Launch the camera if the permission exists
                launchCamera();
            }
        });

        mStartGallery.setOnClickListener(v -> {
            // Check for the external storage permission
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // If you do not have permission, request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                // Launch the camera if the permission exists
                Intent intent = new Intent();
                intent.setType("image/*");
//                        progressdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                        progressdialog.setContentView(R.layout.progressbarlayout);
//                        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//                        Window window = progressdialog.getWindow();
//                        lp.copyFrom(window.getAttributes());
//
//                        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//                        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                        window.setAttributes(lp);
//                        progressdialog.show();
//                        progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                        progressdialog.setCancelable(false);uploasetType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);//
                startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
            }
        });

        mSave.setOnClickListener((View v) -> {
            mAppExcutor.diskIO().execute(() -> {
                // Delete the temporary image file
                BitmapUtils.deleteImageFile(this, mTempPhotoPath);

                // Save the image
                BitmapUtils.saveImage(this, mResultsBitmap);

            });

            Toast.makeText(this, "Image Save", Toast.LENGTH_LONG).show();

        });

        mClear.setOnClickListener(v -> {
            // Clear the image and toggle the view visibility
            mImageView.setImageResource(0);
            mStartCamera.setVisibility(View.VISIBLE);
            mSave.setVisibility(View.GONE);
            mShare.setVisibility(View.GONE);
            mClear.setVisibility(View.GONE);

            mAppExcutor.diskIO().execute(() -> {
                // Delete the temporary image file
                BitmapUtils.deleteImageFile(this, mTempPhotoPath);
            });

        });

        mShare.setOnClickListener((View v) -> {

            mAppExcutor.diskIO().execute(() -> {
                // Delete the temporary image file
                BitmapUtils.deleteImageFile(this, mTempPhotoPath);

                // Save the image
                BitmapUtils.saveImage(this, mResultsBitmap);

            });

            // Share the image
            BitmapUtils.shareImage(this, mTempPhotoPath);

        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    //launchCamera();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Process the image and set it to the TextView
                processAndSetImage();
            } else if (requestCode == SELECT_FILE) {
                if (data != null) {
                    try {

                        // String path = uri.getPath();

                        //File file = new File(data);
                        mResultsBitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
                      actualimagebitmap =mResultsBitmap;
                        byte[] ba;
                        ByteArrayOutputStream bao = new ByteArrayOutputStream();
                        actualimagebitmap.compress(Bitmap.CompressFormat.JPEG,100, bao);
                        ba = bao.toByteArray();
                        actualimage = ba.length/1024;
                      //  Uri tempUri = getImageUri(getApplicationContext(), mResultsBitmap);

                        // CALL THIS METHOD TO GET THE ACTUAL PATH

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                mImageView.setImageBitmap(mResultsBitmap);
                onSelectFromGalleryResult(mResultsBitmap);
            }
        } else {

            // Otherwise, delete the temporary image file
            BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        }
    }

    /**
     * Creates a temporary image file and captures a picture to store in it.
     *
     */

    private void onSelectFromGalleryResult(Bitmap mResultsBitmap) {

        if (mResultsBitmap != null) {

            dialogmobilenumber = new Dialog(MainActivity.this);
            dialogmobilenumber.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialogmobilenumber.setContentView(R.layout.qualityenter);
            EditText edtval = dialogmobilenumber.findViewById(R.id.input);
            TextView btncompress1 = dialogmobilenumber.findViewById(R.id.btncencel);
            TextView btncompress2 = dialogmobilenumber.findViewById(R.id.btnsubmit);
            LinearLayout cancellayout = dialogmobilenumber.findViewById(R.id.cancellayout);
            LinearLayout wishcontinue = dialogmobilenumber.findViewById(R.id.wishcontinue);
            LinearLayout editlayout = dialogmobilenumber.findViewById(R.id.prevmpinlayout);

            TextView  yescontinue = dialogmobilenumber.findViewById(R.id.yescontinue);
            TextView  nocontinue = dialogmobilenumber.findViewById(R.id.no);

            TextView detailimage = dialogmobilenumber.findViewById(R.id.textactual);
            btncompress1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   dialogmobilenumber.dismiss();


                }
            });
            btncompress2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String quality = edtval.getText().toString();
                    int quantity = Integer.parseInt(quality);

                    byte[] ba;
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    mResultsBitmap.compress(Bitmap.CompressFormat.JPEG, quantity, bao);
                    ba = bao.toByteArray();

compressimage =ba.length/1024;
detailimage.setText("Actual  Size Approx " + actualimage +"KB" +"& the compressed size is" + compressimage + "KB");
cancellayout.setVisibility(View.GONE);
editlayout.setVisibility(View.GONE);
wishcontinue.setVisibility(View.VISIBLE);
yescontinue.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        progressdialog = new Dialog(MainActivity.this);
        progressdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressdialog.setContentView(R.layout.progressbarlayout);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = progressdialog.getWindow();
        lp.copyFrom(window.getAttributes());

        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        progressdialog.show();
        progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressdialog.setCancelable(false);
        image = Base64.encodeToString(ba, Base64.DEFAULT);
        imagename = UUID.randomUUID().toString();
        imagename = imagename + "_" + quality;
        uploadphoto(getApplicationContext(), image);

    }
});
nocontinue.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
detailimage.setText("");
cancellayout.setVisibility(View.VISIBLE);
wishcontinue.setVisibility(View.GONE);
editlayout.setVisibility(View.VISIBLE);
    }
});


                }
            });


            WindowManager.LayoutParams layoutparams = new WindowManager.LayoutParams();
            Window window = dialogmobilenumber.getWindow();
            layoutparams.copyFrom(window.getAttributes());
            layoutparams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutparams.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(layoutparams);
            dialogmobilenumber.getWindow().setBackgroundDrawable(new ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
            dialogmobilenumber.setCancelable(true);
            dialogmobilenumber.show();


        }

    }

//    private void onSelectFromGalleryResult(Bitmap mResultsBitmap) {
//
//        if (mResultsBitmap != null) {
//
//            dialogmobilenumber = new Dialog(MainActivity.this);
//            dialogmobilenumber.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            dialogmobilenumber.setContentView(R.layout.dialog);
//            Button btncompress1 = dialogmobilenumber.findViewById(R.id.yestxt);
//            Button btncompress2 = dialogmobilenumber.findViewById(R.id.notxt);
//            Button btncompress3 = dialogmobilenumber.findViewById(R.id.ytxt);
//            Button btncompress4 = dialogmobilenumber.findViewById(R.id.notvxt);
//            btncompress1.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    progressdialog = new Dialog(MainActivity.this);
//                    progressdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                    progressdialog.setContentView(R.layout.progressbarlayout);
//                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//                    Window window = progressdialog.getWindow();
//                    lp.copyFrom(window.getAttributes());
//
//                    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                    window.setAttributes(lp);
//                    progressdialog.show();
//                    progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                    progressdialog.setCancelable(false);
//
//                    byte[] ba;
//                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
//                    mResultsBitmap.compress(Bitmap.CompressFormat.JPEG, 80, bao);
//                    ba = bao.toByteArray();
//                    image = Base64.encodeToString(ba, Base64.DEFAULT);
//                    imagename = UUID.randomUUID().toString();
//                    imagename = imagename + "_80";
//                    //saveFileToDrive(bm);
//                    uploadphoto(getApplicationContext(), image);
//
//
//                }
//            });
//            btncompress2.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    progressdialog = new Dialog(MainActivity.this);
//                    progressdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                    progressdialog.setContentView(R.layout.progressbarlayout);
//                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//                    Window window = progressdialog.getWindow();
//                    lp.copyFrom(window.getAttributes());
//
//                    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                    window.setAttributes(lp);
//                    progressdialog.show();
//                    progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                    progressdialog.setCancelable(false);
//
//                    byte[] ba;
//                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
//                    mResultsBitmap.compress(Bitmap.CompressFormat.JPEG, 60, bao);
//                    ba = bao.toByteArray();
//                    image = Base64.encodeToString(ba, Base64.DEFAULT);
//                    imagename = UUID.randomUUID().toString();
//                    imagename = imagename + "_60";
//
//                    uploadphoto(getApplicationContext(), image);
//
//
//                }
//            });
//            btncompress3.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    progressdialog = new Dialog(MainActivity.this);
//                    progressdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                    progressdialog.setContentView(R.layout.progressbarlayout);
//                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//                    Window window = progressdialog.getWindow();
//                    lp.copyFrom(window.getAttributes());
//
//                    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                    window.setAttributes(lp);
//                    progressdialog.show();
//                    progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                    progressdialog.setCancelable(false);
//                    byte[] ba;
//                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
//                    mResultsBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bao);
//                    ba = bao.toByteArray();
//                    image = Base64.encodeToString(ba, Base64.DEFAULT);
//                    imagename = UUID.randomUUID().toString();
//                    imagename = imagename + "_90";
//
//                    uploadphoto(getApplicationContext(), image);
//
//
//                }
//            });
//            btncompress4.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    progressdialog = new Dialog(MainActivity.this);
//                    progressdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                    progressdialog.setContentView(R.layout.progressbarlayout);
//                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//                    Window window = progressdialog.getWindow();
//                    lp.copyFrom(window.getAttributes());
//
//                    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                    window.setAttributes(lp);
//                    progressdialog.show();
//                    progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                    progressdialog.setCancelable(false);
//                    byte[] ba;
//                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
//                    mResultsBitmap.compress(Bitmap.CompressFormat.JPEG, 70, bao);
//                    ba = bao.toByteArray();
//                    image = Base64.encodeToString(ba, Base64.DEFAULT);
//                    imagename = UUID.randomUUID().toString();
//                    imagename = imagename + "_90";
//
//                    uploadphoto(getApplicationContext(), image);
//
//                }
//            });
//
//            WindowManager.LayoutParams layoutparams = new WindowManager.LayoutParams();
//            Window window = dialogmobilenumber.getWindow();
//            layoutparams.copyFrom(window.getAttributes());
//            layoutparams.height = WindowManager.LayoutParams.WRAP_CONTENT;
//            layoutparams.width = WindowManager.LayoutParams.MATCH_PARENT;
//            window.setAttributes(layoutparams);
//            dialogmobilenumber.getWindow().setBackgroundDrawable(new ColorDrawable(
//                    android.graphics.Color.TRANSPARENT));
//            dialogmobilenumber.setCancelable(true);
//            dialogmobilenumber.show();
//
//
//        }
//
//    }

    private void launchCamera() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        // Ensure that there's a camera activity to handle the intent
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            // Create the File where the photo should go
//            File photoFile = null;
//            try {
//                photoFile = createImageFile();
//            } catch (IOException ex) {
//                // Error occurred while creating the File
//
//            }
//            // Continue only if the File was successfully created
//            if (photoFile != null) {
//                Uri photoURI = FileProvider.getUriForFile(this,
//                        "com.example.android.fileprovider",
//                        photoFile);
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//            }
//        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    private File createImageFile() throws IOException {
        // Create an image file name

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";


        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File images = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = images.getAbsolutePath();

        // Set the new bitmap to the ImageView
       // mImageView.setImageBitmap(mResultsBitmap);
        return images;
    }

    /**
     * Method for processing the captured image and setting it to the TextView.
     */
    private void processAndSetImage() {

        // Toggle Visibility of the views
        mStartCamera.setVisibility(View.GONE);
        mSave.setVisibility(View.VISIBLE);
        //  mShare.setVisibility(View.VISIBLE);
        mClear.setVisibility(View.VISIBLE);
        mImageView.setVisibility(View.VISIBLE);

        // Resample the saved image to fit the ImageView
        mResultsBitmap = BitmapUtils.resamplePic(this, currentPhotoPath);
        byte[] ba;
       actualimagebitmap = mResultsBitmap;
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        actualimagebitmap.compress(Bitmap.CompressFormat.JPEG, 100, bao);
        ba = bao.toByteArray();
       actualimage  = ba.length/1024;
//        byte[] ba;
//        ByteArrayOutputStream bao = new ByteArrayOutputStream();
//        mResultsBitmap.compress(Bitmap.CompressFormat.JPEG, 95, bao);
//        ba = bao.toByteArray();
//        image = Base64.encodeToString(ba, Base64.DEFAULT);
//       imagename = UUID.randomUUID().toString();
//        imagename = imagename + "_camera_90";
//        progressdialog = new Dialog(MainActivity.this);
//        progressdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        progressdialog.setContentView(R.layout.progressbarlayout);
//        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
//        Window window = progressdialog.getWindow();
//        lp.copyFrom(window.getAttributes());
//
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
//        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//        window.setAttributes(lp);
//        progressdialog.show();
//        progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        progressdialog.setCancelable(false);
//        uploadphoto(getApplicationContext(), image);
//        // Set the new bitmap to the ImageView
        mImageView.setImageBitmap(mResultsBitmap);
        dialogmobilenumber = new Dialog(MainActivity.this);
        dialogmobilenumber.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogmobilenumber.setContentView(R.layout.qualityenter);
        EditText edtval = dialogmobilenumber.findViewById(R.id.input);
        TextView btncompress1 = dialogmobilenumber.findViewById(R.id.btncencel);
        TextView btncompress2 = dialogmobilenumber.findViewById(R.id.btnsubmit);
        LinearLayout cancellayout = dialogmobilenumber.findViewById(R.id.cancellayout);
        LinearLayout wishcontinue = dialogmobilenumber.findViewById(R.id.wishcontinue);
        LinearLayout editlayout = dialogmobilenumber.findViewById(R.id.prevmpinlayout);

        TextView  yescontinue = dialogmobilenumber.findViewById(R.id.yescontinue);
        TextView  nocontinue = dialogmobilenumber.findViewById(R.id.no);

        TextView detailimage = dialogmobilenumber.findViewById(R.id.textactual);

        btncompress1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogmobilenumber.dismiss();


            }
        });
        btncompress2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String quality = edtval.getText().toString();
                int quantity = Integer.parseInt(quality);

                byte[] ba;
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                mResultsBitmap.compress(Bitmap.CompressFormat.JPEG, quantity, bao);
                ba = bao.toByteArray();

                compressimage =ba.length/1024;
                detailimage.setText("Actual  Size Approx " + actualimage +"KB" +"& the compressed size is" + compressimage + "KB");
                cancellayout.setVisibility(View.GONE);
                editlayout.setVisibility(View.GONE);
                wishcontinue.setVisibility(View.VISIBLE);
                yescontinue.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        progressdialog = new Dialog(MainActivity.this);
                        progressdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        progressdialog.setContentView(R.layout.progressbarlayout);
                        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                        Window window = progressdialog.getWindow();
                        lp.copyFrom(window.getAttributes());

                        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        window.setAttributes(lp);
                        progressdialog.show();
                        progressdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        progressdialog.setCancelable(false);
                        image = Base64.encodeToString(ba, Base64.DEFAULT);
                        imagename = UUID.randomUUID().toString();
                        imagename = imagename + "_" + quality;
                        uploadphoto(getApplicationContext(), image);

                    }
                });
                nocontinue.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        detailimage.setText("");
                        cancellayout.setVisibility(View.VISIBLE);
                        wishcontinue.setVisibility(View.GONE);
                        editlayout.setVisibility(View.VISIBLE);
                    }
                });


            }
        });


        WindowManager.LayoutParams layoutparams = new WindowManager.LayoutParams();
        Window window = dialogmobilenumber.getWindow();
        layoutparams.copyFrom(window.getAttributes());
        layoutparams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutparams.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutparams);
        dialogmobilenumber.getWindow().setBackgroundDrawable(new ColorDrawable(
                android.graphics.Color.TRANSPARENT));
        dialogmobilenumber.setCancelable(true);
        dialogmobilenumber.show();


    }



    private void uploadphoto(final Context context, final String image) {
        String webAddress = "http://busyfcm.btmp.in/FCMServices.ashx?";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, webAddress, new Response.Listener<String>() {
            public void onResponse(String response) {

                if (response.equalsIgnoreCase("T")) {
                    progressdialog.dismiss();
                    dialogmobilenumber.dismiss();
                    Toast.makeText(context, "Done,Image Uploaded", Toast.LENGTH_LONG).show();


                } else {

                    progressdialog.dismiss();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                progressdialog.dismiss();
                dialogmobilenumber.dismiss();
                Toast.makeText(MainActivity.this, "Unable to connect to remote server", Toast.LENGTH_LONG).show();

            }

        }) {


            @Override
            protected Response parseNetworkResponse(NetworkResponse response) {
                try {


                    String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers));


                    return Response.success(jsonString,
                            HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException e) {
                    return Response.error(new ParseError(e));


                }

            }

            public byte[] getBody() throws com.android.volley.AuthFailureError {
                String str = image;
                return str.getBytes();
            }

            ;


            public String getBodyContentType() {
                return "application/jpg;";
            }

            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();


                params.put("SC", "31");
                params.put("FileName", imagename + ".jpg");
                params.put("BDEPCode", "1002");


                return params;
            }
        };


        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        stringRequest.setShouldCache(false);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }
}



