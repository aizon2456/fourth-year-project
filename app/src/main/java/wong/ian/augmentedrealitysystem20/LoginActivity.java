package wong.ian.augmentedrealitysystem20;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class LoginActivity extends Activity {

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
        InputMethodManager inputManager =
                (InputMethodManager) getApplicationContext().
                        getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(
                findViewById(R.id.password_text).getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        String username = ((EditText)findViewById(R.id.input_text)).getText().toString();
        String password = ((EditText)findViewById(R.id.password_text)).getText().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (username == null || username.isEmpty()) {
            final AlertDialog warning = builder.create();
            warning.setTitle("Empty Username");
            warning.setMessage("You must provide a username.");
            warning.show();
            return;
        }
        else if (password == null || password.isEmpty()) {
            final AlertDialog warning = builder.create();
            warning.setTitle("Empty Password");
            warning.setMessage("You must provide a password.");
            warning.show();
            return;
        }

        // TODO: check the username and password here

        Intent redirect = new Intent(getApplicationContext(), MainActivity.class);
        redirect.putExtra("user", username);
        startActivity(redirect);
    }
}
