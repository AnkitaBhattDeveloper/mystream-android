package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Configs extends Application {


    // IMPORTANT: Replace the red strings below with your own App ID and Client Key of your app on back4app.com
    public static String PARSE_APP_ID = "LShV0Q5wA4FwXCjTmblIXRLuOkc52NR6aEbhzpyU";
    public static String PARSE_CLIENT_KEY = "6Usbm2XCVaBQQTR0xDuies5HBhUS7F9G3iayPAdO";
    // -----------------------------------------------------------------------------------------


    // IMPORTANT: REPLACE THE STRINGS BELOW WITH THE URL's TO YOUR FACEBOOK AND TWITTER PAGE OF YOUR APP (OR YOUR OWN ACCOUNT)
    public static String FACEBOOK_URL = "https://www.facebook.com/cubycode";
    public static String TWITTER_URL = "https://twitter.com/cubycode";


    // IMPORTANT: SET THE EXACT AMOUNT OF STICKER IMAGES YOU'VE PLACED INTO THE 'drawable' FOLDER
    public static int STICKERS_AMOUNT = 13;


    // YOU CAN CHANGE THE HEX VALUES OF THIS COLOR AS YOU WISH
    public static String MAIN_COLOR = "#8560a8";
    public static String LIGHT_BLUE = "#eef5fb";


    // DECLARE FONT FILES
    // (the font files are into app/src/main/assets/font folder)
    public static Typeface titBlack, titLight, titRegular, titSemibold;


    /***************  DO NOT EDIT THE CODE BELOW!! *****************/
    public static final String INSTALLATION_GCM_SENDER_ID_KEY = "GCMSenderId";
    public static final String INSTALLATION_USER_ID = "userID";
    public static final String INSTALLATION_USERNAME = "username";

    public static String USER_CLASS_NAME = "_User";
    public static String USER_USERNAME = "username";
    public static String USER_EMAIL = "email";
    public static String USER_FULLNAME = "fullName";
    public static String USER_AVATAR = "avatar";
    public static String USER_COVER_IMAGE = "cover";
    public static String USER_IS_REPORTED = "isReported";
    public static String USER_REPORT_MESSAGE = "reportMessage";
    public static String USER_ABOUT_ME = "aboutMe";
    public static String USER_HAS_BLOCKED = "hasBlocked";
    public static String IMAGE_FORMAT = ".jpg";


    public static String STREAMS_CLASS_NAME = "Streams";
    public static String STREAMS_USER_POINTER = "userPointer";
    public static String STREAMS_TEXT = "text";
    public static String STREAMS_VIDEO = "video";
    public static String STREAMS_IMAGE = "image";
    public static String STREAMS_AUDIO = "audio";
    public static String STREAMS_LIKED_BY = "likedBy"; // Array
    public static String STREAMS_KEYWORDS = "keywords"; // Array
    public static String STREAMS_LIKES = "likes";
    public static String STREAMS_VIEWS = "views";
    public static String STREAMS_PROFILE_CLICKS = "profileClicks";
    public static String STREAMS_SHARES = "shares";
    public static String STREAMS_COMMENTS = "comments";
    public static String STREAMS_REPORTED_BY = "reportedBy"; // Array
    public static String STREAMS_CREATED_AT = "createdAt";

    public static String COMMENTS_CLASS_NAME = "Comments";
    public static String COMMENTS_USER_POINTER = "userPointer";
    public static String COMMENTS_STREAM_POINTER = "streamPointer";
    public static String COMMENTS_COMMENT = "comment";
    public static String COMMENTS_REPORTED_BY = "reportedBy"; // Array
    public static String COMMENTS_CREATED_AT = "createdAt";

    public static String FOLLOW_CLASS_NAME = "Follow";
    public static String FOLLOW_CURR_USER = "currUser";
    public static String FOLLOW_IS_FOLLOWING = "isFollowing";
    public static String FOLLOW_CREATED_AT = "createdAt";

    public static String ACTIVITY_CLASS_NAME = "Activity";
    public static String ACTIVITY_CURRENT_USER = "currentUser";
    public static String ACTIVITY_OTHER_USER = "otherUser";
    public static String ACTIVITY_STREAM_POINTER = "streamPointer";
    public static String ACTIVITY_TEXT = "text";
    public static String ACTIVITY_CREATED_AT = "createdAt";

    public static String MESSAGES_CLASS_NAME = "Messages";
    public static String MESSAGES_LAST_MESSAGE = "lastMessage";
    public static String MESSAGES_USER_POINTER = "userPointer";
    public static String MESSAGES_OTHER_USER = "otherUser";
    public static String MESSAGES_ID = "chatID";
    public static String MESSAGES_CREATED_AT = "createdAt";

    public static String INBOX_CLASS_NAME = "Inbox";
    public static String INBOX_SENDER = "sender";
    public static String INBOX_RECEIVER = "receiver";
    public static String INBOX_INBOX_ID = "inboxID";
    public static String INBOX_MESSAGE = "message";
    public static String INBOX_IMAGE = "image";
    public static String INBOX_CREATED_AT = "createdAt";


    public static String selectedStickerImage = "";
    public static boolean mustRefresh = false;
    private static Configs instance;

    public static Configs getInstance() {
        return instance;
    }



    boolean isParseInitialized = false;

    public void onCreate() {
        super.onCreate();

        instance = this;

        if (!isParseInitialized) {
            Parse.initialize(new Parse.Configuration.Builder(this)
                    .applicationId(String.valueOf(PARSE_APP_ID))
                    .clientKey(String.valueOf(PARSE_CLIENT_KEY))
                    .server("https://parseapi.back4app.com")
                    .build()
            );
            Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);
            ParseUser.enableAutomaticUser();
            isParseInitialized = true;


            // Init Facebook Utils
            ParseFacebookUtils.initialize(this);
        }

        // Init fonts
        titBlack = Typeface.createFromAsset(getAssets(), "font/Titillium-Black.otf");
        titLight = Typeface.createFromAsset(getAssets(), "font/Titillium-Light.otf");
        titRegular = Typeface.createFromAsset(getAssets(), "font/Titillium-Regular.otf");
        titSemibold = Typeface.createFromAsset(getAssets(), "font/Titillium-Semibold.otf");


    }// end onCreate()


    // MARK: - SHORTCUT TO GET PARSE IMAGE
    public static void getParseImage(final ImageView imgView, ParseObject parseObj, String className) {
        // Get Image
        ParseFile fileObject = parseObj.getParseFile(className);
        if (fileObject != null) {
            fileObject.getDataInBackground(new GetDataCallback() {
                public void done(byte[] data, ParseException error) {
                    if (error == null && data!=null) {
                        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                        if (bmp != null) {
                            imgView.setImageBitmap(bmp);
                        }
                    }
                }
            });
        }
    }


    // MARK: - SHORTCUT TO SAVE A PARSE IMAGE
    public static void saveParseImage(ImageView imgView, ParseObject parseObj, String className) {
        // Save image
        Bitmap bitmap = ((BitmapDrawable) imgView.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        ParseFile imageFile = new ParseFile("image.jpg", byteArray);
        parseObj.put(className, imageFile);
    }


    // SHORTCUT TO SAVE ACTIVITY
    public static void saveActivity(final ParseObject currUser, final ParseObject streamObj, final String text) {
        // Get User Pointer
        streamObj.getParseObject(Configs.STREAMS_USER_POINTER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject userPointer, ParseException e) {
                ParseObject aObj = new ParseObject(Configs.ACTIVITY_CLASS_NAME);
                aObj.put(Configs.ACTIVITY_CURRENT_USER, userPointer);
                aObj.put(Configs.ACTIVITY_OTHER_USER, currUser);
                aObj.put(Configs.ACTIVITY_STREAM_POINTER, streamObj);
                aObj.put(Configs.ACTIVITY_TEXT, text);
                aObj.saveInBackground();
            }
        });// end userPointer
    }


    // MARK: - SHORTCUT TO SEND PUSH NOTIFICATIONS
    public static void sendPushNotification(final String pushMessage, final ParseUser userObj, final Context ctx) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("someKey", userObj.getObjectId());
        params.put("data", pushMessage);
        ParseCloud.callFunctionInBackground("pushAndroid", params, new FunctionCallback<String>() {
            @Override
            public void done(String object, ParseException e) {
                if (e == null) {
                    Log.i("log-", "PUSH SENT TO: " + userObj.getString(Configs.USER_USERNAME)
                            + "\nMESSAGE: "
                            + pushMessage);

                    // error
                } else {
                    simpleAlert(e.getMessage(), ctx);
                }
            }
        });
    }


    // MARK: - SCALE BITMAP TO MAX SIZE ---------------------------------------
    public static Bitmap scaleBitmapToMaxSize(int maxSize, Bitmap bm) {
        int outWidth;
        int outHeight;
        int inWidth = bm.getWidth();
        int inHeight = bm.getHeight();
        if (inWidth > inHeight) {
            outWidth = maxSize;
            outHeight = (inHeight * maxSize) / inWidth;
        } else {
            outHeight = maxSize;
            outWidth = (inWidth * maxSize) / inHeight;
        }
        return Bitmap.createScaledBitmap(bm, outWidth, outHeight, false);
    }


    // MARK: - GET TIME AGO SINCE DATE ------------------------------------------------------------
    public static String timeAgoSinceDate(Date date) {
        String dateString = "";
        try {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss");
            String sentStr = (df.format(date));
            Date past = df.parse(sentStr);
            Date now = new Date();
            long seconds = TimeUnit.MILLISECONDS.toSeconds(now.getTime() - past.getTime());
            long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - past.getTime());
            long hours = TimeUnit.MILLISECONDS.toHours(now.getTime() - past.getTime());
            long days = TimeUnit.MILLISECONDS.toDays(now.getTime() - past.getTime());

            if (seconds < 60) {
                dateString = seconds + " seconds ago";
            } else if (minutes < 60) {
                dateString = minutes + " minutes ago";
            } else if (hours < 24) {
                dateString = hours + " hours ago";
            } else {
                dateString = days + " days ago";
            }
        } catch (Exception j) {
            j.printStackTrace();
        }
        return dateString;
    }


    // ROUND THOUSANDS NUMBERS ------------------------------------------
    public static String roundThousandsIntoK(Number number) {
        char[] suffix = {' ', 'k', 'M', 'B', 'T', 'P', 'E'};
        long numValue = number.longValue();
        int value = (int) Math.floor(Math.log10(numValue));
        int base = value / 3;
        if (value >= 3 && base < suffix.length) {
            return new DecimalFormat("#0.0").format(numValue / Math.pow(10, base * 3)) + suffix[base];
        } else {
            return new DecimalFormat("#,##0").format(numValue);
        }
    }


    // MARK: - CUSTOM PROGRESS DIALOG -----------
    public static AlertDialog pd;

    public static void showPD(String mess, Context ctx) {
        AlertDialog.Builder db = new AlertDialog.Builder(ctx);
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View dialogView = inflater.inflate(R.layout.pd, null);
        TextView messTxt = dialogView.findViewById(R.id.pdMessTxt);
        messTxt.setText(mess);
        db.setView(dialogView);
        db.setCancelable(false);
        pd = db.create();
        pd.show();
    }

    public static void hidePD() {
        pd.dismiss();
    }

    public static AlertDialog showLoadingDialog(String mess, Context ctx) {
        AlertDialog.Builder db = new AlertDialog.Builder(ctx);
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View dialogView = inflater.inflate(R.layout.pd, null);
        TextView messTxt = dialogView.findViewById(R.id.pdMessTxt);
        messTxt.setText(mess);
        db.setView(dialogView);
        db.setCancelable(false);
        AlertDialog pd = db.create();
        pd.show();
        return pd;
    }

    // MARK: - SIMPLE ALERT ------------------------------
    public static void simpleAlert(String mess, Context activity) {
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setMessage(mess)
                .setTitle(R.string.app_name)
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.logo);
        alert.create().show();
    }


    // Method to get URI of a stored image
    public static Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "image", null);
        return Uri.parse(path);
    }


} // @end
