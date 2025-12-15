package ac.kr.project;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;

public class AnalyzeTip extends AppCompatActivity {

    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyzetip);

        layout = findViewById(R.id.layoutTips);

        loadTips();
    }

    private void loadTips() {
        SharedPreferences prefs = getSharedPreferences("AnalyzeTips", MODE_PRIVATE);
        String tipsJson = prefs.getString("tips_json", "[]");

        try {
            JSONArray tipsArray = new JSONArray(tipsJson);

            // 기존에 있던 뷰 제거
            layout.removeAllViews();

            // 팁 버튼들 추가
            for (int i = 0; i < tipsArray.length(); i++) {
                String tip = tipsArray.getString(i);
                addTipButton(i + 1, tip);
            }

            // 초기화 버튼 추가 (맨 아래)
            addClearButton();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "팁 로딩 중 오류 발생", Toast.LENGTH_SHORT).show();
        }
    }

    private void addTipButton(int index, String tipText) {
        MaterialButton button = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText("분석 결과 #" + index);

        // 1. 레이아웃 파라미터 설정 (여백 추가)
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        // 하단 여백 16dp (16 * (화면밀도))
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        params.setMargins(0, 0, 0, margin);
        button.setLayoutParams(params);

        // 3. 'PRO Sleep' 스타일 적용 (Java 코드로)
        int accentColor = ContextCompat.getColor(this, R.color.pro_sleep_accent); // (예: #7FFFD4)
        button.setTextColor(accentColor);
        button.setStrokeColor(ColorStateList.valueOf(accentColor));
        button.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density)); // 2dp
        button.setCornerRadius((int) (8 * getResources().getDisplayMetrics().density)); // 8dp
        button.setTextSize(16);

        // 4. 클릭 리스너 (기존과 동일)
        button.setOnClickListener(v -> {
            Intent intent = new Intent(AnalyzeTip.this, AnalyzeDetail.class);
            intent.putExtra("tip_text", tipText);
            startActivity(intent);
        });
        layout.addView(button);
    }

    private void addClearButton() {
        MaterialButton clearBtn = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        clearBtn.setText("기록 초기화"); // "=====" 대신 깔끔하게 변경

        // 1. 레이아웃 파라미터 설정 (여백 추가)
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        params.setMargins(0, margin, 0, margin); // 위아래 여백
        clearBtn.setLayoutParams(params);

        // 2. 스타일 적용 (초기화 버튼은 메인 스타일 사용)
        int secondaryColor = ContextCompat.getColor(this, R.color.pro_sleep_secondary_text); // (예: #A0B1C4)
        clearBtn.setTextColor(secondaryColor);
        clearBtn.setStrokeColor(ColorStateList.valueOf(secondaryColor));
        clearBtn.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density)); // 1dp
        clearBtn.setCornerRadius((int) (8 * getResources().getDisplayMetrics().density)); // 8dp
        clearBtn.setTextSize(18);

        // 4. 클릭 리스너 (기존과 동일)
        clearBtn.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("AnalyzeTips", MODE_PRIVATE);
            prefs.edit().remove("tips_json").apply();

            layout.removeAllViews();
            addClearButton();
            Toast.makeText(this, "기록이 초기화되었습니다.", Toast.LENGTH_SHORT).show();
        });

        layout.addView(clearBtn);
    }
}
