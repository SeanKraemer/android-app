package com.weatherplanner.app;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for WeatherData: deserialization of an OpenWeatherMap current-weather
 * response and the display formatting used by DetailsActivity.
 */
public class WeatherDataTest {

    // Trimmed real-shape OpenWeatherMap /data/2.5/weather response.
    private static final String SAMPLE_RESPONSE = "{"
            + "\"coord\":{\"lon\":-87.65,\"lat\":41.85},"
            + "\"weather\":[{\"id\":501,\"main\":\"Rain\",\"description\":\"moderate rain\",\"icon\":\"10d\"}],"
            + "\"main\":{\"temp\":283.15,\"feels_like\":281.86,\"temp_min\":281.94,\"temp_max\":284.26,"
            + "\"pressure\":1016,\"humidity\":93},"
            + "\"wind\":{\"speed\":4.47,\"deg\":90,\"gust\":7.2},"
            + "\"dt\":1714668000,"
            + "\"timezone\":-18000,"
            + "\"name\":\"Chicago\"}";

    @Test
    public void parsesOpenWeatherMapResponse() {
        WeatherData data = new Gson().fromJson(SAMPLE_RESPONSE, WeatherData.class);

        assertEquals("Chicago", data.name);
        assertEquals(-87.65, data.coord.lon, 1e-9);
        assertEquals(41.85, data.coord.lat, 1e-9);
        assertEquals(93, data.main.humidity);
        assertEquals("moderate rain", data.weather.get(0).description);
        assertEquals(Double.valueOf(7.2), data.wind.gust);
    }

    @Test
    public void formatsParsedResponseForDisplay() {
        WeatherData data = new Gson().fromJson(SAMPLE_RESPONSE, WeatherData.class);

        assertEquals("50°F", data.getTemperatureFahrenheit());
        assertEquals("Moderate Rain", data.getWeatherCondition());
        assertEquals("93%", data.getHumidity());
        assertEquals("10 mph E", data.getWindCondition());
    }

    @Test
    public void windDirectionCoversCompassBoundaries() {
        WeatherData data = new Gson().fromJson(SAMPLE_RESPONSE, WeatherData.class);

        data.wind.deg = 0;
        assertEquals("10 mph N", data.getWindCondition());
        data.wind.deg = 180;
        assertEquals("10 mph S", data.getWindCondition());
        data.wind.deg = 270;
        assertEquals("10 mph W", data.getWindCondition());
        data.wind.deg = 359;
        assertEquals("10 mph N", data.getWindCondition());
    }

    @Test
    public void missingFieldsFallBackToNotAvailable() {
        WeatherData empty = new Gson().fromJson("{}", WeatherData.class);

        assertEquals("N/A", empty.getTemperatureFahrenheit());
        assertEquals("N/A", empty.getWeatherCondition());
        assertEquals("N/A", empty.getHumidity());
        assertEquals("N/A", empty.getWindCondition());
    }
}
