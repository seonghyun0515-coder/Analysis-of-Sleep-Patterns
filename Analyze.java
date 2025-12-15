package ac.kr.project;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Analyze extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String API_KEY = "AIzaSyDuYhjHNhAY2GV3yD6Un5nXEBz7AP_uCXM";
    private static final String url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    private Button btnAnalyze, btnPickImage;
    private TextView tvResult;
    private ImageView imageView;

    private Uri selectedImageUri;

    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyze);

        btnAnalyze = findViewById(R.id.btnAnalyze);
        btnPickImage = findViewById(R.id.btnPickImage);
        tvResult = findViewById(R.id.tvResult);
        imageView = findViewById(R.id.imageView);

        // 이미지 선택 버튼
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // 분석 시작 버튼
        btnAnalyze.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(this, "이미지를 선택하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            analyzeImage(selectedImageUri);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            // 1. 이미지를 설정합니다.
            imageView.setImageURI(selectedImageUri);

            // 2. 이미지 꽉 차게 보기 (선택 사항)
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // ★★★ 3. (핵심!) 기존의 회색 틴트(색상 필터)를 제거합니다 ★★★
            imageView.setImageTintList(null);
        }
    }

    private void analyzeImage(Uri imageUri) {
        try {
            String base64Image = encodeImageToBase64(imageUri);

            // --- 자동 입력 프롬프트 ---
            String autoPrompt =
                    "이 이미지를 분석하여 '한국어로' 다음 내용을 알려줘:\n" +
                            "1. 사용자의 수면 상태 분석\n" +
                            "2. 개선할 수 있는 수면 팁 3가지" +
                            "3. 너가 생각하는 수면 점수를 알려줘 (0~100)\n"
                    ;

            JSONObject textPart = new JSONObject();
            textPart.put("text", autoPrompt);

            JSONObject imagePart = new JSONObject();
            imagePart.put("inline_data", new JSONObject()
                    .put("mimeType", "image/png")
                    .put("data", base64Image));

            JSONArray partsArray = new JSONArray();
            partsArray.put(textPart);
            partsArray.put(imagePart);

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", partsArray);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", new JSONArray().put(content));

            sendRequest(requestBody.toString());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "이미지 처리 중 오류 발생", Toast.LENGTH_SHORT).show();
        }
    }

    private String encodeImageToBase64(Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
    }

    private void sendRequest(String jsonBody) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvResult.setText("요청 실패: " + e.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String resp = response.body().string();
                    try {
                        JSONObject json = new JSONObject(resp);
                        String result = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        runOnUiThread(() -> {
                            tvResult.setText(result);
                            saveTipToPreferences(result);
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> tvResult.setText("응답 파싱 오류"));
                    }
                } else {
                    runOnUiThread(() -> tvResult.setText("응답 실패 코드: " + response.code()));
                }
            }
        });
    }

    private void saveTipToPreferences(String fullResult) {
        SharedPreferences prefs = getSharedPreferences("AnalyzeTips", MODE_PRIVATE);
        String existingJson = prefs.getString("tips_json", "[]");
        try {
            JSONArray tipsArray = new JSONArray(existingJson);

            for (int i = 0; i < tipsArray.length(); i++) {
                if (tipsArray.getString(i).equals(fullResult)) {
                    Log.d("AnalyzeSave", "중복 결과 저장 방지됨");
                    return;
                }
            }

            tipsArray.put(fullResult);
            prefs.edit().putString("tips_json", tipsArray.toString()).apply();
            Log.d("AnalyzeSave", "새로운 결과 저장됨");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
