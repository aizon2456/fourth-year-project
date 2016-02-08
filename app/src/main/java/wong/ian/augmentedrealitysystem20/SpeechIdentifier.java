package wong.ian.augmentedrealitysystem20;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class SpeechIdentifier implements TextToSpeech.OnInitListener {

    private DatabaseConnection db = null;
    private TextToSpeech converter = null;
    private COMMANDS responseType = null;
    private boolean keepListening = false;
    private String currentChemical = null;
    private String currentLocation = null;
    private String currentRoom = null;
    private String currentCabinet = null;

    public enum COMMANDS {
        IDENTIFY, CHEMICAL_NAME, LOCATION, ROOM, CABINET
    }

    public SpeechIdentifier(Context context, String location, String room) {
        currentLocation = location;
        currentRoom = room;
        db = DatabaseConnection.getInstance();
        converter = new TextToSpeech(context, this);
    }

    // returns true if there is a specific command associated with the input
    public boolean executeSpecificCommand(String command) {
        if (command != null) {
            String regexCommand = command.toLowerCase();

            // always check if the word is cancel or reset, and if so, reset everything
            if ("cancel".equals(regexCommand) || "reset".equals(regexCommand)) {
                converter.speak("Voice off.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = false;
                responseType = null;
                return true;
            }

            // response is expected
            if (responseType != null) {
                handleResponse(regexCommand);
                return true;
            }
            // identify
            else if (regexCommand.contains("identify")) {
                // check that a cabinet has been identified
                if (currentCabinet == null) {
                    converter.speak("No cabinet is currently open. Please identify a cabinet before adding chemicals.", TextToSpeech.QUEUE_FLUSH, null);
                    return true;
                }

                // TODO: use the DIP to get the name of the chemical
                currentChemical = "water";

                converter.speak("Is your chemical: " + currentChemical + "?", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = COMMANDS.IDENTIFY;
                return true;
            }
            // location
            else if (regexCommand.contains("location")) {
                // allow the user to say the location name
                converter.speak("Please say your location now.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = COMMANDS.LOCATION;
                return true;
            }
            // room
            else if (regexCommand.contains("room")) {
                // allow the user to say the room name
                converter.speak("Please say the room name now.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = COMMANDS.ROOM;
                return true;
            }
            // cabinet
            else if (regexCommand.contains("cabinet")) {
                // allow the user to say the cabinet name
                converter.speak("Please say the cabinet name now.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = COMMANDS.CABINET;
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
                    return performDBAdd(currentChemical);
                }
                else {
                    converter.speak("No container currently identified, please try again.", TextToSpeech.QUEUE_FLUSH, null);
                    keepListening = true;
                    responseType = null;
                    return true;
                }
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
            return performDBAdd(lcCommand);
        }
        // the user was asked for the current location
        else if (responseType == COMMANDS.LOCATION) {
            if (lcCommand != null) {
                currentLocation = lcCommand;
                responseType = null;
                return true;
            }
            converter.speak("Your location could not be ascertained, please try again.", TextToSpeech.QUEUE_FLUSH, null);
            keepListening = true;
            return false;
        }
        // the user was asked for the current room
        else if (responseType == COMMANDS.ROOM) {
            if (lcCommand != null) {
                currentRoom = lcCommand;
                responseType = null;
                return true;
            }
            converter.speak("The room name could not be ascertained, please try again.", TextToSpeech.QUEUE_FLUSH, null);
            keepListening = true;
            return true;
        }
        // the user was asked for the current location
        else if (responseType == COMMANDS.CABINET) {
            if (lcCommand != null) {
                currentCabinet = lcCommand;
                responseType = null;
                return true;
            }
            converter.speak("The cabinet name could not be ascertained, please try again.", TextToSpeech.QUEUE_FLUSH, null);
            keepListening = true;
            return true;
        }

        return false;
    }

    private boolean performDBAdd(String lcCommand) {
        if (!db.queryChemical(lcCommand)) {
            Log.i("TextToSpeech", "There is no such chemical " + lcCommand + " in the database.");
            converter.speak("There is no chemical " + lcCommand + " in the database. Please say the name of the chemical you wish to add.", TextToSpeech.QUEUE_FLUSH, null);
            keepListening = true;
            responseType = COMMANDS.CHEMICAL_NAME;
            currentChemical = null;
            return false;
        }

        currentChemical = lcCommand;

        // TODO: database add new container (need to not hardcode the inputs)
        if (!db.createContainer(currentLocation, currentRoom, currentCabinet, currentChemical)) {
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
