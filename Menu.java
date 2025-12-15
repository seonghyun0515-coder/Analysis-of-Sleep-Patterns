package ac.kr.project;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class Menu extends AppCompatActivity {
    MaterialCardView score, calendar, alarm, tip,analyze,AiTip;
    BottomNavigationView bottomNavigationView;
    private TextView tvTodayScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
// 1. 오늘의 점수 TextView 연결
        tvTodayScore = findViewById(R.id.tvTodayScore);

        // 2. 저장된 점수 불러오기
        loadTodayScore();

        // (예) 점수 카드 클릭 시 Score 화면으로 이동
        findViewById(R.id.score).setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, Score.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 점수 화면 다녀오면 다시 업데이트
        loadTodayScore();
    }

    // ⭐ 저장된 점수 불러와서 표시하는 함수
    private void loadTodayScore() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int lastScore = prefs.getInt("lastSleepScore", -1);

        if (lastScore == -1) {
            tvTodayScore.setText("—");
        } else {
            tvTodayScore.setText(lastScore + "점");
        }
        // SharedPreferences에서 로그인 상태 확인
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        // 로그인되지 않은 경우, 로그인 화면으로 이동
        if (!isLoggedIn) {
            Intent intent = new Intent(this, MainActivity.class);  // 로그인 화면
            startActivity(intent);
            finish();  // 현재 Activity 종료
            return;  // 뒤에 코드가 실행되지 않도록 return
        }

        // 로그인 상태인 경우 메뉴 화면 초기화
        score = findViewById(R.id.score);
        analyze =findViewById(R.id.analyze);
        calendar = findViewById(R.id.calendar);
        alarm = findViewById(R.id.alarm);
        tip = findViewById(R.id.tip);
        AiTip = findViewById(R.id.AiTip);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        analyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Analyze.class);
                startActivity(intent);
            }
        });

        score.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Score.class);
                startActivity(intent);
            }
        });

        calendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Calendar.class);
                startActivity(intent);
            }
        });

        alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Alarm.class);
                startActivity(intent);
            }
        });

        tip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Tip.class);
                startActivity(intent);
            }
        });
        AiTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SleepTrackingActivity.class);
                startActivity(intent);
            }
        });
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                // '홈' 탭 클릭 (현재 화면)
                return true;
            } else if (item.getItemId() == R.id.nav_stats) {
                // '통계' 탭 클릭 (나중에 구현)
                Toast.makeText(this, "통계 (준비중)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                // '설정' 탭 클릭 (나중에 구현)
                Toast.makeText(this, "설정 (준비중)", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }
    }

