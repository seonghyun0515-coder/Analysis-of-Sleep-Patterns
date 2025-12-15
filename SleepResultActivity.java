package ac.kr.project;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SleepResultActivity extends AppCompatActivity {

    private TextView tvStartTime, tvEndTime, tvDuration;
    private Button btnSaveImage, btnConfirm;
    private SleepGraphView sleepGraphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_result);

        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime   = findViewById(R.id.tvEndTime);
        tvDuration  = findViewById(R.id.tvDuration);
        btnSaveImage = findViewById(R.id.btnSaveImage);
        btnConfirm = findViewById(R.id.btnConfirm);
        sleepGraphView = findViewById(R.id.sleepGraphView);

        long startTime = getIntent().getLongExtra("startTime", 0);
        long endTime   = getIntent().getLongExtra("endTime", 0);

        showSleepTime(startTime, endTime);

        List<SleepStage> stages = SleepDataHolder.getStages();
        if (stages != null && !stages.isEmpty()) {
            sleepGraphView.setSleepData(stages);
        }

        btnSaveImage.setOnClickListener(v -> saveGraphImage());
        btnConfirm.setOnClickListener(v -> finish());
    }

    private void showSleepTime(long startTime, long endTime) {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE a h:mm", Locale.KOREA);

        if (startTime > 0) tvStartTime.setText(fmt.format(new Date(startTime)));
        if (endTime > 0) tvEndTime.setText(fmt.format(new Date(endTime)));

        long diff = Math.max(0, endTime - startTime);

        long h = diff / (1000 * 60 * 60);
        long m = (diff / (1000 * 60)) % 60;
        long s = (diff / 1000) % 60;

        tvDuration.setText(h + "시간 " + m + "분 " + s + "초");
    }

    private void saveGraphImage() {
        Bitmap bitmap = Bitmap.createBitmap(
                sleepGraphView.getWidth(),
                sleepGraphView.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        sleepGraphView.draw(new android.graphics.Canvas(bitmap));

        String fileName = "sleep_graph_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(new Date()) + ".png";

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SleepImages");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                if (out != null) out.close();

                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);

                Toast.makeText(this, "갤러리에 저장되었습니다!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show();
        }
    }
}
