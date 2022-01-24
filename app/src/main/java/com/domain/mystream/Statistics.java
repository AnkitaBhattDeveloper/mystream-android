package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.content.pm.ActivityInfo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

public class Statistics extends AppCompatActivity {

    /* Views */
    TextView fullNameTxt, usernameTxt, streamTextTxt, viewsTxt,
            likesTxt, profileClicksTxt, commentsTxt, sharesTxt;

    /* Variables */
    ParseObject sObj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Change StatusBar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.main_color));


        // Init views
        fullNameTxt = findViewById(R.id.statFullNameTxt);
        fullNameTxt.setTypeface(Configs.titSemibold);
        usernameTxt = findViewById(R.id.statUsernameTxt);
        usernameTxt.setTypeface(Configs.titRegular);
        streamTextTxt = findViewById(R.id.statStreamTxt);
        streamTextTxt.setTypeface(Configs.titRegular);
        viewsTxt = findViewById(R.id.statViewsTxt);
        viewsTxt.setTypeface(Configs.titRegular);
        likesTxt = findViewById(R.id.statLikesTxt);
        likesTxt.setTypeface(Configs.titRegular);
        profileClicksTxt = findViewById(R.id.statClicksTxt);
        profileClicksTxt.setTypeface(Configs.titRegular);
        commentsTxt = findViewById(R.id.statCommentsTxt);
        commentsTxt.setTypeface(Configs.titRegular);
        sharesTxt = findViewById(R.id.statSharesTxt);
        sharesTxt.setTypeface(Configs.titRegular);


        // Get objectID from previous .java
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        String objectID = extras.getString("objectID");
        sObj = ParseObject.createWithoutData(Configs.STREAMS_CLASS_NAME, objectID);
        try {
            sObj.fetchIfNeeded().getParseObject(Configs.STREAMS_CLASS_NAME);

            // SHOW USER AND STREAM DATA --------------------------
            ParseUser currUser = ParseUser.getCurrentUser();

            fullNameTxt.setText(currUser.getString(Configs.USER_FULLNAME));
            usernameTxt.setText(getString(R.string.username_formatted, currUser.getString(Configs.USER_USERNAME)));
            streamTextTxt.setText(sObj.getString(Configs.STREAMS_TEXT));

            int views = sObj.getInt(Configs.STREAMS_VIEWS);
            viewsTxt.setText(Configs.roundThousandsIntoK(views));

            int likes = sObj.getInt(Configs.STREAMS_LIKES);
            likesTxt.setText(Configs.roundThousandsIntoK(likes));

            int profileClicks = sObj.getInt(Configs.STREAMS_PROFILE_CLICKS);
            profileClicksTxt.setText(Configs.roundThousandsIntoK(profileClicks));

            int comments = sObj.getInt(Configs.STREAMS_COMMENTS);
            commentsTxt.setText(Configs.roundThousandsIntoK(comments));

            int shares = sObj.getInt(Configs.STREAMS_SHARES);
            sharesTxt.setText(Configs.roundThousandsIntoK(shares));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        // MARK: - BACK BUTTON ------------------------------------
        Button backButt = findViewById(R.id.statBackButt);
        backButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }// end onCreate()
}//@end
