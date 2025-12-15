package ac.kr.project;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    private Ringtone ringtone;
    private static final String CHANNEL_ID = "ALARM_SERVICE_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 1. 알림 채널 생성 (Android 8.0 이상)
        createNotificationChannel();

        // 2. 알람 소리 준비
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(this, alarmUri);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 3. 인텐트가 "STOP_ALARM" 액션을 가지고 있는지 확인 (중지 버튼 클릭 시)
        if (intent != null && "STOP_ALARM".equals(intent.getAction())) {
            stopAlarm();
            return START_NOT_STICKY; // 서비스 중지
        }

        // 4. 알람 소리 재생
        if (ringtone != null && !ringtone.isPlaying()) {
            ringtone.play();
        }

        // 5. '알람 중지' 버튼이 있는 알림(Notification) 생성
        Notification notification = createNotification();

        // 6. 서비스를 Foreground로 시작 (이걸 해야 안 꺼짐)
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY; // 서비스가 강제 종료되어도 다시 시작
    }

    // 알람 중지 로직
    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        stopForeground(true); // 알림 제거
        stopSelf(); // 서비스 종료
    }

    @Override
    public void onDestroy() {
        // 서비스가 파괴될 때 확실히 알람을 멈춤
        stopAlarm();
        super.onDestroy();
    }

    // '알람 중지' 버튼이 있는 알림 생성
    private Notification createNotification() {
        // '알람 중지' 버튼 인텐트
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction("STOP_ALARM");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 알림을 눌렀을 때 앱의 'Alarm' 화면으로 이동
        Intent notificationIntent = new Intent(this, Alarm.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⏰ 알람이 울리고 있습니다 ⏰")
                .setContentText("알람을 중지하려면 '중지'를 누르세요.")
                .setSmallIcon(R.drawable.ic_alarm) // (필수!) 3단계에서 추가할 아이콘
                .setContentIntent(pendingIntent) // 알림 클릭 시 Alarm.class 열기
                .addAction(R.drawable.ic_alarm_off, "알람 중지", stopPendingIntent) // '중지' 버튼
                .setOngoing(true); // 사용자가 스와이프로 끌 수 없게 함

        return builder.build();
    }

    // 알림 채널 생성 (Android 8.0 이상 필수)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "알람 서비스 채널",
                    NotificationManager.IMPORTANCE_HIGH // 소리 알림을 위해 HIGH로 설정
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}