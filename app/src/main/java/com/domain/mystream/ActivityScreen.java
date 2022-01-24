package com.domain.mystream;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
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

import java.util.List;

public class ActivityScreen extends AppCompatActivity {

    /* Views */
    ListView activityListView;


    /* Variables */
    List<ParseObject> activityArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Change StatusBar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.main_color));

        // Init views
        activityListView = findViewById(R.id.actActivityListView);

        // Call query
        queryActivity();

        // MARK: - BACK BUTTON ------------------------------------
        Button backButt = findViewById(R.id.actBackButt);
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

    // MARK: - QUERY ACTIVITY ------------------------------------------------------------
    void queryActivity() {
        ParseUser currUser = ParseUser.getCurrentUser();
        Configs.showPD(getString(R.string.loading_dialog_please_wait), ActivityScreen.this);

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.ACTIVITY_CLASS_NAME);
        query.whereEqualTo(Configs.ACTIVITY_CURRENT_USER, currUser);
        query.orderByDescending(Configs.ACTIVITY_CREATED_AT);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    Configs.hidePD();
                    activityArray = objects;
                    reloadData();

                    // error
                } else {
                    Configs.hidePD();
                    Configs.simpleAlert(e.getMessage(), ActivityScreen.this);
                }
            }
        });
    }

    // MARK: - RELOAD LISTVIEW DATA -------------------------------------------------
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
                    cell = inflater.inflate(R.layout.cell_activity, null);
                }

                // Init views
                final ImageView avatarImg = cell.findViewById(R.id.cactAvatarImg);
                final TextView actTxt = cell.findViewById(R.id.cactActivityTxt);
                actTxt.setTypeface(Configs.titRegular);

                // Get Parse obj
                final ParseObject aObj = activityArray.get(position);

                // Get userPointer
                aObj.getParseObject(Configs.ACTIVITY_OTHER_USER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                    public void done(ParseObject userPointer, ParseException e) {

                        // Get Avatar
                        Configs.getParseImage(avatarImg, userPointer, Configs.USER_AVATAR);

                        // Get activity text
                        actTxt.setText(aObj.getString(Configs.ACTIVITY_TEXT));

                    }
                });// end userPointer


                return cell;
            }

            @Override
            public int getCount() {
                return activityArray.size();
            }

            @Override
            public Object getItem(int position) {
                return activityArray.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }
        }

        // Init ListView and set its adapter
        activityListView.setAdapter(new ListAdapter(ActivityScreen.this));
        activityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                // Get Parse obj
                final ParseObject aObj = activityArray.get(position);

                // Get streamPointer
                aObj.getParseObject(Configs.ACTIVITY_STREAM_POINTER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                    public void done(ParseObject streamPointer, ParseException e) {

                        Intent i = new Intent(ActivityScreen.this, StreamDetails.class);
                        Bundle extras = new Bundle();
                        extras.putString("objectID", streamPointer.getObjectId());
                        i.putExtras(extras);
                        startActivity(i);

                    }
                });// end streamPointer

            }
        });
    }
}//@end
