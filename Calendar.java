package ac.kr.project;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Calendar extends AppCompatActivity {

    private CalendarView calendarView;
    private EditText etSleepScore;
    private Button btnSaveScore;
    private TextView tvSavedScore;

    private String selectedDate;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        calendarView = findViewById(R.id.calendarView);
        etSleepScore = findViewById(R.id.etSleepScore);
        btnSaveScore = findViewById(R.id.btnSaveScore);
        tvSavedScore = findViewById(R.id.tvSavedScore);

        prefs = getSharedPreferences("SleepScores", MODE_PRIVATE);

        // 초기 날짜 설정
        selectedDate = getDateString(calendarView.getDate());
        showSavedScore(selectedDate);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            showSavedScore(selectedDate);
        });

        btnSaveScore.setOnClickListener(v -> {
            String score = etSleepScore.getText().toString().trim();
            if (!score.isEmpty()) {
                prefs.edit().putString(selectedDate, score).apply();
                Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show();
                showSavedScore(selectedDate);
            } else {
                Toast.makeText(this, "점수를 입력하세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSavedScore(String date) {
        String score = prefs.getString(date, null);
        if (score != null) {
            tvSavedScore.setText(date + "의 수면 점수: " + score);
        } else {
            tvSavedScore.setText(date + "에 저장된 수면 점수가 없습니다.");
        }
    }

    private String getDateString(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(millis));
    }
}
