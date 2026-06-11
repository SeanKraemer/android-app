package com.weatherplanner.app;

import com.google.gson.Gson;

/**
 * Parses JSON payloads returned by Gemini text models.
 *
 * Gemini sometimes wraps JSON responses in markdown code fences even when the
 * prompt asks for raw JSON, so responses are stripped of fences before parsing.
 */
final class GeminiJson {

    private static final Gson GSON = new Gson();

    private GeminiJson() {
    }

    /**
     * Strips markdown code fences from a Gemini response and parses it as JSON.
     *
     * @param rawResponse Raw text returned by the model (may include ```json fences)
     * @param type Target class to deserialize into
     * @return Parsed object, or null if the response is null or blank
     * @throws com.google.gson.JsonSyntaxException if the cleaned text is not valid JSON
     */
    static <T> T parse(String rawResponse, Class<T> type) {
        if (rawResponse == null) {
            return null;
        }
        String cleanJson = rawResponse
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
        return GSON.fromJson(cleanJson, type);
    }
}
