package wong.ian.fourth_year_project_glass;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Provides the internal database calls.
 */
public class DatabaseConnection {

    private static DatabaseConnection singleInstance = null;

    // the URL for contacting the database
    private final String dbURL = "http://chemicaltracker.elasticbeanstalk.com/api/";

    public static final String SUCCESS = "success";
    private String loginProperty = null;

    private transient ChemicalContainer currentContainer = null;

    // Single-thread, singleton database executor to ensure database integrity
    private ExecutorService executor = null;

    // Enumeration with all types of geovariable options
    public enum GEO_VAR {
        LOCATION, ROOM, CABINET
    }

    private DatabaseConnection() {
        executor = Executors.newSingleThreadExecutor();
        currentContainer = new ChemicalContainer();
    }

    // singleton accessor
    public static DatabaseConnection getInstance() {
        if (singleInstance == null) {
            singleInstance = new DatabaseConnection();
        }
        return singleInstance;
    }

    /**
     * Send the container to the database.
     * @return
     */
    public boolean createDBContainer() {
        // if the chemical was not validated ahead of time, the container will be null
        if (currentContainer == null) {
            return false;
        }

        JSONObject response = modifyChemical(true);

        // if no response was acquired, then log an error
        if (response == null) {
            currentContainer = null;
            Log.e("CreateContainer", "The container could not be created.");

            return false;
        }

        // handle the results in the UI
        try {
            // if the response indicates a failure
            if (!response.getBoolean("success")) {
                Log.e("DatabaseError-" + response.getString("status"), response.getString("message"));
                currentContainer = null;
                return false;
            }

            // otherwise log the information
            Log.d("DatabaseResponse-" + response.getString("status"), response.getString("message"));

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets the current location,  by creating the JSON required for the addition or removal.
     * @param type The particular variable to update
     * @param value The new value
     * @return True if successful, false if not
     */
    public boolean setGeoVariable(GEO_VAR type, String value) {
        if (currentContainer == null) {
            Log.e("DatabaseAccess", "The currently identified container is empty.");
            return false;
        }
        else if (value == null) {
            Log.e("DatabaseAccess", "The variable identified is null.");
            return false;
        }
        else if (type == null) {
            Log.e("DatabaseAccess", "The type is null.");
            return false;
        }

        JSONObject updateObj = new JSONObject();

        try {
            updateObj.put("request", "ADD"); // add

            /**
             * Create a cascading JSONObject parameter creator, and simultaneously update
             * the DatabaseConnection local variable with the new value.
             *
             *  -> Add Location (stop if type is location)
             *      -> Add Room (stop if type is room)
             *          -> Add Cabinet (stop if type is cabinet)
             */
            // check if location
            if (type == GEO_VAR.LOCATION) {
                updateObj.put("location", value);
                currentContainer.setLocation(value);
            }
            else {
                updateObj.put("location", currentContainer.getLocation());

                // check if room
                if (type == GEO_VAR.ROOM) {
                    updateObj.put("room", value);
                    currentContainer.setRoom(value);
                }
                else {
                    updateObj.put("room", currentContainer.getRoom());

                    // check if cabinet
                    if (type == GEO_VAR.CABINET) {
                        updateObj.put("cabinet", value);
                        currentContainer.setCabinet(value);
                    }
                    else {
                        Log.e("DatabaseAccess", "There is an erroneous type in use: " + type.name());
                        return false;
                    }
                }
            }

            // passes the JSONObject and the URL path for the operation
            JSONObject response = performDBOperation(updateObj, type.name().toLowerCase());

            // check for success
            if (!response.getBoolean("success")) {
                // acceptable error message (already exists in DB)
                if ("STORAGE_ALREADY_EXISTS".equals(response.getString("status"))) {}
                // TODO: identify specific problem messages
                else{
                    Log.i("DatabaseAccess", "Unsuccessful: " + response.getString("status") + ", " +
                            response.getString("message"));
                    return false;
                }
            }

            // return that everything worked fine
            return true;

        } catch (Exception e) {
            e.printStackTrace();

        }

        return false;
    }

    /**
     * Creates the JSON to modify or add an existing chemical in the database.
     * @param isAddOperation True if the operation is Add, False if Remove
     * @return The result of the operation to parse
     */
    public JSONObject modifyChemical(boolean isAddOperation) {
        if (currentContainer == null) {
            Log.i("DatabaseAccess", "The currently identified container is empty.");
            return null;
        }

        JSONObject updateObj = new JSONObject();

        try {
            updateObj.put("request", (isAddOperation) ? "ADD" : "REMOVE"); // add or remove
            updateObj.put("location", currentContainer.getLocation());
            updateObj.put("room", currentContainer.getRoom());
            updateObj.put("cabinet", currentContainer.getCabinet());
            updateObj.put("chemical", currentContainer.getChemicalName());

            return performDBOperation(updateObj, "chemical");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Checks if a chemical name exists, and sets the chemical properties of the
     * current chemical if so.
     * @param chemicalName
     * @return True if the chemical exists in the database.
     */
    public boolean queryChemical(String chemicalName) {

        JSONObject queryObj = new JSONObject();

        try {
            queryObj.put("chemical", chemicalName);

            JSONObject response = performDBOperation(queryObj);

            // if the chemical exists, then set the current container
            if (response != null && response.getBoolean("match")) {
                JSONObject properties = response.getJSONObject("properties");
                currentContainer.setFlammability(properties.getInt("flammability"));
                currentContainer.setHealth(properties.getInt("health"));
                currentContainer.setInstability(properties.getInt("instability"));
                currentContainer.setNotice(properties.getString("notice"));
                currentContainer.setChemicalName(chemicalName);
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if a list chemical names exist, and returns the first match
     * @param chemicalNames
     * @return True if the chemical exists in the database.
     */
    public String queryChemicals(String[] chemicalNames) {

        JSONObject queryObj = new JSONObject();

        try {
            JSONArray chemicals = new JSONArray(chemicalNames);
            queryObj.put("chemicals", chemicals);

            JSONObject response = performDBOperation(queryObj, "partial");

            Log.d("JSONReturn", "JSON:\n" + response.toString(2));

            // if the chemical exists, then set the current container
            if (response != null) {
                JSONArray matches = response.getJSONArray("matches");
                // return the first match in the array
                return matches.getString(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Execute the query given by the JSONObject parameter.
     * @param body The JSONObject used to query
     * @return The response to the query from the database.
     */
    private JSONObject performDBOperation(final JSONObject body) {
        return performDBOperation(body, null);
    }

    /**
     * Execute the query or update given by the JSONObject parameter.
     * @param body The JSONObject used to query or update
     * @return The response to the query or update from the database.
     */
    private JSONObject performDBOperation(final JSONObject body, final String updateType) {
        // sets up a callable DB thread
        Callable<JSONObject> activeDBTask = new Callable<JSONObject>() {
            @Override
            public JSONObject call() throws Exception {
                try {
                    boolean isUpdate = updateType != null && !("partial").equals(updateType);

                    Log.d("DBCall", "Performing " + ((isUpdate) ? "an update" : "a query") + " operation...");
                    Log.d("JSONLOG-SentToDB", "JSON:\n" + body.toString(2));

                    // create the URL from the hardcoded URL
                    String urlString = dbURL;
                    if (isUpdate) {
                        urlString += "update/" + updateType;
                    }
                    else {
                        if (("partial").equals(updateType)) {
                            urlString += "partialQueries";
                        }
                        else {
                            urlString += "query";
                        }
                    }
                    URL url = new URL(urlString);

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setRequestProperty("Authorization", loginProperty);

                    // send the JSONObject to the database
                    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                    writer.write(body.toString());
                    writer.flush();

                    // get the response from the query if the database responded
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                connection.getInputStream(), Charset.defaultCharset()));

                        StringBuilder builder = new StringBuilder();

                        String currentLine;
                        while ((currentLine = reader.readLine()) != null) {
                            builder.append(currentLine);
                        }

                        reader.close();

                        // build the response string to a JSONObject
                        JSONObject result = new JSONObject(builder.toString());

                        Log.d("JSONLOG-ReplyFromDB", "JSON:\n" + result.toString(2));

                        return result;
                    }

                } catch (Exception e) { // can catch timeouts
                    Log.d("JSONLOG-Error", "JSON: " + e.getMessage());
                }

                return null;
            }
        };

        // setup the future return for the callable
        Future<JSONObject> future = executor.submit(activeDBTask);

        // get the database result in under 3000 milliseconds (or 3 seconds)
        try {
            JSONObject response = future.get();
            return response;
        } catch (Exception e) {
            Log.d("FutureThread-Error", "Thread: " + e.getMessage());
        }

        return null;
    }

    /**
     * Execute a login query, after which the user can access the system if successful.
     * @param username
     * @param password
     * @return
     */
    public String performLogin(final String username, final String password) {
        // sets up a callable DB thread
        final String auth = "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
        Callable<String> activeDBTask = new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    // setup the hardcoded URL
                    URL url = new URL(dbURL + "authorize");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setRequestProperty("Authorization", auth);

                    // no body is required in order to get a response, but output needs to be written
                    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                    writer.write("");
                    writer.flush();

                    // log the response
                    Log.d("LoginResponse", connection.getResponseMessage());

                    // get the response from the query if the database responded
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        Log.i("Login", "Login for " + username + " was successful!");
                        return SUCCESS;
                    }

                    Log.i("Login", "Login for " + username + " failed!\n");
                    return "Invalid credentials, please try again.";

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // null indicates an error occurred during the process
                return null;
            }
        };

        // setup the future return for the callable
        Future<String> future = executor.submit(activeDBTask);

        try {
            // if true returned, save the value
            if (future.get() != null) {
                loginProperty = auth;
                return future.get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "There was an error contacting the database.";
    }

    public ChemicalContainer getCurrentContainer() {
        return currentContainer;
    }
}