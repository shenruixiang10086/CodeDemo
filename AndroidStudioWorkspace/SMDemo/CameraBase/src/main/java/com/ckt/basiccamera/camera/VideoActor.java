package com.ckt.basiccamera.camera;

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ckt.basiccamera.R;
import com.ckt.basiccamera.log.Log;
import com.ckt.basiccamera.logic.VideoSaveRequest;

public class VideoActor extends Actor {
    public static final String TAG = "VideoActor";
    private MediaRecorder mMediaRecorder;
    protected long mRecordingStartTime;
    protected TextView mRecordingTime;
    public static boolean isRecord = false;
    protected CamcorderProfile mProfile = null;
    /* recorded duration */
    protected long deltaTime;
    protected final int defaultQuality = 111;
    protected int quality = defaultQuality;
    protected boolean recordAudio = true;

    // 4G - 50M
    private static final long MAX_FILE_SIZE = (4 * 1024L * 1024 * 1024 - 50 * 1024 * 1024);
    protected long deltaTimeExtra = 0;

    protected final static int STATUS_IDLE = 0;
    protected final static int STATUS_RECORDING = 1;
    protected final static int STATUS_PAUSE = 2;
    protected final static int STATUS_STOP = 3;
    private int videoStatus = STATUS_IDLE;
    protected static final int UPDATE_RECORD_TIME = 0;
    protected long mRecordedTime;
    private ImageView videoBtn;

    // private Context mContext;
    public VideoActor(Context context, ViewGroup container) {
        super(context, container);
    }
    @Override
    protected void initContentView() {
        if (modeContentView == null) {
            modeContentView = LayoutInflater.from(mContext).inflate(
                    R.layout.camera_video, null);
            videoBtn = (ImageView) modeContentView.findViewById(R.id.videoBtn);
            mRecordingTime = (TextView) modeContentView.findViewById(R.id.recording_time);
            mRecordingTime.setVisibility(View.GONE);
        }
    }

    protected void initCamCorderProfile() {
        if(mProfile == null){
            mProfile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_HIGH);
        }
    }

    public void start() {
        super.start();
        videoBtn.setOnClickListener(onClicklistener);
    }

    @Override
    public void stop() {
        super.stop();
        videoBtn.setOnClickListener(null);
    }

    public void updateCameraParamters() {
        super.updateCameraParamters();
        initCamCorderProfile();
        Log.e(TAG, "updateCameraParamters,mCamera=" + mCamera);
        if (mCamera != null) {
            Parameters p = mCamera.getParameters();
            Size previewSize = getMatchedSize(p.getSupportedPreviewSizes(),
                    mProfile.videoFrameWidth, mProfile.videoFrameHeight);
            previewWidth = previewSize.width;
            previewHeight = previewSize.height;
            android.util.Log.i("srx", "updateCameraParamters: previewWidth=="+previewWidth);
            android.util.Log.i("srx", "updateCameraParamters: previewHeight=="+previewHeight);
            p.setPreviewSize(previewWidth, previewHeight);


            List<String> focusModes = p.getSupportedFocusModes();
            for (String tmp : focusModes) {
                if (Parameters.FOCUS_MODE_CONTINUOUS_VIDEO.equals(tmp)) {
                    p.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    break;
                }
            }
            p.setPreviewFrameRate(30);
            p.setRecordingHint(true);
            p.setAntibanding("auto");
            mCamera.setParameters(p);
            Log.i(TAG, "previewSize=" + previewWidth + "," + previewHeight);
        }
        mCameraLogic.stopPreview();
        mCameraLogic.startPreview();
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        mMediaRecorder = null;
    }

    protected void updateRecordingTime() {
        if (videoStatus == STATUS_RECORDING) {
            long now = SystemClock.uptimeMillis();
            long delta = now - mRecordingStartTime;
            deltaTime = delta;
            mRecordingTime.setText(formatTime(deltaTimeExtra + deltaTime));
            mainHandler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME, 1000);
        }
    }

    protected String formatTime(long time) {
        int second = (int) (time / 1000);
        int hour = second / 3600 % 100;
        int minute = second / 60 % 60;
        second = second % 60;
        String format = "%02d:%02d:%02d";
        String result = String.format(Locale.ENGLISH, format, hour, minute,
                second);
        return result;
    }

    private MediaRecorder.OnErrorListener mOnErrorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            Log.e(TAG, "onError what=" + what);
            if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
                // We may have run out of space on the sdcard.
                Log.e(TAG,
                        "onError MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN stopRecord");
                stopRecord();
            }
        }

    };
    private MediaRecorder.OnInfoListener mOnInfoListener = new MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            Log.i(TAG, "what = " + what);
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                stopRecord();
            } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                stopRecord();
                deltaTimeExtra = 0;
            }
        }

    };

    protected Handler mainHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case UPDATE_RECORD_TIME: {
                updateRecordingTime();
                break;
            }
            }
        }
    };

    private OnClickListener onClicklistener = new OnClickListener() {
        public void onClick(View view) {
            if (videoStatus == STATUS_IDLE) {
                startRecord();
            } else if (videoStatus == STATUS_RECORDING) {
                stopRecord();
            }
        }
    };

    protected boolean startRecord() {
        Log.i(TAG, "startRecord");
        if (mCameraLogic.isReleased()) {
            return false;
        }
        if (mCamera == null || mProfile == null) {
            Log.i(TAG, "mcamera = " + mCamera + "  mProfile = " + mProfile);
            return false;
        }
        deltaTime = 0;
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        int audioSource = MediaRecorder.AudioSource.CAMCORDER;
        audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        mMediaRecorder.setAudioSource(audioSource);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(mProfile.fileFormat);
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        mMediaRecorder.setVideoSize(mProfile.videoFrameWidth,
                mProfile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
        mMediaRecorder.setVideoEncoder(mProfile.videoCodec);
        mMediaRecorder.setAudioEncodingBitRate(mProfile.audioBitRate);
        mMediaRecorder.setAudioChannels(mProfile.audioChannels);
        mMediaRecorder.setAudioSamplingRate(mProfile.audioSampleRate);
        mMediaRecorder.setAudioEncoder(mProfile.audioCodec);

        long maxFileSize = MAX_FILE_SIZE;

        try {
            mMediaRecorder.setMaxFileSize(maxFileSize);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "setMaxFileSize:" + e.getMessage());
        }

        deltaTimeExtra = 0;
        request = new VideoSaveRequest();
        request.prepareRequest(mFileNamer.getVideoPath(), mFileNamer
                .getVideoName(request.getDateTaken(), mProfile.videoFrameRate));
        request.setSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        mMediaRecorder.setOrientationHint(90);
        mMediaRecorder.setOutputFile(request.getTemporaryPath());
        mMediaRecorder.setOnErrorListener(mOnErrorListener);
        mMediaRecorder.setOnInfoListener(mOnInfoListener);
        mRecordingTime.setVisibility(View.VISIBLE);
        Log.i(TAG, "video file name : " + request.getTemporaryPath());
        try {
            isRecord = true;
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            videoStatus = STATUS_RECORDING;
            mRecordingStartTime = SystemClock.uptimeMillis();
            updateRecordingTime();
            Log.e(TAG, "mMediaRecorder start record");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "start:" + e.getMessage());
            e.printStackTrace();
            stopRecord();
        }
        return false;
    }

    protected  void stopRecord() {
        Log.d(TAG, "stopRecord");
        if (!isRecord) {
            return;
        }
        isRecord = false;
        mRecordedTime = 0;
        mRecordingTime.setVisibility(View.GONE);
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setOnInfoListener(null);
            try {
                mMediaRecorder.stop();
                Log.e(TAG, "mMediaRecorder stop record");
            } catch (RuntimeException e) {
                Log.e(TAG, "catched RuntimeException e : " + e.getMessage());
            }
        }
        releaseMediaRecorder();
        mFileSaver.save(request);
        videoStatus = STATUS_IDLE;
    }
}
