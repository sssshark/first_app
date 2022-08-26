package com.kw.cpp_test;

import static android.content.ContentValues.TAG;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.kw.cpp_test.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.util.Log;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FaceSDKNative faceSDKNative = new FaceSDKNative();
    TextView infoResult;
    ImageView imageView;
    private Bitmap yourSelectedImage = null;

    //Check Permissions
    private static final int REQUEST_CODE_PERMISSION = 2;
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceSDKNative.FaceDetectionModelUnInit();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        try {
            //RFB-320-quant-ADMM-32
            copyBigDataToSD("RFB-320.mnn");
            copyBigDataToSD("RFB-320-quant-ADMM-32.mnn");
            copyBigDataToSD("RFB-320-quant-KL-5792.mnn");
            copyBigDataToSD("slim-320.mnn");
            copyBigDataToSD("slim-320-quant-ADMM-50.mnn");

        } catch (IOException e) {
            e.printStackTrace();
        }
        File sdDir = Environment.getExternalStorageDirectory();//get model store dir
        String sdPath = sdDir.toString() + "/facesdk/";
//        faceSDKNative.FaceDetectionModelInit(sdPath);
        faceSDKNative.FaceDetectionModelInit(getCacheDir().toString());

//        TextView infoResult = binding.infoResult;
//        ImageView imageView = binding.imageView;
        infoResult = (TextView) findViewById(R.id.infoResult);
        imageView = (ImageView) findViewById(R.id.imageView);

        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                someActivityResultLauncher.launch(i);
            }
        });

        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;
                int width = yourSelectedImage.getWidth();
                int height = yourSelectedImage.getHeight();
                byte[] imageDate = getPixelsRGBA(yourSelectedImage);

                long timeDetectFace = System.currentTimeMillis();
                //do FaceDetect
                int faceInfo[] =  faceSDKNative.FaceDetect(imageDate, width, height,4);
                timeDetectFace = System.currentTimeMillis() - timeDetectFace;

                //Get Results
                if (faceInfo.length>1) {
                    int faceNum = faceInfo[0];
                    infoResult.setText("detect time："+timeDetectFace+"ms,   face number：" + faceNum);
                    Log.i(TAG, "detect time："+timeDetectFace);
                    Log.i(TAG, "face num：" + faceNum );

                    Bitmap drawBitmap = yourSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
                    for (int i=0; i<faceNum; i++) {
                        int left, top, right, bottom;
                        Canvas canvas = new Canvas(drawBitmap);
                        Paint paint = new Paint();
                        left = faceInfo[1+4*i];
                        top = faceInfo[2+4*i];
                        right = faceInfo[3+4*i];
                        bottom = faceInfo[4+4*i];
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(5);
                        //Draw rect
                        canvas.drawRect(left, top, right, bottom, paint);

                    }
                    imageView.setImageBitmap(drawBitmap);
                }else{
                    infoResult.setText("no face found");
                }

            }
        });


    }


    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && null != result.getData()) {
                        // There are no request codes
                        Intent data = result.getData();
                        Uri selectedImage = data.getData();

                        try {
                            Bitmap bitmap = decodeUri(selectedImage);

                            Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                            yourSelectedImage = rgba;

                            imageView.setImageBitmap(yourSelectedImage);

                        } catch (FileNotFoundException e) {
                            Log.e("MainActivity", "FileNotFoundException");
                            return;
                        }
                    }
                }
            });

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 400;

        //// Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        //// Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

    private byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
    }

    private void copyBigDataToSD(String strOutFileName) throws IOException {
        Log.i(TAG, "start copy file " + strOutFileName);
        File sdDir = Environment.getExternalStorageDirectory();//get root dir
//        File file = new File(sdDir.toString()+"/facesdk/");
//        if (!file.exists()) {
//            file.mkdirs();
//        }

//        String tmpFile = sdDir.toString()+"/facesdk/" + strOutFileName;
//        File f = new File(tmpFile);
//        if (f.exists()) {
//            Log.i(TAG, "file exists " + strOutFileName);
//            return;
//        }
        InputStream myInput;
//        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString()+"/facesdk/"+ strOutFileName);
        java.io.OutputStream myOutput = new FileOutputStream(getCacheDir() + strOutFileName);
        myInput = this.getAssets().open(strOutFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
        Log.i(TAG, "end copy file " + strOutFileName);

    }

    /**
     * Checks if the app has permission to write to device storage or open camera
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_permission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }
}