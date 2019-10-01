package com.moleculight.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Request code for Camera Permission
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    // Request code for Storage Permission
    private static final int REQUEST_STORAGE_PERMISSION = 100;
    // For maintaining state of camera
    private static final String[] CAMERA_STATUS = {"START", "STOP", "IDLE"};
    // For setting name of Camera on text views and buttons
    private static final String[] CAMERA_NAME = {"FRONT", "BACK"};

    // Views declaration
    private Button btn_front_camera, btn_back_camera, btn_take_picture;
    private TextView tv_timer, tv_front_camera_fps, tv_back_camera_fps, tv_front_camera_number,
            tv_back_camera_number;
    private TextureView textureView_front_camera, textureView_back_camera;

    // To keep note of time between two frames
    private long previousTime = 0L, previousTime1 = 0L;
    // To maintain current status of camera
    private boolean isFrontCameraOn = true, isBackCameraOn = false;
    // For setting camera ID's on text views and buttons
    private String cameraId, cameraIdA, cameraIdB;

    // Camera objects
    private CameraDevice cameraDevice, cameraDeviceFront, cameraDeviceBack;
    private CameraCaptureSession cameraCaptureSessionFront, cameraCaptureSessionBack;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;

    // Activity context to show Toast messages
    private Context context;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;

        tv_timer = findViewById(R.id.tv_timer);
        tv_front_camera_fps = findViewById(R.id.tv_front_camera_fps);
        tv_back_camera_fps = findViewById(R.id.tv_back_camera_fps);
        tv_front_camera_number = findViewById(R.id.tv_front_camera_number);
        tv_back_camera_number = findViewById(R.id.tv_back_camera_number);
        btn_take_picture = findViewById(R.id.btn_take_picture);
        textureView_front_camera = findViewById(R.id.textureView_front_camera);
        textureView_back_camera = findViewById(R.id.textureView_back_camera);
        btn_front_camera = findViewById(R.id.btn_front_camera);
        btn_back_camera = findViewById(R.id.btn_back_camera);

        // Setting current date and time after every second.
        final Handler dateTimeHandler = new Handler(getMainLooper());
        dateTimeHandler.postDelayed(new Runnable() {

            public void run() {
                tv_timer.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
                dateTimeHandler.postDelayed(this, 1000);
            }
        }, 10);

        askPermissions();

    }

    private void onPermissionReceived() {
        // Setting listeners to texture views
        textureView_front_camera.setSurfaceTextureListener(frontCameraTextureListener);
        textureView_back_camera.setSurfaceTextureListener(backCameraTextureListener);
        openCamera(CAMERA_NAME[0]);

        // capturing picture of all active cameras
        btn_take_picture.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (isFrontCameraOn) {
                    takePicture(cameraIdA);
                }
                if (isBackCameraOn) {
                    takePicture(cameraIdB);
                }
            }
        });

        // To start and stop front camera
        btn_front_camera.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (isFrontCameraOn) {
                    isFrontCameraOn = false;
                    // Closing front camera
                    cameraCaptureSessionFront.close();
                    showToast(getString(R.string.stop_front_camera));
                } else {
                    isFrontCameraOn = true;
                    // Opening front camera
                    openCamera(CAMERA_NAME[0]);
                    showToast(getString(R.string.start_front_camera));
                }
                // updating respective UI views
                updateFrontCameraButtonText();


            }
        });

        // To start and stop back camera
        btn_back_camera.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (isBackCameraOn) {
                    isBackCameraOn = false;
                    // Closing back camera
                    cameraCaptureSessionBack.close();
                    showToast(getString(R.string.stop_back_camera));
                } else {
                    isBackCameraOn = true;
                    // Opening front camera
                    openCamera(CAMERA_NAME[1]);
                    showToast(getString(R.string.start_back_camera));
                }
                // updating respective UI views
                updateBackCameraButtonText();
            }
        });

        /* Opening both cameras simultaneously resulted in listener and camera object access issues.
         * Hence, opened the back camera after 5 seconds delay.
         * */
        final Handler openBackCameraHandler = new Handler();
        openBackCameraHandler.postDelayed(new Runnable() {

            public void run() {
                btn_back_camera.performClick();
            }
        }, 5000);

        // updating respective UI views
        updateFrontCameraButtonText();
        updateBackCameraButtonText();
    }

    /* Surface Texture listener for front camera to find if the texture is available to show the
    camera view.
     * Also, has other related events to surface.
     * */
    TextureView.SurfaceTextureListener frontCameraTextureListener =
            new TextureView.SurfaceTextureListener() {

                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                                      int height) {
                    //open your front camera here
                    openCamera(CAMERA_NAME[0]);
                }

                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                                        int height) {
                    // Transform you image captured size according to the surface width and height
                }


                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }


                public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
                    /* Calculating frames per seconds for front camera by finding the time elapsed
                     * between two frames.            *
                     */
                    if (surface.getTimestamp() - previousTime != 0) {
                        long frontCameraFPS = 1000000000 / (surface.getTimestamp() - previousTime);
                        if (isFrontCameraOn)
                            tv_front_camera_fps.setText(frontCameraFPS + " fps");
                    }

                    previousTime = surface.getTimestamp();
                }
            };

    /* Surface Texture listener for back camera to find if the texture is available to show the
    camera view.
     * Also, has other related events to surface.
     * */
    TextureView.SurfaceTextureListener backCameraTextureListener =
            new TextureView.SurfaceTextureListener() {

                // Not opening back camera here, as it would be opened on click of the start back
                // camera
                // button.
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                                      int height) {
                }


                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                                        int height) {
                    // Transform you image captured size according to the surface width and height
                }


                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }


                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                    /* Calculating frames per seconds for back camera by finding the time elapsed
                     * between two frames.
                     */
                    if (surface.getTimestamp() - previousTime1 != 0) {
                        long backCameraFPS = 1000000000 / (surface.getTimestamp() - previousTime1);
                        if (isBackCameraOn)
                            tv_back_camera_fps.setText(backCameraFPS + " fps");

                    }
                    previousTime1 = surface.getTimestamp();
                }
            };

    // Callback for front camera states
    private final CameraDevice.StateCallback frontCameraStateCallback =
            new CameraDevice.StateCallback() {

                public void onOpened(@NonNull CameraDevice camera) {
                    //This is called when the camera is open
                    cameraDevice = camera;
                    cameraDeviceFront = cameraDevice;

                    if (isFrontCameraOn) {
                        createFrontCameraPreview();
                    }
                }


                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                }


                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            };

    // Callback for back camera states
    private final CameraDevice.StateCallback backCameraStateCallback =
            new CameraDevice.StateCallback() {

                public void onOpened(@NonNull CameraDevice camera) {
                    //This is called when the camera is open
                    cameraDevice = camera;
                    cameraDeviceBack = cameraDevice;
                    if (isBackCameraOn) {
                        createBackCameraPreview();
                    }
                }


                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                }


                public void onError(@NonNull CameraDevice camera, int error) {
                    if (cameraDevice != null)
                        cameraDevice.close();
                    cameraDevice = null;
                }
            };

    // Capturing current frame of active camera
    protected void takePicture(String cameraId) {

        Bitmap bitmap;
        File file;
        // storing to root of the external storage
        String path = Environment.getExternalStorageDirectory().toString();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = date.format(calendar.getTime());

        // specifying file name and capturing the bitmap from camera
        if (cameraId.equals(cameraIdA)) {
            bitmap = textureView_front_camera.getBitmap();
            file = new File(path,
                    CAMERA_NAME[0] + cameraIdA + "_" + strDate + "_" + System.currentTimeMillis() + ".jpg");

        } else {
            bitmap = textureView_back_camera.getBitmap();
            file = new File(path,
                    CAMERA_NAME[1] + cameraIdB + "_" + strDate + "_" + System.currentTimeMillis() + ".jpg");
        }

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            if (checkPermission()) {
                if (!file.exists()) {
                    try {
                        // Saving the file to external storage
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.flush();
                        fos.close();
                        showToast(getString(R.string.image_saved));
                    } catch (Exception e) {
                        showToast(getString(R.string.image_failed));
                        e.printStackTrace();
                    }
                }
            } else {
                // Ask for storage permission if not yet allowed.
                requestPermission();
            }
        }
    }

    // creating preview for the front camera
    protected void createFrontCameraPreview() {
        try {
            SurfaceTexture texture = textureView_front_camera.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {

                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            //The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            cameraCaptureSessionFront = cameraCaptureSession;
                            updateFrontCameraPreview();
                        }


                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast(getString(R.string.configuration_changed));
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // creating preview for the back camera
    protected void createBackCameraPreview() {
        try {
            SurfaceTexture texture1 = textureView_back_camera.getSurfaceTexture();
            texture1.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface1 = new Surface(texture1);
            captureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface1);
            cameraDevice.createCaptureSession(Collections.singletonList(surface1),
                    new CameraCaptureSession.StateCallback() {

                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            //The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }
                            cameraCaptureSessionBack = cameraCaptureSession;
                            updateBackCameraPreview();
                        }


                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast(getString(R.string.configuration_changed));
                        }
                    }, null);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    // Retrieving camera service from the OS to start the camera with the specified configurations
    @SuppressLint("MissingPermission")
    private void openCamera(String cameraName) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // Opens either front or back camera at a time
            if (cameraName.equals(CAMERA_NAME[0])) {
                cameraIdA = manager.getCameraIdList()[1];
                cameraId = cameraIdA;
            }
            if (cameraName.equals(CAMERA_NAME[1])) {
                cameraIdB = manager.getCameraIdList()[0];
                cameraId = cameraIdB;
            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];


            if (cameraId.equals(cameraIdA)) {
                manager.openCamera(cameraId, frontCameraStateCallback, null);
                updateFrontCameraButtonText();
            } else {

                manager.openCamera(cameraId, backCameraStateCallback, null);
                updateBackCameraButtonText();
            }


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /*
     * the front camera device will continually capture images using the settings in the provided
     * CaptureRequest,
     * at the maximum rate possible.
     *
     * */
    protected void updateFrontCameraPreview() {
        if (null == cameraDeviceFront) {
            showToast(getString(R.string.update_preview_error));
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessionFront.setRepeatingRequest(captureRequestBuilder.build(), null,
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /*
     * the back camera device will continually capture images using the settings in the provided
     * CaptureRequest,
     * at the maximum rate possible.
     *
     * */
    protected void updateBackCameraPreview() {
        if (null == cameraDeviceBack) {
            showToast(getString(R.string.update_preview_error));
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessionBack.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    // Checking if user allowed or denied the needed permissions
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED && ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // close the app
                showToast(getString(R.string.warning_camera_permission));
                finish();
            } else {
                onPermissionReceived();
            }
        }
        if (requestCode == REQUEST_STORAGE_PERMISSION) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showToast(getString(R.string.warning_storage_permission));
            }
        }
    }

    // To update the respective views based on current status of front camera
    protected void updateFrontCameraButtonText() {
        // Temporary Strings
        String frontCameraButtonText;
        if (isFrontCameraOn) {
            frontCameraButtonText = CAMERA_STATUS[1] + cameraIdA;

        } else {
            frontCameraButtonText = CAMERA_STATUS[0] + cameraIdA;
            tv_front_camera_fps.setText(CAMERA_STATUS[2]);

        }
        if (cameraIdA != null)
            tv_front_camera_number.setText("CAM " + cameraIdA + ": ");
        btn_front_camera.setText(frontCameraButtonText);
    }

    // To update the respective views based on current status of back camera
    protected void updateBackCameraButtonText() {
        String backCameraButtonText;
        if (isBackCameraOn) {
            backCameraButtonText = CAMERA_STATUS[1] + cameraIdB;
        } else {
            if (cameraIdB != null) {
                backCameraButtonText = CAMERA_STATUS[0] + cameraIdB;
            } else {
                backCameraButtonText = CAMERA_STATUS[0];
            }
            tv_back_camera_fps.setText(CAMERA_STATUS[2]);
        }
        if (cameraIdB != null)
            tv_back_camera_number.setText("CAM " + cameraIdB + ": ");
        btn_back_camera.setText(backCameraButtonText);
    }

    // To check if the app has the needed storage permission to execute the features in the program
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    // To request for storage function.
    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showToast(getString(R.string.grant_storage_permission));
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    // To show toast messages for different events
    private void showToast(String message) {

        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

    }


    private void askPermissions() {
        // Add permission for camera and let user grant the permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION);
            return;
        } else {
            onPermissionReceived();
        }
    }
}
