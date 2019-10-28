package june.third.autoemotionsticker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Semaphore;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static java.util.Arrays.asList;


public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "opencv";

    //OpenCV 매트릭스--------------------------------------------------------------------------------
    private Mat matInput;   //카메라 프리뷰에서 받는 매트릭스
    private Mat matResult;  //프리뷰에서 받은 matInput 의 변환 후의 출력되는 매트릭스

    private Mat matSepia;   //세피아 필터가 적용되는 매트릭스
    private Mat matSketch;    //스케치 필터(정확히는 쓰레스홀드 필터)가 적용되는 매트릭스
    private Mat matInvert;  //반전 필터가 적용되는 매트릭스
    private Mat matGray;    //그레이 필터가 적용되는 매트릭스
    //----------------------------------------------------------------------------------------------

    //미리보기 이미지뷰-------------------------------------------------------------------------------
    ImageView ivOriginal;   //원본 미리보기하는 이미지뷰
    ImageView ivSepia;      //세피아 필터 적용된 모습 미리 볼 수 있는 이미지뷰
    ImageView ivSketch;     //스케치 필터 적용된 모습 미리 볼 수 있는 이미지뷰
    ImageView ivInvert;     //반전 필터 적용된 모습 미리 볼 수 있는 이미지뷰
    ImageView ivGray;       //그레이 필터 적용된 모습 미리 볼 수 있는 이미지뷰
    //----------------------------------------------------------------------------------------------

    //핵심 변수들------------------------------------------------------------------------------------
    //OpenCV 카메라뷰
    private CameraBridgeViewBase mOpenCvCameraView;
    //카메라 버튼(원 이미지)
    ImageView imageCameraButton;
    //필터적용된 모습을 실시간으로 보여주게 해주는 핸들러
    Handler previewHandler;
    //회전용 매트릭스
    Matrix rotatedMatrix;
    //필터 변환용 int 값
    private int filterChoice;

    //----------------------------------------------------------------------------------------------

    //opencv 필터들----------------------------------------------------------------------------------
    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);

    public native void ConvertInvertFilter(long matAddrInput, long matAddrResult);

    public native void ConvertSepiaFilter(long matAddrInput, long matAddrResult);

    public native void ConvertBoxFilter(long matAddrInput, long matAddrResult);

    public native void ConvertMorphologyFilter(long matAddrInput, long matAddrResult);

    public native void ConvertMorphologyExFilter(long matAddrInput, long matAddrResult);

    public native void ConvertSketchFilter(long matAddrInput, long matAddrResult);

    public native void ConvertMyFilter(long matAddrInput, long matAddrResult);

    //----------------------------------------------------------------------------------------------

    //얼굴인식---------------------------------------------------------------------------------------
    public native long loadCascade(String cascadeFileName);

    //얼굴하고 눈 찾아주는 detect
    public native void detect(long cascadeClassifier_face,
                              long cascadeClassifier_eye, long matAddrInput, long matAddrResult);
    public long cascadeClassifier_face = 0;
    public long cascadeClassifier_eye = 0;

    //세마포어 (동기화?) 아직 모름
    private final Semaphore writeLock = new Semaphore(1);

    public void getWriteLock() throws InterruptedException {
        writeLock.acquire();
    }

    public void releaseWriteLock() {
        writeLock.release();
    }
    //----------------------------------------------------------------------------------------------




    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d(TAG, "copyFile :: 다음 경로로 파일복사 " + pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 " + e.toString());
        }

    }


    //분류파일 가져오기
    private void read_cascade_file() {
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");

        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_face = loadCascade("haarcascade_frontalface_alt.xml");
        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_eye = loadCascade("haarcascade_eye_tree_eyeglasses.xml");
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)

        //필터적용 미리보는 이미지뷰들
        ivOriginal =  findViewById(R.id.ivOriginal);
        ivSepia =  findViewById(R.id.ivSepia);
        ivSketch =  findViewById(R.id.ivSketch);
        ivInvert =  findViewById(R.id.ivInvert);
        ivGray =  findViewById(R.id.ivGray);


        rotatedMatrix = new Matrix();
        rotatedMatrix.postRotate(90);

        previewHandler = new Handler() {

            public void handleMessage(Message msg) {
                /*  아직 매트릭스를 마지막 부분에서 임시로 돌린 상태 ( 처음부분(근본적인 부분)에서 돌린게 아니라)
                 *  그래서 나중에 근본적인 매트릭스를 회전시켜줘야함 (근본적인 단계에서 안 돌려주면 얼굴인식을 portrait(세로)에서 했을때
                 *  옆으로 기울여야 인식이 됨 나중에 시간 여유되면 천천히 공부하면서 구현
                 */
                // 원본
                Bitmap bmOriginal = Bitmap.createBitmap(matInput.cols(), matInput.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(matInput, bmOriginal);
                Bitmap rotatedBmOriginal = Bitmap.createBitmap(bmOriginal, 0, 0, bmOriginal.getWidth(), bmOriginal.getHeight(), rotatedMatrix, true);
                ivOriginal.setImageBitmap(rotatedBmOriginal);

                //세피아
                Bitmap bmSepia = Bitmap.createBitmap(matSepia.cols(), matSepia.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(matSepia, bmSepia);
                Bitmap rotatedBmSepia = Bitmap.createBitmap(bmSepia, 0, 0, bmSepia.getWidth(), bmSepia.getHeight(), rotatedMatrix, true);
                ivSepia.setImageBitmap(rotatedBmSepia);

                //스케치
                Bitmap bmSketch = Bitmap.createBitmap(matSketch.cols(), matSketch.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(matSketch, bmSketch);
                Bitmap rotatedBmSketch = Bitmap.createBitmap(bmSketch, 0, 0, bmSketch.getWidth(), bmSketch.getHeight(), rotatedMatrix, true);
                ivSketch.setImageBitmap(rotatedBmSketch);

                //반전
                Bitmap bmInvert = Bitmap.createBitmap(matInvert.cols(), matInvert.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(matInvert, bmInvert);
                Bitmap rotatedBmInvert = Bitmap.createBitmap(bmInvert, 0, 0, bmInvert.getWidth(), bmInvert.getHeight(), rotatedMatrix, true);
                ivInvert.setImageBitmap(rotatedBmInvert);

                //그레이
                Bitmap bmGray = Bitmap.createBitmap(matGray.cols(), matGray.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(matGray, bmGray);
                Bitmap rotatedBmGray = Bitmap.createBitmap(bmGray, 0, 0, bmGray.getWidth(), bmGray.getHeight(), rotatedMatrix, true);
                ivGray.setImageBitmap(rotatedBmGray);


            }

        };


        //이미지 미리보기를 누를때 해당 필터 번호로 바뀐다.
        //ex) 원본은 0, 스케치는 2
        ivOriginal.setOnClickListener(v ->{
            Toast.makeText(getApplicationContext()  , "원본", Toast.LENGTH_LONG).show();
            filterChoice = 0;
        });


        ivSepia.setOnClickListener(v ->{
            Toast.makeText(getApplicationContext()  , "세피아", Toast.LENGTH_LONG).show();
            filterChoice = 1;
        });

        ivSketch.setOnClickListener(v ->{
            Toast.makeText(getApplicationContext()  , "스케치", Toast.LENGTH_LONG).show();
            filterChoice = 2;
        });
        ivInvert.setOnClickListener(v ->{
            Toast.makeText(getApplicationContext()  , "반전", Toast.LENGTH_LONG).show();
            filterChoice = 3;
        });

        ivGray.setOnClickListener(v ->{
            Toast.makeText(getApplicationContext()  , "그레이", Toast.LENGTH_LONG).show();
            filterChoice = 4;
        });


        imageCameraButton = findViewById(R.id.imageCameraButton);
        imageCameraButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    getWriteLock();

                    File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
                    path.mkdirs();
                    File file = new File(path, "image.jpg");

                    String filename = file.toString();

                    Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_BGR2RGBA);

                    //matResult 의 가로세로 바꾸어넣음..(원래 rows 다음에 colos 가 옴)
                    //돌린 사진을 저장하기 위해서
                    Mat saveMat = new Mat(matResult.cols(), matResult.rows(), matResult.type());

                    Bitmap bmDraw = Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(matResult, bmDraw);
                    //저장할때 안 돌아간 매트릭스로(프리뷰에서는 돌려놔서 제대로 되어있는 것 처럼 보이지만) 저장된다(돌아가 있음)
                    //그래서 저장할떄 회전 시켜서 저장해줘야 제대로 돌아간 상태로 저장됨
                    Bitmap rotatedbmDraw = Bitmap.createBitmap(bmDraw, 0, 0, bmDraw.getWidth(), bmDraw.getHeight(), rotatedMatrix, true);
                    Utils.bitmapToMat(rotatedbmDraw, saveMat);

                    boolean ret = Imgcodecs.imwrite(filename, saveMat);
                    if (ret) Log.d(TAG, "SUCESS");
                    else Log.d(TAG, "FAIL");


                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(file));
                    sendBroadcast(mediaScanIntent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                releaseWriteLock();

            }
        });

        //카메라 버튼 눌러을때 누른 느낌들게 효과줌
        imageCameraButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //눌렀을떄 원래 원 이미지도 보다 선이 굵은 원 이미지를 보여줘서 눌렸다는 느낌을 준다
                    //다만 너무 짧게 눌렀을때 티가 안나서 나중에 다른 방식으로 보완해서 구현하기
                    imageCameraButton.setImageDrawable(getResources().getDrawable(R.drawable.circle_click));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    imageCameraButton.setImageDrawable(getResources().getDrawable(R.drawable.circle));

                }

                return false;
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();


    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();


    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        try {
            getWriteLock();
            //카메라 프레임(매트릭스) 받아오기
            //rgba => rgb + 알파채널(투명도)
            matInput = inputFrame.rgba();


            if (matResult == null)
                matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());


            matSepia = matResult.clone();
            matSketch = matResult.clone();
            matInvert = matResult.clone();
            matGray = matResult.clone();


            //1. sepia 필터
            ConvertSepiaFilter(matInput.getNativeObjAddr(), matSepia.getNativeObjAddr());


            //2. 연필로 그린거 같은 필터(Sketch Filter =>  쓰레쉬 홀드 필터)
            ConvertSketchFilter(matInput.getNativeObjAddr(), matSketch.getNativeObjAddr());

            //3. 반전필터
            ConvertInvertFilter(matInput.getNativeObjAddr(), matInvert.getNativeObjAddr());

            //4. 그레이 필터
            ConvertRGBtoGray(matInput.getNativeObjAddr(), matGray.getNativeObjAddr());


            //필터 선택하는 값에 따라 알맞은 필터 적용
            switch (filterChoice) {

                case 1:
                    //1. sepia 필터
                    ConvertSepiaFilter(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
                    Log.w(TAG, "ConvertSepiaFilter: " +filterChoice);
                    break;

                case 2:
                    //2. 연필로 그린거 같은 필터(draw Filter/Sketch Filter => 쓰레쉬 홀드)
                    ConvertSketchFilter(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
                    Log.w(TAG, "ConvertSketchFilter: " +filterChoice);
                    break;

                case 3:
                    //3. 반전필터
                    ConvertInvertFilter(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
                    Log.w(TAG, "ConvertInvertFilter: " +filterChoice);
                    break;

                case 4:
                    //4. 그레이 필터
                    ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
                    Log.w(TAG, "ConvertRGBtoGray: " +filterChoice);
                    break;
                case 0:

                default:
                    matResult =matInput.clone();
                    break;




            }
            Log.w(TAG, "onCameraFrame: " +filterChoice);


            //2. 얼굴인식 필터
/*        Core.flip(matInput, matInput, 1);

        detect(cascadeClassifier_face,cascadeClassifier_eye, matInput.getNativeObjAddr(),
                matResult.getNativeObjAddr());*/


            //4. 뭉개지는 필터?
//            ConvertMorphologyFilter(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());

            //5. 뭉개지는 필터 ex?
//            ConvertMorphologyExFilter(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());

/*           //8. 박스 필터
            ConvertBoxFilter(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());*/

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        releaseWriteLock();

        // find the imageview and draw it!
        Message msg = previewHandler.obtainMessage();
        previewHandler.sendMessage(msg);


        return matResult;
    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
//        return Collections.singletonList(mOpenCvCameraView);
        List<? extends CameraBridgeViewBase> cameraList = asList(mOpenCvCameraView);
        return cameraList;
    }


    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase : cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
                read_cascade_file();
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
//            onCameraPermissionGrantedSample();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] ==
                PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
//            onCameraPermissionGrantedSample();
        } else {
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }


}
