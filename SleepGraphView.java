package ac.kr.project;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SleepGraphView extends View {

    private List<SleepStage> sleepData;
    private Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int padding = 40;

    public SleepGraphView(Context context) {
        super(context);
        init();
    }

    public SleepGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SleepGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 배경 색
        bgPaint.setColor(0xFF1E1E1E);

        // 글자 스타일
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setSleepData(List<SleepStage> data) {
        this.sleepData = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (sleepData == null || sleepData.isEmpty()) return;

        // 전체 배경
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        long totalStart = sleepData.get(0).startTime;
        long totalEnd = sleepData.get(sleepData.size() - 1).endTime;
        long totalDuration = totalEnd - totalStart;
        if (totalDuration <= 0) return;

        int width = getWidth() - padding * 2;
        int height = getHeight();

        // 그래프 영역
        int graphTop = 50;
        int graphBottom = height - 150; // 밑에 범례와 전체 시간 공간 확보

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (SleepStage ss : sleepData) {
            float startX = padding + (float) (ss.startTime - totalStart) / totalDuration * width;
            float endX = padding + (float) (ss.endTime - totalStart) / totalDuration * width;

            // 단계별 색 지정
            switch (ss.stage) {
                case 0: barPaint.setColor(0xFFFF5555); break; // Awake
                case 1: barPaint.setColor(0xFF4DA6FF); break; // Light
                case 2: barPaint.setColor(0xFF003366); break; // Deep
                case 3: barPaint.setColor(0xFFAA44FF); break; // REM
            }

            // 수면 바
            canvas.drawRect(startX, graphTop, endX, graphBottom, barPaint);
        }

        // 전체 시작/끝 시간 표시 (맨 아래)
        int bottomY = height - 40;
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(sdf.format(new Date(totalStart)), padding, bottomY, textPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(sdf.format(new Date(totalEnd)), getWidth() - padding, bottomY, textPaint);

        // 범례 표시 (그래프 아래)
        int legendY = graphBottom + 50;
        int legendSpacing = 200; // 범례 간 간격
        int startLegendX = padding;

        drawLegend(canvas, startLegendX, legendY, 0xFFFF5555, "Awake");
        drawLegend(canvas, startLegendX + legendSpacing, legendY, 0xFF4DA6FF, "Light");
        drawLegend(canvas, startLegendX + legendSpacing * 2, legendY, 0xFF003366, "Deep");
        drawLegend(canvas, startLegendX + legendSpacing * 3, legendY, 0xFFAA44FF, "REM");
    }

    private void drawLegend(Canvas canvas, float x, float y, int color, String label) {
        // 색상 박스
        barPaint.setColor(color);
        canvas.drawRect(x, y - 15, x + 30, y + 15, barPaint);

        // 이름 표시
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(label, x + 40, y + 8, textPaint); // 색상 박스 오른쪽에 텍스트
        textPaint.setTextAlign(Paint.Align.CENTER);
    }
}
