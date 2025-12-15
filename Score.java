package ac.kr.project;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Score extends AppCompatActivity {

    // 1. UI 변수 선언
    private EditText etSleepHours, etEfficiency;
    private Button btnCalculate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score);

        // 2. XML의 ID와 연결
        etSleepHours = findViewById(R.id.etSleepHours); // 수면 시간 입력창
        etEfficiency = findViewById(R.id.etEfficiency); // 수면 효율 입력창
        btnCalculate = findViewById(R.id.btnCalculate); // 계산 버튼

        // 3. 버튼 클릭 리스너 설정
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateAndSaveScore();
            }
        });
    }

    // 4. 점수 계산 및 저장 함수
    private void calculateAndSaveScore() {
        // 입력된 텍스트 가져오기
        String strHours = etSleepHours.getText().toString().trim();
        String strEff = etEfficiency.getText().toString().trim();

        // 빈 칸 확인
        if (strHours.isEmpty() || strEff.isEmpty()) {
            Toast.makeText(this, "수면 시간과 효율을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 숫자로 변환
            float hours = Float.parseFloat(strHours);
            int efficiency = Integer.parseInt(strEff);

            // --- [점수 계산 로직] ---
            // 1. 수면 시간 점수 (8시간을 50점 만점으로 계산)
            // 예: 6시간 잤으면 (6/8)*50 = 37.5점
            int scoreTime = (int) ((hours / 8.0) * 50);
            if (scoreTime > 50) scoreTime = 50; // 최대 50점 제한

            // 2. 수면 효율 점수 (100%를 50점 만점으로 계산)
            // 예: 효율 90%면 (90/100)*50 = 45점
            int scoreEff = (int) ((efficiency / 100.0) * 50);
            if (scoreEff > 50) scoreEff = 50; // 최대 50점 제한

            // 3. 총점 합산
            int totalScore = scoreTime + scoreEff;

            // 100점을 넘지 않도록 조정 (보너스 점수 등이 없으므로)
            if (totalScore > 100) totalScore = 100;
            if (totalScore < 0) totalScore = 0;

            // --- [저장 로직] ---
            // "UserPrefs"라는 이름의 저장소에 "lastSleepScore"라는 이름으로 점수 저장
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("lastSleepScore", totalScore);
            editor.apply(); // 저장 확정

            SharedPreferences calPrefs = getSharedPreferences("SleepScores", MODE_PRIVATE);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            calPrefs.edit().putString(today, String.valueOf(totalScore)).apply();

            // 결과 안내 및 화면 종료
            Toast.makeText(this, "점수(" + totalScore + "점)가 저장되었습니다!", Toast.LENGTH_SHORT).show();
            finish(); // 현재 화면 닫고 메뉴(Menu) 화면으로 돌아가기

        } catch (NumberFormatException e) {
            Toast.makeText(this, "올바른 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
        }
    }
}