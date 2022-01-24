package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    /* Views */
    private ListView streamsListView;
    private EditText searchTxt;
    private SwipeRefreshLayout refreshControl;


    /* Variables */
    private List<ParseObject> streamsArray;
    private MarshMallowPermission mmp;

    private AlertDialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mmp = new MarshMallowPermission(getActivity());

        setUpViews();
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

        setUpInterstitialAd();
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

    private void setUpViews() {
        // Init views
        streamsListView = getActivity().findViewById(R.id.sStreamsListView);
        searchTxt = getActivity().findViewById(R.id.sSearchTxt);
        searchTxt.setTypeface(Configs.titRegular);
        Button cancelButt = getActivity().findViewById(R.id.sCancelButt);
        cancelButt.setTypeface(Configs.titSemibold);

        // Init a refreshControl
        refreshControl = getActivity().findViewById(R.id.search_swiperefresh);
        refreshControl.setOnRefreshListener(this);

        // MARK: - SEARCH BY KEYWORDS -------------------------------------------------------------------
        searchTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    // Call query
                    queryStreams();
                    dismissKeyboard();

                    return true;
                }
                return false;
            }
        });

        // MARK: - CANCEL SEARCH BUTTON ------------------------------------
        cancelButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchTxt.setText("");
                dismissKeyboard();
                streamsListView.setVisibility(View.INVISIBLE);
            }
        });
    }

    // MARK: - QUERY STREAMS -------------------------------------------------
    void queryStreams() {
        loadingDialog = Configs.showLoadingDialog(getString(R.string.loading_dialog_please_wait), getActivity());
        ParseUser currUser = ParseUser.getCurrentUser();
        List<String> currUserID = new ArrayList<>();
        currUserID.add(currUser.getObjectId());

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.STREAMS_CLASS_NAME);
        query.whereNotContainedIn(Configs.STREAMS_REPORTED_BY, currUserID);

        // Search by keywords (show all Streams)
        List<String> keywords = new ArrayList<>();
        String[] one = searchTxt.getText().toString().toLowerCase().split(" ");
        Collections.addAll(keywords, one);

        query.whereContainedIn(Configs.STREAMS_KEYWORDS, keywords);
        query.setLimit(10000);

        query.orderByDescending(Configs.STREAMS_CREATED_AT);

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    streamsArray = objects;
                    hideLoadingDialog();

                    // Show or hide the Streams ListView
                    if (streamsArray.size() == 0) {
                        streamsListView.setVisibility(View.INVISIBLE);
                    } else {
                        streamsListView.setVisibility(View.VISIBLE);
                        reloadData();
                    }

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
        class ListAdapter extends BaseAdapter {
            private Context context;

            private ListAdapter(Context context, List<ParseObject> objects) {
                super();
                this.context = context;
            }

            // CONFIGURE CELL
            @SuppressLint("InflateParams")
            @Override
            public View getView(int position, View cell, ViewGroup parent) {
                if (cell == null) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                    cell = inflater.inflate(R.layout.cell_stream, null);
                }

                // Get Parse obj
                final ParseObject sObj = streamsArray.get(position);
                final ParseUser currUser = ParseUser.getCurrentUser();

                // Init views
                final ImageView avatarImg = cell.findViewById(R.id.csAvatarImg);
                final ImageView streamImg = cell.findViewById(R.id.csStreamImg);
                final TextView streamTxt = cell.findViewById(R.id.csStreamTxt);
                streamTxt.setTypeface(Configs.titRegular);
                final TextView likesTxt = cell.findViewById(R.id.csLikesTxt);
                likesTxt.setTypeface(Configs.titRegular);
                final TextView commentsTxt = cell.findViewById(R.id.csCommentsTxt);
                commentsTxt.setTypeface(Configs.titRegular);
                final TextView fullnameTxt = cell.findViewById(R.id.csFullnameTxt);
                fullnameTxt.setTypeface(Configs.titSemibold);
                final TextView usernameTimeTxt = cell.findViewById(R.id.csUsernameTimeTxt);
                usernameTimeTxt.setTypeface(Configs.titRegular);
                final Button likeButt = cell.findViewById(R.id.csLikeButt);
                final Button commentsButt = cell.findViewById(R.id.csCommentsButt);
                final Button shareButt = cell.findViewById(R.id.csShareButt);


                // Get userPointer
                sObj.getParseObject(Configs.STREAMS_USER_POINTER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                    @SuppressLint("SetTextI18n")
                    public void done(final ParseObject userPointer, ParseException e) {

                        // Get Stream image
                        if (sObj.getParseFile(Configs.STREAMS_IMAGE) != null) {
                            Configs.getParseImage(streamImg, sObj, Configs.STREAMS_IMAGE);
                            streamImg.setVisibility(View.VISIBLE);

                            streamImg.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent i = new Intent(getActivity(), StreamDetails.class);
                                    Bundle extras = new Bundle();
                                    extras.putString("objectID", sObj.getObjectId());
                                    i.putExtras(extras);
                                    startActivity(i);
                                }
                            });

                            // No Stream image
                        } else {
                            streamImg.setVisibility(View.INVISIBLE);
                            streamImg.getLayoutParams().height = 1;
                        }


                        // Get Stream text
                        streamTxt.setText(sObj.getString(Configs.STREAMS_TEXT));
                        streamTxt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent i = new Intent(getActivity(), StreamDetails.class);
                                Bundle extras = new Bundle();
                                extras.putString("objectID", sObj.getObjectId());
                                i.putExtras(extras);
                                startActivity(i);
                            }
                        });


                        // Get likes
                        int likes = sObj.getInt(Configs.STREAMS_LIKES);
                        likesTxt.setText(Configs.roundThousandsIntoK(likes));

                        // Show liked icon
                        List<String> likedBy = sObj.getList(Configs.STREAMS_LIKED_BY);
                        if (likedBy.contains(currUser.getObjectId())) {
                            likeButt.setBackgroundResource(R.drawable.liked_butt_small);
                        } else {
                            likeButt.setBackgroundResource(R.drawable.like_butt_small);
                        }

                        // Get comments
                        int comments = sObj.getInt(Configs.STREAMS_COMMENTS);
                        commentsTxt.setText(Configs.roundThousandsIntoK(comments));


                        // Get userPointer details
                        Configs.getParseImage(avatarImg, userPointer, Configs.USER_AVATAR);

                        fullnameTxt.setText(userPointer.getString(Configs.USER_FULLNAME));

                        String sDate = Configs.timeAgoSinceDate(sObj.getCreatedAt());
                        usernameTimeTxt.setText(getString(R.string.username_formatted_with_date,
                                userPointer.getString(Configs.USER_USERNAME), sDate));


                        // MARK: - AVATAR BUTTON ------------------------------------
                        avatarImg.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent i = new Intent(getActivity(), OtherUserProfile.class);
                                Bundle extras = new Bundle();
                                extras.putString("userID", userPointer.getObjectId());
                                i.putExtras(extras);
                                startActivity(i);
                            }
                        });


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
                                    Configs.sendPushNotification(pushMessage, (ParseUser) userPointer, getActivity());

                                    // Save Activity
                                    Configs.saveActivity(currUser, sObj, pushMessage);
                                }

                            }
                        });


                        // MARK: - COMMENTS BUTTON ------------------------------------
                        commentsButt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent i = new Intent(getActivity(), Comments.class);
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
                                        bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.logo);
                                    }
                                    Uri uri = Configs.getImageUri(getActivity(), bitmap);
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("image/jpeg");
                                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.stream_share_formatted,
                                            sObj.getString(Configs.STREAMS_TEXT), getString(R.string.app_name)));
                                    startActivity(Intent.createChooser(intent, getString(R.string.stream_share_on)));
                                }


                                // Increment shares amount
                                sObj.increment(Configs.STREAMS_SHARES, 1);
                                sObj.saveInBackground();

                            }
                        });


                    }
                });// end userPointer


                return cell;
            }

            @Override
            public int getCount() {
                return streamsArray.size();
            }

            @Override
            public Object getItem(int position) {
                return streamsArray.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }
        }

        // Init ListView and set its adapter
        streamsListView.setAdapter(new ListAdapter(getActivity(), streamsArray));
    }

    // MARK: - REFRESH DATA ----------------------------------------
    @Override
    public void onRefresh() {
        if (!searchTxt.getText().toString().matches("")) {
            // Recall query
            queryStreams();

        } else {
            Configs.simpleAlert(getString(R.string.search_validation_error), getActivity());
        }

        if (refreshControl.isRefreshing()) {
            refreshControl.setRefreshing(false);
        }
    }

    // MARK: - DISMISS KEYBOARD
    void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchTxt.getWindowToken(), 0);
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }
}
