package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.modules.visuals.FakeWeather;

public class FakeWeatherSetRainApi extends ApiBase {

    @Override
    public String getRoute() {
        return "fake-weather-set-rain";
    }

    @Override
    public String post(String body) {
        Request request = gson.fromJson(body, Request.class);
        FakeWeather.instance.setRain(request.value);
        return "{ \"ok\": true }";
    }

    public static class Request {
        public float value;
    }
}
