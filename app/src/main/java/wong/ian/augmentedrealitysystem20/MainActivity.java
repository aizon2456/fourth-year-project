package wong.ian.augmentedrealitysystem20;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private SimpleScreen screen = null;
    private Camera mCamera = null;
    private Preview mPreview = null;
    private SyncInt syncVal = null;
    private ProgressBar progressBar = null;
    private SpeechRecognizer sr = null;
    private boolean recording = false;
    private boolean screenActive = true;
    private DatabaseConnection database = null;
    private SpeechIdentifier identifier = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        setContentView(R.layout.main_layout);

        // display the valid login notification
        AlertDialog loginSuccess = new AlertDialog.Builder(this).create();
        loginSuccess.setTitle("Success!");
        loginSuccess.setMessage(getIntent().getStringExtra("user") + " logged in successfully!");
        loginSuccess.show();

        database = new DatabaseConnection();
        identifier = new SpeechIdentifier(this, database);

        FrameLayout layout = (FrameLayout) findViewById(R.id.systemFrame);
        if (layout != null) {
            mPreview = new Preview(getBaseContext());
            layout.addView(mPreview,0);
        }

        if (safeCameraOpen()) {
            Log.d(getString(R.string.app_name), " opened Camera successfully!");
            mPreview.setCamera(mCamera);
        }

        syncVal = new SyncInt(100);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        /*progressBar = new ProgressBar(getBaseContext(), null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams parameters = new FrameLayout.LayoutParams(display.getWidth(), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        if (layout != null) {
            layout.addView(progressBar, parameters);
            progressBar.bringToFront();
            Log.d("progressBar", "Successfully added progress bar.");
        }*/

        screen = (SimpleScreen)findViewById(R.id.my_screen);
        screen.setOnTouchListener(new TouchListener());

        // the thread for the progress bar
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // thread synchronization
                    int localValue;
                    synchronized (syncVal) {
                        localValue = syncVal.getValue();

                        // check for <100
                        if (localValue < 100) {
                            localValue++;
                            syncVal.setValue(localValue);
                            progressBar.setProgress(localValue);
                        }
                        else {
                        }
                    }

                    // update the UI in the main thread
                    if (localValue < 100) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.VISIBLE);
                                progressBar.bringToFront();
                            }
                        });
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        });
                    }

                    // sleep for a designated period of time before reading any more containers
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        sr = SpeechRecognizer.createSpeechRecognizer(getBaseContext());
        sr.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("SpeechRecognizer", "ready");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("simple", "startOfSpeech");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                Log.d("simple", "rmsChanged");
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.d("simple", "buffer");
            }

            @Override
            public void onEndOfSpeech() {
                Log.d("simple", "endOfSpeech");
            }

            @Override
            public void onError(int error) {
                // if speech timeout
                if (error == 6) {
                    ListView output = (ListView) findViewById(R.id.voiceChoicesList);
                    output.setVisibility(View.INVISIBLE);

                    TextView textOutput = (TextView) findViewById(R.id.textArea);
                    textOutput.setVisibility(View.INVISIBLE);

                    ImageButton b = (ImageButton) findViewById(R.id.imageButton);
                    b.setImageResource(R.mipmap.voice_recorder);
                    recording = false;
                    identifier.executeSpecificCommand("reset");
                }
                else {
                    Log.e("SpeechRecognizer", "Error: " + error);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> resultData = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                for (int i = 0; i < resultData.size(); i++) {
                    Log.d("options", resultData.get(i));
                }

                recording = false;
                ImageButton b = (ImageButton) findViewById(R.id.imageButton);
                b.setImageResource(R.mipmap.voice_recorder);

                TextView textOutput = (TextView) findViewById(R.id.textArea);
                textOutput.setVisibility(View.INVISIBLE);

                // check if the first entry matches a specific command
                if (identifier.executeSpecificCommand(resultData.get(0))) {
                    return;
                }

                ListView output = (ListView) findViewById(R.id.voiceChoicesList);
                final BackgroundArrayAdapter chemicalChoices = new BackgroundArrayAdapter(getBaseContext(), android.R.layout.test_list_item, resultData);
                output.setAdapter(chemicalChoices);
                output.setVisibility(View.VISIBLE);

                output.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        ListView output = (ListView) findViewById(R.id.voiceChoicesList);
                        output.setVisibility(View.INVISIBLE);

                        String voiceString = (String) output.getItemAtPosition(position);
                        setTextResponse("Container: " + voiceString);
                    }

                });
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                Log.d("SpeechRecognizer", "Partial results.");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                Log.d("SpeechRecognizer", "Event occurred");
            }
        });

        // create a thread to check if the speech recognition needs to be activated
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if (identifier.isKeepListening()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listenForSpeech(null);
                            }
                        });
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }).start();
    }

    public void setTextResponse(String text) {
        TextView textOutput = (TextView) findViewById(R.id.textArea);
        textOutput.setText(text);
        if (text.length() > 0) {
            textOutput.setVisibility(View.VISIBLE);
        }

        // start a thread that will hide the textarea after a period of time
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textOutput = (TextView) findViewById(R.id.textArea);
                        textOutput.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }).start();
    }

    public void listenForSpeech(View view) {
        ImageButton b = (ImageButton) findViewById(R.id.imageButton);
        if (!recording) {
            sr.cancel();

            // start the intent and speak away
            Intent recordVoice = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recordVoice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recordVoice.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            recordVoice.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "voice.recognition.test");
            sr.startListening(recordVoice);

            // change the icon to active
            b.setImageResource(R.mipmap.voice_active);

            // display the textbox with "Speak Now!"
            TextView textOutput = (TextView) findViewById(R.id.textArea);
            textOutput.setText("Speak Now!");
            textOutput.setVisibility(View.VISIBLE);

            // hide the list of options
            ListView output = (ListView) findViewById(R.id.voiceChoicesList);
            output.setVisibility(View.INVISIBLE);

            recording = true;
        }
        else {
            // deactivate the listener
            sr.cancel();

            // change the icon to inactive
            b.setImageResource(R.mipmap.voice_recorder);

            // hide the textbox
            TextView textOutput = (TextView) findViewById(R.id.textArea);
            textOutput.setText("");
            textOutput.setVisibility(View.INVISIBLE);

            recording = false;
            identifier.executeSpecificCommand("reset");
        }
    }

    private void releaseCameraAndPreview() {
        mPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private boolean safeCameraOpen() {
        boolean qOpened = false;

        try {
            releaseCameraAndPreview();
            mCamera = Camera.open();
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    public void pausePlayScreen(View view) {
        ImageButton btn = (ImageButton) findViewById(R.id.pausePlayButton);
        // if active, make inactive
        if (screenActive) {
            mPreview.stopPreviewAndFreeCamera();
            screenActive = false;

            // change the display icon to play
            btn.setImageResource(R.mipmap.play_button);
        }
        // else, make active
        else {
            if (safeCameraOpen()) {
                Log.d(getString(R.string.app_name), " reopened Camera successfully!");
                mPreview.setCamera(mCamera);
                screenActive = true;

                // change the display icon to pause
                btn.setImageResource(R.mipmap.pause_button);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        screen.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        screen.onResume();
    }

    // disables the back button
    @Override
    public void onBackPressed() {
    }

    private class BackgroundArrayAdapter extends ArrayAdapter<String> {


        public BackgroundArrayAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView resultingView = (TextView) super.getView(position,convertView,parent);

            resultingView.setBackgroundResource(R.mipmap.blue_button);
            resultingView.setGravity(Gravity.CENTER);

            return resultingView;
        }

    }

    private class TouchListener implements View.OnTouchListener {

        public boolean onTouch(View v, MotionEvent m) {
            if (m.getAction() == MotionEvent.ACTION_DOWN || m.getAction() == MotionEvent.ACTION_MOVE) {
                TextView output = (TextView) findViewById(R.id.textArea);
                screen.handleScreenTouch(m, output);
                synchronized (syncVal) {
                    syncVal.setValue(0);
                }
            }
            else if (m.getAction() == MotionEvent.ACTION_UP) {
                screen.resetOffsets();
            }

            return true;
        }
    }

}
