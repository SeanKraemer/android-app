package com.weatherplanner.app;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class WeatherInsightsTest {

    private static final int LLM_TIMEOUT_MS = 5000;

    // --- helper to create mock intent (city and info) ---
    private Intent createValidIntent() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, WeatherInsightsActivity.class);
        intent.putExtra("cityName", "Champaign");
        intent.putExtra("temperature", "72°F");
        intent.putExtra("weatherCondition", "Sunny");
        intent.putExtra("humidity", "50%");
        intent.putExtra("windCondition", "10 mph");
        return intent;
    }

    // --- TEST 1: Valid response from WeatherInsightsGenerator, check first question ---
    @Test
    public void checkWeatherInsightsGeneration() throws InterruptedException {
        Intent intent = createValidIntent();

        try (ActivityScenario<WeatherInsightsActivity> ignored = ActivityScenario.launch(intent)) {
            Thread.sleep(LLM_TIMEOUT_MS);
            onView(withId(R.id.questionsContainer)).check(matches(isDisplayed()));

            ViewInteraction firstQuestionButton = onView(
                    allOf(childAtPosition(withId(R.id.questionsContainer), 0),
                            isDisplayed()));
            firstQuestionButton.perform(click());

            Thread.sleep(LLM_TIMEOUT_MS);
            onView(withId(R.id.answerTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.answerText)).check(matches(isDisplayed()));
        }
    }

    // --- TEST 2: Valid response from WeatherInsightsGenerator, multiple interactions ---
    @Test
    public void multipleInteractions() throws InterruptedException {
        Intent intent = createValidIntent();

        try (ActivityScenario<WeatherInsightsActivity> ignored = ActivityScenario.launch(intent)) {
            Thread.sleep(LLM_TIMEOUT_MS);
            onView(allOf(childAtPosition(withId(R.id.questionsContainer), 0), isDisplayed()))
                    .perform(click());

            Thread.sleep(LLM_TIMEOUT_MS);
            onView(withId(R.id.answerTitle)).check(matches(withText(containsString("Question 1"))));

            onView(allOf(childAtPosition(withId(R.id.questionsContainer), 1), isDisplayed()))
                    .perform(click());

            Thread.sleep(LLM_TIMEOUT_MS);
            onView(withId(R.id.answerTitle)).check(matches(withText(containsString("Question 2"))));
        }
    }

    // --- TEST 3: No data provided ---
    @Test
    public void missingData() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, WeatherInsightsActivity.class);

        try (ActivityScenario<WeatherInsightsActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    // --- helper to find child view at specific position in parent ---
    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
