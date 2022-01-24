package com.domain.mystream;

/* ================================

    - MyStream -

    created by cubycode @2018
    All Rights reserved

===================================*/

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.CountCallback;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class AccountFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    /* Views */
    private ListView streamsListView;
    private ImageView avatarImg, coverImg;
    private SwipeRefreshLayout refreshControl;
    private TextView usernameTxt, fullNameTxt, aboutMeTxt;
    private Button followersButt, followingButt;

    /* Variables */
    private List<ParseObject> streamsArray;
    private MarshMallowPermission mmp;

    private AlertDialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mmp = new MarshMallowPermission(getActivity());
        setUpViews();
        showUserDetails();
        getFollowersAndFollowing();
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

        // Call queries
        showUserDetails();
        getFollowersAndFollowing();
    }

    private void setUpViews() {
        // Init views
        avatarImg = getActivity().findViewById(R.id.accAvatarImg);
        coverImg = getActivity().findViewById(R.id.accCoverImg);
        usernameTxt = getActivity().findViewById(R.id.accUsernameTxt);
        usernameTxt.setTypeface(Configs.titSemibold);
        followersButt = getActivity().findViewById(R.id.accFollowersButt);
        followersButt.setTypeface(Configs.titRegular);
        followingButt = getActivity().findViewById(R.id.accFollowingButt);
        followingButt.setTypeface(Configs.titRegular);
        fullNameTxt = getActivity().findViewById(R.id.accFullnameTxt);
        fullNameTxt.setTypeface(Configs.titBlack);
        aboutMeTxt = getActivity().findViewById(R.id.accAboutMeTxt);
        aboutMeTxt.setTypeface(Configs.titRegular);
        streamsListView = getActivity().findViewById(R.id.accStreamsListView);

        // Init a refreshControl
        refreshControl = getActivity().findViewById(R.id.account_swiperefresh);
        refreshControl.setOnRefreshListener(this);

        // Call query
        queryStreams();

        // MARK: - ADD STREAM BUTTON ------------------------------------
        final Button addStreamButt = getActivity().findViewById(R.id.accAddStreamButt);
        addStreamButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AddStream.class));
            }
        });


        // MARK: - SETTINGS BUTTON ------------------------------------
        final Button settButt = getActivity().findViewById(R.id.accSettingsButt);
        settButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), Settings.class));
            }
        });


        // MARK: - FOLLOWING BUTTON ------------------------------------
        followingButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), Follow.class);
                Bundle extras = new Bundle();
                extras.putString("isFollowing", "true");
                extras.putString("userID", ParseUser.getCurrentUser().getObjectId());
                i.putExtras(extras);
                startActivity(i);
            }
        });


        // MARK: - FOLLOWERS BUTTON ------------------------------------
        followersButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), Follow.class);
                Bundle extras = new Bundle();
                extras.putString("isFollowing", "false");
                extras.putString("userID", ParseUser.getCurrentUser().getObjectId());
                i.putExtras(extras);
                startActivity(i);
            }
        });
    }

    // MARK: - GET FOLLOWERS AND FOLLOWING AMOUNT -------------------------------------------------
    void getFollowersAndFollowing() {
        final ParseUser currUser = ParseUser.getCurrentUser();

        // QUERY FOLLOWERS
        final ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.FOLLOW_CLASS_NAME);
        query.whereEqualTo(Configs.FOLLOW_IS_FOLLOWING, currUser);
        query.orderByDescending(Configs.FOLLOW_CREATED_AT);
        query.countInBackground(new CountCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void done(int amount, ParseException e) {
                if (e == null) {
                    String foll = Configs.roundThousandsIntoK(amount);
                    followersButt.setText(getString(R.string.user_profile_followers_count, foll));
                    // QUERY FOLLOWING
                    ParseQuery<ParseObject> query2 = ParseQuery.getQuery(Configs.FOLLOW_CLASS_NAME);
                    query2.whereEqualTo(Configs.FOLLOW_CURR_USER, currUser);
                    query2.orderByDescending(Configs.FOLLOW_CREATED_AT);
                    query2.countInBackground(new CountCallback() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void done(int amount, ParseException e) {
                            if (e == null) {
                                String foll = Configs.roundThousandsIntoK(amount);
                                followingButt.setText(getString(R.string.user_profile_following_count, foll));
                            }
                        }
                    });
                }
            }
        });
    }


    // MARK: - SHOW USER'S DETAILS ------------------------------------------------------
    void showUserDetails() {
        ParseUser currUser = ParseUser.getCurrentUser();

        // Get username
        usernameTxt.setText(getString(R.string.username_formatted, currUser.getString(Configs.USER_USERNAME)));
        // Get fullName
        fullNameTxt.setText(currUser.getString(Configs.USER_FULLNAME));

        // Get aboutMe
        if (currUser.getString(Configs.USER_ABOUT_ME) != null) {
            aboutMeTxt.setText(currUser.getString(Configs.USER_ABOUT_ME));
        } else {
            aboutMeTxt.setText("");
        }

        // Get avatar
        Configs.getParseImage(avatarImg, currUser, Configs.USER_AVATAR);

        // Get cover
        if (currUser.getParseFile(Configs.USER_COVER_IMAGE) != null) {
            Configs.getParseImage(coverImg, currUser, Configs.USER_COVER_IMAGE);
        } else {
            coverImg.setImageDrawable(null);
        }

    }


    // MARK: - QUERY STREAMS -------------------------------------------------
    void queryStreams() {
        loadingDialog = Configs.showLoadingDialog(getString(R.string.loading_dialog_please_wait), getActivity());
        ParseUser currUser = ParseUser.getCurrentUser();

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.STREAMS_CLASS_NAME);
        query.whereEqualTo(Configs.STREAMS_USER_POINTER, currUser);
        query.orderByDescending(Configs.STREAMS_CREATED_AT);
        query.setLimit(10000);

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
                final Button deleteButt = cell.findViewById(R.id.csDeleteButt);
                deleteButt.setVisibility(View.VISIBLE);
                final Button statsButt = cell.findViewById(R.id.csStatsButt);
                statsButt.setVisibility(View.VISIBLE);


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


                        // MARK: - COMMENTS BUTTON -------------------------------------------
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


                        // MARK: - SHARE BUTTON ------------------------------------------------
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


                        // MARK: - STATISTICS BUTTON ------------------------------------
                        statsButt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent i = new Intent(getActivity(), Statistics.class);
                                Bundle extras = new Bundle();
                                extras.putString("objectID", sObj.getObjectId());
                                i.putExtras(extras);
                                startActivity(i);
                            }
                        });


                        // MARK: - DELETE STREAM BUTTON ------------------------------------------
                        deleteButt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                                alert.setMessage(R.string.account_delete_stream_title)
                                        .setTitle(R.string.app_name)
                                        .setPositiveButton(R.string.account_delete_option, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                loadingDialog = Configs.showLoadingDialog(getString(R.string.loading_dialog_please_wait), getActivity());
                                                sObj.deleteInBackground(new DeleteCallback() {
                                                    @Override
                                                    public void done(ParseException e) {
                                                        if (e == null) {
                                                            hideLoadingDialog();
                                                            Configs.mustRefresh = true;
                                                            streamsArray.remove(position);
                                                            streamsListView.invalidateViews();
                                                            streamsListView.refreshDrawableState();


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
                                                                        }
                                                                    }
                                                                }
                                                            });

                                                            // error on deletion
                                                        } else {
                                                            hideLoadingDialog();
                                                            Configs.simpleAlert(e.getMessage(), getActivity());
                                                        }
                                                    }
                                                });

                                            }
                                        })
                                        .setNegativeButton(R.string.cancel_button, null)
                                        .setIcon(R.drawable.logo);
                                alert.create().show();


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
        streamsListView.setAdapter(new ListAdapter(getActivity()));
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
