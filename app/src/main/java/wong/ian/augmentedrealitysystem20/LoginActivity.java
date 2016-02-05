package wong.ian.augmentedrealitysystem20;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends Activity {

    private final String TOKEN = "token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

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

    public void doLogin(View view) {
        InputMethodManager inputManager = (InputMethodManager) getApplicationContext().
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(findViewById(R.id.password_text).getWindowToken(),
                                             InputMethodManager.HIDE_NOT_ALWAYS);

        String username = ((EditText)findViewById(R.id.input_text)).getText().toString();
        String password = ((EditText)findViewById(R.id.password_text)).getText().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (username.isEmpty()) {
            final AlertDialog warning = builder.create();
            warning.setTitle("Empty Username");
            warning.setMessage("You must provide a username.");
            warning.show();
            return;
        }
        else if (password.isEmpty()) {
            final AlertDialog warning = builder.create();
            warning.setTitle("Empty Password");
            warning.setMessage("You must provide a password.");
            warning.show();
            return;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.contains(TOKEN)) {
            Log.i("TokenExists", "Already logged in, continuing...");
        }
        else {
            // check the username and password
            DatabaseConnection db = DatabaseConnection.getInstance();
            // TODO: get the result
            String result = "SOMETHING";

            SharedPreferences.Editor editor = sp.edit();
            editor.putString(TOKEN, result);
        }

        Intent redirect = new Intent(getApplicationContext(), SetupActivity.class);
        redirect.putExtra("user", username);
        startActivity(redirect);
    }
}
