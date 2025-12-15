package ac.kr.project;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AnalyzeDetail extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyzedetail);

        // (수정) XML에 있는 TextView를 ID로 찾기
        TextView textView = findViewById(R.id.tvDetailText);

        // (기존 코드)
        String tipDetail = getIntent().getStringExtra("tip_text");
        textView.setText(tipDetail != null ? tipDetail : "내용 없음");
    }
}
