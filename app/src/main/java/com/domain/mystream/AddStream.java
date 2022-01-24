package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.domain.mystream.utils.RealPathUtil;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class AddStream extends AppCompatActivity {

    /* Views */
    ImageView avatarImg, streamImg;
    EditText streamTxt;
    TextView fullnameTxt, recordDurationTxt;
    Button removeImageButt, playButt;
    RelativeLayout keyboardToolbar, recordAudioLayout;
    WebView recordingWebView;


    /* Variables */
    String streamAttachment;
    MarshMallowPermission mmp = new MarshMallowPermission(this);
    boolean audioIsPlaying = false;
    String audioURL = null;
    MediaRecorder mediaRecorder ;
    MediaPlayer mediaPlayer;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_stream);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Change StatusBar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.lightBlueBg));



        // Init views
        TextView titleTxt = findViewById(R.id.asTitleTxt);
        titleTxt.setTypeface(Configs.titSemibold);
        avatarImg = findViewById(R.id.asAvatarimg);
        streamImg = findViewById(R.id.asStreamImg);
        streamTxt = findViewById(R.id.asStreamTxt);
        streamTxt.setTypeface(Configs.titRegular);
        fullnameTxt = findViewById(R.id.asFullnameTxt);
        fullnameTxt.setTypeface(Configs.titSemibold);
        removeImageButt = findViewById(R.id.asRemoveImageButt);
        keyboardToolbar = findViewById(R.id.asKeyboardToolbar);
        recordAudioLayout = findViewById(R.id.asRecordAudioLayout);
        hideRecordAudioLayout();
        playButt = findViewById(R.id.asPlayButt);
        playButt.setVisibility(View.INVISIBLE);

        recordingWebView = findViewById(R.id.asRecordingWebView);
        recordingWebView.loadUrl("file:///android_asset/recording.gif");
        recordingWebView.getSettings().setLoadWithOverviewMode(true);
        recordingWebView.getSettings().setUseWideViewPort(true);

        recordDurationTxt = findViewById(R.id.asRecordDurationTxt);
        recordDurationTxt.setTypeface(Configs.titRegular);



        // Get user's details
        ParseUser currUser = ParseUser.getCurrentUser();
        Configs.getParseImage(avatarImg, currUser, Configs.USER_AVATAR);
        fullnameTxt.setText(currUser.getString(Configs.USER_FULLNAME));




        // Get streamAttachment variable
        streamAttachment = "";
        Bundle extras = getIntent().getExtras();
        if (extras != null) { streamAttachment = extras.getString("streamAttachment"); }
        if (streamAttachment != null) {
            switch (streamAttachment) {
                case "image":
                    addImage();
                    break;
                case "video":
                    addVideo();
                    break;
                case "audio":
                    addAudio();
                    break;
                default:break; }
        }





        // MARK: - REMOVE STREAM IMAGE BUTTON ------------------------------------
        removeImageButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reset views and variables
                streamAttachment = null;
                videoURI = null;
                audioURL = null;
                streamImg.setImageDrawable(null);
                playButt.setVisibility(View.INVISIBLE);
                removeImageButt.setVisibility(View.INVISIBLE);
            }});




        // MARK: - CANCEL (CLOSE ACTIVITY) ------------------------------------
        Button cancelButt = findViewById(R.id.asCancelButt);
        cancelButt.setTypeface(Configs.titSemibold);
        cancelButt.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              if (mediaPlayer != null) {
                  mediaPlayer.stop();
                  mediaPlayer.reset();
                  mediaPlayer.release();
                  mediaPlayer = null;
              }
              finish();
          }});





        // MARK: - ADD STREAM BUTTON (ON KEYBOARD TOOLBAR) -------------------------------
        Button addStreamButt = findViewById(R.id.asAddStreamButt);
        addStreamButt.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              dismissKeyboard();
              showAddStreamPanel();
        }});




        // MARK: - STOP RECORDING AUDIO BUTTON ------------------------------------
        Button stopRecordingButt = findViewById(R.id.asStopRecordingButt);
        stopRecordingButt.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              handler.removeCallbacksAndMessages(null);

              mediaRecorder.stop();
              Log.i("log-", "AUDIO URL ON STOP: " + audioURL);

              streamImg.setImageResource(R.drawable.audio_image);
              playButt.setVisibility(View.VISIBLE);
              streamAttachment = "audio";
              videoURI = null;
              removeImageButt.setVisibility(View.VISIBLE);

              hideRecordAudioLayout();
        }});






        // MARK: - PLAY VIDEO OR AUDIO BUTTON -----------------------------------------------
        playButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // PLAY VIDEO PREVIEW -------------
                if (streamAttachment.matches("video") ){

                     Intent i = new Intent(AddStream.this, ShowVideo.class);
                     Bundle extras = new Bundle();
                     String videoURL = videoURI.toString();
                     extras.putString("videoURL", videoURL);
                     i.putExtras(extras);
                     startActivity(i);



                // PLAY AUDIO PREVIEW ----------------
                } else if (streamAttachment.matches("audio")) {

                    // Init mediaPlayer
                    mediaPlayer = new MediaPlayer();

                    // Start Audio playing
                    if (!audioIsPlaying) {
                        try {
                            mediaPlayer.setDataSource(audioURL);
                            mediaPlayer.prepare();
                        } catch (IOException e) { e.printStackTrace(); }
                        mediaPlayer.start();

                        audioIsPlaying = true;
                        playButt.setBackgroundResource(R.drawable.stop_butt);

                        // Check when audio finished
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                handler.removeCallbacksAndMessages(null);
                                audioIsPlaying = false;
                                playButt.setBackgroundResource(R.drawable.play_butt);
                        }});


                    // Stop Audio playing
                    } else {
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        mediaPlayer.release();
                        mediaPlayer = null;
                        audioIsPlaying = false;
                        playButt.setBackgroundResource(R.drawable.play_butt);
                    }

                }
        }});







        // MARK: - POST STREAM BUTTON ------------------------------------
        Button postButt = findViewById(R.id.asPostButt);
        postButt.setTypeface(Configs.titSemibold);
        postButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("log-", "VIDEO URI: " + videoURI);
                Log.i("log-", "AUDIO URL: " + audioURL);
                Log.i("log-", "STREAM ATTACHMENT: " + streamAttachment);

                ParseObject sObj = new ParseObject(Configs.STREAMS_CLASS_NAME);
                ParseUser currentUser = ParseUser.getCurrentUser();

                if (streamTxt.getText().toString().matches("") ){
                    Configs.simpleAlert("You must type something!", AddStream.this);

                } else {
                    Configs.showPD("Please wait...", AddStream.this);
                    dismissKeyboard();

                    // Prepare data
                    sObj.put(Configs.STREAMS_USER_POINTER, currentUser);
                    sObj.put(Configs.STREAMS_TEXT, streamTxt.getText().toString());
                    sObj.put(Configs.STREAMS_LIKES, 0);
                    sObj.put(Configs.STREAMS_COMMENTS, 0);
                    sObj.put(Configs.STREAMS_VIEWS, 0);
                    sObj.put(Configs.STREAMS_PROFILE_CLICKS, 0);
                    sObj.put(Configs.STREAMS_SHARES, 0);

                    List<String> reportedBy = new ArrayList<>();
                    sObj.put(Configs.STREAMS_REPORTED_BY, reportedBy);
                    List<String> likedBy = new ArrayList<>();
                    sObj.put(Configs.STREAMS_LIKED_BY, likedBy);

                    // Prepare keywords
                    List<String> keywords = new ArrayList<>();
                    String[] k1 = streamTxt.getText().toString().toLowerCase().split(" ");
                    String[] k2 = currentUser.getString(Configs.USER_USERNAME).toLowerCase().split(" ");
                    String[] k3 = currentUser.getString(Configs.USER_FULLNAME).toLowerCase().split(" ");
                    Collections.addAll(keywords, k1);
                    Collections.addAll(keywords, k2);
                    Collections.addAll(keywords, k3);
                    sObj.put(Configs.STREAMS_KEYWORDS, keywords);


                    // Prepare Image
                    if (streamImg.getDrawable() != null) {
                        Configs.saveParseImage(streamImg, sObj, Configs.STREAMS_IMAGE);
                    }


                    if (streamAttachment != null) {
                        // Prepare Video
                        if (streamAttachment.matches("video")) {
                            ParseFile videoFile = new ParseFile("video.mp4", convertVideoToBytes(videoURI));
                            sObj.put(Configs.STREAMS_VIDEO, videoFile);

                            // Save video thumbnail
                            Configs.saveParseImage(streamImg, sObj, Configs.STREAMS_IMAGE);

                            // Prepare Audio
                        } else if (streamAttachment.matches("audio")) {
                            byte[] soundBytes;
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(Uri.fromFile(new File(audioURL)));
                                assert inputStream != null;
                                soundBytes = new byte[inputStream.available()];
                                soundBytes = toByteArray(inputStream);

                                ParseFile audioFile = new ParseFile("message.mp4", soundBytes);
                                sObj.put(Configs.STREAMS_AUDIO, audioFile);

                            } catch (Exception e) { e.printStackTrace(); }
                        }

                    }

                    // Saving block
                    sObj.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Configs.hidePD();
                                AlertDialog.Builder alert = new AlertDialog.Builder(AddStream.this);
                                alert.setMessage("Your Stream has been posted!")
                                        .setTitle(R.string.app_name)
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                // Dismiss Activity
                                                Configs.mustRefresh = true;
                                                finish();
                                            }})
                                        .setCancelable(false)
                                        .setIcon(R.drawable.logo);
                                alert.create().show();

                            } else {
                                Configs.hidePD();
                                Configs.simpleAlert(e.getMessage(), AddStream.this);
                            }}});


                }// end IF
        }});



    }// end onCreate()








    // MARK: - ADD IMAGE -------------------------------------
    void addImage() {
        dismissKeyboard();
        if (addStreamPanel != null) { hideAddStreamPanel(); }

        AlertDialog.Builder alert  = new AlertDialog.Builder(AddStream.this);
        alert.setTitle("SELECT SOURCE")
                .setIcon(R.drawable.logo)
                .setItems(new CharSequence[] {
                                "Take a picture",
                                "Pick from Gallery"
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {

                            // OPEN CAMERA
                            case 0:
                                if (!mmp.checkPermissionForCamera()) {
                                    mmp.requestPermissionForCamera();
                                } else { openCamera(); }
                                break;

                            // OPEN GALLERY
                            case 1:
                                if (!mmp.checkPermissionForReadExternalStorage()) {
                                    mmp.requestPermissionForReadExternalStorage();
                                } else { openGallery(); }
                                break;

                        }}})
                .setNegativeButton("Cancel", null);
        alert.create().show();
    }







    // MARK: - ADD VIDEO -------------------------------------
    void addVideo() {
        dismissKeyboard();
        if (addStreamPanel != null) { hideAddStreamPanel(); }

        AlertDialog.Builder alert  = new AlertDialog.Builder(AddStream.this);
        alert.setTitle("Select source")
                .setIcon(R.drawable.logo)
                .setItems(new CharSequence[] {
                                "Take a video",
                                "Pick from Gallery"
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {

                            // OPEN VIDEO CAMERA
                            case 0:
                                if (!mmp.checkPermissionForCamera()) {
                                    mmp.requestPermissionForCamera();
                                } else { openVideoCamera(); }
                                break;

                            // OPEN VIDEO GALLERY
                            case 1:
                                if (!mmp.checkPermissionForReadExternalStorage()) {
                                    mmp.requestPermissionForReadExternalStorage();
                                } else { openVideoGallery(); }
                                break;

                }}})
                .setNegativeButton("Cancel", null);
        alert.create().show();
    }






    // IMAGE/VIDEO HANDLING METHODS ------------------------------------------------------------------------
    int CAMERA = 0;
    int GALLERY = 1;
    int VIDEO_CAMERA = 2;
    int VIDEO_GALLERY = 3;
    String videoPath = null;
    Uri videoURI = null;
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
        startActivityForResult(Intent.createChooser(intent, "Select Image"), GALLERY);
    }


    // OPEN VIDEO CAMERA
    public void openVideoCamera() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        startActivityForResult(intent, VIDEO_CAMERA);
    }

    // OPEN VIDEO GALLERY
    public void openVideoGallery() {
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), VIDEO_GALLERY);
    }





    // IMAGE/VIDEO PICKED DELEGATE ------------------------------
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
                } catch (IOException | OutOfMemoryError e) { Log.i("log-", e.getMessage()); }



            // Image from Gallery
            } else if (requestCode == GALLERY) {
                try { bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
                } catch (IOException e) { e.printStackTrace(); }




            // Video from Camera or Gallery
            } else if (requestCode == VIDEO_CAMERA || requestCode == VIDEO_GALLERY) {
                videoURI = data.getData();
                videoPath = getRealPathFromURI(videoURI);
                Log.i("log-", "VIDEO PATH: " + videoPath);
                Log.i("log-", "VIDEO URI: " + videoURI);

                if (videoPath == null) {
                    Configs.simpleAlert("The video file could not be retrieved", this);
                    return;
                }

                // Check video duration
                MediaPlayer mp = MediaPlayer.create(this, videoURI);
                int videoDuration = mp.getDuration();
                mp.release();
                Log.i("log-", "VIDEO DURATION: " + videoDuration);

                // Set video thumbnail
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND);
                streamImg.setImageBitmap(Configs.scaleBitmapToMaxSize(600, thumbnail));

                removeImageButt.setVisibility(View.VISIBLE);
                streamAttachment = "video";
                audioURL = null;
                playButt.setVisibility(View.VISIBLE);
            }



            // Set Image
            if (bm != null) {
                Bitmap scaledBm = Configs.scaleBitmapToMaxSize(1080, bm);
                streamImg.setImageBitmap(scaledBm);

                removeImageButt.setVisibility(View.VISIBLE);

                // Reset variables
                videoURI = null;
                audioURL = null;
                streamAttachment = "image";
                playButt.setVisibility(View.INVISIBLE);
            }
        }
    }
    //---------------------------------------------------------------------------------------------






    // GET VIDEO PATH AS A STRING -------------------------------------
    public String getRealPathFromURI(Uri contentUri) {
        return RealPathUtil.getRealPathFromUri(this, contentUri);
    }



    // CONVERT VIDEO TO BYTES -----------------------------------
    private byte[] convertVideoToBytes(Uri uri){
        byte[] videoBytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(new File(getRealPathFromURI(uri)));

            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = fis.read(buf)))
                baos.write(buf, 0, n);

            videoBytes = baos.toByteArray();
        } catch (IOException e) { e.printStackTrace(); }
        return videoBytes;
    }










    // MARK: - ADD AUDIO -------------------------------------
    void addAudio() {
        dismissKeyboard();
        if (addStreamPanel != null) { hideAddStreamPanel(); }

        if (!mmp.checkPermissionForRecord()) {
            mmp.requestPermissionForRecord();

        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(AddStream.this);
            alert.setMessage("Select option")
                    .setTitle(R.string.app_name)
                    .setPositiveButton("Record Audio", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (!mmp.checkPermissionForRecord()) {
                                mmp.requestPermissionForRecord();
                            } else {
                                showRecordAudioLayoutAndRecord();
                            }
                        }
                    })

                    .setNegativeButton("Cancel", null)
                    .setIcon(R.drawable.logo);
            alert.create().show();
        }

    }





    // CONVERT AUDIO FILE TO BYTE ARRAY ----------------------------------------
    public byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read = 0;
        byte[] buffer = new byte[1024];
        while (read != -1) {
            read = in.read(buffer);
            if (read != -1)
                out.write(buffer,0,read);
        }
        out.close();
        return out.toByteArray();
    }





    // HIDE RECORD AUDIO LAYOUT ----------------------
    void hideRecordAudioLayout() {
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(recordAudioLayout.getLayoutParams());
        marginParams.setMargins(0, 5000, 0, 0);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(marginParams);
        recordAudioLayout.setLayoutParams(layoutParams);
    }







    // SHOW RECORD AUDIO LAYOUT AND START RECORDING AUDIO -----------------
    Handler handler = new Handler();
    void showRecordAudioLayoutAndRecord() {
        dismissKeyboard();

        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(recordAudioLayout.getLayoutParams());
        marginParams.setMargins(0, 0, 0, 0);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(marginParams);
        recordAudioLayout.setLayoutParams(layoutParams);

        audioURL = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "_audio.mp4";

        // MediaRecorder settings
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(16000);
        mediaRecorder.setAudioChannels(1);
        mediaRecorder.setOutputFile(audioURL);


        try {
            // Prepare and start mediaRecorder
            mediaRecorder.prepare();
            mediaRecorder.start();


            // SET RECORDING TIMER
            recordDurationTxt.setText(R.string.stream_details_default_duration);

            handler.postDelayed(new Runnable()  {
                long time = 0;
                @Override
                public void run() {
                    time += 1000;
                    Log.d("TimerExample", "Going for... " + time);

                    @SuppressLint("DefaultLocale")
                    String formattedTimer = String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(time),
                            TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
                    );
                    recordDurationTxt.setText(formattedTimer);


                    handler.postDelayed(this, 1000);
            }}, 1000); // 1 second delay


        } catch (IllegalStateException | IOException e) { e.printStackTrace(); }

    }







    // MARK: - ADD STICKER -------------------------------------
    void addSticker() {
        dismissKeyboard();
        if (addStreamPanel != null) { hideAddStreamPanel(); }

        showStickersPanel();
    }





    // MARK: - STICKERS LAYOUT --------------------------------------
    AlertDialog stickersPanel;
    void showStickersPanel() {
        AlertDialog.Builder db = new AlertDialog.Builder(AddStream.this);
        LayoutInflater inflater = (LayoutInflater) AddStream.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View dialogView = inflater.inflate(R.layout.stickers_panel, null);

        final List<String>stickersArr = new ArrayList<>();
        for (int i = 0; i< Configs.STICKERS_AMOUNT; i++) {
            stickersArr.add("s" + i);
        }
        Log.i("log-", "STICKERS ARR: " + stickersArr);


        // CUSTOM GRID ADAPTER
        class GridAdapter extends BaseAdapter {
            private Context context;
            public GridAdapter(Context context) {
                super();
                this.context = context;
            }


            // CONFIGURE CELL
            @Override
            public View getView(int position, View cell, ViewGroup parent) {
                if (cell == null) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                    cell = inflater.inflate(R.layout.cell_sticker, null);
                }

                // Get Image
                ImageView sImg = cell.findViewById(R.id.cellsStickerImg);
                String sName = stickersArr.get(position);
                int resID = getResources().getIdentifier(sName, "drawable", getPackageName());
                sImg.setImageResource(resID);

            return cell;
            }

            @Override public int getCount() { return Configs.STICKERS_AMOUNT; }
            @Override public Object getItem(int position) { return stickersArr.get(position); }
            @Override public long getItemId(int position) { return position; }
        }


        // Init GridView and set its adapter
        GridView aGrid = dialogView.findViewById(R.id.spStickersGridView);
        aGrid.setAdapter(new GridAdapter(AddStream.this));

        // Set number of Columns accordingly to the device used
        float scalefactor = getResources().getDisplayMetrics().density * 100; // 100 is the cell's width
        int number = getWindowManager().getDefaultDisplay().getWidth();
        int columns = (int) ((float) number / (float) scalefactor);
        aGrid.setNumColumns(columns);

        aGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                // Set selected sticker as stream Image
                String sName = stickersArr.get(position);
                int resID = getResources().getIdentifier(sName, "drawable", getPackageName());
                streamImg.setImageResource(resID);
                hideStickersPanel();
                removeImageButt.setVisibility(View.VISIBLE);
        }});



        // MARK: - DISMISS STICKERS PANEL BUTTON ------------------------------------
        Button dismissButt = dialogView.findViewById(R.id.spCancelButt);
        dismissButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideStickersPanel();
                showKeyboard();
        }});


        db.setView(dialogView);
        db.setCancelable(false);
        stickersPanel = db.create();
        stickersPanel.show();
    }

    void hideStickersPanel(){ stickersPanel.dismiss(); }








    // MARK: - ADD STREAM PANEL ----------------------------
    AlertDialog addStreamPanel;
    void showAddStreamPanel() {
        AlertDialog.Builder db = new AlertDialog.Builder(AddStream.this);
        LayoutInflater inflater = (LayoutInflater) AddStream.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View dialogView = inflater.inflate(R.layout.addstream_panel, null);


        // PANEL BUTTONS -------------------------------------
        Button photoButt = dialogView.findViewById(R.id.asAddPhotoButt);
        photoButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { addImage(); }});

        Button videoButt = dialogView.findViewById(R.id.asAddVideoButt);
        videoButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { addVideo(); }});

        Button audioButt = dialogView.findViewById(R.id.asAddAudioButt);
        audioButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { addAudio(); }});

        Button stickerButt = dialogView.findViewById(R.id.asAddStickerButt);
        stickerButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { addSticker(); }});

        // MARK: - DISMISS ADD STREAM PANEL BUTTON ------------------------------------
        Button dismissButt = dialogView.findViewById(R.id.asDismissAddStreamLayoutButt);
        dismissButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideAddStreamPanel();
                showKeyboard();
        }});


        db.setView(dialogView);
        db.setCancelable(false);
        addStreamPanel = db.create();
        addStreamPanel.show();
    }

    void hideAddStreamPanel(){ addStreamPanel.dismiss(); }





    // MARK: - DISMISS KEYBOARD ------------------------------------------
    void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(streamTxt.getWindowToken(), 0);
        }
    }


    // MARK: - SHOW KEYBOARD ------------------------------------------
    void showKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(streamTxt, 0);
        }
    }



}// @end
