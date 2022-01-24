package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

public class Login extends AppCompatActivity {

    /* Views */
    EditText usernameTxt;
    EditText passwordTxt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        // Init views
        usernameTxt = findViewById(R.id.usernameTxt);
        passwordTxt = findViewById(R.id.passwordTxt);
        usernameTxt.setTypeface(Configs.titRegular);
        passwordTxt.setTypeface(Configs.titRegular);

        // Get Title
        TextView titleTxt = findViewById(R.id.loginTitleTxt);
        titleTxt.setTypeface(Configs.titSemibold);
        titleTxt.setText(getString(R.string.login_title, getString(R.string.app_name)));


        // MARK: - LOGIN BUTTON ------------------------------------------------------------------
        Button loginButt = findViewById(R.id.loginButt);
        loginButt.setTypeface(Configs.titSemibold);
        loginButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Configs.showPD(getString(R.string.loading_dialog_please_wait), Login.this);

                ParseUser.logInInBackground(usernameTxt.getText().toString(), passwordTxt.getText().toString(),
                        new LogInCallback() {
                            public void done(ParseUser user, ParseException error) {
                                if (user != null) {
                                    Configs.hidePD();
                                    // Go to Home screen
                                    Intent homeIntent = new Intent(Login.this, HomeActivity.class);
                                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(homeIntent);

                                    // error
                                } else {
                                    Configs.hidePD();
                                    Configs.simpleAlert(error.getMessage(), Login.this);
                                }
                            }
                        });
            }
        });


        // MARK: - SIGN UP BUTTON ------------------------------------------------------------------
        Button signupButt = findViewById(R.id.signUpButt);
        signupButt.setTypeface(Configs.titSemibold);
        signupButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this, SignUp.class));
            }
        });


        // MARK: - FORGOT PASSWORD BUTTON ------------------------------------------------------------------
        Button fpButt = findViewById(R.id.forgotPassButt);
        fpButt.setTypeface(Configs.titSemibold);
        fpButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(Login.this);
                alert.setTitle(R.string.app_name);
                alert.setMessage(R.string.login_input_validation_error);

                // Add an EditTxt
                final EditText editTxt = new EditText(Login.this);
                editTxt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                alert.setView(editTxt)
                        .setNegativeButton(R.string.cancel_button, null)
                        .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                // Reset password
                                ParseUser.requestPasswordResetInBackground(editTxt.getText().toString(), new RequestPasswordResetCallback() {
                                    public void done(ParseException error) {
                                        if (error == null) {
                                            Configs.simpleAlert(getString(R.string.login_reset_password_success), Login.this);
                                        } else {
                                            Configs.simpleAlert(error.getMessage(), Login.this);
                                        }
                                    }
                                });

                            }
                        });
                alert.show();

            }
        });

        // MARK: - DISMISS BUTTON ----------------------------------------------
        Button dismButt = findViewById(R.id.lDismissButt);
        dismButt.setTypeface(Configs.titSemibold);
        dismButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


    }// end onCreate()
} // @end
