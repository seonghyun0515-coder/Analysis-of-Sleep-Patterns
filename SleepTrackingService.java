package ac.kr.project;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class SleepTrackingService extends Service implements SensorEventListener {

    private static final String TAG = "SleepTrackingService";
    private static final String CHANNEL_ID = "sleep_tracking_channel";

    public static final String ACTION_START = "ac.kr.project.action.START";
    public static final String ACTION_STOP  = "ac.kr.project.action.STOP";

    // sensors
    private SensorManager sensorManager;
    private Sensor accelSensor;
    private final Object accelLock = new Object();
    private ArrayList<float[]> accelWindow = new ArrayList<>();

    // audio
    private AudioRecord audioRecord;
    private volatile boolean audioRunning = false;
    private int audioSampleRate = 16000;
    private HandlerThread audioThread;
    private Handler audioHandler;
    private final ArrayList<Short> audioSamples = new ArrayList<>();

    // processing
    private HandlerThread procThread;
    private Handler procHandler;
    private static final int PROCESS_INTERVAL_MS = 15_000;

    // session
    private long sessionStartTime = 0L;

    // smoothing + warmup + stabilization
    private float lastMovRms = 0f;
    private boolean warmUp = false;
    private long warmUpUntil = 0L;
    private int lastRawStage = -1;
    private int stableCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        createNotificationChannel();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        procThread = new HandlerThread("sleep_processing");
        procThread.start();
        procHandler = new Handler(procThread.getLooper());

        audioThread = new HandlerThread("audio_thread");
        audioThread.start();
        audioHandler = new Handler(audioThread.getLooper());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "수면 측정 서비스",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("수면 측정 중")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: " + (intent != null ? intent.getAction() : "null"));

        if (intent == null) return START_STICKY;

        String action = intent.getAction();

        if (ACTION_START.equals(action)) {

            if (sessionStartTime == 0) {
                sessionStartTime = System.currentTimeMillis();
                SleepDataHolder.clear();
                SleepDataHolder.setSessionStart(sessionStartTime);
            }

            try {
                startForeground(1, buildNotification("수면 데이터를 측정하는 중입니다"));
            } catch (Exception e) {
                Log.e(TAG, "startForeground 예외: " + e.getMessage(), e);
            }

            startSensors();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startAudio();
            } else {
                Log.w(TAG, "RECORD_AUDIO 권한 없음 — 오디오 수집 건너뜀 (움직임 전용)");
            }

            // warmup 1분
            warmUp = true;
            warmUpUntil = System.currentTimeMillis() + 60_000L;
            lastMovRms = 0f;
            lastRawStage = -1;
            stableCount = 0;

            startProcessingLoop();

        } else if (ACTION_STOP.equals(action)) {

            stopProcessingLoop();
            stopAudio();
            stopSensors();

            long end = System.currentTimeMillis();
            SleepDataHolder.setSessionEnd(end);

            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    // sensors
    private void startSensors() {
        try {
            if (sensorManager != null && accelSensor != null) {
                sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
                Log.i(TAG, "가속도 센서 등록됨");
            } else {
                Log.w(TAG, "가속도 센서를 찾을 수 없음");
            }
        } catch (Exception e) {
            Log.e(TAG, "startSensors 예외: " + e.getMessage(), e);
        }
    }

    private void stopSensors() {
        try {
            if (sensorManager != null)
                sensorManager.unregisterListener(this);

            synchronized (accelLock) {
                accelWindow.clear();
            }
            Log.i(TAG, "가속도 센서 해제");
        } catch (Exception e) {
            Log.e(TAG, "stopSensors 예외: " + e.getMessage(), e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float ax = event.values[0];
        float ay = event.values[1];
        float az = event.values[2];

        synchronized (accelLock) {
            accelWindow.add(new float[]{ax, ay, az});
            if (accelWindow.size() > 600) accelWindow.remove(0); // 최근 데이터 유지
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // audio
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startAudio() {
        if (audioRunning) return;
        if (audioHandler == null) return;

        audioSampleRate = getAvailableSampleRate();
        int minBuf = AudioRecord.getMinBufferSize(audioSampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBuf <= 0) return;
        final int bufferSize = Math.max(minBuf, audioSampleRate);

        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, audioSampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        } catch (Exception e) {
            Log.e(TAG, "AudioRecord 생성 실패: " + e.getMessage(), e);
            audioRecord = null;
        }

        if (audioRecord == null) return;
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            try { audioRecord.release(); } catch (Exception ignored) {}
            audioRecord = null;
            return;
        }

        try {
            audioRecord.startRecording();
        } catch (Exception e) {
            try { audioRecord.release(); } catch (Exception ignored) {}
            audioRecord = null;
            return;
        }

        audioRunning = true;
        audioHandler.postDelayed(audioLoop, 50);
    }

    private void stopAudio() {
        audioRunning = false;
        if (audioHandler != null) audioHandler.removeCallbacks(audioLoop);

        if (audioRecord != null) {
            try { audioRecord.stop(); } catch (Exception ignored) {}
            try { audioRecord.release(); } catch (Exception ignored) {}
            audioRecord = null;
        }

        synchronized (audioSamples) { audioSamples.clear(); }
        Log.i(TAG, "오디오 중지");
    }

    private final Runnable audioLoop = new Runnable() {
        @Override
        public void run() {
            if (!audioRunning || audioRecord == null) return;

            short[] buf = new short[1024];
            int r = 0;
            try { r = audioRecord.read(buf, 0, buf.length); }
            catch (Exception e) {
                Log.e(TAG, "audio read 예외: " + e.getMessage(), e);
                audioRunning = false;
                try { audioRecord.release(); } catch (Exception ignored) {}
                audioRecord = null;
                return;
            }

            if (r > 0) {
                synchronized (audioSamples) {
                    for (int i = 0; i < r; i++) audioSamples.add(buf[i]);
                    int max = audioSampleRate * 30; // 30초
                    if (audioSamples.size() > max) {
                        int remove = audioSamples.size() - max;
                        for (int i = 0; i < remove; i++) audioSamples.remove(0);
                    }
                }
            }

            if (audioRunning && audioHandler != null) {
                audioHandler.postDelayed(this, 50);
            }
        }
    };

    // processing loop
    private void startProcessingLoop() {
        if (procHandler == null) return;
        procHandler.post(processRunnable);
        Log.i(TAG, "분석 루프 시작");
    }

    private void stopProcessingLoop() {
        if (procHandler != null) procHandler.removeCallbacks(processRunnable);
        Log.i(TAG, "분석 루프 중지");
    }

    private final Runnable processRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                analyzeWindow();
            } catch (Exception e) {
                Log.e(TAG, "analyzeWindow 예외: " + e.getMessage(), e);
            } finally {
                if (procHandler != null) procHandler.postDelayed(this, PROCESS_INTERVAL_MS);
            }
        }
    };

    // analysis
    private void analyzeWindow() {
        long now = System.currentTimeMillis();

        float movRms = computeMovementRMS();
        movRms = smoothMovement(movRms); // low-pass smoothing

        int breathRate = estimateBreathRate();

        // classify with warmup + breath guard + stabilization
        int rawStage = rawClassify(movRms, breathRate, now);
        int stage = stabilizeStage(rawStage);

        long start = now - PROCESS_INTERVAL_MS;
        long end = now;

        // create stage object and add (SleepDataHolder.addStage will merge if same stage)
        SleepDataHolder.addStage(new SleepStage(start, end, stage, movRms, breathRate));

        Log.d(TAG, "analyzeWindow: stage=" + stage + " mov=" + movRms + " breath=" + breathRate);
    }

    private float computeMovementRMS() {
        float movRms = 0f;
        int count = 0;

        synchronized (accelLock) {
            for (float[] v : accelWindow) {
                float mag = (float) Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
                float g = Math.abs(mag - 9.806f);
                movRms += g * g;
                count++;
            }
        }

        if (count > 0) movRms = (float) Math.sqrt(movRms / count);
        return movRms;
    }

    // smoothing: simple IIR low-pass
    private float smoothMovement(float raw) {
        if (lastMovRms == 0f) {
            lastMovRms = raw;
        } else {
            // alpha = 0.2 (new), 0.8 (old)
            lastMovRms = lastMovRms * 0.8f + raw * 0.2f;
        }
        return lastMovRms;
    }

    // breath estimation (same as previous)
    private int estimateBreathRate() {
        short[] copy;
        synchronized (audioSamples) {
            copy = new short[audioSamples.size()];
            for (int i = 0; i < audioSamples.size(); i++)
                copy[i] = audioSamples.get(i);
        }

        if (copy.length < audioSampleRate * 6) // 적어도 6초 데이터 필요
            return 0;

        int frame = audioSampleRate / 2;  // 0.5초
        ArrayList<Double> env = new ArrayList<>();

        for (int i = 0; i + frame <= copy.length; i += frame / 2) {
            double sum = 0;
            for (int j = 0; j < frame; j++)
                sum += copy[i + j] * copy[i + j];
            env.add(Math.log10(sum + 1));
        }

        int peaks = 0;
        for (int i = 1; i < env.size() - 1; i++) {
            double v = env.get(i);
            if (v > env.get(i - 1) && v > env.get(i + 1) && v > -1) peaks++;
        }

        double sec = copy.length / (double) audioSampleRate;
        if (sec <= 0) return 0;
        return (int) Math.round(peaks / sec * 60.0);
    }

    // raw classification (before stabilization)
    private int rawClassify(float movement, int breathRate, long now) {
        // warmup: first minute => Awake
        if (warmUp && now < warmUpUntil) {
            return 0;
        }

        // strong movement -> Awake
        if (movement > 1.3f) return 0;

        // if no breath info, be conservative: do not return REM/Deep
        if (breathRate == 0) {
            if (movement < 0.15f) return 1; // Light rather than Deep
            if (movement < 0.8f) return 1;  // Light
            return 0;
        }

        // REM: low movement + higher breath (typical REM breathing slightly faster/irregular)
        if (movement < 0.20f && breathRate >= 16 && breathRate <= 26) return 3;

        // Deep: very low movement + slower breath
        if (movement < 0.10f && breathRate >= 8 && breathRate <= 14) return 2;

        // Light
        if (movement < 0.6f) return 1;

        return 0;
    }

    // stabilization: require 2 consecutive identical rawStage to confirm Deep/REM, otherwise soften to Light
    private int stabilizeStage(int rawStage) {
        if (rawStage == lastRawStage) {
            stableCount++;
        } else {
            lastRawStage = rawStage;
            stableCount = 1;
        }

        // for Deep(2) and REM(3) we require stableCount >= 2
        if ((rawStage == 2 || rawStage == 3) && stableCount < 2) {
            return 1; // soften to Light until stable
        }

        // Awake or Light can appear immediately
        return rawStage;
    }

    private int getAvailableSampleRate() {
        int[] rates = {44100, 22050, 16000, 11025, 8000};
        for (int rate : rates) {
            int buffer = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (buffer > 0) return rate;
        }
        return 16000;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopProcessingLoop();
        stopAudio();
        stopSensors();

        if (audioThread != null) {
            try { audioThread.quitSafely(); } catch (Exception ignored) {}
            audioThread = null;
            audioHandler = null;
        }
        if (procThread != null) {
            try { procThread.quitSafely(); } catch (Exception ignored) {}
            procThread = null;
            procHandler = null;
        }

        super.onDestroy();
        Log.i(TAG, "Service destroyed");
    }
}
