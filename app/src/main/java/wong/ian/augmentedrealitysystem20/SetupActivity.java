package wong.ian.augmentedrealitysystem20;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SetupActivity extends AppCompatActivity {

    private DatabaseConnection db = null;
    private String username = null;
    private String location = null, room = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_layout);

        // set the database and username from the login intent
        db = (DatabaseConnection) getIntent().getSerializableExtra("user");
        username = getIntent().getStringExtra("user");

        // display the valid login notification
        AlertDialog loginSuccess = new AlertDialog.Builder(this).create();
        loginSuccess.setTitle("Success!");
        loginSuccess.setMessage(username + " logged in successfully!");
        loginSuccess.show();
    }

    public void startChemicalTracking(View view) {
        // TODO: set dialog underneath the respective field to indicate error
        if (room == null || room.length() == 0) {
            return;
        }
        else if (location == null || location.length() == 0) {
            return;
        }

        Intent redirect = new Intent(getApplicationContext(), MainActivity.class);
        redirect.putExtra("user", username);
        redirect.putExtra("user", username);
        redirect.putExtra("user", username);
        redirect.putExtra("database", db);
        startActivity(redirect);
    }
}
