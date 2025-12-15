package ac.kr.project;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Calendar;

public class Alarm extends Activity {

    private ToggleButton cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private TimePicker timePicker;
    private Button btnSetAlarm, btnCancelAlarm;

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm);

        cbMon = findViewById(R.id.checkbox_mon);
        cbTue = findViewById(R.id.checkbox_tue);
        cbWed = findViewById(R.id.checkbox_wed);
        cbThu = findViewById(R.id.checkbox_thu);
        cbFri = findViewById(R.id.checkbox_fri);
        cbSat = findViewById(R.id.checkbox_sat);
        cbSun = findViewById(R.id.checkbox_sun);

        timePicker = findViewById(R.id.timePicker);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        btnCancelAlarm = findViewById(R.id.btnCancelAlarm);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        btnSetAlarm.setOnClickListener(v -> setRepeatingAlarms());
        btnCancelAlarm.setOnClickListener(v -> cancelAlarms());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setIs24HourView(true);
        }
    }

    private void setRepeatingAlarms() {
        boolean anyChecked = cbMon.isChecked() || cbTue.isChecked() || cbWed.isChecked() ||
                cbThu.isChecked() || cbFri.isChecked() || cbSat.isChecked() || cbSun.isChecked();

        if (!anyChecked) {
            Toast.makeText(this, "요일을 하나 이상 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int hour, minute;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }

        // 알람을 요일별로 설정
        if (cbMon.isChecked()) setAlarmForDay(Calendar.MONDAY, hour, minute);
        if (cbTue.isChecked()) setAlarmForDay(Calendar.TUESDAY, hour, minute);
        if (cbWed.isChecked()) setAlarmForDay(Calendar.WEDNESDAY, hour, minute);
        if (cbThu.isChecked()) setAlarmForDay(Calendar.THURSDAY, hour, minute);
        if (cbFri.isChecked()) setAlarmForDay(Calendar.FRIDAY, hour, minute);
        if (cbSat.isChecked()) setAlarmForDay(Calendar.SATURDAY, hour, minute);
        if (cbSun.isChecked()) setAlarmForDay(Calendar.SUNDAY, hour, minute);

        Toast.makeText(this, "알람이 설정되었습니다.", Toast.LENGTH_SHORT).show();
        btnCancelAlarm.setEnabled(true);
    }

    private void setAlarmForDay(int dayOfWeek, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long now = System.currentTimeMillis();
        if (calendar.getTimeInMillis() <= now) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1); // 다음 주 같은 요일로 설정
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("dayOfWeek", dayOfWeek); // 필요하면

        int requestCode = dayOfWeek; // 요일별 고유 requestCode로 구분
        PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY * 7, pi);
        }
    }

    private void cancelAlarms() {
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, day,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (alarmManager != null) {
                alarmManager.cancel(pi);
            }
        }
        Toast.makeText(this, "알람이 모두 취소되었습니다.", Toast.LENGTH_SHORT).show();
        btnCancelAlarm.setEnabled(false);
    }
}
