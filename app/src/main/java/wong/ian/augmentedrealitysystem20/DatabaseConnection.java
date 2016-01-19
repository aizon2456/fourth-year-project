package wong.ian.augmentedrealitysystem20;

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

public class DatabaseConnection {

    private final String dbURL = "http://chemicaltracker.elasticbeanstalk.com/api/test/";
    private final String username = "valid";

    private ChemicalContainer currentContainer = null;

    // Single-thread database executor to ensure database integrity
    private ExecutorService executor = Executors.newSingleThreadExecutor();

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

        if (response == null) {
            currentContainer = null;
            Log.e("CreateContainer", "The container could not be created.");
            return false;
        }

        // TODO: handle the results in the UI
        try {
            // if the response indicates a failure, determine why
            if (!response.getBoolean("success")) {
                Log.e("DatabaseError-" + response.getString("status"), response.getString("message"));
                currentContainer = null;
                return false;
            }

            // otherwise log the information
            Log.i("DatabaseResponse-" + response.getString("status"), response.getString("message"));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public JSONObject modifyContainer(boolean isAddOperation) {
        if (currentContainer == null) {
            Log.i("DatabaseAccess", "The currently identified container is empty.");
            return null;
        }

        JSONObject updateObj = new JSONObject();

        try {
            updateObj.put("requestType", (isAddOperation) ? "ADD" : "REMOVE"); // add or remove
            updateObj.put("username", username);
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

    // checks if a chemical name exists
    public boolean queryChemical(String chemicalName) {
        JSONObject queryObj = new JSONObject();

        try {
            queryObj.put("chemical", chemicalName);

            JSONObject response = performDBQuery(queryObj, false);

            // if the chemical exists, then set the current container
            if (("true").equals(response.getString("match"))) {
                currentContainer = new ChemicalContainer(
                        response.getInt("flammability"),
                        response.getInt("health"),
                        response.getInt("instability"),
                        response.getString("notice"),
                        response.getString("chemicalName")
                );
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private JSONObject performDBQuery(final JSONObject body, final boolean isUpdate) {
        // sets up a callable DB thread
        Callable<JSONObject> activeDBTask = new Callable<JSONObject>() {
            @Override
            public JSONObject call() throws Exception {
                try {
                    Log.d("DBCall", "Performing a(n) " + ((isUpdate) ? "update" : "query") + " operation...");
                    Log.d("JSONLOG-SentToDB", "JSON:\n" + body.toString(2));

                    URL url = new URL(dbURL + ((isUpdate) ? "update" : "query"));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());

                    writer.write(body.toString());

                    writer.flush();

                    // get the response from the query if the database responded
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charset.defaultCharset()));

                        StringBuilder builder = new StringBuilder();

                        String currentLine;
                        while ((currentLine = reader.readLine()) != null) {
                            builder.append(currentLine);
                        }

                        reader.close();

                        JSONObject result = new JSONObject(builder.toString());

                        Log.d("JSONLOG-ReplyFromDB", "JSON:\n" + result.toString(2));

                        // build the response JSONObject
                        return result;
                    }

                } catch (Exception e) {
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

    public ChemicalContainer getCurrentContainer() {
        return currentContainer;
    }
}