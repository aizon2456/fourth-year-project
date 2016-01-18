package wong.ian.augmentedrealitysystem20;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class SpeechIdentifier implements TextToSpeech.OnInitListener {

    private DatabaseConnection db = null;
    private TextToSpeech converter = null;
    private String currentChemical = null;
    private COMMANDS responseType = null;
    private boolean keepListening = false;

    public enum COMMANDS {
        IDENTIFY, CHEMICAL_NAME
    }

    public SpeechIdentifier(Context context, DatabaseConnection db) {
        this.db = db;
        converter = new TextToSpeech(context, this);
    }

    // returns true if there is a specific command associated with the input
    public boolean executeSpecificCommand(String command) {
        if (command != null) {
            String regexCommand = command.toLowerCase();

            // always check if the word is cancel or reset, and if so, reset everything
            if ("cancel".equals(regexCommand) || "reset".equals(regexCommand)) {
                converter.speak("Voice commands deactivated.", TextToSpeech.QUEUE_FLUSH, null);
                responseType = null;
                keepListening = false;
                return true;
            }

            // response is expected
            if (responseType != null) {
                handleResponse(regexCommand);
                return true;
            }
            // identify
            else if (regexCommand.contains("identify")) {
                // TODO: use the DIP to get the name of the chemical, and set currentChemical
                currentChemical = "water";
                converter.speak("Is your chemical: " + currentChemical + "?", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = COMMANDS.IDENTIFY;
                return true;
            }
        }
        return false;
    }

    // lcCommand is the lower case command
    // return true if the response was handled correctly
    private boolean handleResponse(String lcCommand) {
        // previous command was identify, now handle the response
        if (responseType == COMMANDS.IDENTIFY) {
            if ("yes".equals(lcCommand)) {
                if (currentChemical != null) {
                    // TODO: database add new container (need to not hardcode the inputs)
                    if (!db.createContainer("valid", "valid", "valid", currentChemical)) {
                        Log.e("TextToSpeech", "There was an error adding " + currentChemical + " to the database.");
                        converter.speak("Chemical " + currentChemical + " could not be added to the database.", TextToSpeech.QUEUE_FLUSH, null);
                        responseType = null;
                        return false;
                    }
                    Log.i("TextToSpeech", "Chemical " + currentChemical + " added to the database.");
                    converter.speak("Added " + currentChemical + " successfully.", TextToSpeech.QUEUE_FLUSH, null);
                }
                else {
                    converter.speak("No container currently identified, please try again.", TextToSpeech.QUEUE_FLUSH, null);
                    keepListening = true;
                }
                responseType = null;
                return true;
            }
            else if ("no".equals(lcCommand)) {
                converter.speak("Please say the name of the chemical.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = COMMANDS.CHEMICAL_NAME;
                currentChemical = null;
                return true;
            }
            else {
                converter.speak("Your response could not be understood, please try again.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                return true;
            }
        }
        // the user was asked for the proper chemical to be added to the database
        else if (responseType == COMMANDS.CHEMICAL_NAME) {
            currentChemical = null;

            if (!db.queryChemical(lcCommand)) {
                Log.i("TextToSpeech", "There is no such chemical " + lcCommand + " in the database.");
                converter.speak("There is no such chemical " + lcCommand + " in the database. Please try again.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                return false;
            }

            currentChemical = lcCommand;

            // TODO: database add new container (need to not hardcode the inputs)
            if (!db.createContainer("Ministry of Love", "Room 101", "1", currentChemical)) {
                Log.e("TextToSpeech", "There was an error adding " + currentChemical + " to the database.");
                converter.speak("Chemical " + currentChemical + " could not be added to the database.", TextToSpeech.QUEUE_FLUSH, null);
                responseType = null;
                return false;
            }
            Log.i("TextToSpeech", "Chemical " + currentChemical + " added to the database.");
            converter.speak("Added " + currentChemical + " successfully.", TextToSpeech.QUEUE_FLUSH, null);

            responseType = null;
            return true;
        }

        return false;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            converter.setLanguage(Locale.US);
            responseType = null;
        } else {
            Log.e("TextToSpeech", "Text to Speech converter could not be created.");
        }
    }

    public boolean isKeepListening() {
        if (keepListening && !converter.isSpeaking()) {
            keepListening = false;
            return true;
        }
        else {
            return false;
        }
    }
}
