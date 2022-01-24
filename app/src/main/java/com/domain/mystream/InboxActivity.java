package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class InboxActivity extends AppCompatActivity {

    /* Views */
    Button optionsButt, sendButt, uploadPicButt;
    TextView usernameTxt;
    EditText messageTxt;
    ListView iListView;
    ImageView imgPreview;

    /* Variables */
    ParseUser userObj;
    ParseObject adObj;
    List<ParseObject> inboxArray;
    List<ParseObject> chatsArray;
    String lastMessageStr = null;
    Timer refreshTimerForInboxQuery = new Timer();
    Bitmap imageToSend = null;
    MarshMallowPermission mmp = new MarshMallowPermission(this);

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inbox_activity);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Init views
        optionsButt = findViewById(R.id.inOptionsButt);
        uploadPicButt = findViewById(R.id.inUploadPicButt);
        sendButt = findViewById(R.id.inSendButt);
        sendButt.setTypeface(Configs.titSemibold);
        usernameTxt = findViewById(R.id.inUsernameTxt);
        usernameTxt.setTypeface(Configs.titSemibold);
        messageTxt = findViewById(R.id.inMessageTxt);
        messageTxt.setTypeface(Configs.titRegular);

        iListView = findViewById(R.id.inInboxListView);


        // Hide imgPreview
        imgPreview = (ImageView) findViewById(R.id.inImagePreview);
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(imgPreview.getLayoutParams());
        marginParams.setMargins(0, 5000, 0, 0);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(marginParams);
        imgPreview.setLayoutParams(layoutParams);

        // Hide imgPreview on click
        imgPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(imgPreview.getLayoutParams());
                marginParams.setMargins(0, 5000, 0, 0);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(marginParams);
                imgPreview.setLayoutParams(layoutParams);
            }
        });

        // Get objectID from previous .java
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        String userID = extras.getString("userID");

        // Get userObj
        userObj = (ParseUser) ParseUser.createWithoutData(Configs.USER_CLASS_NAME, userID);
        try {
            userObj.fetchIfNeeded().getParseUser(Configs.USER_CLASS_NAME);

            // Call query
            queryInbox();

            // Get User's username
            usernameTxt.setText(getString(R.string.username_formatted, userObj.getString(Configs.USER_USERNAME)));


            // Start refresh Timer for Inbox query
            startRefreshTimer();

            ParseUser currUser = ParseUser.getCurrentUser();
            List<String> hasBlocked = currUser.getList(Configs.USER_HAS_BLOCKED);
            Log.i("log-", "HAS BLOCKED: " + hasBlocked);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        // MARK: - UPLOAD IMAGE BUTTON ------------------------------------
        uploadPicButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert = new AlertDialog.Builder(InboxActivity.this);
                alert.setMessage(getString(R.string.select_source))
                        .setTitle(R.string.app_name)
                        .setPositiveButton(R.string.take_picture_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (!mmp.checkPermissionForCamera()) {
                                    mmp.requestPermissionForCamera();
                                } else {
                                    openCamera();
                                }
                            }
                        })

                        .setNegativeButton(R.string.pick_from_gallery_option, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (!mmp.checkPermissionForReadExternalStorage()) {
                                    mmp.requestPermissionForReadExternalStorage();
                                } else {
                                    openGallery();
                                }
                            }
                        })

                        .setNeutralButton(R.string.cancel_button, null)
                        .setIcon(R.drawable.logo);
                alert.create().show();
            }
        });


        // MARK: - SEND MESSAGE BUTTON ---------------------------------------------
        sendButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (messageTxt.getText().toString().matches("")) {
                    Configs.simpleAlert(getString(R.string.inbox_send_picture_error), InboxActivity.this);
                } else {
                    sendMessage();
                }
            }
        });


        // OPTIONS BUTTON -------------------------------------------------
        optionsButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final ParseUser currUser = ParseUser.getCurrentUser();
                final List<String> hasBlocked = currUser.getList(Configs.USER_HAS_BLOCKED);

                // Set blockUser  Action title
                String blockTitle = "";
                if (hasBlocked.contains(userObj.getObjectId())) {
                    blockTitle = getString(R.string.inbox_unblock_user);
                } else {
                    blockTitle = getString(R.string.inbox_block_user);
                }


                AlertDialog.Builder alert = new AlertDialog.Builder(InboxActivity.this);
                final String finalBlockTitle = blockTitle;
                AlertDialog.Builder builder = alert.setTitle(R.string.inbox_select_option)
                        .setIcon(R.drawable.logo)
                        .setItems(new CharSequence[]{
                                blockTitle,
                                getString(R.string.inbox_delete_chat)
                        }, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {

                                    // BLOCK/UNBLOCK USER --------------------------------------------------
                                    case 0:

                                        // Block User
                                        if (finalBlockTitle.matches(getString(R.string.inbox_block_user))) {
                                            hasBlocked.add(userObj.getObjectId());
                                            currUser.put(Configs.USER_HAS_BLOCKED, hasBlocked);
                                            currUser.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    AlertDialog.Builder alert = new AlertDialog.Builder(InboxActivity.this);
                                                    alert.setMessage(getString(R.string.inbox_blocked_user, userObj.getString(Configs.USER_USERNAME)))
                                                            .setTitle(R.string.app_name)
                                                            .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    finish();
                                                                }
                                                            })
                                                            .setIcon(R.drawable.logo);
                                                    alert.create().show();
                                                }
                                            });

                                            // Unblock User
                                        } else {
                                            hasBlocked.remove(userObj.getObjectId());
                                            currUser.put(Configs.USER_HAS_BLOCKED, hasBlocked);
                                            currUser.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    Configs.simpleAlert(getString(R.string.inbox_unblocked_user, userObj.getString(Configs.USER_USERNAME)), InboxActivity.this);
                                                }
                                            });
                                        }

                                        break;


                                    // DELETE CHAT --------------------------------------------------------
                                    case 1:

                                        AlertDialog.Builder alert = new AlertDialog.Builder(InboxActivity.this);
                                        alert.setMessage(getString(R.string.inbox_delete_chat_alert_title, userObj.getString(Configs.USER_USERNAME)))
                                                .setTitle(R.string.app_name)
                                                .setPositiveButton(R.string.inbox_delete_chat, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {

                                                        // Delete all Inbox messages
                                                        for (int j = 0; j < inboxArray.size(); j++) {
                                                            ParseObject iObj = inboxArray.get(j);
                                                            iObj.deleteInBackground(new DeleteCallback() {
                                                                @Override
                                                                public void done(ParseException e) {
                                                                    Log.i("log-", "DELETED INBOX ITEM!");
                                                                }
                                                            });
                                                        }


                                                        // Delete Chat in Chats class
                                                        String inboxId1 = ParseUser.getCurrentUser().getObjectId() + userObj.getObjectId();
                                                        String inboxId2 = userObj.getObjectId() + ParseUser.getCurrentUser().getObjectId();

                                                        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.MESSAGES_CLASS_NAME);
                                                        String[] ids = {inboxId1, inboxId2};
                                                        query.whereContainedIn("chatID", Arrays.asList(ids));
                                                        query.whereContainedIn("chatID", Arrays.asList(ids));

                                                        query.findInBackground(new FindCallback<ParseObject>() {
                                                            public void done(List<ParseObject> objects, ParseException error) {
                                                                if (error == null) {
                                                                    chatsArray = objects;
                                                                    ParseObject chatsObj = chatsArray.get(0);
                                                                    chatsObj.deleteInBackground(new DeleteCallback() {
                                                                        @Override
                                                                        public void done(ParseException e) {
                                                                            if (e == null) {
                                                                                Log.i("log-", "CHAT DELETED IN THE 'Messages' CLASS");
                                                                                finish();
                                                                            }
                                                                        }
                                                                    });

                                                                }
                                                            }
                                                        });

                                                    }
                                                })// end alert

                                                .setNegativeButton(R.string.cancel_button, null)
                                                .setIcon(R.drawable.logo);
                                        alert.create().show();

                                        break;

                                }
                            }
                        })
                        .setNegativeButton(R.string.inbox_delete_chat, null);
                alert.create().show();

            }
        });


        // BACK BUTTON -------------------------------------------------
        Button backButt = findViewById(R.id.inBackButt);
        backButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshTimerForInboxQuery.cancel();
                imageToSend = null;
                finish();
            }
        });


    }// end onCreate()


    // TIMER THAT RECALLS THE QUERY FOR INBOX MESSAGES ------------------------------------
    void startRefreshTimer() {
        int delay = 15000;
        refreshTimerForInboxQuery.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // Call query
                        queryInbox();

                    }
                });
            }
        }, delay, delay);
    }


    // MARK: - QUERY INBOX --------------------------------------------------------
    void queryInbox() {

        String inboxId1 = ParseUser.getCurrentUser().getObjectId() + userObj.getObjectId();
        String inboxId2 = userObj.getObjectId() + ParseUser.getCurrentUser().getObjectId();

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.INBOX_CLASS_NAME);
        String[] ids = {inboxId1, inboxId2};
        query.whereContainedIn("inboxID", Arrays.asList(ids));
        query.whereContainedIn("inboxID", Arrays.asList(ids));

        query.orderByAscending(Configs.INBOX_CREATED_AT);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException error) {
                if (error == null) {
                    inboxArray = objects;


                    // Scroll listView to bottom after 1.5 sec
                    if (objects.size() != 0) {
                        int delay = 1500;
                        new Timer().scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollListViewToBottom();
                                    }
                                });
                            }
                        }, delay, delay);
                    }


                    // Init ListView and set its adapter
                    iListView.setAdapter(new InboxListAdapter(InboxActivity.this, inboxArray));


                    // Error in query
                } else {
                    Configs.simpleAlert(error.getMessage(), InboxActivity.this);
                }
            }
        });

    }


    void scrollListViewToBottom() {
        iListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        iListView.setStackFromBottom(true);
    }


    // INBOX LIST ADAPTER ----------------------------------------------------------------------
    class InboxListAdapter extends BaseAdapter {
        private Context context;

        public InboxListAdapter(Context context, List<ParseObject> objects) {
            super();
            this.context = context;
        }


        // CONFIGURE CELL
        @Override
        public View getView(int position, View cell, ViewGroup parent) {
            if (cell == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                assert inflater != null;
                cell = inflater.inflate(R.layout.cell_inbox, null);
            }
            final View finalCell = cell;

            // Get Parse object
            final ParseObject iObj = inboxArray.get(position);
            final ParseUser currUser = ParseUser.getCurrentUser();


            // Init cells layouts
            final RelativeLayout senderCell = finalCell.findViewById(R.id.senderCell);
            final RelativeLayout receiverCell = finalCell.findViewById(R.id.receiverCell);


            // Get userPointer
            iObj.getParseObject(Configs.INBOX_SENDER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                @SuppressLint("SetTextI18n")
                public void done(ParseObject userPointer, ParseException e) {


                    // CELL WITH MESSAGE FROM CURRENT USER (SENDER) -------------------------------
                    if (userPointer.getString(Configs.USER_USERNAME).matches(currUser.getUsername())) {
                        senderCell.setVisibility(View.VISIBLE);
                        receiverCell.setVisibility(View.INVISIBLE);

                        // Get username
                        TextView usernTxt = finalCell.findViewById(R.id.sUsernameTxt);
                        usernTxt.setTypeface(Configs.titSemibold);
                        usernTxt.setText(getString(R.string.username_formatted, userPointer.getString(Configs.USER_USERNAME)));

                        // Get message
                        TextView messTxt = finalCell.findViewById(R.id.sMessTxt);
                        messTxt.setTypeface(Configs.titLight);
                        messTxt.setText(iObj.getString(Configs.INBOX_MESSAGE));

                        // Get date
                        TextView dateTxt = finalCell.findViewById(R.id.sDateTxt);
                        dateTxt.setTypeface(Configs.titLight);
                        dateTxt.setText(Configs.timeAgoSinceDate(iObj.getCreatedAt()));

                        // Get avatar
                        final ImageView avatarImg = finalCell.findViewById(R.id.sAvatarImg);
                        Configs.getParseImage(avatarImg, userPointer, Configs.USER_AVATAR);


                        // THIS MESSAGE HAS AN IMAGE ------------------------
                        if (iObj.get(Configs.INBOX_IMAGE) != null) {

                            final ImageView inImage = (ImageView) finalCell.findViewById(R.id.sImage);
                            inImage.setClipToOutline(true);
                            inImage.setVisibility(View.VISIBLE);
                            messTxt.setVisibility(View.INVISIBLE);

                            // Get the image
                            final Bitmap[] bm = {null};
                            ParseFile fileObject2 = (ParseFile) iObj.get(Configs.INBOX_IMAGE);
                            fileObject2.getDataInBackground(new GetDataCallback() {
                                public void done(byte[] data, ParseException error) {
                                    if (error == null) {
                                        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                        if (bmp != null) {
                                            inImage.setImageBitmap(bmp);
                                            bm[0] = bmp;
                                        }
                                    }
                                }
                            });

                            inImage.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(imgPreview.getLayoutParams());
                                    marginParams.setMargins(0, 0, 0, 0);
                                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(marginParams);
                                    imgPreview.setLayoutParams(layoutParams);
                                    imgPreview.setImageBitmap(bm[0]);

                                    Toast.makeText(InboxActivity.this, R.string.inbox_tap_picture_to_close, Toast.LENGTH_SHORT).show();
                                }
                            });

                        }


                        // CELL WITH MESSAGE FROM RECEIVER ---------------------------------------------
                    } else {
                        senderCell.setVisibility(View.INVISIBLE);
                        receiverCell.setVisibility(View.VISIBLE);

                        // Get username
                        TextView usernTxt = finalCell.findViewById(R.id.rUsernameTxt);
                        usernTxt.setTypeface(Configs.titSemibold);
                        usernTxt.setText(getString(R.string.username_formatted, userPointer.getString(Configs.USER_USERNAME)));

                        // Get message
                        TextView messTxt = finalCell.findViewById(R.id.rMessTxt);
                        messTxt.setTypeface(Configs.titLight);
                        messTxt.setText(iObj.getString(Configs.INBOX_MESSAGE));

                        // Get date
                        TextView dateTxt = finalCell.findViewById(R.id.rDateTxt);
                        dateTxt.setTypeface(Configs.titLight);
                        dateTxt.setText(Configs.timeAgoSinceDate(iObj.getCreatedAt()));

                        // Get avatar
                        final ImageView avatarImg = finalCell.findViewById(R.id.rAvatarImg);
                        Configs.getParseImage(avatarImg, userPointer, Configs.USER_AVATAR);


                        // THIS MESSAGE HAS AN IMAGE -------------------
                        if (iObj.get(Configs.INBOX_IMAGE) != null) {

                            final ImageView inImage = (ImageView) finalCell.findViewById(R.id.rImage);
                            inImage.setClipToOutline(true);
                            inImage.setVisibility(View.VISIBLE);
                            messTxt.setVisibility(View.INVISIBLE);

                            // Get the image
                            final Bitmap[] bm = {null};
                            ParseFile fileObject2 = (ParseFile) iObj.get(Configs.INBOX_IMAGE);
                            fileObject2.getDataInBackground(new GetDataCallback() {
                                public void done(byte[] data, ParseException error) {
                                    if (error == null) {
                                        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                        if (bmp != null) {
                                            inImage.setImageBitmap(bmp);
                                            bm[0] = bmp;
                                        }
                                    }
                                }
                            });

                            inImage.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(imgPreview.getLayoutParams());
                                    marginParams.setMargins(0, 0, 0, 0);
                                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(marginParams);
                                    imgPreview.setLayoutParams(layoutParams);
                                    imgPreview.setImageBitmap(bm[0]);

                                    Toast.makeText(InboxActivity.this, R.string.inbox_tap_picture_to_close, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }// end IF

                }
            });// end userPointer


            return cell;
        }


        @Override
        public int getCount() {
            return inboxArray.size();
        }

        @Override
        public Object getItem(int position) {
            return inboxArray.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }


    // MARK: - SEND MESSAGE ------------------------------------------------------------------------
    void sendMessage() {
        Configs.showPD(getString(R.string.inbox_sending_loading), InboxActivity.this);

        final ParseObject iObj = new ParseObject(Configs.INBOX_CLASS_NAME);
        final ParseUser currUser = ParseUser.getCurrentUser();


        // Save Message to Inbox Class
        iObj.put(Configs.INBOX_SENDER, currUser);
        iObj.put(Configs.INBOX_RECEIVER, userObj);
        iObj.put(Configs.INBOX_INBOX_ID, currUser.getObjectId() + userObj.getObjectId());
        iObj.put(Configs.INBOX_MESSAGE, messageTxt.getText().toString());
        lastMessageStr = messageTxt.getText().toString();


        // SEND AN IMAGE (if it exists) ------------------
        if (imageToSend != null) {
            // Save image
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageToSend.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            ParseFile imageFile = new ParseFile("image.jpg", byteArray);
            iObj.put(Configs.INBOX_IMAGE, imageFile);

            iObj.put(Configs.INBOX_MESSAGE, "[Picture]");
            lastMessageStr = "[Picture]";
        }


        // Saving block
        iObj.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Configs.hidePD();
                    dismisskeyboard();
                    imageToSend = null;

                    // Call save LastMessage
                    saveLastMessageInChats();

                    // Add message to the array (it's temporary, before a new query gets automatically called)
                    inboxArray.add(iObj);
                    iListView.invalidateViews();
                    iListView.refreshDrawableState();
                    scrollListViewToBottom();


                    // Send push notification
                    final String pushMessage = "@" + ParseUser.getCurrentUser().getString(Configs.USER_USERNAME) + ": '" + lastMessageStr + "'";
                    Configs.sendPushNotification(pushMessage, userObj, InboxActivity.this);

                    // error on Saving
                } else {
                    Configs.hidePD();
                    Configs.simpleAlert(e.getMessage(), InboxActivity.this);
                }
            }
        });

    }


    // MARK: - SAVE LAST MESSAGE IN CHATS CLASS -------------------------------------------------
    void saveLastMessageInChats() {
        final ParseUser currentUser = ParseUser.getCurrentUser();

        String inboxId1 = ParseUser.getCurrentUser().getObjectId() + userObj.getObjectId();
        String inboxId2 = userObj.getObjectId() + ParseUser.getCurrentUser().getObjectId();

        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.MESSAGES_CLASS_NAME);
        String[] ids = {inboxId1, inboxId2};
        query.whereContainedIn("chatID", Arrays.asList(ids));
        query.whereContainedIn("chatID", Arrays.asList(ids));

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException error) {
                if (error == null) {
                    chatsArray = objects;

                    ParseObject mObj = new ParseObject(Configs.MESSAGES_CLASS_NAME);

                    if (chatsArray.size() != 0) {
                        mObj = chatsArray.get(0);
                    }
                    Log.i("log-", "CHATS ARRAY: " + chatsArray);

                    // Update Last message
                    mObj.put(Configs.MESSAGES_LAST_MESSAGE, lastMessageStr);
                    mObj.put(Configs.MESSAGES_USER_POINTER, currentUser);
                    mObj.put(Configs.MESSAGES_OTHER_USER, userObj);
                    mObj.put(Configs.MESSAGES_ID, currentUser.getObjectId() + userObj.getObjectId());

                    // Saving block
                    mObj.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Log.i("log-", "LAST MESS SAVED: " + lastMessageStr);
                            } else {
                                Configs.simpleAlert(e.getMessage(), InboxActivity.this);
                            }
                        }
                    });


                    // error in query
                } else {
                    Configs.simpleAlert(error.getMessage(), InboxActivity.this);
                }
            }
        });

    }


    // IMAGE HANDLING METHODS ------------------------------------------------------------------------
    int CAMERA = 0;
    int GALLERY = 1;
    Uri imageURI;
    File file;


    // OPEN CAMERA
    public void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = new File(Environment.getExternalStorageDirectory(), "image.jpg");
        imageURI = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
        startActivityForResult(intent, CAMERA);
    }


    // OPEN GALLERY
    public void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), GALLERY);
    }


    // IMAGE PICKED DELEGATE -----------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Bitmap bm = null;

            // Image from Camera
            if (requestCode == CAMERA) {

                try {
                    File f = file;
                    ExifInterface exif = new ExifInterface(f.getPath());
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                    int angle = 0;
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                        angle = 90;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                        angle = 180;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        angle = 270;
                    }
                    Log.i("log-", "ORIENTATION: " + orientation);

                    Matrix mat = new Matrix();
                    mat.postRotate(angle);

                    Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(f), null, null);
                    bm = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);
                } catch (IOException | OutOfMemoryError e) {
                    Log.i("log-", e.getMessage());
                }


                // Image from Gallery
            } else if (requestCode == GALLERY) {
                try {
                    bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            // Set image to send
            imageToSend = Configs.scaleBitmapToMaxSize(400, bm);


            // Send message with image
            sendMessage();

        }
    }
    //---------------------------------------------------------------------------------------------


    // RESET imageToSend TO NULL -------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageToSend = null;
    }


    // MARK: - DISMISS KEYBOARD ---------------------------------------------------------------
    public void dismisskeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(messageTxt.getWindowToken(), 0);
        messageTxt.setText("");
    }


}//@end
