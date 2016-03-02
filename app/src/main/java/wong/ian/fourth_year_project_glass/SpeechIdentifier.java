package wong.ian.fourth_year_project_glass;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Speech commands interface.
 */
public class SpeechIdentifier implements TextToSpeech.OnInitListener {

    private DatabaseConnection db = null;
    private static TextToSpeech converter = null;
    private static RESPONSE_TYPES responseType = null;
    private static boolean keepListening = false;
    private static String currentChemical = null;
    private static String currentLocation = null;
    private static String currentRoom = null;
    private static String currentCabinet = null;

    /**
     * List of responses that can be expected from the user.
     */
    public enum RESPONSE_TYPES {
        IDENTIFY, CHEMICAL_NAME, LOCATION, ROOM, CABINET
    }

    public SpeechIdentifier(Context context, String location, String room) {
        currentLocation = location;
        currentRoom = room;
        db = DatabaseConnection.getInstance();
        db.performLogin("kevin", "pass"); // TODO: do real authentication
        converter = new TextToSpeech(context, this);
    }

    /**
     * Takes a string representation of the user's voice command and handles it.
     * @param command
     * @return True if the command was recognized and handled
     */
    public boolean executeSpecificCommand(String command) {
        if (command != null) {
            // always convert the command to lower case for regex matching
            String regexCommand = command.toLowerCase();

            /**
             * Note: The reason for the order of commands is to provide priority.
             *       For example, the reset/cancel command is at the top of the list
             *       so that if the user says "Cancel that, I don't want to change room",
             *       then the system will only pick up the "cancel".
             */

            /**
             * Cancel/Reset = reset the speech interface
             */
            if ("cancel".equals(regexCommand) || "reset".equals(regexCommand)) {
                converter.speak("Voice off.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = false;
                responseType = null;
                return true;
            }
            /**
             * Perform the introduction
             */
            else if (regexCommand.contains("perform introduction")) {
                converter.speak("Welcome to the Chemical Inventory Tracking System, created for the Google Glass.", TextToSpeech.QUEUE_FLUSH, null);
                converter.speak("To activate your voice command system, tap on the Google Glass Touchpad once. You will hear a single click as the system activates.", TextToSpeech.QUEUE_ADD, null);
                converter.speak("To acquire a list of commands, tap the touchpad once, and then say, 'list commands'.", TextToSpeech.QUEUE_ADD, null);
                return true;
            }
            /**
             * List commands TODO: switch over to the Java.Command system and then "for each" the commands instead of hardcoding them
             */
            else if (regexCommand.contains("list commands")) {
                converter.speak("To cancel voice commands once active, say 'cancel' or 'reset'.", TextToSpeech.QUEUE_FLUSH, null);
                converter.speak("To begin an audit or inventory check, say 'start audit' or 'begin inventory'.", TextToSpeech.QUEUE_ADD, null);
                converter.speak("To change your location, say 'change location'.", TextToSpeech.QUEUE_ADD, null);
                converter.speak("To change your room, say 'select room'.", TextToSpeech.QUEUE_ADD, null);
                converter.speak("When opening a cabinet, say 'open cabinet'.", TextToSpeech.QUEUE_ADD, null);
                converter.speak("To be informed as to your current settings, say 'status please'. The please is very important.", TextToSpeech.QUEUE_ADD, null);
                return true;
            }
            /**
             * Expecting a response = handle
             */
            else if (responseType != null) {
                if (handleResponse(regexCommand)) {
                    // TODO: handle this differently
                    MainActivity.playTapSound();
                }
                return true;
            }
            /**
             * Identify/Add Chemical = attempt to add the chemical to the database
             */
            else if (regexCommand.contains("identify") || regexCommand.contains("add chemical")) {
                // check that a location, room and cabinet have been identified
                if (currentLocation == null) {
                    converter.speak("No location is currently identified, please say 'change location' before identify.", TextToSpeech.QUEUE_FLUSH, null);
                    return true;
                }
                else if (currentRoom == null) {
                    converter.speak("No room is currently identified, please say 'select room' before identify.", TextToSpeech.QUEUE_FLUSH, null);
                    return true;
                }
                else if (currentCabinet == null) {
                    converter.speak("No cabinet is currently open. Please say 'open cabinet' to add chemicals, or cancel to stop using voice commands.", TextToSpeech.QUEUE_FLUSH, null);
                    keepListening = true;
                    return true;
                }

                // TODO: use the DIP to get the name of the chemical
                currentChemical = "water";

                converter.speak("Is your chemical: " + currentChemical + "?", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = RESPONSE_TYPES.IDENTIFY;
                return true;
            }
            /**
             * Change Location = change the current location, then prompt for a room change
             */
            else if (regexCommand.contains("change location")) {
                // allow the user to say the location name
                converter.speak("Please say your location now.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = RESPONSE_TYPES.LOCATION;
                return true;
            }
            /**
             * Select Room = attempt to select the current room
             */
            else if (regexCommand.contains("select room")) {
                // if there is no location identified, prioritize getting that value first
                if (currentLocation == null) {
                    converter.speak("There is no location currently identified. Please specify a location before selecting a room.", TextToSpeech.QUEUE_FLUSH, null);
                    return true;
                }

                // allow the user to say the room name
                converter.speak("Please say the room name now.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = RESPONSE_TYPES.ROOM;
                return true;
            }
            /**
             * Cabinet = attempt to open a cabinet
             */
            else if (regexCommand.contains("open cabinet")) {
                // if there is no location identified, prioritize getting that value first
                if (currentLocation == null) {
                    converter.speak("There is no location currently identified. Please specify a location before selecting a room.", TextToSpeech.QUEUE_FLUSH, null);
                    return true;
                }

                // if there is no room identified, prioritize getting that value first
                if (currentRoom == null) {
                    converter.speak("There is no room currently identified. Please say 'select room' to choose a room now.", TextToSpeech.QUEUE_FLUSH, null);
                    keepListening = true;
                    return true;
                }

                // allow the user to say the cabinet name
                converter.speak("Please say the cabinet name now.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = RESPONSE_TYPES.CABINET;
                return true;
            }
            /**
             * Status = Retrieve all current parameters' statuses
             */
            else if (regexCommand.contains("status")) {
                converter.speak("Your location is " + ((currentLocation == null) ? "unspecified" : currentLocation), TextToSpeech.QUEUE_FLUSH, null);
                converter.speak("Your room is " + ((currentRoom == null) ? "unspecified" : currentRoom), TextToSpeech.QUEUE_ADD, null);
                converter.speak("The cabinet you have open is " + ((currentCabinet == null) ? "unspecified" : currentCabinet), TextToSpeech.QUEUE_ADD, null);
                converter.speak("The last chemical you have scanned is " + ((currentChemical == null) ? "unspecified" : currentChemical), TextToSpeech.QUEUE_ADD, null);
                return true;
            }
            /**
             * Audit/Inventory = Start the process
             */
            else if (regexCommand.contains("audit") || regexCommand.contains("inventory")) {
                // allow the user to say the location name
                converter.speak("Let's begin by identifying your current location. Please say your location now.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = RESPONSE_TYPES.LOCATION;
                return true;
            }
        }
        return false;
    }

    /**
     * Handles a response to a prompt from the voice command system.
     * @param lcCommand The lower case voice command that was uttered
     * @return True if the response was handled correctly
     */
    private boolean handleResponse(String lcCommand) {
        // previous command was identify, now handle the response
        if (responseType == RESPONSE_TYPES.IDENTIFY) {
            if ("yes".equals(lcCommand)) {
                if (currentChemical != null) {
                    return performDBAdd(currentChemical);
                }
                else {
                    converter.speak("There was an error adding your chemical to the database.", TextToSpeech.QUEUE_FLUSH, null);
                    responseType = null;
                    return true;
                }
            }
            else if ("no".equals(lcCommand)) {
                converter.speak("Please say the name of the chemical.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = RESPONSE_TYPES.CHEMICAL_NAME;
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
        else if (responseType == RESPONSE_TYPES.CHEMICAL_NAME) {
            return performDBAdd(lcCommand);
        }
        // the user was asked for the current location
        else if (responseType == RESPONSE_TYPES.LOCATION) {
            if (lcCommand != null) {
                // add the location to the database or fail immediately
                if (!db.setGeoVariable(DatabaseConnection.GEO_VAR.LOCATION, lcCommand)) {
                    Log.i("TextToSpeech", "There was an error setting the location to " + lcCommand + ".");
                    converter.speak("There was an error setting the location. Your request could not be completed.", TextToSpeech.QUEUE_FLUSH, null);
                    responseType = null;
                    return false;
                }
                currentLocation = lcCommand;
                currentRoom = null;

                // allow the user to say the room name
                converter.speak("Please say the room name now.", TextToSpeech.QUEUE_FLUSH, null);
                keepListening = true;
                responseType = RESPONSE_TYPES.ROOM;

                return true;
            }
            converter.speak("Your location could not be ascertained, please try again.", TextToSpeech.QUEUE_FLUSH, null);
            keepListening = true;
            return false;
        }
        // the user was asked for the current room
        else if (responseType == RESPONSE_TYPES.ROOM) {
            if (lcCommand != null) {
                // add the room to the database or fail immediately
                if (!db.setGeoVariable(DatabaseConnection.GEO_VAR.ROOM, lcCommand)) {
                    Log.i("TextToSpeech", "There was an error setting the room to " + lcCommand + ".");
                    converter.speak("There was an error setting the room. Your request could not be completed.", TextToSpeech.QUEUE_FLUSH, null);
                    responseType = null;
                    return false;
                }
                currentRoom = lcCommand;
                responseType = null;
                return true;
            }
            converter.speak("The room name could not be ascertained, please try again.", TextToSpeech.QUEUE_FLUSH, null);
            keepListening = true;
            return true;
        }
        // the user was asked for the current location
        else if (responseType == RESPONSE_TYPES.CABINET) {
            if (lcCommand != null) {
                // add the cabinet to the database or fail immediately
                if (!db.setGeoVariable(DatabaseConnection.GEO_VAR.CABINET, lcCommand)) {
                    Log.i("TextToSpeech", "There was an error setting the cabinet to " + lcCommand + ".");
                    converter.speak("There was an error setting the cabinet. Your request could not be completed.", TextToSpeech.QUEUE_FLUSH, null);
                    responseType = null;
                    return false;
                }
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
            responseType = RESPONSE_TYPES.CHEMICAL_NAME;
            currentChemical = null;
            return false;
        }

        currentChemical = lcCommand;

        // database add new container
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

    /**
     * Shuts down the textToSpeech to be restarted later
     */
    public void shutdown() {
        if (converter != null) {
            converter.shutdown();
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
