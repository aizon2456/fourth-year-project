package wong.ian.augmentedrealitysystem20;

import android.util.Base64;
import android.util.Log;

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

    private DatabaseConnection() {
        executor = Executors.newSingleThreadExecutor();
    }

    // singleton accessor
    public static DatabaseConnection getInstance() {
        if (singleInstance != null) {
            return singleInstance;
        }
        return new DatabaseConnection();
    }

    /**
     * Create a new Container based on a location, room, cabinet, and chemical name.
     * @param location
     * @param room
     * @param cabinet
     * @param chemicalName
     * @return
     */
    public boolean createContainer(String location, String room, String cabinet, String chemicalName) {
        // if the chemical was not validated ahead of time, the container will be null
        if (currentContainer == null) {
            return false;
        }

        currentContainer.setCabinet(cabinet);
        currentContainer.setChemicalName(chemicalName);
        currentContainer.setLocation(location);
        currentContainer.setRoom(room);

        JSONObject response = modifyContainer(true);

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
     * Creates the JSON to modify or add an existing container in the database.
     * @param isAddOperation True if the operation is Add, False if Remove
     * @return The result of the operation to parse
     */
    public JSONObject modifyContainer(boolean isAddOperation) {
        if (currentContainer == null) {
            Log.i("DatabaseAccess", "The currently identified container is empty.");
            return null;
        }

        JSONObject updateObj = new JSONObject();

        try {
            updateObj.put("requestType", (isAddOperation) ? "ADD" : "REMOVE"); // add or remove
            updateObj.put("location", currentContainer.getLocation());
            updateObj.put("room", currentContainer.getRoom());
            updateObj.put("cabinet", currentContainer.getCabinet());
            updateObj.put("chemical", currentContainer.getChemicalName());

            return performDBQuery(updateObj, true);

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

            JSONObject response = performDBQuery(queryObj, false);

            // if the chemical exists, then set the current container
            if (response != null && response.getBoolean("match")) {
                JSONObject properties = response.getJSONObject("properties");
                currentContainer = new ChemicalContainer(
                        properties.getInt("flammability"),
                        properties.getInt("health"),
                        properties.getInt("instability"),
                        properties.getString("notice"),
                        response.getString("chemical")
                );
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Execute the query given by the JSONObject parameter.
     * @param body The JSONObject used to query or update
     * @param isUpdate True if an update, False if a query operation
     * @return The response to the query from the database.
     */
    private JSONObject performDBQuery(final JSONObject body, final boolean isUpdate) {
        // sets up a callable DB thread
        Callable<JSONObject> activeDBTask = new Callable<JSONObject>() {
            @Override
            public JSONObject call() throws Exception {
                try {
                    Log.d("DBCall", "Performing " + ((isUpdate) ? "an update" : "a query") + " operation...");
                    Log.d("JSONLOG-SentToDB", "JSON:\n" + body.toString(2));

                    // create the URL from the hardcoded URL
                    URL url = new URL(dbURL + ((isUpdate) ? "update" : "query"));
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
                    e.printStackTrace();
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
            e.printStackTrace();
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