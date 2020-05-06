package com.wl.harddecodeapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.wl.function.AudioDecoder;
import com.wl.function.VideoDecoder;
import com.wl.function.VideoDecoderUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "HardDecodeApp";

    private final static String PATH = Environment.getExternalStorageDirectory() + File.separator + "filefilm" + File.separator + "mediatest2.mp4";
    private Button mPlayVideo;
    private Button mInitVideo;

    private VideoDecoder videoDecoder;
    private AudioDecoder audioDecoder;
    private SurfaceView mSurfaceView;
    private ExecutorService executorService;
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//始终竖屏
        setContentView(R.layout.activity_main);
        mPlayVideo = findViewById(R.id.play_video);
        mPlayVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlayer();
            }
        });

        mInitVideo = findViewById(R.id.init_player);
        mInitVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initPlayer();
            }
        });
        mSurfaceView = findViewById(R.id.surface_view);
        videoDecoder = new VideoDecoder(PATH, mSurfaceView);
        audioDecoder = new AudioDecoder(PATH);
        requestMyPermissions();
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated: ");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged: ");
                setSurface(holder.getSurface());
                setSurfaceSize(width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed: ");
            }
        });

        videoDecoder.setDataCallBack(new VideoDecoderUtils.DataCallBack() {
            @Override
            public void onFrame(byte[] data, int width, int height) {
                rendSurface(data, width, height);
            }
        });
    }

    private void startPlayer() {
        videoDecoder.goOn();
        audioDecoder.goOn();
    }

    private void initPlayer() {
        executorService = Executors.newFixedThreadPool(10);
        executorService.execute(videoDecoder);
        executorService.execute(audioDecoder);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        videoDecoder.pause();
        audioDecoder.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoDecoder.stop();
        audioDecoder.stop();
        executorService.shutdown();
        releaseSurface();
    }

    private void requestMyPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            Log.d(TAG, "requestMyPermissions: 请求写SD权限");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            Log.d(TAG, "requestMyPermissions: 有写SD权限");
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            Log.d(TAG, "requestMyPermissions: 请求读SD权限");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        } else {
            Log.d(TAG, "requestMyPermissions: 有读SD权限");
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void setSurface(Surface surface);
    public native void setSurfaceSize(int width, int height);
    public native void rendSurface(byte[] data,int width,int height);
    public native void releaseSurface();
}
