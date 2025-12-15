package ac.kr.project;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Tip extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tip);

        TextView tvTipList = findViewById(R.id.tvTipList);

        String tips = ""
                + "1. 취침 시간을 일정하게 유지하세요.\n"
                + "2. 잠들기 1시간 전에는 스마트폰을 멀리하세요.\n"
                + "3. 카페인은 오후에 피하세요.\n"
                + "4. 조용하고 어두운 환경을 만들어주세요.\n"
                + "5. 잠들기 전 따뜻한 물로 샤워하세요.\n"
                + "6. 잠자리에서는 TV나 책을 멀리하세요.\n"
                + "7. 낮잠은 30분 이내로 제한하세요.\n"
                + "8. 규칙적으로 운동하되, 잠들기 직전은 피하세요.\n"
                + "9. 무거운 야식은 피하고 가벼운 식사를 하세요.\n"
                + "10. 불면이 지속되면 억지로 자려 하지 말고 자리에서 일어나세요.";

        tvTipList.setText(tips);
    }
}
