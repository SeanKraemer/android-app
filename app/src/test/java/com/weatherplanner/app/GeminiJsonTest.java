package com.weatherplanner.app;

import com.google.gson.JsonSyntaxException;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for GeminiJson, the parser for JSON payloads returned by Gemini text
 * models. Covers the markdown-fence stripping the app relies on because Gemini
 * sometimes wraps JSON in ```json fences despite being asked not to.
 */
public class GeminiJsonTest {

    @Test
    public void parsesPlainThemeJson() {
        String response = "{\"backgroundColor\":\"#F7FAFC\",\"textColor\":\"#1A202C\","
                + "\"accentColor\":\"#2563EB\",\"buttonColor\":\"#10B981\",\"toolbarColor\":\"#1F2937\"}";

        ThemeGenerator.ThemeSpec theme = GeminiJson.parse(response, ThemeGenerator.ThemeSpec.class);

        assertEquals("#F7FAFC", theme.backgroundColor);
        assertEquals("#1A202C", theme.textColor);
        assertEquals("#2563EB", theme.accentColor);
        assertEquals("#10B981", theme.buttonColor);
        assertEquals("#1F2937", theme.toolbarColor);
    }

    @Test
    public void stripsMarkdownFencesBeforeParsing() {
        String response = "```json\n{\"questions\": [\"Should I bring an umbrella?\","
                + " \"Is it safe to drive?\", \"Do I need a jacket?\"]}\n```";

        WeatherInsightsGenerator.QuestionSet questions =
                GeminiJson.parse(response, WeatherInsightsGenerator.QuestionSet.class);

        assertEquals(
                Arrays.asList("Should I bring an umbrella?", "Is it safe to drive?", "Do I need a jacket?"),
                questions.questions);
    }

    @Test
    public void stripsBareFencesAndSurroundingWhitespace() {
        String response = "  ```\n{\"answer\": \"Wear a light jacket.\"}\n```  ";

        WeatherInsightsGenerator.Answer answer =
                GeminiJson.parse(response, WeatherInsightsGenerator.Answer.class);

        assertEquals("Wear a light jacket.", answer.answer);
    }

    @Test
    public void nullResponseReturnsNull() {
        assertNull(GeminiJson.parse(null, ThemeGenerator.ThemeSpec.class));
    }

    @Test
    public void blankResponseReturnsNull() {
        assertNull(GeminiJson.parse("```json\n```", WeatherInsightsGenerator.Answer.class));
    }

    @Test(expected = JsonSyntaxException.class)
    public void malformedJsonThrows() {
        GeminiJson.parse("not json at all", WeatherInsightsGenerator.QuestionSet.class);
    }
}
