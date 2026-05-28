package edu.uiuc.cs427app;

import com.google.genai.Client;

import com.google.genai.types.GenerateImagesConfig;
import com.google.genai.types.GenerateImagesResponse;
import com.google.genai.types.Image;
import com.google.gson.annotations.SerializedName;

import android.util.Log;

import java.util.concurrent.CompletableFuture;

/**
 * WeatherImageGenerator is a utility class that uses the Gemini LLM to generate
 * a weather-themed image (as a base64 encoded PNG string) based on current weather data.
 *
 * This class provides a static method to generate a base64 encoded PNG image representation
 * of the weather conditions. The usage pattern is asynchronous with callbacks.
 */
public class WeatherImageGenerator {
    private static final String TAG = "WeatherImageGenerator";

    /**
     * Data class representing the generated weather image.
     * Contains the base64 encoded PNG image data as a string.
     */
    public static class WeatherImage {
        @SerializedName("imageBytes")
        public byte[] imageBytes;
    }

    /**
     * Callback interface for asynchronous image generation.
     */
    public interface ImageCallback {
        /**
         * Called when the image is successfully generated.
         * @param image The generated weather image
         */
        void onImageGenerated(WeatherImage image);

        /**
         * Called when image generation fails.
         * @param e The exception that occurred
         */
        void onError(Exception e);
    }

    /**
     * Generates a weather-themed base64 encoded PNG image based on current weather conditions.
     *
     * @param apiKey         Gemini API key for authentication
     * @param dateTime       Current dateTime
     * @param cityName       Name of the city
     * @param temperature    Current temperature (e.g., "72°F")
     * @param weatherCondition Weather condition (e.g., "Sunny", "Rainy")
     * @param humidity       Humidity percentage (e.g., "65%")
     * @param windCondition  Wind condition (e.g., "15 mph NE")
     * @param callback       Callback to receive the generated image or an error
     */
    public static void generateImage(
            String apiKey,
            String dateTime,
            String cityName,
            String temperature,
            String weatherCondition,
            String humidity,
            String windCondition,
            ImageCallback callback
    ) {
        if (apiKey == null || apiKey.isEmpty()) {
            // Log a configuration error so missing API key is prominent in logs
            Log.e(TAG, "API key is missing!");
            // Notify the caller that generation cannot proceed without a valid API key and terminate the operation
            callback.onError(new IllegalArgumentException("API key missing"));
            return;
        }

        // Informational log indicating the operation has started for this city
        Log.d(TAG, "Generating weather image for " + cityName);
        Client genAIClient = Client.builder().apiKey(apiKey).build();

        // Debug which backend is selected to help troubleshoot environment differences
        if (genAIClient.vertexAI()) {
            Log.d(TAG, "Using Vertex AI");
        } else {
            Log.d(TAG, "Using Gemini Developer API");
        }

        String prompt = "You are a creative weather artist. Based on the following weather data, generate a simple and visually appealing image that represents the weather in the city during this time.\n\n" +
                "WEATHER DATA:\n" +
                "- Date/Time: " + dateTime + "\n" +
                "- City: " + cityName + "\n" +
                "- Temperature: " + temperature + "\n" +
                "- Condition: " + weatherCondition + "\n" +
                "- Humidity: " + humidity + "\n" +
                "- Wind: " + windCondition + "\n\n" +
                "REQUIREMENTS:\n" +
                "1. Depict the city as it would appear in the current weather conditions.\n" +
                "2. The colors should reflect the weather conditions (e.g., warm colors for sunny, cool colors for rainy/cold).\n" +
                "3. Do not include text or human figures.";

        // Verbose log of the complete prompt for debugging
        Log.v(TAG, "Full prompt for image: " + prompt);

        GenerateImagesConfig config =
                GenerateImagesConfig.builder()
                        .numberOfImages(1)
                        .outputMimeType("image/png")
                        .includeSafetyAttributes(true)
                        .build();

        // Initiates an asynchronous image generation request to the Gemini Imagen API
        CompletableFuture<GenerateImagesResponse> responseFuture = genAIClient.async.models.generateImages(
                        "imagen-4.0-generate-001", prompt, config); // Use the actual prompt

        responseFuture
                // Callback invoked when the CompletableFuture completes successfully
                .thenAccept(
                        response -> {
                            if (response == null || response.images() == null || response.images().isEmpty()) {
                                // Error log when API returned no images
                                Log.e(TAG, "Unable to generate images.");
                                // Notify caller that the API returned no images
                                callback.onError(new Exception("No image generated."));
                            }
                            else {
                                // Select the first generated image from the response's images list
                                Image generatedImage = response.images().get(0);
                                if (generatedImage.imageBytes().isPresent()) {
                                    WeatherImage weatherImage = new WeatherImage();
                                    weatherImage.imageBytes = generatedImage.imageBytes().get();
                                    // Forward the successfully decoded image bytes to the caller
                                    callback.onImageGenerated(weatherImage);
                                } else {
                                    // Notify caller that the image object lacked byte data (malformed image payload)
                                    callback.onError(new Exception("Generated image data is null."));
                                }
                            }
                        })
                // Callback invoked when the CompletableFuture completes exceptionally
                .exceptionally(e -> {
                    // Log the exception encountered during async processing to aid debugging and error tracking
                    Log.e(TAG, "Image generation failed: " + e.getMessage());
                    // Propagate the error to the ImageCallback so upstream code (UI or retry logic) can handle it appropriately
                    callback.onError(new Exception("Image generation failed: " + e.getMessage()));
                    return null;
                })
                // Ensures the image generation request fully completes before this method returns
                .join();
    }
}