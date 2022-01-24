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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StreamDetails extends AppCompatActivity {

    /* Views */
    ImageView avatarImg, streamImg;
    TextView fullnameTxt, usernameTxt, streamTxt, likesTxt, commentsTxt, playingTimeTxt;
    Button optionsButt, likeButt, commentsButt, shareButt, playButt;

    /* Variables */
    ParseObject sObj;
    MarshMallowPermission mmp = new MarshMallowPermission(this);
    boolean audioIsPlaying = false;
    MediaPlayer mediaPlayer;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stream_details);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Change StatusBar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.lightBlueBg));

        // Init views
        avatarImg = findViewById(R.id.sdAvatarImg);
        streamImg = findViewById(R.id.sdStreamImg);
        fullnameTxt = findViewById(R.id.sdFullnameTxt);
        fullnameTxt.setTypeface(Configs.titSemibold);
        usernameTxt = findViewById(R.id.sdUsernameTxt);
        usernameTxt.setTypeface(Configs.titRegular);
        streamTxt = findViewById(R.id.sdStreamTxt);
        streamTxt.setTypeface(Configs.titRegular);
        optionsButt = findViewById(R.id.sdOptionsButt);
        likeButt = findViewById(R.id.sdLikeButt);
        commentsButt = findViewById(R.id.sdCommentsButt);
        shareButt = findViewById(R.id.sdShareButt);
        likesTxt = findViewById(R.id.sdLikesTxt);
        likesTxt.setTypeface(Configs.titRegular);
        commentsTxt = findViewById(R.id.sdCommentsTxt);
        commentsTxt.setTypeface(Configs.titRegular);
        playButt = findViewById(R.id.sdPlayButt);
        playingTimeTxt = findViewById(R.id.sdPlayingTimeTxt);
        playingTimeTxt.setTypeface(Configs.titSemibold);

        // Get objectID from previous .java
        Bundle extras = getIntent().getExtras();
        String objectID = extras.getString("objectID");
        sObj = ParseObject.createWithoutData(Configs.STREAMS_CLASS_NAME, objectID);
        try {
            sObj.fetchIfNeeded().getParseObject(Configs.STREAMS_CLASS_NAME);

            // SHOW STREAM AND USER'S DETAILS ----------
            final ParseUser currUser = ParseUser.getCurrentUser();

            // Increment Stream views
            sObj.increment(Configs.STREAMS_VIEWS, 1);
            sObj.saveInBackground();


            // Get User Pointer
            sObj.getParseObject(Configs.STREAMS_USER_POINTER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                public void done(final ParseObject userPointer, ParseException e) {
                    if (e == null) {

                        // Get full name
                        fullnameTxt.setText(userPointer.getString(Configs.USER_FULLNAME));

                        // Get Avatar
                        Configs.getParseImage(avatarImg, userPointer, Configs.USER_AVATAR);
                        avatarImg.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Increment profile clicks
                                sObj.increment(Configs.STREAMS_PROFILE_CLICKS, 1);
                                sObj.saveInBackground();

                                // Go to OtherUserProfile
                                Intent i = new Intent(StreamDetails.this, OtherUserProfile.class);
                                Bundle extras = new Bundle();
                                extras.putString("userID", userPointer.getObjectId());
                                i.putExtras(extras);
                                startActivity(i);
                            }
                        });

                        // Get Username
                        usernameTxt.setText(getString(R.string.username_formatted, userPointer.getString(Configs.USER_USERNAME)));

                        // Get Stream text
                        streamTxt.setText(sObj.getString(Configs.STREAMS_TEXT));


                        // Get like/liked
                        List<String> likedBy = sObj.getList(Configs.STREAMS_LIKED_BY);
                        if (likedBy.contains(currUser.getObjectId())) {
                            likeButt.setBackgroundResource(R.drawable.liked_butt_small);
                        } else {
                            likeButt.setBackgroundResource(R.drawable.like_butt_small);
                        }

                        // Get Likes
                        int likes = sObj.getInt(Configs.STREAMS_LIKES);
                        likesTxt.setText(Configs.roundThousandsIntoK(likes));

                        // Get Comments
                        int comments = sObj.getInt(Configs.STREAMS_COMMENTS);
                        commentsTxt.setText(Configs.roundThousandsIntoK(comments));


                        // Get Stream Image (if any)
                        if (sObj.getParseFile(Configs.STREAMS_IMAGE) != null) {
                            Configs.getParseImage(streamImg, sObj, Configs.STREAMS_IMAGE);
                        } else {
                            streamImg.getLayoutParams().height = 1;
                        }

                        // Get Stream Video (if any)
                        if (sObj.getParseFile(Configs.STREAMS_VIDEO) != null) {
                            playButt.setVisibility(View.VISIBLE);
                        }

                        // Get Stream Audio (if any)
                        if (sObj.getParseFile(Configs.STREAMS_AUDIO) != null) {
                            playButt.setVisibility(View.VISIBLE);
                            playingTimeTxt.setVisibility(View.VISIBLE);
                        }


                        // MARK: - LIKE STREAM BUTTON ------------------------------------
                        likeButt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                // Get likedBy
                                List<String> likedBy = sObj.getList(Configs.STREAMS_LIKED_BY);

                                // UNLIKE THIS STREAM
                                if (likedBy.contains(currUser.getObjectId())) {
                                    likedBy.remove(currUser.getObjectId());
                                    sObj.put(Configs.STREAMS_LIKED_BY, likedBy);
                                    sObj.increment(Configs.STREAMS_LIKES, -1);
                                    sObj.saveInBackground();

                                    likeButt.setBackgroundResource(R.drawable.like_butt_small);
                                    int likes = sObj.getInt(Configs.STREAMS_LIKES);
                                    likesTxt.setText(Configs.roundThousandsIntoK(likes));


                                    // LIKE THIS STREAM
                                } else {
                                    likedBy.add(currUser.getObjectId());
                                    sObj.put(Configs.STREAMS_LIKED_BY, likedBy);
                                    sObj.increment(Configs.STREAMS_LIKES, 1);
                                    sObj.saveInBackground();

                                    likeButt.setBackgroundResource(R.drawable.liked_butt_small);
                                    int likes = sObj.getInt(Configs.STREAMS_LIKES);
                                    likesTxt.setText(Configs.roundThousandsIntoK(likes));

                                    // Send push notification
                                    String pushMessage = getString(R.string.user_liked_stream,
                                            currUser.getString(Configs.USER_FULLNAME), sObj.getString(Configs.STREAMS_TEXT));
                                    Configs.sendPushNotification(pushMessage, (ParseUser) userPointer, StreamDetails.this);

                                    // Save Activity
                                    Configs.saveActivity(currUser, sObj, pushMessage);
                                }
                            }
                        });


                        // MARK: - COMMENTS BUTTON ------------------------------------
                        commentsButt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent i = new Intent(StreamDetails.this, Comments.class);
                                Bundle extras = new Bundle();
                                extras.putString("objectID", sObj.getObjectId());
                                i.putExtras(extras);
                                startActivity(i);
                            }
                        });


                        // MARK: - SHARE BUTTON ------------------------------------
                        shareButt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!mmp.checkPermissionForWriteExternalStorage()) {
                                    mmp.requestPermissionForWriteExternalStorage();
                                } else {
                                    Bitmap bitmap;
                                    if (sObj.getParseFile(Configs.STREAMS_IMAGE) != null) {
                                        bitmap = ((BitmapDrawable) streamImg.getDrawable()).getBitmap();
                                    } else {
                                        bitmap = BitmapFactory.decodeResource(StreamDetails.this.getResources(), R.drawable.logo);
                                    }
                                    Uri uri = Configs.getImageUri(StreamDetails.this, bitmap);
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("image/jpeg");
                                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.stream_share_formatted, sObj.getString(Configs.STREAMS_TEXT), getString(R.string.app_name)));
                                    startActivity(Intent.createChooser(intent, getString(R.string.stream_share_on)));
                                }

                                // Increment shares amount
                                sObj.increment(Configs.STREAMS_SHARES, 1);
                                sObj.saveInBackground();

                            }
                        });


                        // MARK: - PLAY VIDEO OR AUDIO BUTTON -----------------------------------------------
                        playButt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                // PLAY VIDEO PREVIEW -------------------
                                if (sObj.getParseFile(Configs.STREAMS_VIDEO) != null) {
                                    ParseFile videoFile = sObj.getParseFile(Configs.STREAMS_VIDEO);
                                    String videoURL = videoFile.getUrl();

                                    Intent i = new Intent(StreamDetails.this, ShowVideo.class);
                                    Bundle extras = new Bundle();
                                    extras.putString("videoURL", videoURL);
                                    i.putExtras(extras);
                                    startActivity(i);


                                    // PLAY AUDIO PREVIEW ----------------------
                                } else if (sObj.getParseFile(Configs.STREAMS_AUDIO) != null) {
                                    ParseFile audioFile = sObj.getParseFile(Configs.STREAMS_AUDIO);
                                    String audioURL = audioFile.getUrl();

                                    // Init mediaPlayer
                                    mediaPlayer = new MediaPlayer();

                                    // Start Audio playing
                                    if (!audioIsPlaying) {
                                        try {
                                            mediaPlayer.setDataSource(audioURL);
                                            mediaPlayer.prepare();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        mediaPlayer.start();

                                        // SET PLAYER TIMER
                                        playingTimeTxt.setText(R.string.stream_details_default_duration);

                                        handler.postDelayed(new Runnable() {
                                            long time = 0;

                                            @Override
                                            public void run() {
                                                time += 1000;

                                                @SuppressLint("DefaultLocale")
                                                String formattedTimer = String.format("%02d:%02d",
                                                        TimeUnit.MILLISECONDS.toMinutes(time),
                                                        TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
                                                );
                                                playingTimeTxt.setText(formattedTimer);

                                                handler.postDelayed(this, 1000);
                                            }
                                        }, 1000); // 1 second delay


                                        audioIsPlaying = true;
                                        playButt.setBackgroundResource(R.drawable.stop_butt);

                                        // Check when audio finished
                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                            @Override
                                            public void onCompletion(MediaPlayer mp) {
                                                handler.removeCallbacksAndMessages(null);
                                                audioIsPlaying = false;
                                                playButt.setBackgroundResource(R.drawable.play_butt);
                                            }
                                        });


                                        // Stop Audio playing
                                    } else {
                                        handler.removeCallbacksAndMessages(null);
                                        mediaPlayer.stop();
                                        mediaPlayer.reset();
                                        mediaPlayer.release();
                                        mediaPlayer = null;
                                        audioIsPlaying = false;
                                        playButt.setBackgroundResource(R.drawable.play_butt);
                                    }

                                }// end IF
                            }
                        });


                        // MARK: - OPTIONS BUTTON ------------------------------------
                        optionsButt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                // Init list Items
                                List<String> listItems = new ArrayList<String>();
                                listItems.add(getString(R.string.stream_details_report_option));
                                listItems.add(getString(R.string.stream_details_report_details, userPointer.getString(Configs.USER_USERNAME)));
                                if (userPointer.getObjectId().matches(currUser.getObjectId())) {
                                    listItems.add(getString(R.string.stream_details_delete_option));
                                }
                                final CharSequence[] options = listItems.toArray(new CharSequence[listItems.size()]);

                                // Fire alert
                                AlertDialog.Builder alert = new AlertDialog.Builder(StreamDetails.this);
                                alert.setTitle(R.string.select_source)
                                        .setIcon(R.drawable.logo)
                                        .setItems(options, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                switch (which) {


                                                    // REPORT STREAM ---------------------
                                                    case 0:
                                                        AlertDialog.Builder alert = new AlertDialog.Builder(StreamDetails.this);
                                                        alert.setMessage(R.string.stream_details_report_alert_title)
                                                                .setTitle(R.string.app_name)
                                                                .setPositiveButton(R.string.stream_details_report_option, new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                                        Configs.showPD(getString(R.string.stream_details_report_loading), StreamDetails.this);
                                                                        List<String> reportedBy = sObj.getList(Configs.STREAMS_REPORTED_BY);
                                                                        reportedBy.add(currUser.getObjectId());
                                                                        sObj.put(Configs.STREAMS_REPORTED_BY, reportedBy);

                                                                        sObj.saveInBackground(new SaveCallback() {
                                                                            @Override
                                                                            public void done(ParseException e) {
                                                                                if (e == null) {
                                                                                    Configs.hidePD();
                                                                                    Configs.simpleAlert(getString(R.string.stream_details_report_success), StreamDetails.this);
                                                                                    Configs.mustRefresh = true;
                                                                                }
                                                                            }
                                                                        });

                                                                    }
                                                                })
                                                                .setNegativeButton(R.string.cancel_button, null)
                                                                .setIcon(R.drawable.logo);
                                                        alert.create().show();

                                                        break;


                                                    // REPORT USER ----------------------------------
                                                    case 1:
                                                        AlertDialog.Builder alert2 = new AlertDialog.Builder(StreamDetails.this);
                                                        alert2.setMessage(getString(R.string.stream_details_report_user_alert_title, userPointer.getString(Configs.USER_USERNAME)))
                                                                .setTitle(R.string.app_name)
                                                                .setPositiveButton(getString(R.string.stream_details_report_details, userPointer.getString(Configs.USER_USERNAME)), new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                                        Configs.showPD(getString(R.string.stream_details_report_loading), StreamDetails.this);

                                                                        // Report user via Cloud Code
                                                                        HashMap<String, Object> params = new HashMap<String, Object>();
                                                                        params.put("userId", userPointer.getObjectId());
                                                                        params.put("reportMessage", "OFFENSIVE USER");

                                                                        ParseCloud.callFunctionInBackground("reportUser", params, new FunctionCallback<ParseUser>() {
                                                                            public void done(ParseUser user, ParseException error) {
                                                                                if (error == null) {
                                                                                    Configs.hidePD();
                                                                                    Configs.simpleAlert(getString(R.string.user_profile_report_success, userPointer.getString(Configs.USER_FULLNAME)), StreamDetails.this);

                                                                                    Configs.mustRefresh = true;

                                                                                    // Automatically report all User's streams
                                                                                    ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.STREAMS_CLASS_NAME);
                                                                                    query.whereEqualTo(Configs.STREAMS_USER_POINTER, userPointer);
                                                                                    query.findInBackground(new FindCallback<ParseObject>() {
                                                                                        @Override
                                                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                                                            for (int i = 0; i < objects.size(); i++) {
                                                                                                ParseObject stObj = objects.get(i);
                                                                                                List<String> reportedBy = stObj.getList(Configs.STREAMS_REPORTED_BY);
                                                                                                reportedBy.add(currUser.getObjectId());
                                                                                                stObj.put(Configs.STREAMS_REPORTED_BY, reportedBy);
                                                                                                stObj.saveInBackground();
                                                                                            }
                                                                                        }
                                                                                    });


                                                                                    // Automatically report all User's comments
                                                                                    ParseQuery<ParseObject> query2 = ParseQuery.getQuery(Configs.COMMENTS_CLASS_NAME);
                                                                                    query2.whereEqualTo(Configs.COMMENTS_USER_POINTER, userPointer);
                                                                                    query2.findInBackground(new FindCallback<ParseObject>() {
                                                                                        @Override
                                                                                        public void done(List<ParseObject> objects, ParseException e) {

                                                                                            for (int i = 0; i < objects.size(); i++) {
                                                                                                ParseObject commObj = objects.get(i);
                                                                                                List<String> reportedBy = commObj.getList(Configs.COMMENTS_REPORTED_BY);
                                                                                                reportedBy.add(currUser.getObjectId());
                                                                                                commObj.put(Configs.COMMENTS_REPORTED_BY, reportedBy);
                                                                                                commObj.saveInBackground();
                                                                                            }
                                                                                        }
                                                                                    });

                                                                                    // Error in Cloud Code
                                                                                } else {
                                                                                    Configs.hidePD();
                                                                                    Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                                                                                }
                                                                            }
                                                                        });

                                                                    }
                                                                })
                                                                .setNegativeButton(R.string.cancel_button, null)
                                                                .setIcon(R.drawable.logo);
                                                        alert2.create().show();

                                                        break;


                                                    // DELETE STREAM (IF IT'S YOURS) -----------------------------------------
                                                    case 2:
                                                        Configs.showPD(getString(R.string.loading_dialog_please_wait), StreamDetails.this);
                                                        sObj.deleteInBackground(new DeleteCallback() {
                                                            @Override
                                                            public void done(ParseException e) {
                                                                if (e == null) {
                                                                    Configs.hidePD();
                                                                    Configs.mustRefresh = true;

                                                                    // Delete those rows from the Activity class which have this Stream as a Pointer
                                                                    ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.ACTIVITY_CLASS_NAME);
                                                                    query.whereEqualTo(Configs.ACTIVITY_STREAM_POINTER, sObj);
                                                                    query.findInBackground(new FindCallback<ParseObject>() {
                                                                        @Override
                                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                                            if (e == null) {
                                                                                for (int i = 0; i < objects.size(); i++) {
                                                                                    ParseObject aObj = objects.get(i);
                                                                                    aObj.deleteInBackground();

                                                                                    if (i == objects.size() - 1) {
                                                                                        finish();
                                                                                    }
                                                                                }
                                                                            } else {
                                                                                Configs.simpleAlert(e.getMessage(), StreamDetails.this);
                                                                            }
                                                                        }
                                                                    });

                                                                }
                                                            }
                                                        });

                                                        break;


                                                }
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel_button, null);
                                alert.create().show();
                            }
                        });


                        // error
                    } else {
                        Configs.hidePD();
                        Configs.simpleAlert(e.getMessage(), StreamDetails.this);

                    }
                }
            });// end userPointer


        } catch (ParseException e) {
            e.printStackTrace();
        }

        // MARK: - BACK BUTTON ------------------------------------
        Button backButt = findViewById(R.id.sdBackButt);
        backButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    handler.removeCallbacksAndMessages(null);
                }
                finish();
            }
        });
    }// end onCreate()
}// @end
