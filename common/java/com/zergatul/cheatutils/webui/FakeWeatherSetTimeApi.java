package com.zergatul.cheatutils.webui;

import com.zergatul.cheatutils.modules.visuals.FakeWeather;

public class FakeWeatherSetTimeApi extends ApiBase {

    @Override
    public String getRoute() {
        return "fake-weather-set-time";
    }

    @Override
    public String post(String body) {
        Request request = gson.fromJson(body, Request.class);
        FakeWeather.instance.setTime(request.value);
        return "{ \"ok\": true }";
    }

    public static class Request {
        public int value;
    }
}