package wong.ian.augmentedrealitysystem20;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SetupActivity extends Activity {

    private static final String ADD_NEW = "Add...";

    private DatabaseConnection db = null;
    private String username = null;
    private boolean locationNew = false, roomNew = false;

    private HashMap<String,HashMap<String,String[]>> areaMap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_layout);

        // set the database and username from the login intent
        db = DatabaseConnection.getInstance();
        username = getIntent().getStringExtra("user");

        // setup all the information from the database
        // TODO: get lists from the database (remove hardcoding)
        areaMap = new HashMap();
        areaMap.put("Steacie", new HashMap<String, String[]>(){{
                    put("Room 101", new String[] {"Fire Cabinet 1", "Cabinet 1"});
                    put("Room 102", new String[] {"Biohazard Cabinet 1"});
                }});
        areaMap.put("Minto", new HashMap<String, String[]>(){{
            put("Room 111", new String[] {"Narcotics Cabinet 1", "Cabinet 1"});
            put("Room 612", new String[] {"Fire Cabinet 1"});
        }});
        areaMap.put("University Centre", new HashMap<String, String[]>(){{
            put("Room 1", new String[] {"Narcotics Cabinet 1", "Cabinet 1"});
            put("Atrium", new String[] {"Test Cabinet 1", "Test Cabinet 2"});
        }});

        // location spinner
        Spinner locationSpinner = ((Spinner) findViewById(R.id.location_spinner));

        List<String> locations = new ArrayList(areaMap.keySet());
        locations.add(ADD_NEW);

        locationSpinner.setAdapter(new ArrayAdapter<>(
                this, R.layout.spinner_textbox, locations
        ));

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
                EditText locationText = (EditText) findViewById(R.id.location_input);
                EditText roomText = (EditText) findViewById(R.id.room_input);
                Spinner roomSpinner = ((Spinner) findViewById(R.id.room_spinner));

                // check if the item is ADD_NEW
                if (ADD_NEW.equals(item)) {
                    locationText.setVisibility(View.VISIBLE);
                    roomText.setVisibility(View.VISIBLE);
                    roomSpinner.setVisibility(View.INVISIBLE);
                    locationNew = true;
                }
                else {
                    locationText.setVisibility(View.INVISIBLE);
                    roomText.setVisibility(View.INVISIBLE);
                    locationNew = false;

                    // populate the bottom spinner
                    roomSpinner.setVisibility(View.VISIBLE);
                    List<String> rooms = new ArrayList(areaMap.get(item).keySet());
                    rooms.add(ADD_NEW);

                    roomSpinner.setAdapter(new ArrayAdapter<>(
                            view.getContext(), R.layout.spinner_textbox, rooms
                    ));

                    roomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String item = parent.getItemAtPosition(position).toString();
                            EditText roomText = (EditText) findViewById(R.id.room_input);

                            // check if the item is ADD_NEW
                            if (ADD_NEW.equals(item)) {
                                roomText.setVisibility(View.VISIBLE);
                                roomNew = true;
                            }
                            else {
                                roomText.setVisibility(View.INVISIBLE);
                                roomNew = false;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });



        // display the valid login notification
        AlertDialog loginSuccess = new AlertDialog.Builder(this).create();
        loginSuccess.setTitle("Success!");
        loginSuccess.setMessage(username + " logged in successfully!");
        loginSuccess.show();
    }

    public void startChemicalTracking(View view) {
        String location = (locationNew) ?
                ((EditText) findViewById(R.id.location_input)).getText().toString() :
                ((Spinner) findViewById(R.id.location_spinner)).getSelectedItem().toString();
        String room = (roomNew || locationNew) ?
                ((EditText) findViewById(R.id.room_input)).getText().toString() :
                ((Spinner) findViewById(R.id.room_spinner)).getSelectedItem().toString();

        // if the fields are not properly filled, notify the user
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (location == null || location.length() == 0) {
            final AlertDialog warning = builder.create();
            warning.setTitle("Error - Location");
            warning.setMessage("Please enter a valid location.");
            warning.show();
            return;
        }
        else if (room == null || room.length() == 0) {
            final AlertDialog warning = builder.create();
            warning.setTitle("Error - Room");
            warning.setMessage("Please enter a valid room.");
            warning.show();
            return;
        }

        Intent redirect = new Intent(getApplicationContext(), MainActivity.class);
        redirect.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        redirect.putExtra("user", username);
        redirect.putExtra("location", location);
        redirect.putExtra("room", room);
        startActivity(redirect);
    }
}
