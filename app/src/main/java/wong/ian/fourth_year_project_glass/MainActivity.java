package wong.ian.fourth_year_project_glass;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Main Activity.
 */
public class MainActivity extends Activity{

    private Preview mPreview = null;
    private SpeechRecognizer sr = null;
    private boolean isListening = false;
    private SpeechIdentifier identifier = null;
    private static AudioManager audio = null;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // create the audio handler
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // create the speech identifier given a particular location and room
        identifier = new SpeechIdentifier(this, getIntent().getStringExtra("location"), getIntent().getStringExtra("room"));

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.main_area);
        if (layout != null) {
            mPreview = new Preview(getBaseContext());
            mPreview.setVisibility(View.VISIBLE);
            layout.addView(mPreview, 0);
        }

        sr = SpeechRecognizer.createSpeechRecognizer(getBaseContext());
        sr.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.v("SpeechRecognizer", "ready");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.v("SpeechRecognizer", "startOfSpeech");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                Log.v("SpeechRecognizer", "rmsChanged");
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.v("SpeechRecognizer", "buffer");
            }

            @Override
            public void onEndOfSpeech() {
                Log.v("SpeechRecognizer", "endOfSpeech");
            }

            @Override
            public void onError(int error) {
                // if speech timeout
                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    setTextResponse("Voice off.");
                    identifier.executeSpecificCommand("reset");
                    sr.stopListening();
                    sr.cancel();
                    isListening = false;
                } else {
                    Log.e("SpeechRecognizer", "Error: " + error);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> resultData = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                for (int i = 0; i < resultData.size(); i++) {
                    Log.d("SpeechRecognizer", resultData.get(i));
                }

                // check for exit
                if (resultData.get(0).toLowerCase().contains("exit")) {
                    // TODO: exit to glass menu?
                }

                // check if the first entry matches a command in a particular list
                if (identifier.executeSpecificCommand(resultData.get(0))) {
                    isListening = false;
                    Log.d("SpeechRecognizer", "Stopped Listening");
                    return;
                }

                // if not, show the user what they said
                setTextResponse("Input: " + resultData.get(0));
                isListening = false;
                Log.i("SpeechRecognizer", "Stopped Listening");
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
                                Intent recordVoice = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                                recordVoice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                                recordVoice.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                                recordVoice.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "voice.recognition.test");
                                sr.startListening(recordVoice);
                                isListening = true;
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
        TextView textOutput = (TextView) findViewById(R.id.user_feedback_textbox);
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
                        TextView textOutput = (TextView) findViewById(R.id.user_feedback_textbox);
                        textOutput.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.releaseCameraAndPreview();
        sr.stopListening();
        sr.cancel();
        isListening = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPreview.safeCameraOpen();
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        // swipe up for voice commands
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
            // if not active, start the intent
            if (!isListening) {
                Intent recordVoice = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recordVoice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                recordVoice.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                recordVoice.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "voice.recognition.test");
                sr.startListening(recordVoice);
                isListening = true;

                // make a sound to indicate voice start
                playTapSound();

                return true;
            }

            // if active, then end the listening
            sr.stopListening();
            sr.cancel();
            isListening = false;

            // make a sound to indicate voice ending
            playTapSound();

            return true;
        }
        return super.onKeyDown(keycode, event);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            getMenuInflater().inflate(R.menu.voice_menu, menu);
            return true;
        }
        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.voice_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (item.getItemId()) {
                case R.id.start_the_audit:
                    // handle top-level dogs menu item
                    break;
                case R.id.view_a_report:
                    // handle top-level cats menu item
                    break;
                default:
                    return true;
            }
            return true;
        }
        // Not a voice command
        return super.onMenuItemSelected(featureId, item);
    }

    public static void playTapSound() {
        audio.playSoundEffect(Sounds.TAP);
    }
}
