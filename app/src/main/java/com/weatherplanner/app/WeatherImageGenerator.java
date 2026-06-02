package com.weatherplanner.app;

import com.google.genai.Client;

import com.google.genai.types.GenerateImagesConfig;
import com.google.genai.types.GenerateImagesResponse;
import com.google.genai.types.Image;
import com.google.gson.annotations.SerializedName;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
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
            Log.w(TAG, "Gemini API key missing; returning demo weather image.");
            WeatherImage weatherImage = new WeatherImage();
            weatherImage.imageBytes = createDemoImageBytes(weatherCondition);
            callback.onImageGenerated(weatherImage);
            return;
        }

        // Informational log indicating the operation has started for this city
        Log.d(TAG, "Generating weather image for " + cityName);
        Client genAIClient;
        try {
            genAIClient = Client.builder().apiKey(apiKey).build();
        } catch (Exception e) {
            Log.e(TAG, "Unable to initialize Gemini image client", e);
            callback.onError(e);
            return;
        }

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
                        GeminiModels.FAST_IMAGE_MODEL, prompt, config);

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
                });
    }

    private static byte[] createDemoImageBytes(String weatherCondition) {
        Bitmap bitmap = Bitmap.createBitmap(400, 240, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        String condition = weatherCondition == null
                ? ""
                : weatherCondition.toLowerCase(Locale.US);
        int skyTop = condition.contains("rain") ? Color.rgb(98, 125, 152)
                : condition.contains("snow") ? Color.rgb(196, 215, 230)
                : condition.contains("cloud") ? Color.rgb(146, 173, 190)
                : Color.rgb(88, 168, 224);
        int skyBottom = condition.contains("rain") ? Color.rgb(143, 163, 183)
                : condition.contains("snow") ? Color.rgb(234, 241, 245)
                : condition.contains("cloud") ? Color.rgb(215, 226, 232)
                : Color.rgb(229, 244, 255);

        for (int y = 0; y < bitmap.getHeight(); y++) {
            float ratio = y / (float) bitmap.getHeight();
            int red = (int) (Color.red(skyTop) + ratio * (Color.red(skyBottom) - Color.red(skyTop)));
            int green = (int) (Color.green(skyTop) + ratio * (Color.green(skyBottom) - Color.green(skyTop)));
            int blue = (int) (Color.blue(skyTop) + ratio * (Color.blue(skyBottom) - Color.blue(skyTop)));
            paint.setColor(Color.rgb(red, green, blue));
            canvas.drawLine(0, y, bitmap.getWidth(), y, paint);
        }

        paint.setColor(Color.rgb(255, 193, 77));
        canvas.drawCircle(78, 62, 34, paint);

        paint.setColor(Color.argb(235, 255, 255, 255));
        canvas.drawOval(new RectF(110, 68, 245, 142), paint);
        canvas.drawOval(new RectF(190, 52, 330, 140), paint);
        canvas.drawOval(new RectF(70, 92, 205, 165), paint);

        paint.setColor(Color.rgb(57, 84, 111));
        canvas.drawRect(42, 174, 370, 214, paint);
        paint.setColor(Color.rgb(42, 63, 87));
        canvas.drawRect(74, 144, 126, 214, paint);
        canvas.drawRect(154, 126, 210, 214, paint);
        canvas.drawRect(246, 152, 312, 214, paint);

        if (condition.contains("rain")) {
            paint.setStrokeWidth(5f);
            paint.setColor(Color.rgb(75, 111, 151));
            for (int x = 95; x <= 310; x += 38) {
                canvas.drawLine(x, 150, x - 14, 190, paint);
            }
        } else if (condition.contains("snow")) {
            paint.setColor(Color.WHITE);
            for (int x = 90; x <= 318; x += 44) {
                canvas.drawCircle(x, 164, 7, paint);
                canvas.drawCircle(x + 16, 190, 5, paint);
            }
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        bitmap.recycle();
        return stream.toByteArray();
    }
}
