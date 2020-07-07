package com.access.careplanning;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import com.access.careplanning.database.User;
import com.access.careplanning.databinding.ActivityMainBinding;
import com.access.careplanning.permission.Permission;
import com.access.careplanning.permission.PermissionUtil;
import com.access.careplanning.viewmodel.CarePlanningViewModel;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Locale;

/**
 * Main screen, to manage and provide user controls for:
 * - the times that the phone is on vibrate,
 * - QR scanning
 * - Battery threshold
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    /**
     * Use the databinding object to avoid countless definitions of TextView, Button etc
     */
    private ActivityMainBinding binding;

    /**
     * ViewModel to hold data and state, and keep the activity clean
     */
    private CarePlanningViewModel mViewModel;

    private enum TimePeriod {START, END}

    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmStartIntent;
    private PendingIntent mAlarmEndIntent;
    private PendingIntent mBatteryIntent;
    private int mBatteryThreshold = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mViewModel = new ViewModelProvider(this).get(CarePlanningViewModel.class);

        // Process signed-in user info, show name on screen
        final String googleId = getIntent().getStringExtra(IntentEnum.USER_ID.name());
        final String name = getIntent().getStringExtra(IntentEnum.USER_NAME.name());
        User user = new User(googleId, name);
        binding.txtName.setText(user.getName());

        binding.btnStartTime.setOnClickListener((view) ->
                requestTimeChoice(TimePeriod.START, R.string.select_start_time,
                        mViewModel.getStartHour(), mViewModel.getStartMin())
        );

        binding.btnEndTime.setOnClickListener((view) ->
                requestTimeChoice(TimePeriod.END, R.string.select_end_time,
                        mViewModel.getEndHour(), mViewModel.getEndMin())
        );

        binding.btnScan.setOnClickListener((view) -> {
            // Check and request the camera permission before launching the QR scanner
            if (!PermissionUtil.askPermission(this, Permission.CAMERA)) {
                scanQR();
            }
        });

        binding.seekBatteryThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.txtBatteryThresholdValue.setText(
                        String.format(Locale.US, "%d%%", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mViewModel.setBatterySaverPercent(seekBar.getProgress());
            }
        });

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        registerReceiver(broadcastReceiver, new IntentFilter("com.access.careplanning") );

        Intent intentAlarm = new Intent("com.access.careplanning");//new Intent(this, AlarmReceiver.class);
        mAlarmStartIntent = PendingIntent.getBroadcast(getBaseContext(), IntentEnum.ALARM.getCode(), intentAlarm, 0);
        mAlarmEndIntent = PendingIntent.getBroadcast(getBaseContext(), IntentEnum.ALARM.getCode(), intentAlarm, 0);

        Intent intentBattery = new Intent("com.access.careplanning");
        intentBattery.putExtra(IntentEnum.BATTERY_CHECK.name(), true);
        mBatteryIntent = PendingIntent.getBroadcast(getBaseContext(), IntentEnum.BATTERY_CHECK.getCode(), intentBattery, 0);
        setBatteryCheckAlarm();

        mViewModel.initUser(user, CarePlanningViewModel.DEFAULT_TIME_RANGE,
                () -> runOnUiThread(() ->
                        observeAll()
                ));
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(IntentEnum.BATTERY_CHECK.name(), false)) {
                Log.d(TAG, "onReceive battery");
                checkBatteryThreshold();
            } else {
                Log.d(TAG, "onReceive ringer");
                setRinger();
            }
        }
    };

    /**
     * Observe LiveData, when values are changed or read for the first time, update the UI/actions
     */
    private void observeAll() {
        mViewModel.getTimeRangeLiveData().observe(this, (timeRange) -> {
            binding.btnStartTime.setText(formatTime(
                    timeRange.getStartHour(), timeRange.getStartMinute()));
            binding.btnEndTime.setText(formatTime(
                    timeRange.getEndHour(), timeRange.getEndMinute()));
            mViewModel.setEndTime(timeRange.getEndHour(), timeRange.getEndMinute());
            mViewModel.setStartTime(timeRange.getStartHour(), timeRange.getStartMinute());
            setRinger();
        });

        mViewModel.getBattery().observe(this, (battery) -> {
            final int threshold = battery != null
                    ? battery.getThreshold() : CarePlanningViewModel.DEFAULT_BATTERY_THRESHOLD;

            binding.txtBatteryThresholdValue.setText(
                    String.format(Locale.US, "%d%%", threshold));
            binding.seekBatteryThreshold.setProgress(threshold, true);
            mBatteryThreshold = threshold;
            checkBatteryThreshold();
        });
    }

    public static String formatTime(int hour, int min) {
        return String.format(Locale.US, "%d:%02d", hour, min);
    }

    /**
     * Launch qr scanner
     */
    private void scanQR() {
        startActivityForResult(new Intent(this, ScanningActivity.class),
                IntentEnum.SCAN.getCode());
    }

    /**
     * Handle QR scan result
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IntentEnum.SCAN.getCode()) {
            processQRCode(data.getStringExtra(IntentEnum.SCAN.name()));
        }
    }

    /**
     * Activate device wifi if QR code is valid.
     * Wifi enabling is deprecated from Android Q onwards
     *
     * @param code scanned code
     */
    private void processQRCode(String code) {
        if (CarePlanningViewModel.isValidQR(code)) {
            setWifiMode(true);
        } else {
            Log.w(TAG, "Invalid QR code");
            Toast.makeText(this, R.string.invalid_qr_code, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Wifi can't be enabled/disabled in Android Q onwards. It works in earlier versions.
     * @param enable true to enable, or false to disable
     * @return true if wifi was successfully changed (enabled or disabled), else false
     */
    @SuppressWarnings("deprecation")
    private boolean setWifiMode(boolean enable) {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (wifiManager.isWifiEnabled() != enable) {
                boolean changed = wifiManager.setWifiEnabled(enable);
                if (changed) {
                    Log.i(TAG, "Changed wifi on/off");
                    sendNotification(getString(
                            enable ? R.string.notification_wifi_on : R.string.notification_wifi_off));
                    return true;
                }
            }
        } else {
            Log.w(TAG, "Could not enable/disable wifi on this device");
        }
        return false;
    }

    /**
     * Show time picker dialog.
     * @param timePeriod Period eg Start or End
     * @param resTitle title
     * @param hour last chosen or default hour
     * @param minute last chosen or default minute
     */
    private void requestTimeChoice(TimePeriod timePeriod, int resTitle, int hour, int minute) {
        TimePickerDialog mTimePicker = new TimePickerDialog(this,
                (timePicker, selectedHour, selectedMinute) -> {
                    setNewTime(timePeriod, selectedHour, selectedMinute);
                }, hour, minute, true); // 24 hour time in dialog
        mTimePicker.setTitle(resTitle);
        mTimePicker.show();
    }

    /**
     * Set the new time from the user's choice.
     * Validate times are valid and the end time is not earlier than the start time.
     * @param timePeriod Period eg Start or End
     * @param selectedHour chosen hour
     * @param selectedMinute chosen minute
     */
    private void setNewTime(TimePeriod timePeriod, int selectedHour, int selectedMinute) {
        try {
            switch (timePeriod) {
                case START:
                    mViewModel.updateStartTime(selectedHour, selectedMinute);
                    setAlarm(TimePeriod.START, selectedHour, selectedMinute);
                    break;
                case END:
                    mViewModel.updateEndTime(selectedHour, selectedMinute);
                    setAlarm(TimePeriod.END, selectedHour, selectedMinute);
            }
        } catch (InvalidParameterException ipe) {
            Toast.makeText(getApplicationContext(), ipe.getMessage(), Toast.LENGTH_SHORT).show();
            // todo possibly use a snackbar warning
        }
    }

    /**
     * Set the ringer mode, notify the user if the mode has changed.
     * Requires and checks for system policy access.
     */
    private void setRinger() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null && checkPolicyAccess()) {
            final int ringerMode = mViewModel.getCurrentRingerMode(hour, minute);
            if (am.getRingerMode() != ringerMode) {
                am.setRingerMode(ringerMode);
                sendNotification(getString(R.string.notification_ringer));
            }
        }
    }

    /**
     * Checks and asks for system policy settings, so that the app can change the ringer
     * @return true if policy access is already granted, else false
     */
    private boolean checkPolicyAccess() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
            return false;
        }
        return true;
    }

    private void checkBatteryThreshold() {
        if (getBatteryLevel() < mBatteryThreshold && !mViewModel.isBatterySaverOn()) {
            mViewModel.setBatterySaverOn(true);
            setBatterySavingMode(true);
            sendNotification(getString(R.string.notification_power_saving));
        }
    }

    /**
     * Get battery percent
     * https://developer.android.com/training/monitoring-device-state/battery-monitoring
     * @return current percent of battery level
     */
    private int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100 / (float) scale;
        return (int) batteryPct;
    }

    /**
     * Power saving alternatives below but are less fine grained checks, so I used an alarm:
     * https://stackoverflow.com/questions/25065635/checking-for-power-saver-mode-programmatically?rq=1
     * https://developer.android.com/training/monitoring-device-state/battery-monitoring
     *     Monitor significant changes in battery level
     *     You can't easily continually monitor the battery state, but you don't need to.
     *     Generally speaking, the impact of constantly monitoring the battery level has a greater impact on the battery than your app's normal behavior, so it's good practice to only monitor significant changes in battery level—specifically when the device enters or exits a low battery state.
     *     The manifest snippet below is extracted from the intent filter element within a broadcast receiver. The receiver is triggered whenever the device battery becomes low or exits the low condition by listening for ACTION_BATTERY_LOW and ACTION_BATTERY_OKAY.
     * <receiver android:name=".BatteryLevelReceiver">
     *   <intent-filter>
     *     <action android:name="android.intent.action.BATTERY_LOW"/>
     *     <action android:name="android.intent.action.BATTERY_OKAY"/>
     *   </intent-filter>
     * </receiver>
     * @param batterySavingMode
     */
    private void setBatterySavingMode(boolean batterySavingMode) {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isDeviceIdleMode() && batterySavingMode) {
            // Checks the power manager mode, start saving power on the device, turn off services, and bluetooth?
            setWifiMode(false);
        }
    }

    /**
     * If the camera permission has now been granted, do the QR scanning
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (PermissionUtil.granted(Permission.CAMERA, permissions, grantResults)) {
            scanQR();
        }
    }

    /**
     * Set an alarm for the start/end eg (loud/vibrate) time period
     * Todo For production app RECEIVE_BOOT_COMPLETED, BroadcastReceiver to restore alarm after restart,
     * also repeat add a repeat for every 24hours
     *
     * @param timePeriod
     * @param hour
     * @param minute
     */
    private void setAlarm(TimePeriod timePeriod, int hour, int minute) {
        mAlarmManager.cancel(timePeriod == TimePeriod.START ? mAlarmStartIntent : mAlarmEndIntent);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                timePeriod == TimePeriod.START ? mAlarmStartIntent : mAlarmEndIntent);
    }

    private void setBatteryCheckAlarm() {
        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 5, mBatteryIntent);
    }

    /**
     * Show notification on screen
     *
     * @param message
     */
    @SuppressLint("NewApi")
    private void sendNotification(final String message) {
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel("ID", "Name", importance);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(getApplicationContext(), notificationChannel.getId());
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        builder.setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(alarmSound);
        Notification notification = builder.build();
        // Hide the notification after it's selected
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }

}
