package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.CountCallback;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.HashMap;
import java.util.List;

public class OtherUserProfile extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    /* Views */

    ImageView avatarImg, coverImg;
    SwipeRefreshLayout refreshControl;
    TextView usernameTxt, fullNameTxt, aboutMeTxt;
    Button followersButt, followingButt, reportUserButt, followButt;
    private RecyclerView streamRV;

    /* Variables */
    List<ParseObject> streamsArray;
    ParseUser userObj;
    MarshMallowPermission mmp = new MarshMallowPermission(this);


    @Override
    protected void onStart() {
        super.onStart();

        // Get objectID from previous .java
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        String objectID = extras.getString("userID");
        userObj = (ParseUser) ParseUser.createWithoutData(Configs.USER_CLASS_NAME, objectID);
        try {
            userObj.fetchIfNeeded().getParseObject(Configs.USER_CLASS_NAME);

            // Call queries
            showUserDetails();
            getFollowersAndFollowing();

            // Recall query in case something has been reported (either a User or a Stream)
            if (Configs.mustRefresh) {
                queryStreams();
                Configs.mustRefresh = false;
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_user_profile);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Change StatusBar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.main_color));


        // Init views
        avatarImg = findViewById(R.id.oupAvatarImg);
        coverImg = findViewById(R.id.oupCoverImg);
        usernameTxt = findViewById(R.id.oupUsernameTxt);
        usernameTxt.setTypeface(Configs.titSemibold);
        followersButt = findViewById(R.id.oupFollowersButt);
        followersButt.setTypeface(Configs.titRegular);
        followingButt = findViewById(R.id.oupFollowingButt);
        followingButt.setTypeface(Configs.titRegular);
        fullNameTxt = findViewById(R.id.oupFullnameTxt);
        fullNameTxt.setTypeface(Configs.titBlack);
        aboutMeTxt = findViewById(R.id.oupAboutMeTxt);
        aboutMeTxt.setTypeface(Configs.titRegular);
        streamRV = findViewById(R.id.stream_rv);

        // Init recyclerView
        if (streamRV != null) {
            streamRV.setLayoutManager(new LinearLayoutManager(this));
            streamRV.setItemAnimator(new DefaultItemAnimator());

        }
        reportUserButt = findViewById(R.id.oupReportUserButt);
        followButt = findViewById(R.id.oupFollowButt);

        // Init a refreshControl
        refreshControl = findViewById(R.id.swiperefresh);
        refreshControl.setOnRefreshListener(this);


        // Get objectID from previous .java
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        String objectID = extras.getString("userID");
        userObj = (ParseUser) ParseUser.createWithoutData(Configs.USER_CLASS_NAME, objectID);
        try {
            userObj.fetchIfNeeded().getParseObject(Configs.USER_CLASS_NAME);


            // Call query
            queryStreams();


            // MARK: - MESSAGE BUTTON ------------------------------------
            Button messageButt = findViewById(R.id.oupMessageButt);
            messageButt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ParseUser currUser = ParseUser.getCurrentUser();
                    List<String> blockedUsers = userObj.getList(Configs.USER_HAS_BLOCKED);

                    // THIS USER HAS BLOCKED YOU!
                    if (blockedUsers.contains(currUser.getObjectId())) {
                        Configs.simpleAlert(getString(R.string.messages_you_blocked_error,
                                userObj.getString(Configs.USER_USERNAME)), OtherUserProfile.this);

                        // YOU CAN CHAT WITH THIS USER
                    } else {
                        Intent i = new Intent(OtherUserProfile.this, InboxActivity.class);
                        Bundle extras = new Bundle();
                        extras.putString("userID", userObj.getObjectId());
                        i.putExtras(extras);
                        startActivity(i);
                    }
                }
            });


        } catch (ParseException e) {
            e.printStackTrace();
        }


        // MARK: - BACK BUTTON ------------------------------------
        Button backButt = findViewById(R.id.oupBackButt);
        backButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


    }// end onCreate()


    // MARK: - GET FOLLOWERS AND FOLLOWING AMOUNT -------------------------------------------------
    void getFollowersAndFollowing() {
        final ParseUser currUser = ParseUser.getCurrentUser();

        // QUERY FOLLOWERS
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.FOLLOW_CLASS_NAME);
        query.whereEqualTo(Configs.FOLLOW_IS_FOLLOWING, userObj);
        query.orderByDescending(Configs.FOLLOW_CREATED_AT);
        query.countInBackground(new CountCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void done(int amount, ParseException e) {
                if (e == null) {
                    String foll = Configs.roundThousandsIntoK(amount);
                    followersButt.setText(getString(R.string.user_profile_followers_count, foll));

                    // QUERY FOLLOWING
                    ParseQuery<ParseObject> query2 = ParseQuery.getQuery(Configs.FOLLOW_CLASS_NAME);
                    query2.whereEqualTo(Configs.FOLLOW_CURR_USER, userObj);
                    query2.orderByDescending(Configs.FOLLOW_CREATED_AT);
                    query2.countInBackground(new CountCallback() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void done(int amount, ParseException e) {
                            if (e == null) {
                                String foll = Configs.roundThousandsIntoK(amount);
                                followingButt.setText(getString(R.string.user_profile_following_count, foll));
                            }
                        }
                    });
                }
            }
        });
    }


    // MARK: - SHOW USER'S DETAILS ------------------------------------------------------
    void showUserDetails() {
        final ParseUser currUser = ParseUser.getCurrentUser();

        // Get username
        usernameTxt.setText(getString(R.string.username_formatted, userObj.getString(Configs.USER_USERNAME)));
        // Get fullName
        fullNameTxt.setText(userObj.getString(Configs.USER_FULLNAME));

        // Get aboutMe
        if (userObj.getString(Configs.USER_ABOUT_ME) != null) {
            aboutMeTxt.setText(userObj.getString(Configs.USER_ABOUT_ME));
        } else {
            aboutMeTxt.setText("");
        }

        // Get avatar
        Configs.getParseImage(avatarImg, userObj, Configs.USER_AVATAR);

        // Get cover
        if (userObj.getParseFile(Configs.USER_COVER_IMAGE) != null) {
            Configs.getParseImage(coverImg, userObj, Configs.USER_COVER_IMAGE);
        } else {
            coverImg.setImageDrawable(null);
        }


        // Show Follow button
        if (!userObj.getObjectId().matches(currUser.getObjectId())) {

            // Set Follow Button
            ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.FOLLOW_CLASS_NAME);
            query.whereEqualTo(Configs.FOLLOW_CURR_USER, currUser);
            query.whereEqualTo(Configs.FOLLOW_IS_FOLLOWING, userObj);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {

                        // You're following this user
                        if (objects.size() != 0) {
                            followButt.setText(R.string.user_profile_following_button);
                            followButt.setBackgroundResource(R.drawable.rounded_button);
                            followButt.setTextColor(Color.WHITE);

                            // You're not following this user
                        } else {
                            followButt.setText(R.string.user_profile_follow_button);
                            followButt.setBackgroundResource(R.drawable.rounded_button_empty);
                            followButt.setTextColor(Color.parseColor(Configs.MAIN_COLOR));
                        }

                    } else {
                        Configs.simpleAlert(e.getMessage(), OtherUserProfile.this);
                    }
                }
            });


            // Hide Follow button since the User is YOU
        } else {
            followButt.setVisibility(View.INVISIBLE);
        }


        // MARK: - FOLLOWING BUTTON ------------------------------------
        followingButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(OtherUserProfile.this, Follow.class);
                Bundle extras = new Bundle();
                extras.putString("isFollowing", "true");
                extras.putString("userID", userObj.getObjectId());
                i.putExtras(extras);
                startActivity(i);
            }
        });


        // MARK: - FOLLOWERS BUTTON ------------------------------------
        followersButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(OtherUserProfile.this, Follow.class);
                Bundle extras = new Bundle();
                extras.putString("isFollowing", "false");
                extras.putString("userID", userObj.getObjectId());
                i.putExtras(extras);
                startActivity(i);
            }
        });


        // MARK: - FOLLOW/UNFOLLOW THIS USER BUTTON ------------------------------------
        followButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Configs.showPD(getString(R.string.loading_dialog_please_wait), OtherUserProfile.this);
                final ParseUser currUser = ParseUser.getCurrentUser();

                // FOLLOW USER --------------------------------
                if (followButt.getText().toString().matches(getString(R.string.user_profile_follow_button))) {
                    ParseObject fObj = new ParseObject(Configs.FOLLOW_CLASS_NAME);

                    // Save data
                    fObj.put(Configs.FOLLOW_CURR_USER, currUser);
                    fObj.put(Configs.FOLLOW_IS_FOLLOWING, userObj);

                    // Saving block
                    fObj.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Configs.hidePD();

                                followButt.setText(R.string.user_profile_following_button);
                                followButt.setBackgroundResource(R.drawable.rounded_button);
                                followButt.setTextColor(Color.WHITE);

                                // Send push notification
                                String pushMess = getString(R.string.user_profile_started_follow, currUser.getString(Configs.USER_USERNAME));
                                Configs.sendPushNotification(pushMess, userObj, OtherUserProfile.this);

                                // error
                            } else {
                                Configs.hidePD();
                                Configs.simpleAlert(e.getMessage(), OtherUserProfile.this);
                            }
                        }
                    });


                    // UNFOLLOW USER ---------------------------------------
                } else {
                    ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.FOLLOW_CLASS_NAME);
                    query.whereEqualTo(Configs.FOLLOW_CURR_USER, currUser);
                    query.whereEqualTo(Configs.FOLLOW_IS_FOLLOWING, userObj);
                    query.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> objects, ParseException e) {
                            if (e == null) {
                                ParseObject fObj = objects.get(0);
                                fObj.deleteInBackground(new DeleteCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        Configs.hidePD();
                                        followButt.setText(R.string.user_profile_follow_button);
                                        followButt.setBackgroundResource(R.drawable.rounded_button_empty);
                                        followButt.setTextColor(Color.parseColor(Configs.MAIN_COLOR));
                                    }
                                });

                            } else {
                                Configs.hidePD();
                                Configs.simpleAlert(e.getMessage(), OtherUserProfile.this);
                            }
                        }
                    });
                }

            }
        });


        // MARK: - REPORT USER BUTTON --------------------------------------------------------
        reportUserButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(OtherUserProfile.this);

                alert.setMessage(getString(R.string.user_profile_report_alert_title, userObj.getString(Configs.USER_USERNAME)))
                        .setTitle(R.string.app_name)
                        .setIcon(R.drawable.logo)
                        .setNegativeButton(R.string.cancel_button, null)
                        .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Configs.showPD(getString(R.string.user_profile_report_in_progress), OtherUserProfile.this);

                                // Report user via Cloud Code
                                HashMap<String, Object> params = new HashMap<String, Object>();
                                params.put("userId", userObj.getObjectId());
                                params.put("reportMessage", "OFFENSIVE USER");

                                ParseCloud.callFunctionInBackground("reportUser", params, new FunctionCallback<ParseUser>() {
                                    public void done(ParseUser user, ParseException error) {
                                        if (error == null) {
                                            Configs.hidePD();
                                            Configs.mustRefresh = true;

                                            // Automatically report all User's streams
                                            ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.STREAMS_CLASS_NAME);
                                            query.whereEqualTo(Configs.STREAMS_USER_POINTER, userObj);
                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if (e == null) {
                                                        for (int i = 0; i < objects.size(); i++) {
                                                            ParseObject stObj = objects.get(i);
                                                            List<String> reportedBy = stObj.getList(Configs.STREAMS_REPORTED_BY);
                                                            reportedBy.add(currUser.getObjectId());
                                                            stObj.put(Configs.STREAMS_REPORTED_BY, reportedBy);
                                                            stObj.saveInBackground();
                                                        }
                                                    }
                                                }
                                            });


                                            // Automatically report all User's comments
                                            ParseQuery<ParseObject> query2 = ParseQuery.getQuery(Configs.COMMENTS_CLASS_NAME);
                                            query2.whereEqualTo(Configs.COMMENTS_USER_POINTER, userObj);
                                            query2.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if (e == null) {
                                                        for (int i = 0; i < objects.size(); i++) {
                                                            ParseObject commObj = objects.get(i);
                                                            List<String> reportedBy = commObj.getList(Configs.COMMENTS_REPORTED_BY);
                                                            reportedBy.add(currUser.getObjectId());
                                                            commObj.put(Configs.COMMENTS_REPORTED_BY, reportedBy);
                                                            commObj.saveInBackground();
                                                        }
                                                    }
                                                }
                                            });


                                            // Show Alert
                                            AlertDialog.Builder alert = new AlertDialog.Builder(OtherUserProfile.this);
                                            alert.setMessage(getString(R.string.user_profile_report_success, userObj.getString(Configs.USER_FULLNAME)))
                                                    .setTitle(R.string.app_name)
                                                    .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            finish();
                                                        }
                                                    })
                                                    .setIcon(R.drawable.logo);
                                            alert.create().show();


                                            // Error in Cloud Code
                                        } else {
                                            Configs.hidePD();
                                            Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });


                            }
                        });
                alert.create().show();


            }
        });

    }


    // MARK: - QUERY STREAMS -------------------------------------------------
    void queryStreams() {
        Configs.showPD(getString(R.string.loading_dialog_please_wait), OtherUserProfile.this);
        ParseUser currUser = ParseUser.getCurrentUser();

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.STREAMS_CLASS_NAME);
        query.whereEqualTo(Configs.STREAMS_USER_POINTER, userObj);
        query.orderByDescending(Configs.STREAMS_CREATED_AT);
        query.setLimit(10000);

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    streamsArray = objects;
                    Configs.hidePD();

                    reloadData();

                    // error in query
                } else {
                    Configs.hidePD();
                    Configs.simpleAlert(e.getMessage(), OtherUserProfile.this);
                }
            }
        });
    }


    // MARK: - RELOAD  DATA --------------------------------------------------------
    void reloadData() {
        streamRV.setAdapter(new OtherUserStreamAdapter(this,streamsArray,mmp,userObj));
    }


    // MARK: - REFRESH DATA ----------------------------------------
    @Override
    public void onRefresh() {
        // Recall query
        queryStreams();

        if (refreshControl.isRefreshing()) {
            refreshControl.setRefreshing(false);
        }
    }


}//@end
