package com.hitsz.autotrack;

import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.support.v4.app.Fragment;

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView.OnCameraListener;

import java.util.ArrayList;
import java.util.List;

public class DetecterActivity extends Fragment implements OnCameraListener, View.OnTouchListener, Camera.AutoFocusCallback {
	private final String TAG = this.getClass().getSimpleName();

	private int mWidth, mHeight, mFormat;
	private CameraSurfaceView mSurfaceView;
	private CameraGLSurfaceView mGLSurfaceView;
	private Camera mCamera;

	AFT_FSDKVersion version = new AFT_FSDKVersion();
	AFT_FSDKEngine engine = new AFT_FSDKEngine();
	List<AFT_FSDKFace> result = new ArrayList<>();

	byte[] mImageNV21 = null;
	AFT_FSDKFace mAFT_FSDKFace = null;
    MainActivity mainActivity;
	Handler mHandler;
	Handler blueHandler;
    private int right;
    private int bottom;
    private Rect rectangle;
    private String horizon;
    private String vertical;
    //long mill1;
    //long mill2;

	Runnable hide = new Runnable() {
		@Override
		public void run() {
		}
	};

	private  TextView mTextView2;

	@Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        //Toast.makeText(getActivity(), "hhhh", Toast.LENGTH_SHORT).show();
	    return inflater.inflate(R.layout.activity_camera,container,false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        //Toast.makeText(getActivity(), screenHeight+","+screenWidth, Toast.LENGTH_SHORT).show();
        mHandler = mainActivity.getUiHandler();
        blueHandler = mainActivity.getbHandler();
        mGLSurfaceView = (CameraGLSurfaceView) view.findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setOnTouchListener(this);
        mSurfaceView = (CameraSurfaceView) view.findViewById(R.id.surfaceView);
        mSurfaceView.setOnCameraListener(this);
        mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, true, 270);
        mSurfaceView.debug_print_fps(true, false);
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        mTextView2 = (TextView) view.findViewById(R.id.textView2);
        mWidth = 1280;
        mHeight = 960;
        mFormat = ImageFormat.NV21;

        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        //err = engine.AFT_FSDK_GetVersion(version);
        //Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());
    }

    @Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());
	}

	@Override
	public Camera setupCamera() {
		// TODO Auto-generated method stub

		try {
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(mWidth, mHeight);
			parameters.setPreviewFormat(mFormat);

			for( Camera.Size size : parameters.getSupportedPreviewSizes()) {
				Log.d(TAG, "SIZE:" + size.width + "x" + size.height);
			}
			for( Integer format : parameters.getSupportedPreviewFormats()) {
				Log.d(TAG, "FORMAT:" + format);
			}

			List<int[]> fps = parameters.getSupportedPreviewFpsRange();
			for(int[] count : fps) {
				Log.d(TAG, "T:");
				for (int data : count) {
					Log.d(TAG, "V=" + data);
				}
			}
			mCamera.setParameters(parameters);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (mCamera != null) {
			mWidth = mCamera.getParameters().getPreviewSize().width;
			mHeight = mCamera.getParameters().getPreviewSize().height;
		}
		return mCamera;
	}

	@Override
	public void setupChanged(int format, int width, int height) {

	}

	@Override
	public boolean startPreviewLater() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
		AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
		Log.d(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
		Log.d(TAG, "Face=" + result.size());
		/*for (AFT_FSDKFace face : result) {
			Log.d(TAG, "Face:" + face.toString());
		}*/
		if (mImageNV21 == null) {
			if (!result.isEmpty()) {
				mAFT_FSDKFace = result.get(0).clone();

				mImageNV21 = data.clone();
			} else {
				mHandler.postDelayed(hide, 500);
			}
		}
		//copy rects
		Rect[] rects = new Rect[result.size()];
		/*mill1 = mill2;
        mill2 = System.currentTimeMillis();
        Log.d(TAG, "onPreview: "+(mill2-mill1));*/
		for (int i = 0; i < result.size(); i++) {
			rects[i] = new Rect(result.get(i).getRect());
		}
		if(result.size()!=0) {
            rectangle = result.get(0).getRect();
            right = (rectangle.left + rectangle.right) / 2;
            bottom = (rectangle.top + rectangle.bottom) / 2;
            mHandler.removeCallbacks(hide);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mTextView2.setText("坐标值：" + right + "," + bottom);
                }
            });
            if(bottom > 510){
                if(bottom > 750){
                    horizon = "i";
                }else if(bottom >650){
                    horizon = "h";
                }else if(bottom > 550){
                    horizon = "g";
                }else{
                    horizon = "f";
                }
            }else if(bottom < 390){
                if(bottom < 150){
                    horizon = "e";
                }else if(bottom < 250){
                    horizon = "d";
                }else if(bottom < 350){
                    horizon = "c";
                }else {
                    horizon = "b";
                }
            }else{
                horizon = "a";
            }
            if(right > 660){
                if(right > 1000){
                    vertical = "o";
                }else if(right > 900){
                    vertical = "n";
                }else if(right > 750){
                    vertical = "m";
                }else {
                    vertical = "l";
                }
            }else if(right < 540){
                if(right < 200){
                    vertical = "u";
                }else if(right < 300){
                    vertical = "t";
                }else if(right < 450){
                    vertical = "s";
                }else {
                    vertical = "r";
                }
            }else{
                vertical = "a";
            }
            Message message = new Message();
            message.what = Params.DETECT_CONNECT;
            message.obj = horizon+""+vertical;
            blueHandler.sendMessage(message);
        }
		//clear result.
		result.clear();
		//return the rects for render.
		return rects;
	}

	@Override
	public void onBeforeRender(CameraFrameData data) {

	}

	@Override
	public void onAfterRender(CameraFrameData data) {
		mGLSurfaceView.getGLES2Render().draw_rect((Rect[])data.getParams(), Color.GREEN, 2);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		CameraHelper.touchFocus(mCamera, event, v, this);
		return false;
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if (success) {
			Log.d(TAG, "Camera Focus SUCCESS!");
		}
	}
}
