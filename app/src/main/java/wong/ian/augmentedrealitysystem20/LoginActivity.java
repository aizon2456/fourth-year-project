package wong.ian.augmentedrealitysystem20;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * The activity that handles user login.
 */
public class LoginActivity extends Activity {

    private final String USERNAME = "username";
    private final String AUTH = "auth";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        // check if the user is already logged in
        loginAsUser(null, null);

        // set the "Done" button of the Android keyboard to actively login the user
        EditText editText = (EditText) findViewById(R.id.password_text);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    doLogin(null);
                }
                return true;
            }
        });
    }

    /**
     * Attempts to perform a login with the username and password given on the form.
     * @param view
     */
    public void doLogin(View view) {
        // for security reasons, filter the input being passed through and hide it
        InputMethodManager inputManager = (InputMethodManager) getApplicationContext().
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(findViewById(R.id.password_text).getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        // retrieve the user input
        EditText unField = ((EditText)findViewById(R.id.input_text));
        EditText pwField = ((EditText)findViewById(R.id.password_text));
        String username = unField.getText().toString();
        String password = pwField.getText().toString();

        // check if the user provided a username or password
        if (username.isEmpty()) {
            unField.setError("Username missing. (Required)");
            unField.requestFocus();
            inputManager.showSoftInput(unField, InputMethodManager.SHOW_IMPLICIT);
            return;
        }
        else if (password.isEmpty()) {
            pwField.setError("Password missing. (Required)");
            pwField.requestFocus();
            inputManager.showSoftInput(pwField, InputMethodManager.SHOW_IMPLICIT);
            return;
        }

        loginAsUser(username, password);
    }

    /**
     * Attempt to logon the system using a distinct username and password.
     * Note: If the user is saved in the cache, then there is no need to re-login.
     * @param username
     * @param password
     */
    private void loginAsUser(String username, String password) {
        // if the user is already saved in the system, then log the user in automatically
        String displayUsername;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.contains(USERNAME)) {
            displayUsername = sp.getString(USERNAME, "ERROR");
            DatabaseConnection.getInstance().setLoginProperty(sp.getString(AUTH, "ERROR"));
            Log.i("TokenExists", "Already logged in as " + displayUsername + ", continuing...");
        }
        // since the UI can never submit null for field values, this comes from the onCreate() and is thus returned
        else if (username == null || password == null) {
            return;
        }
        // otherwise the user has submitted a login and password for validation
        else {
            // check the username and password
            String result = DatabaseConnection.getInstance().performLogin(username, password);

            // if the result shows that the login failed, then inform the user and return
            if (!DatabaseConnection.SUCCESS.equals(result)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final AlertDialog warning = builder.create();
                warning.setTitle("Wrong Login");
                warning.setMessage(result);
                warning.show();

                // clear the password and set focus on it
                EditText pwField = ((EditText)findViewById(R.id.password_text));
                pwField.setText("");
                pwField.requestFocus();

                return;
            }

            // set the shared preferences to state that [USERNAME] is logged in
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(USERNAME, username);
            editor.putString(AUTH, DatabaseConnection.getInstance().getLoginProperty());
            editor.commit();

            displayUsername = username;
        }

        // start the next activity, SetupActivity, and close the current activity
        Intent redirect = new Intent(getApplicationContext(), SetupActivity.class);
        redirect.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        redirect.putExtra("user", displayUsername);
        startActivity(redirect);
    }
}
