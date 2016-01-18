package wong.ian.augmentedrealitysystem20;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DatabaseConnection {

    private static String username = "Ian";
    private ChemicalContainer currentContainer = null;

    // Single-thread database executor to ensure database integrity
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public JSONObject modifyContainer(boolean isAddOperation) {
        if (currentContainer == null) {
            Log.i("DatabaseAccess", "The currently identified container is empty.");
            return null;
        }

        JSONObject updateObj = new JSONObject();

        try {
            updateObj.put("request", (isAddOperation) ? "ADD" : "REMOVE"); // add or remove
            updateObj.put("username", username);
            updateObj.put("Location", currentContainer.getLocation());
            updateObj.put("Room", currentContainer.getRoom());
            updateObj.put("Cabinet", currentContainer.getCabinet());
            updateObj.put("Chemical", currentContainer.getChemicalName());

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

            Log.d("JSONLOG", response.toString(2));

            // if the chemical exists, then go ahead
            if (("true").equals(response.getString("match"))) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createContainer(String location, String room, String cabinet, String chemicalName) {
        currentContainer = new ChemicalContainer(location, room, cabinet, chemicalName);
        JSONObject response = modifyContainer(true);

        if (response == null) {
            currentContainer = null;
            Log.e("CreateContainer", "The container could not be created.");
            return false;
        }

        // TODO: handle the results in the UI
        try {
            Log.d("JSONLOG",response.toString(2));

            // if the container already existed, then return false
            if ("true".equals(response.get("match"))) {
                return false;
            }
            return true;
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
                    URL url = new URL("http://chemicaltracker.elasticbeanstalk.com/api/test/" + ((isUpdate) ? "update" : "query"));
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

                        // build the response JSONObject
                        return new JSONObject(builder.toString());
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

}