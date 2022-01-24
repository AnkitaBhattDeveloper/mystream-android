package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Intro extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Hide Status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        // Get App name
        TextView appNameTxt = findViewById(R.id.inAppNameTxt);
        appNameTxt.setTypeface(Configs.titSemibold);
        appNameTxt.setText(getString(R.string.app_name));


        // MARK: - GET STARTED BUTTON ------------------------------------
        Button gsButt = findViewById(R.id.inGetStartedButt);
        gsButt.setTypeface(Configs.titSemibold);
        gsButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intro.this, SignUp.class));
            }
        });


        // MARK: - LOGIN BUTTON ------------------------------------
        Button loginButt = findViewById(R.id.inLoginButt);
        loginButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intro.this, Login.class));
            }
        });


        // MARK: - FACEBOOK LOGIN BUTTON ------------------------------------------------------------------
        Button fbButt = findViewById(R.id.facebookButt);
        fbButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> permissions = Arrays.asList("public_profile", "email");
                Configs.showPD(getString(R.string.loading_dialog_please_wait), Intro.this);

                ParseFacebookUtils.logInWithReadPermissionsInBackground(Intro.this, permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e)  {
                        if (user == null) {
                            Log.i("log-", "CANCELLED Facebook login.");
                            Configs.hidePD();

                        } else if (user.isNew()) {
                            getUserDetailsFromFB();

                        } else {
                            Configs.hidePD();
                            Log.i("log-", "RETURNING User logged in through Facebook!");
                            startHomeScreen();
                        }
                    }
                });
            }
        });


        // This code generates a KeyHash that you'll have to copy from your Logcat console and paste it into Key Hashes field in the 'Settings' section of your Facebook Android App
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.i("log-", "HASH KEY TO COPY: " + Base64.encodeToString(md.digest(), Base64.DEFAULT) + "\nPACKAGE NAME: " + getPackageName());
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
        }


    }// end onCreate()

    // MARK: - FACEBOOK GRAPH REQUEST --------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }


    void getUserDetailsFromFB() {
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                String facebookID = "";
                String name = "";
                String email = "";
                String username = "";

                try {
                    name = object.getString("name");
                    email = object.getString("email");
                    facebookID = object.getString("id");

                    String[] one = name.toLowerCase().split(" ");
                    for (String word : one) {
                        username += word;
                    }
                    Log.i("log-", "USERNAME: " + username + "\n");
                    Log.i("log-", "email: " + email + "\n");
                    Log.i("log-", "name: " + name + "\n");

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                // SAVE NEW USER IN YOUR PARSE DASHBOARD -> USER CLASS
                final String finalFacebookID = facebookID;
                final String finalUsername = username;
                final String finalEmail = email;
                final String finalName = name;
                Log.i("log-", "FINAL FACEBOOK ID: " + finalFacebookID);

                final ParseUser currUser = ParseUser.getCurrentUser();
                currUser.put(Configs.USER_USERNAME, finalUsername);
                if (finalEmail != null) {
                    currUser.put(Configs.USER_EMAIL, finalEmail);
                } else {
                    currUser.put(Configs.USER_EMAIL, facebookID + "@facebook.com");
                }

                // Other data
                currUser.put(Configs.USER_FULLNAME, finalName);
                currUser.put(Configs.USER_IS_REPORTED, false);
                List<String> hasBlocked = new ArrayList<>();
                currUser.put(Configs.USER_HAS_BLOCKED, hasBlocked);

                currUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Log.i("log-", "NEW USER signed up and logged in through Facebook...");


                        if (!finalFacebookID.matches("")) {

                            // Get and Save avatar from Facebook
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    try {
                                        URL imageURL = new URL("https://graph.facebook.com/" + finalFacebookID + "/picture?type=large");
                                        Bitmap avatarBm = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());

                                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                        avatarBm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                        byte[] byteArray = stream.toByteArray();
                                        ParseFile imageFile = new ParseFile("image.jpg", byteArray);
                                        currUser.put(Configs.USER_AVATAR, imageFile);
                                        currUser.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException error) {
                                                Log.i("log-", "... AND AVATAR SAVED!");
                                                Configs.hidePD();

                                                startHomeScreen();
                                            }
                                        });

                                    } catch (IOException error) {
                                        error.printStackTrace();
                                    }

                                }
                            }, 1000); // 1 second


                            // SAVE AVATAR AS APP LOGO
                        } else {
                            Bitmap logoBm = BitmapFactory.decodeResource(Intro.this.getResources(), R.drawable.default_avatar);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            logoBm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            byte[] byteArray = stream.toByteArray();
                            ParseFile imageFile = new ParseFile("image.jpg", byteArray);
                            currUser.put(Configs.USER_AVATAR, imageFile);
                            currUser.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException error) {
                                    Log.i("log-", "... AND LOGO AVATAR SAVED!");
                                    Configs.hidePD();

                                    startHomeScreen();
                                }
                            });
                        }


                    }
                }); // end saveInBackground

            }
        }); // end graphRequest


        Bundle parameters = new Bundle();
        parameters.putString("fields", "email, name, picture.type(large)");
        request.setParameters(parameters);
        request.executeAsync();
    }
    // END FACEBOOK GRAPH REQUEST --------------------------------------------------------------------

    private void startHomeScreen() {
        Intent homeIntent = new Intent(Intro.this, HomeActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }
}// @end
