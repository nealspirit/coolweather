package com.coolweather.android;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private NestedScrollView weatherLayout;
    private TextView degreeText,weatherInfoText,windDirText;
    private LinearLayout forecastLayout;
    private TextView aqiText,pm25Text;
    private TextView comfortText,carwashText,sportText;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private Toolbar toolbar;
    private ImageView weather_pic,weather_icon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        //初始化各种控件
        weatherLayout = findViewById(R.id.weather_layout);
        degreeText = findViewById(R.id.degree_text);
        windDirText = findViewById(R.id.wind_dir_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carwashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        weather_pic = findViewById(R.id.weather_pic_img);
        weather_icon = findViewById(R.id.weather_icon);
        //读取SharedPreferences中的本地缓存数据，第一次启动APP则没有数据
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        if (weatherString != null){
            //有缓存数据直接解析天气数据
            Weather weather = handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        }else {
            //无缓存数据则去服务器查询天气
            String weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
    }

    /*
    * 处理并展示Weather实体类中的数据
    * */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updataTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String windDir = weather.now.windDir;
        String weatherInfo = weather.now.more.info;
        int weatherCode = Integer.parseInt(weather.now.more.code);
        collapsingToolbarLayout.setTitle(cityName);
        degreeText.setText(degree);
        windDirText.setText(windDir);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carwashText.setText(carWash);
        sportText.setText(sport);
        //设置天气图片
        if (weatherCode == 100){
            weather_pic.setImageResource(R.drawable.sunshine);
            weather_icon.setImageResource(R.drawable.sunshine_icon);
        }else if (weatherCode >= 101 && weatherCode <= 103){
            weather_pic.setImageResource(R.drawable.cloudy);
            weather_icon.setImageResource(R.drawable.cloudy_icon);
        }else if (weatherCode == 104){
            weather_pic.setImageResource(R.drawable.overcast);
            weather_icon.setImageResource(R.drawable.overcast_icon);
        }else if (weatherCode >= 300 && weatherCode <= 301){
            weather_pic.setImageResource(R.drawable.shower);
            weather_icon.setImageResource(R.drawable.shower_icon);
        }else if (weatherCode >= 302 && weatherCode <= 304){
            weather_pic.setImageResource(R.drawable.thunder);
            weather_icon.setImageResource(R.drawable.thunder_icon);
        }else if (weatherCode >= 305 && weatherCode <= 313){
            weather_pic.setImageResource(R.drawable.rain);
            weather_icon.setImageResource(R.drawable.rain_icon);
        }else if (weatherCode >= 400 && weatherCode <= 407){
            weather_pic.setImageResource(R.drawable.snow);
            weather_icon.setImageResource(R.drawable.snow_icon);
        }
        weatherLayout.setVisibility(View.VISIBLE);
    }
    /*
     * 根据天气id请求城市天气信息
     * */
    private void requestWeather(String weatherId) {
        final String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=81459eab494c45d28c14e96556e8c032";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(weatherUrl).build();
                    Response response = client.newCall(request).execute();
                    final String responseText = response.body().string();
                    final Weather weather = handleWeatherResponse(responseText);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (weather != null && "ok".equals(weather.status)){
                                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                                editor.putString("weather",responseText);
                                editor.apply();
                                showWeatherInfo(weather);
                            }else {
                                Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WeatherActivity.this, "连接服务器失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public static Weather handleWeatherResponse(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent,Weather.class);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
