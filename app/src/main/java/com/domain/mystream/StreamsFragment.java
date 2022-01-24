package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class StreamsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    /* Views */

    private ImageView currUserAvatarImg;
    private SwipeRefreshLayout refreshControl;


    /* Variables */
    private List<ParseObject> streamsArray;
    private MarshMallowPermission mmp;
    private RecyclerView streamRV;
    private AlertDialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_streams, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mmp = new MarshMallowPermission(getActivity());

        setUpViews();

        ParseUser currUser = ParseUser.getCurrentUser();

        // Get user's avatar
        Configs.getParseImage(currUserAvatarImg, currUser, Configs.USER_AVATAR);

        // Call query
        if (ParseUser.getCurrentUser().getObjectId() != null) {
            queryStreams();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (!isVisibleToUser) {
            return;
        }

        if (getActivity() == null) {
            return;
        }

        // Recall query in case something has been reported (either a User or a Stream)
        if (Configs.mustRefresh) {
            queryStreams();
            Configs.mustRefresh = false;
        }

        // Request Storage permission
        if (!mmp.checkPermissionForReadExternalStorage()) {
            mmp.requestPermissionForReadExternalStorage();
        }

        setUpInterstitialAd();
    }

    private void setUpViews() {
        // Init views
        currUserAvatarImg = getActivity().findViewById(R.id.hcurrUserAvatarImg);
        streamRV = getActivity().findViewById(R.id.stream_rv);

        // Init recyclerView
        if (streamRV != null) {
             streamRV.setLayoutManager(new LinearLayoutManager(getActivity()));
             streamRV.setItemAnimator(new DefaultItemAnimator());

        }

        // Init a refreshControl
        refreshControl = getActivity().findViewById(R.id.streams_swiperefresh);
        refreshControl.setOnRefreshListener(this);

        // MARK: - ADD STREAM BUTTON ------------------------------------
        Button addStreamButt = getActivity().findViewById(R.id.hAddStreamButt);
        addStreamButt.setTypeface(Configs.titRegular);
        addStreamButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AddStream.class));
            }
        });

        // MARK: - ADD PHOTO BUTTON ------------------------------------
        Button addPhotoButt = getActivity().findViewById(R.id.hAddPhotoButt);
        addPhotoButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mmp.checkPermissionForReadExternalStorage()) {
                    mmp.requestPermissionForReadExternalStorage();
                } else {
                    Intent i = new Intent(getActivity(), AddStream.class);
                    Bundle extras = new Bundle();
                    extras.putString("streamAttachment", "image");
                    i.putExtras(extras);
                    startActivity(i);
                }
            }
        });

        // MARK: - ADD VIDEO BUTTON ------------------------------------
        Button addVideoButt = getActivity().findViewById(R.id.hAddVideoButt);
        addVideoButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mmp.checkPermissionForCamera()) {
                    mmp.requestPermissionForCamera();
                } else {
                    Intent i = new Intent(getActivity(), AddStream.class);
                    Bundle extras = new Bundle();
                    extras.putString("streamAttachment", "video");
                    i.putExtras(extras);
                    startActivity(i);
                }
            }
        });

        // MARK: - ADD AUDIO BUTTON ------------------------------------
        Button addAudioButt = getActivity().findViewById(R.id.hAddAudioButt);
        addAudioButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mmp.checkPermissionForRecord()) {
                    mmp.requestPermissionForRecord();
                } else {
                    Intent i = new Intent(getActivity(), AddStream.class);
                    Bundle extras = new Bundle();
                    extras.putString("streamAttachment", "audio");
                    i.putExtras(extras);
                    startActivity(i);
                }
            }
        });
    }

    private void setUpInterstitialAd() {
        // INTERSTITIAL AD IMPLEMENTATION ------------------------------------
        final InterstitialAd interstitialAd = new InterstitialAd(getActivity());
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
    }

    // MARK: - QUERY STREAMS -------------------------------------------------
    private void queryStreams() {
        loadingDialog = Configs.showLoadingDialog(getString(R.string.loading_dialog_please_wait), getActivity());
        ParseUser currUser = ParseUser.getCurrentUser();
        List<String> currUserID = new ArrayList<>();
        currUserID.add(currUser.getObjectId());

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.STREAMS_CLASS_NAME);
        query.whereNotContainedIn(Configs.STREAMS_REPORTED_BY, currUserID);
        query.orderByDescending(Configs.STREAMS_CREATED_AT);

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    streamsArray = objects;
                    hideLoadingDialog();

                    reloadData();

                    // error in query
                } else {
                    hideLoadingDialog();
                    Configs.simpleAlert(e.getMessage(), getActivity());
                }
            }
        });
    }

    // MARK: - RELOAD LISTVIEW DATA --------------------------------------------------------
    void reloadData() {
        streamRV.setAdapter(new StreamAdapter(getActivity(),streamsArray,mmp));
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

    private void hideLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }
}
