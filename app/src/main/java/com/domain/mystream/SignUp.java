package com.domain.mystream;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SignUp extends AppCompatActivity {

    /* Views */
    EditText usernameTxt, passwordTxt, emailTxt, fullnameTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Init views
        usernameTxt = findViewById(R.id.usernameTxt2);
        passwordTxt = findViewById(R.id.passwordTxt2);
        emailTxt = findViewById(R.id.emailTxt2);
        fullnameTxt = findViewById(R.id.fullnameTxt2);
        usernameTxt.setTypeface(Configs.titRegular);
        passwordTxt.setTypeface(Configs.titRegular);
        emailTxt.setTypeface(Configs.titRegular);
        fullnameTxt.setTypeface(Configs.titRegular);

        // Get App name
        TextView titleTxt = findViewById(R.id.suTitleTxt);
        titleTxt.setTypeface(Configs.titSemibold);
        titleTxt.setText(getString(R.string.signup_title, getString(R.string.app_name)));

        // SIGN UP BUTTON ------------------------------------------------------------------------
        Button signupButt = findViewById(R.id.signUpButt2);
        signupButt.setTypeface(Configs.titSemibold);
        signupButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (usernameTxt.getText().toString().matches("") || passwordTxt.getText().toString().matches("") ||
                        emailTxt.getText().toString().matches("") || fullnameTxt.getText().toString().matches("")) {
                    Configs.simpleAlert(getString(R.string.signup_input_validation_error), SignUp.this);
                } else {
                    Configs.showPD(getString(R.string.loading_dialog_please_wait), SignUp.this);
                    dismissKeyboard();

                    final ParseUser user = new ParseUser();
                    user.setUsername(usernameTxt.getText().toString());
                    user.setPassword(passwordTxt.getText().toString());
                    user.setEmail(emailTxt.getText().toString());

                    // Other data
                    user.put(Configs.USER_FULLNAME, fullnameTxt.getText().toString());
                    user.put(Configs.USER_IS_REPORTED, false);
                    List<String> hasBlocked = new ArrayList<>();
                    user.put(Configs.USER_HAS_BLOCKED, hasBlocked);

                    // Signup block
                    user.signUpInBackground(new SignUpCallback() {
                        public void done(ParseException error) {
                            if (error == null) {

                                // Save default avatar
                                Bitmap bitmap = BitmapFactory.decodeResource(SignUp.this.getResources(), R.drawable.default_avatar);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                byte[] byteArray = stream.toByteArray();
                                ParseFile imageFile = new ParseFile("avatar.jpg", byteArray);
                                user.put(Configs.USER_AVATAR, imageFile);
                                user.saveInBackground();


                                Configs.hidePD();
                                // Go to Home screen
                                Intent homeIntent = new Intent(SignUp.this, HomeActivity.class);
                                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(homeIntent);

                                // error
                            } else {
                                Configs.hidePD();
                                Configs.simpleAlert(error.getMessage(), SignUp.this);
                            }
                        }
                    });
                }
            }
        });

        // MARK: - TERMS OF USE BUTTON ----------------------------------------------------------
        Button touButt = findViewById(R.id.touButt);
        touButt.setTypeface(Configs.titSemibold);
        touButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUp.this, TermsOfUse.class));
            }
        });

        // MARK: - DISMISS BUTTON ---------------------------------------------------------------
        Button dismissButt = findViewById(R.id.signupDismissButt);
        dismissButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }// end onCreate()

    // DISMISS KEYBOARD
    public void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(usernameTxt.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(passwordTxt.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(emailTxt.getWindowToken(), 0);
    }
} // @end
