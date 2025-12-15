package ac.kr.project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SleepTrackingActivity extends AppCompatActivity {

    private Button btnStartSleep, btnStopSleep;
    private TextView tvStatus;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_tracking);

        btnStartSleep = findViewById(R.id.btnStartSleep);
        btnStopSleep = findViewById(R.id.btnStopSleep);
        tvStatus = findViewById(R.id.tvStatus);

        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        if (!isFinishing()) {
                            startService();
                        }
                    } else {
                        tvStatus.setText("마이크 권한 필요");
                    }
                });
        btnStartSleep.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                return;
            }
            startService();
        });

        btnStopSleep.setOnClickListener(v -> {
            stopServiceAndShowResult();
        });

        // 초기 버튼 상태
        btnStartSleep.setEnabled(true);
        btnStopSleep.setEnabled(false);
    }

    private void startService() {
        Intent i = new Intent(this, SleepTrackingService.class);
        i.setAction(SleepTrackingService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        // UI
        tvStatus.setText("측정 중...");
        btnStartSleep.setEnabled(false);
        btnStopSleep.setEnabled(true);
    }

    private void stopServiceAndShowResult() {
        Intent i = new Intent(this, SleepTrackingService.class);
        i.setAction(SleepTrackingService.ACTION_STOP);
        startService(i);

        // 측정 종료 시간 기록
        long s = SleepDataHolder.getSessionStart();
        long e = System.currentTimeMillis();
        SleepDataHolder.setSessionEnd(e);

        // ★ 1분 미만 감지하여 보정 적용
        SleepDataHolder.adjustForShortSession();

        // 결과 화면으로 이동
        Intent r = new Intent(this, SleepResultActivity.class);
        r.putExtra("startTime", s);
        r.putExtra("endTime", e);
        startActivity(r);

        finish();
    }
}
