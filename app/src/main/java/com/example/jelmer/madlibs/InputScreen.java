package com.example.jelmer.madlibs;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/* Most of the code about speech to text is from http://www.androidhive.info/2014/07/android-speech-to-text-tutorial/ */
public class InputScreen extends AppCompatActivity {

    final static int SIMPLE = R.raw.madlib0_simple;
    final static int TARZAN = R.raw.madlib1_tarzan;
    final static int UNIVERSITY = R.raw.madlib2_university;
    final static int CLOTHES = R.raw.madlib3_clothes;
    final static int DANCE = R.raw.madlib4_dance;

    public static final String PLACEHOLDER_LIST_FILE = "placeholder_file";
    public static final String STORY_NUMBER_FILE = "story_number_file";
    private final int REQ_CODE_SPEECH_INPUT = 100;

    Button submitButton;
    private ImageButton btnSpeak;
    TextToSpeech speech;
    Story story;
    EditText placeholderField;
    TextView wordsLeftText;
    Set<String> placeholderList;
    int storyIndex = 404;
    int placeholderCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_screen);
        speech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    speech.setLanguage(Locale.UK);
                }
            }
        });

        // Restore preferences
        SharedPreferences placeholderSettings = getSharedPreferences(PLACEHOLDER_LIST_FILE, 0);
        placeholderList = placeholderSettings.getStringSet("placeholders", null);
        if(placeholderList == null)
        {
            placeholderList = new HashSet<>();
        }

        SharedPreferences storyNumberSettings = getSharedPreferences(STORY_NUMBER_FILE, 0);
        storyIndex = storyNumberSettings.getInt("story_number", 404);
        storyIndex = 404; // Used to test spinner

        if(storyIndex == 404) {
            storyIndex = getIntent().getIntExtra("selected_story", 404);
        }

        InputStream is;
        is = this.getResources().openRawResource(getStoryNumber(storyIndex));
        story = new Story(is);

        for(String currentPlaceholder : placeholderList)
        {
            story.fillInPlaceholder(currentPlaceholder);
        }

        wordsLeftText = (TextView) findViewById(R.id.words_left);
        placeholderCount = story.getPlaceholderRemainingCount();
        wordsLeftText.setText("Words left: " + Integer.toString(placeholderCount));

        placeholderField = (EditText)findViewById(R.id.placeholder_field);
        String currentPlaceholder = story.getNextPlaceholder();
        placeholderField.setHint("submit a/an " + currentPlaceholder);

        // HERE TEXT TO SPEECH
        speech.speak("submit a/an " + currentPlaceholder, TextToSpeech.QUEUE_FLUSH, null); // This one doesn't speak?

        submitButton = (Button) findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String placeholderUserText = placeholderField.getText().toString();
                if(!placeholderUserText.equals("") && !placeholderList.contains(placeholderUserText)) {
                    story.fillInPlaceholder(placeholderUserText);
                    placeholderList.add(placeholderUserText);
                    placeholderField.setText("");
                    placeholderCount = story.getPlaceholderRemainingCount();
                    wordsLeftText.setText("Words left: " + Integer.toString(placeholderCount));

                    String currentPlaceholder = story.getNextPlaceholder();
                    placeholderField.setHint("submit a/an " + currentPlaceholder);

                    // HERE TEXT TO SPEECH
                    speech.speak("submit a/an " + currentPlaceholder, TextToSpeech.QUEUE_FLUSH, null); // Talks one time extra when finished

                    if(placeholderCount == 0) {
                        placeholderList.clear();
                        storyIndex = 404;
                        Intent i = new Intent(getApplicationContext(), StoryScreen.class);
                        i.putExtra("finished_story", story.toString());
                        startActivity(i);
                    }
                } else {
                    Toast errorEmptyField = Toast.makeText(getApplicationContext(), "The field is empty or you already entered this, try again.",
                            Toast.LENGTH_LONG);
                    errorEmptyField.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                    errorEmptyField.show();
                }
            }
        });

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    placeholderField.setText(result.get(0));
                }
                break;
            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences placeholderSettings = getSharedPreferences(PLACEHOLDER_LIST_FILE, 0);
        SharedPreferences.Editor editor1 = placeholderSettings.edit();
        editor1.putStringSet("placeholders", placeholderList);
        editor1.apply();

        SharedPreferences storyNumberSettings = getSharedPreferences(STORY_NUMBER_FILE, 0);
        SharedPreferences.Editor editor2 = storyNumberSettings.edit();
        editor2.putInt("story_number", storyIndex);
        editor2.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_title_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Not useful anymore with the spinner
    private int getRandomStoryIndex() {
        File storyDirectory = new File("../../res/");
        final int directorySize = storyDirectory.listFiles().length;
        Random randomGenerator = new Random();
        return randomGenerator.nextInt(directorySize);
    }

    private int getStoryNumber(int storyIndex) {
        switch (storyIndex) {
            case 0:
                return SIMPLE;
            case 1:
                return TARZAN;
            case 2:
                return UNIVERSITY;
            case 3:
                return CLOTHES;
            case 4:
                return DANCE;
            default:
                return 404;
        }
    }
}