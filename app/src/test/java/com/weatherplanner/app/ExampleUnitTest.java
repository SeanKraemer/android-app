package com.weatherplanner.app;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExampleUnitTest {
    @Test
    public void demoWeatherFormatsExpectedValues() {
        WeatherData weatherData = WeatherData.demo("Chicago");

        assertEquals("72°F", weatherData.getTemperatureFahrenheit());
        assertEquals("Clear Sky", weatherData.getWeatherCondition());
        assertEquals("55%", weatherData.getHumidity());
        assertEquals("8 mph SW", weatherData.getWindCondition());
        assertTrue(weatherData.getLocalDateTime().matches("[A-Z][a-z]{2} \\d{2}, \\d{4} \\d{2}:\\d{2}"));
    }
}
