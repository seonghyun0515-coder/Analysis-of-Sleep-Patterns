package ac.kr.project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    // (Ringtone 객체 삭제 - 더 이상 여기서 울리지 않습니다)

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "⏰ 알람 시간이 되었습니다!", Toast.LENGTH_LONG).show();

        // 1. AlarmService를 실행할 인텐트(명령) 생성
        Intent serviceIntent = new Intent(context, AlarmService.class);

        // 2. (핵심) Android 8.0 (Oreo) 이상에서는 ForegroundService로 실행
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}