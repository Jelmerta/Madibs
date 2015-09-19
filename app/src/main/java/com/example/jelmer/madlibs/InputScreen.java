package com.example.jelmer.madlibs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class InputScreen extends AppCompatActivity {

    final static int SIMPLE = R.raw.madlib0_simple;
    final static int TARZAN = R.raw.madlib1_tarzan;
    final static int UNIVERSITY = R.raw.madlib2_university;
    final static int CLOTHES = R.raw.madlib3_clothes;
    final static int DANCE = R.raw.madlib4_dance;

    public static final String PLACEHOLDER_LIST_FILE = "placeholder_file";
    public static final String STORY_NUMBER_FILE = "story_number_file";
    Button submitButton;

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

        // Restore preferences
        SharedPreferences placeholderSettings = getSharedPreferences(PLACEHOLDER_LIST_FILE, 0);
        placeholderList = placeholderSettings.getStringSet("placeholders", null);
        if(placeholderList == null)
        {
            placeholderList = new HashSet<>();
        }

        SharedPreferences storyNumberSettings = getSharedPreferences(STORY_NUMBER_FILE, 0);
        storyIndex = storyNumberSettings.getInt("story_number", 404);

        if(storyIndex == 404) {
            storyIndex = getRandomStoryIndex();
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
                    if(placeholderCount == 0) { // this part: not perfect, but it's fine
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
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences placeholderSettings = getSharedPreferences(PLACEHOLDER_LIST_FILE, 0);
        SharedPreferences.Editor editor1 = placeholderSettings.edit();
        editor1.putStringSet("placeholders", placeholderList);
        editor1.commit();

        SharedPreferences storyNumberSettings = getSharedPreferences(STORY_NUMBER_FILE, 0);
        SharedPreferences.Editor editor2 = storyNumberSettings.edit();
        editor2.putInt("story_number", storyIndex);
        editor2.commit();
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