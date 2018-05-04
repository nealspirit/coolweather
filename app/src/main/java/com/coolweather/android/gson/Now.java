package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Now {
    @SerializedName("tmp")
    public String temperature;

    @SerializedName("wind_dir")
    public String windDir;

    @SerializedName("cond")
    public More more;

    public class More {

        @SerializedName("code")
        public String code;

        @SerializedName("txt")
        public String info;
    }
}
