package com.coolweather.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.coolweather.android.gson.Weather;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 1*60*60*1000;  //这是1小时的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String weatherString = prefs.getString("weather",null);
        if (weatherString != null){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Weather weather = WeatherActivity.handleWeatherResponse(weatherString);
                    String weatherId = weather.basic.weatherId;
                    String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=81459eab494c45d28c14e96556e8c032";
                    try {
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder().url(weatherUrl).build();
                        Response response = client.newCall(request).execute();
                        String responseText = response.body().string();
                        Weather weather1 = WeatherActivity.handleWeatherResponse(responseText);
                        if (weather1 != null && "ok".equals(weather1.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                        }
                        Log.d("WeatherActivity","刷新成功");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("WeatherActivity","刷新失败");
                    }
                }
            }).start();
        }
    }
}
