package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class Follow extends AppCompatActivity {

    /* Views */
    TextView titleTxt;
    ListView followListView;


    /* Variables */
    String isFollowing;
    ParseUser userObj;
    List<ParseObject> followArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.follow);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Change StatusBar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.main_color));


        // Init views
        titleTxt = findViewById(R.id.follTitleTxt);
        followListView = findViewById(R.id.follListView);


        // Get isFollowing (from previous java)
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        isFollowing = extras.getString("isFollowing");
        assert isFollowing != null;
        if (isFollowing.matches("true")) {
            titleTxt.setText(R.string.user_profile_following_button);
        } else {
            titleTxt.setText(R.string.follow_screen_title);
        }


        // Get userObj
        String objectID = extras.getString("userID");
        userObj = (ParseUser) ParseObject.createWithoutData(Configs.USER_CLASS_NAME, objectID);
        try {
            userObj.fetchIfNeeded().getParseObject(Configs.USER_CLASS_NAME);

            // Call query
            queryFollow();

        } catch (ParseException e) {
            e.printStackTrace();
        }


        // MARK: - BACK BUTTON ------------------------------------
        Button backButt = findViewById(R.id.follBackButt);
        backButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }// end onCreate()


    // MARK: - QUERY FOLLOW -------------------------------------------------------
    void queryFollow() {
        if (userObj == null) {
            userObj = ParseUser.getCurrentUser();
        }

        Configs.showPD(getString(R.string.loading_dialog_please_wait), Follow.this);

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.FOLLOW_CLASS_NAME);
        if (isFollowing.matches("true")) {
            query.whereEqualTo(Configs.FOLLOW_CURR_USER, userObj);
        } else {
            query.whereEqualTo(Configs.FOLLOW_IS_FOLLOWING, userObj);
        }
        query.orderByDescending(Configs.FOLLOW_CREATED_AT);

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    followArray = objects;
                    Configs.hidePD();
                    reloadData();
                } else {
                    Configs.hidePD();
                    Configs.simpleAlert(e.getMessage(), Follow.this);
                }
            }
        });
    }

    // MARK: - REALOD LISTVIEW DATA -------------------------------------------------
    void reloadData() {
        // CUSTOM LIST ADAPTER
        class ListAdapter extends BaseAdapter {
            private Context context;

            public ListAdapter(Context context) {
                super();
                this.context = context;
            }

            // CONFIGURE CELL
            @Override
            public View getView(int position, View cell, ViewGroup parent) {
                if (cell == null) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                    cell = inflater.inflate(R.layout.cell_follow, null);
                }

                // Init views
                final ImageView avatarImg = cell.findViewById(R.id.cfAvatarimg);
                final TextView fullnameTxt = cell.findViewById(R.id.cfFullnameTxt);
                fullnameTxt.setTypeface(Configs.titSemibold);
                final TextView usernameTxt = cell.findViewById(R.id.cfUsernameTxt);
                usernameTxt.setTypeface(Configs.titRegular);
                final TextView aboutTxt = cell.findViewById(R.id.cfAboutTxt);
                aboutTxt.setTypeface(Configs.titRegular);


                // Get Parse obj
                ParseObject fObj = followArray.get(position);

                // Get User Pointer
                ParseUser userPointer;
                if (isFollowing.matches("true")) {
                    userPointer = fObj.getParseUser(Configs.FOLLOW_IS_FOLLOWING);
                } else {
                    userPointer = fObj.getParseUser(Configs.FOLLOW_CURR_USER);
                }
                // Get userPointer
                userPointer.fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                    public void done(ParseObject userPointer, ParseException e) {

                        // Get Avatar
                        Configs.getParseImage(avatarImg, userPointer, Configs.USER_AVATAR);

                        // Get full name
                        fullnameTxt.setText(userPointer.getString(Configs.USER_FULLNAME));

                        // Get username
                        usernameTxt.setText(getString(R.string.username_formatted, userPointer.getString(Configs.USER_USERNAME)));

                        // Get aboutMe
                        if (userPointer.getString(Configs.USER_ABOUT_ME) != null) {
                            aboutTxt.setText(userPointer.getString(Configs.USER_ABOUT_ME));
                        } else {
                            aboutTxt.setText(R.string.not_available);
                        }

                    }
                });// end userPointer


                return cell;
            }

            @Override
            public int getCount() {
                return followArray.size();
            }

            @Override
            public Object getItem(int position) {
                return followArray.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }
        }


        // Init ListView and set its adapter
        followListView.setAdapter(new ListAdapter(Follow.this));
        followListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                // Get Parse Obj
                ParseObject fObj = followArray.get(position);

                // Get User Pointer
                ParseUser userPointer;
                if (isFollowing.matches("true")) {
                    userPointer = fObj.getParseUser(Configs.FOLLOW_IS_FOLLOWING);
                } else {
                    userPointer = fObj.getParseUser(Configs.FOLLOW_CURR_USER);
                }
                userPointer.fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                    public void done(ParseObject userPointer, ParseException e) {
                        Intent i = new Intent(Follow.this, OtherUserProfile.class);
                        Bundle extras = new Bundle();
                        extras.putString("userID", userPointer.getObjectId());
                        i.putExtras(extras);
                        startActivity(i);
                    }
                });// end userPointer
            }
        });
    }


}// @end
