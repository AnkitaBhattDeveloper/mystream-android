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
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment {

    /* Views */
    private ListView messListView;
    private RelativeLayout noMessLayout;


    /* Variables */
    private List<ParseObject> messagesArray;

    private AlertDialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setUpViews();

        // Call query
        queryMessages();
    }

    private void setUpViews() {
        // Init views
        messListView = getActivity().findViewById(R.id.messagesListView);
        noMessLayout = getActivity().findViewById(R.id.messNoMessLayout);
    }

    // MARK: - QUERY CHATS ----------------------------------------------------------------------
    private void queryMessages() {
        loadingDialog = Configs.showLoadingDialog(getString(R.string.loading_dialog_please_wait), getActivity());

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Configs.MESSAGES_CLASS_NAME);
        query.include(Configs.USER_CLASS_NAME);
        query.whereContains(Configs.MESSAGES_ID, ParseUser.getCurrentUser().getObjectId());
        query.orderByDescending(Configs.MESSAGES_CREATED_AT);

        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> objects, final ParseException error) {
                if (error == null) {
                    messagesArray = objects;
                    hideLoadingDialog();

                    // Show/hide NO messages Layout
                    if (messagesArray.size() == 0) {
                        messListView.setVisibility(View.INVISIBLE);
                        noMessLayout.setVisibility(View.VISIBLE);
                    } else {
                        messListView.setVisibility(View.VISIBLE);
                        noMessLayout.setVisibility(View.INVISIBLE);
                    }


                    // CUSTOM LIST ADAPTER
                    class ListAdapter extends BaseAdapter {
                        private Context context;

                        public ListAdapter(Context context, List<ParseObject> objects) {
                            super();
                            this.context = context;
                        }

                        // CONFIGURE CELL
                        @Override
                        public View getView(int position, View cell, ViewGroup parent) {
                            if (cell == null) {
                                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                assert inflater != null;
                                cell = inflater.inflate(R.layout.cell_messages, null);
                            }
                            final View finalCell = cell;


                            // Get Parse object
                            final ParseObject mObj = messagesArray.get(position);
                            final ParseUser currUser = ParseUser.getCurrentUser();

                            // Get userPointer
                            mObj.getParseObject(Configs.MESSAGES_USER_POINTER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                                public void done(final ParseObject userPointer, ParseException e) {

                                    // Get otherPointer
                                    mObj.getParseObject(Configs.MESSAGES_OTHER_USER).fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                                        @SuppressLint("SetTextI18n")
                                        public void done(ParseObject otherPointer, ParseException e) {

                                            // Init views
                                            ImageView avatarImg = finalCell.findViewById(R.id.cmessAvatarImg);
                                            TextView fullnameTxt = finalCell.findViewById(R.id.cmessFullnameTxt);
                                            fullnameTxt.setTypeface(Configs.titSemibold);
                                            TextView senderTxt = finalCell.findViewById(R.id.cmessSenderTxt);
                                            senderTxt.setTypeface(Configs.titRegular);


                                            // Get Sender's username
                                            if (userPointer.getObjectId().matches(currUser.getObjectId())) {
                                                senderTxt.setText(R.string.messages_sent_message_prefix);
                                                Configs.getParseImage(avatarImg, otherPointer, Configs.USER_AVATAR);
                                                fullnameTxt.setText(otherPointer.getString(Configs.USER_FULLNAME));
                                            } else {
                                                senderTxt.setText(getString(R.string.username_formatted, userPointer.getString(Configs.USER_USERNAME)));
                                                fullnameTxt.setText(userPointer.getString(Configs.USER_FULLNAME));
                                                Configs.getParseImage(avatarImg, userPointer, Configs.USER_AVATAR);
                                            }


                                            // Get date
                                            TextView dateTxt = finalCell.findViewById(R.id.cmessDateTxt);
                                            dateTxt.setTypeface(Configs.titRegular);
                                            dateTxt.setText(Configs.timeAgoSinceDate(mObj.getCreatedAt()));

                                            // Get last Message
                                            TextView lastMessTxt = finalCell.findViewById(R.id.cmessLastMessTxt);
                                            lastMessTxt.setTypeface(Configs.titRegular);
                                            lastMessTxt.setText(mObj.getString(Configs.MESSAGES_LAST_MESSAGE));


                                        }
                                    });// end otherUser

                                }
                            });// end userPointer


                            return cell;
                        }

                        @Override
                        public int getCount() {
                            return messagesArray.size();
                        }

                        @Override
                        public Object getItem(int position) {
                            return messagesArray.get(position);
                        }

                        @Override
                        public long getItemId(int position) {
                            return position;
                        }
                    }


                    // Init ListView and set its adapter
                    messListView.setAdapter(new ListAdapter(getActivity(), messagesArray));
                    messListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                            final ParseObject mObj = messagesArray.get(position);

                            // Get userPointer
                            mObj.getParseUser(Configs.MESSAGES_USER_POINTER).fetchIfNeededInBackground(new GetCallback<ParseUser>() {
                                public void done(final ParseUser userPointer, final ParseException e) {

                                    // Get otherPointer
                                    mObj.getParseUser(Configs.MESSAGES_OTHER_USER).fetchIfNeededInBackground(new GetCallback<ParseUser>() {
                                        public void done(final ParseUser otherUser, ParseException e) {

                                            ParseUser currUser = ParseUser.getCurrentUser();
                                            List<String> blockedUsers = new ArrayList<>();
                                            String blockMessage = "";

                                            if (userPointer.getObjectId().matches(currUser.getObjectId())) {
                                                blockedUsers = otherUser.getList(Configs.USER_HAS_BLOCKED);
                                                blockMessage = getString(R.string.messages_you_blocked_error,
                                                        otherUser.getString(Configs.USER_USERNAME));
                                            } else {
                                                blockedUsers = userPointer.getList(Configs.USER_HAS_BLOCKED);
                                                blockMessage = getString(R.string.messages_you_blocked_error,
                                                        userPointer.getString(Configs.USER_USERNAME));
                                            }

                                            // otherUser user has blocked you
                                            if (blockedUsers.contains(currUser.getObjectId())) {
                                                Configs.simpleAlert(blockMessage, getActivity());

                                                // You can chat with otherUser
                                            } else {
                                                Intent i = new Intent(getActivity(), InboxActivity.class);
                                                Bundle extras = new Bundle();
                                                String userID;
                                                if (userPointer.getObjectId().matches(currUser.getObjectId())) {
                                                    userID = otherUser.getObjectId();
                                                } else {
                                                    userID = userPointer.getObjectId();
                                                }

                                                extras.putString("userID", userID);
                                                i.putExtras(extras);
                                                startActivity(i);
                                            }
                                        }
                                    });// end otherPointer
                                }
                            });// end userPointer
                        }
                    });

                    // Error in query
                } else {
                    hideLoadingDialog();
                    Configs.simpleAlert(error.getMessage(), getActivity());
                }
            }
        });
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }
}
