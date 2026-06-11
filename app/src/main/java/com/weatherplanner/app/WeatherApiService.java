package com.weatherplanner.app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service class for fetching weather data from OpenWeatherMap API.
 * Uses OkHttp for network requests and Gson for JSON parsing.
 */
public class WeatherApiService {

    private static final String TAG = "WeatherApiService";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private final OkHttpClient client;
    private final Gson gson;
    private final String apiKey;
    private final ExecutorService executor;
    private final Handler mainHandler;

    /**
     * Constructor
     * @param apiKey OpenWeatherMap API key
     */
    public WeatherApiService(String apiKey) {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.apiKey = apiKey;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Callback interface for weather data fetch results.
     * Implementations receive either the parsed WeatherData on success or an error message on failure.
     */
    public interface WeatherCallback {
        /**
         * Called when weather data has been successfully fetched and parsed.
         * @param weatherData Parsed WeatherData object containing the current weather
         */
        void onSuccess(WeatherData weatherData);

        /**
         * Called when there was an error fetching or parsing the weather data.
         * @param errorMessage Short description of the error (network, API, parsing, etc.)
         */
        void onError(String errorMessage);
    }

    /**
     * Fetch current weather data for a city by name.
     * Uses OpenWeatherMap 2.5 API (free tier).
     *
     * @param cityName Name of the city
     * @param callback Callback to handle the result
     */
    public void getCurrentWeather(String cityName, WeatherCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "OpenWeatherMap API key missing; returning demo weather.");
            mainHandler.post(() -> callback.onSuccess(WeatherData.demo(cityName)));
            return;
        }

        // Build the URL for API 2.5
        String url = BASE_URL + "?q=" + cityName + "&appid=" + apiKey;

        // Execute network request on background thread
        executor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();

                    // Parse JSON response
                    WeatherData weatherData = gson.fromJson(jsonResponse, WeatherData.class);

                    // Post result to main thread
                    mainHandler.post(() -> callback.onSuccess(weatherData));
                } else {
                    String errorMsg = "API Error: " + response.code() + " - " + response.message();
                    Log.e(TAG, errorMsg);
                    mainHandler.post(() -> callback.onError(errorMsg));
                }

                response.close();

            } catch (IOException e) {
                Log.e(TAG, "Network error", e);
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error", e);
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executor.shutdown();
    }
}
