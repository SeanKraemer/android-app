package com.weatherplanner.app;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

/**
 * Data model for OpenWeatherMap API response.
 * Represents the current weather data for a location.
 */
public class WeatherData {

    @SerializedName("coord")
    public Coordinates coord;

    @SerializedName("weather")
    public List<Weather> weather;

    @SerializedName("main")
    public Main main;

    @SerializedName("wind")
    public Wind wind;

    @SerializedName("dt")
    public long dt; // Time of data calculation, unix, UTC

    @SerializedName("timezone")
    public int timezone; // Shift in seconds from UTC

    @SerializedName("name")
    public String name; // City name

    public static WeatherData demo(String cityName) {
        WeatherData data = new WeatherData();
        data.name = cityName;
        data.coord = new Coordinates();
        data.coord.lon = -87.6298;
        data.coord.lat = 41.8781;
        data.main = new Main();
        data.main.temp = 295.15;
        data.main.feelsLike = 295.15;
        data.main.tempMin = 293.15;
        data.main.tempMax = 297.15;
        data.main.pressure = 1013;
        data.main.humidity = 55;
        data.wind = new Wind();
        data.wind.speed = 3.6;
        data.wind.deg = 220;
        Weather condition = new Weather();
        condition.id = 800;
        condition.main = "Clear";
        condition.description = "clear sky";
        condition.icon = "01d";
        data.weather = Collections.singletonList(condition);
        return data;
    }

    /**
     * Coordinates (longitude and latitude)
     */
    public static class Coordinates {
        @SerializedName("lon")
        public double lon;

        @SerializedName("lat")
        public double lat;
    }

    /**
     * Weather condition information
     */
    public static class Weather {
        @SerializedName("id")
        public int id;

        @SerializedName("main")
        public String main; // Group of weather parameters (Rain, Snow, Extreme etc.)

        @SerializedName("description")
        public String description; // Weather condition within the group

        @SerializedName("icon")
        public String icon; // Weather icon id
    }

    /**
     * Main weather parameters
     */
    public static class Main {
        @SerializedName("temp")
        public double temp; // Temperature. Unit Default: Kelvin

        @SerializedName("feels_like")
        public double feelsLike;

        @SerializedName("temp_min")
        public double tempMin;

        @SerializedName("temp_max")
        public double tempMax;

        @SerializedName("pressure")
        public int pressure; // Atmospheric pressure (on the sea level), hPa

        @SerializedName("humidity")
        public int humidity; // Humidity, %
    }

    /**
     * Wind information
     */
    public static class Wind {
        @SerializedName("speed")
        public double speed; // Wind speed. Unit Default: meter/sec

        @SerializedName("deg")
        public int deg; // Wind direction, degrees (meteorological)

        @SerializedName("gust")
        public Double gust; // Wind gust. Unit Default: meter/sec
    }

    /**
     * Get temperature in Fahrenheit
     */
    public String getTemperatureFahrenheit() {
        if (main != null) {
            double fahrenheit = (main.temp - 273.15) * 9/5 + 32;
            return String.format(java.util.Locale.US, "%.0f°F", fahrenheit);
        }
        return "N/A";
    }

    /**
     * Get weather condition description
     */
    public String getWeatherCondition() {
        if (weather != null && !weather.isEmpty()) {
            // Capitalize first letter of each word
            String description = weather.get(0).description;
            String[] words = description.split(" ");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    result.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1))
                          .append(" ");
                }
            }
            return result.toString().trim();
        }
        return "N/A";
    }

    /**
     * Get humidity percentage
     */
    public String getHumidity() {
        if (main != null) {
            return main.humidity + "%";
        }
        return "N/A";
    }

    /**
     * Get wind condition with direction
     */
    public String getWindCondition() {
        if (wind != null) {
            // Convert m/s to mph
            double mph = wind.speed * 2.23694;
            String direction = getWindDirection(wind.deg);
            return String.format(java.util.Locale.US, "%.0f mph %s", mph, direction);
        }
        return "N/A";
    }

    /**
     * Convert wind degree to cardinal direction
     */
    private String getWindDirection(int degree) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                               "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) ((degree + 11.25) / 22.5);
        return directions[index % 16];
    }

    /**
     * Get formatted date and time in the city's local timezone.
     * Shows current time adjusted to the city's timezone based on longitude.
     * Updates to show current time each time this is called.
     */
    public String getLocalDateTime() {
        if (coord != null) {
            // Calculate timezone offset from longitude
            // Each 15 degrees of longitude = 1 hour offset from UTC
            // Longitude ranges from -180 to +180
            int timezoneOffsetHours = (int) Math.round(coord.lon / 15.0);

            // Get current UTC time
            java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));

            // Add the timezone offset to get city's local time
            calendar.add(java.util.Calendar.HOUR_OF_DAY, timezoneOffsetHours);

            // Format the time
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return sdf.format(calendar.getTime());
        }

        // Fallback to current local time if we don't have coordinates
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US);
        return sdf.format(new java.util.Date());
    }
}
