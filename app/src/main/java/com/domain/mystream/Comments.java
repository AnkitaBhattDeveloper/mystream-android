package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Comments extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    /* Views */
    ListView commentsListView;
    TextView fullnameTxt, streamTextTxt;
    EditText commentTxt;
    SwipeRefreshLayout refreshControl;

    /* Variables */
    ParseObject sObj;
    List<ParseObject> commentsArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comments);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Change StatusBar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.lightBlueBg));


        // Init views
        commentsListView = findViewById(R.id.commListView);
        fullnameTxt = findViewById(R.id.commFullnameTxt);
        fullnameTxt.setTypeface(Configs.titSemibold);
        streamTextTxt = findViewById(R.id.commStreamTxt);
        streamTextTxt.setTypeface(Configs.titRegular);
        commentTxt = findViewById(R.id.commCommentTxt);
        commentTxt.setTypeface(Configs.titRegular);


        // Init a refreshControl
        refreshControl = findViewById(R.id.swiperefresh);
        refreshControl.setOnRefreshListener(this);


        // Get objectID from previous .java
        Bundle extras = getIntent().getExtras();
        String objectID = extras.getString("objectID");
        sObj = ParseObject.createWithoutData(Configs.STREAMS_CLASS_NAME, objectID);
        try {
            sObj.fetchIfNeeded().getParseObject(Configs.STREAMS_CLASS_NAME);

            // Get User Pointer
            sObj.getParseObject(Configs.STREAMS_USER_POINTER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                public void done(ParseObject userPointer, ParseException e) {
                    if (e == null) {
                        // Get full name
                        fullnameTxt.setText(userPointer.getString(Configs.USER_FULLNAME));

                        // Get Stream text
                        streamTextTxt.setText(sObj.getString(Configs.STREAMS_TEXT));

                        // Call query
                        queryComments();

                    } else {
                        Configs.simpleAlert(e.getMessage(), Comments.this);
                    }
                }
            }); // end userPointer


            // MARK: - SEND COMMENT BUTTON ------------------------------------
            Button sendButt = findViewById(R.id.commSendButt);
            sendButt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (commentTxt.getText().toString().matches("")) {
                        Configs.simpleAlert(getString(R.string.comments_validation_error), Comments.this);

                    } else {
                        Configs.showPD(getString(R.string.loading_dialog_please_wait), Comments.this);
                        ParseObject cObj = new ParseObject(Configs.COMMENTS_CLASS_NAME);
                        final ParseUser currUser = ParseUser.getCurrentUser();

                        // Save data
                        cObj.put(Configs.COMMENTS_USER_POINTER, currUser);
                        cObj.put(Configs.COMMENTS_STREAM_POINTER, sObj);
                        cObj.put(Configs.COMMENTS_COMMENT, commentTxt.getText().toString());
                        List<String> reportedBy = new ArrayList<>();
                        cObj.put(Configs.COMMENTS_REPORTED_BY, reportedBy);

                        // Saving block
                        cObj.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    Configs.hidePD();
                                    dismissKeyboard();
                                    commentTxt.setText("");

                                    // Increment comments of this Stream
                                    sObj.increment(Configs.STREAMS_COMMENTS, 1);
                                    sObj.saveInBackground();

                                    // Get userPointer
                                    sObj.getParseObject(Configs.STREAMS_USER_POINTER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                                        public void done(ParseObject userPointer, ParseException e) {

                                            // Send push notification
                                            String pushMessage =
                                                    getString(R.string.comments_notification_commented,
                                                            currUser.getString(Configs.USER_FULLNAME), sObj.getString(Configs.STREAMS_TEXT));
                                            Configs.sendPushNotification(pushMessage, (ParseUser) userPointer, Comments.this);

                                            // Save Activity
                                            Configs.saveActivity(currUser, sObj, pushMessage);

                                        }
                                    });// end userPointer


                                    // Recall query
                                    queryComments();

                                    // error
                                } else {
                                    Configs.hidePD();
                                    Configs.simpleAlert(e.getMessage(), Comments.this);
                                }
                            }
                        });
                    }
                }
            });


        } catch (ParseException e) {
            e.printStackTrace();
        }


        // MARK: - BACK BUTTON ------------------------------------
        Button backButt = findViewById(R.id.commBackButt);
        backButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        // INTERSTITIAL AD IMPLEMENTATION ------------------------------------
        final InterstitialAd interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.ADMOB_INTERSTITIAL_UNIT_ID));
        AdRequest requestForInterstitial = new AdRequest.Builder().build();
        interstitialAd.loadAd(requestForInterstitial);
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.i("log-", "INTERSTITIAL is loaded!");
                if (interstitialAd.isLoaded()) {
                    interstitialAd.show();
                }
            }
        });

    }// end onCreate()


    // MARK: - QUERY COMMENTS ------------------------------------------------
    void queryComments() {
        Configs.showPD(getString(R.string.loading_dialog_please_wait), Comments.this);
        ParseUser currUser = ParseUser.getCurrentUser();
        List<String> currUserID = new ArrayList<>();
        currUserID.add(currUser.getObjectId());


        ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.COMMENTS_CLASS_NAME);
        query.whereEqualTo(Configs.COMMENTS_STREAM_POINTER, sObj);
        query.whereNotContainedIn(Configs.COMMENTS_REPORTED_BY, currUserID);
        query.orderByDescending(Configs.COMMENTS_CREATED_AT);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    Configs.hidePD();
                    commentsArray = objects;
                    reloadData();

                } else {
                    Configs.hidePD();
                    Configs.simpleAlert(e.getMessage(), Comments.this);
                }
            }
        });
    }


    // MARK: - RELOAD LISTVIEW DATA ----------------------------------------------------------
    void reloadData() {
        class ListAdapter extends BaseAdapter {
            private Context context;

            public ListAdapter(Context context) {
                super();
                this.context = context;
            }

            // CONFIGURE CELL
            @Override
            public View getView(final int position, View cell, ViewGroup parent) {
                if (cell == null) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                    cell = inflater.inflate(R.layout.cell_comment, null);
                }

                // Init views
                final ImageView avatarImg = cell.findViewById(R.id.ccommAvatarimg);
                final TextView fullnameTxt = cell.findViewById(R.id.ccommFullnameTxt);
                fullnameTxt.setTypeface(Configs.titSemibold);
                final TextView commentTxt = cell.findViewById(R.id.ccommCommTxt);
                commentTxt.setTypeface(Configs.titRegular);
                final TextView dateTxt = cell.findViewById(R.id.ccommDateTxt);
                dateTxt.setTypeface(Configs.titRegular);
                final Button optionsButt = cell.findViewById(R.id.ccommOptionsButt);

                // Get Parse obj
                final ParseObject cObj = commentsArray.get(position);

                // Get userPointer
                cObj.getParseObject(Configs.COMMENTS_USER_POINTER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                    public void done(final ParseObject userPointer, ParseException e) {

                        // Get Avatar
                        Configs.getParseImage(avatarImg, userPointer, Configs.USER_AVATAR);

                        // TAP AVATAR TO SEE OTHE USER PROFILE -----------------------------
                        avatarImg.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dismissKeyboard();

                                Intent i = new Intent(Comments.this, OtherUserProfile.class);
                                Bundle extras = new Bundle();
                                extras.putString("userID", userPointer.getObjectId());
                                i.putExtras(extras);
                                startActivity(i);
                            }
                        });


                        // Get full name
                        fullnameTxt.setText(userPointer.getString(Configs.USER_FULLNAME));

                        // Get comment
                        commentTxt.setText(cObj.getString(Configs.COMMENTS_COMMENT));

                        // Get comment date
                        Date now = new Date();
                        String cDate = Configs.timeAgoSinceDate(now);
                        dateTxt.setText(cDate);


                        // MARK: - OPTIONS BUTTON ---------------------------------------------
                        optionsButt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                final ParseUser currUser = ParseUser.getCurrentUser();
                                dismissKeyboard();

                                AlertDialog.Builder alert = new AlertDialog.Builder(Comments.this);
                                alert.setTitle(R.string.comments_select_source)
                                        .setIcon(R.drawable.logo)
                                        .setItems(new CharSequence[]{
                                                getString(R.string.comments_report_option),
                                                getString(R.string.comments_copy_option)
                                        }, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                switch (which) {

                                                    // REPORT COMMENT -------------------------
                                                    case 0:
                                                        AlertDialog.Builder alert = new AlertDialog.Builder(Comments.this);
                                                        alert.setMessage(R.string.comments_report_title)
                                                                .setTitle(R.string.app_name)
                                                                .setPositiveButton(R.string.comments_report_button, new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                                        Configs.showPD(getString(R.string.loading_dialog_please_wait), Comments.this);
                                                                        List<String> reportedBy = cObj.getList(Configs.COMMENTS_REPORTED_BY);
                                                                        reportedBy.add(currUser.getObjectId());
                                                                        cObj.put(Configs.COMMENTS_REPORTED_BY, reportedBy);

                                                                        cObj.saveInBackground(new SaveCallback() {
                                                                            @Override
                                                                            public void done(ParseException e) {
                                                                                if (e == null) {
                                                                                    Configs.hidePD();
                                                                                    Configs.simpleAlert(getString(R.string.comments_report_success), Comments.this);

                                                                                    // Remove selected row
                                                                                    commentsArray.remove(position);
                                                                                    commentsListView.invalidateViews();
                                                                                    commentsListView.refreshDrawableState();
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                })
                                                                .setNegativeButton(R.string.cancel_button, null)
                                                                .setIcon(R.drawable.logo);
                                                        alert.create().show();
                                                        break;


                                                    // COPY COMMMENT ---------------------------
                                                    case 1:
                                                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                                        ClipData clip = ClipData.newPlainText(getString(R.string.comments_copy_success), cObj.getString(Configs.COMMENTS_COMMENT));
                                                        clipboard.setPrimaryClip(clip);
                                                        break;
                                                }
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel_button, null);
                                alert.create().show();

                            }
                        });// end optionsButt


                    }
                });// end userPointer


                return cell;
            }

            @Override
            public int getCount() {
                return commentsArray.size();
            }

            @Override
            public Object getItem(int position) {
                return commentsArray.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }
        }


        // Init ListView and set its adapter
        commentsListView.setAdapter(new ListAdapter(Comments.this));


    }


    // MARK: - DISMISS KEYBOARD
    void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(commentTxt.getWindowToken(), 0);
    }


    // MARK: - REFRESH DATA ----------------------------------------
    @Override
    public void onRefresh() {
        // Recall query
        queryComments();

        if (refreshControl.isRefreshing()) {
            refreshControl.setRefreshing(false);
        }
    }


}// @end
