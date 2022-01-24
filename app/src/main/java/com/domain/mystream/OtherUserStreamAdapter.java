package com.domain.mystream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.List;

/**
 * @author cubycode
 * @since 05/08/2018
 * All rights reserved
 */
public class OtherUserStreamAdapter extends RecyclerView.Adapter<OtherUserStreamAdapter.StreamVH> {

     private Context context;
    private List<ParseObject> streamsArray;
    MarshMallowPermission mmp;
    ParseUser userObj;
    public OtherUserStreamAdapter(Context context, List<ParseObject> streamsArray, MarshMallowPermission mmp,ParseUser userObj) {
        this.streamsArray = streamsArray;
        this.context = context;
        this.mmp = mmp;
        this.userObj = userObj;
    }


    @NonNull
    @Override
    public StreamVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.cell_stream, parent, false);
        return new StreamVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final StreamVH holder, int position) {
        // Get Parse obj
        final ParseObject sObj = streamsArray.get(position);
        final ParseUser currUser = ParseUser.getCurrentUser();



        holder.streamTxt.setTypeface(Configs.titRegular);

        holder.likesTxt.setTypeface(Configs.titRegular);
        holder.commentsTxt.setTypeface(Configs.titRegular);
        holder.fullnameTxt.setTypeface(Configs.titSemibold);
        holder.usernameTimeTxt.setTypeface(Configs.titRegular);


// Get Stream image
        if (sObj.getParseFile(Configs.STREAMS_IMAGE) != null) {
            Configs.getParseImage( holder.streamImg, sObj, Configs.STREAMS_IMAGE);
            holder.streamImg.setVisibility(View.VISIBLE);

            holder.streamImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(context, StreamDetails.class);
                    Bundle extras = new Bundle();
                    extras.putString("objectID", sObj.getObjectId());
                    i.putExtras(extras);
                    context.startActivity(i);
                }
            });


            // No Stream image
        } else {
            holder.streamImg.setVisibility(View.INVISIBLE);
            holder.streamImg.getLayoutParams().height = 1;
        }


        // Get Stream text
        holder.streamTxt.setText(sObj.getString(Configs.STREAMS_TEXT));
        holder.streamTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(context, StreamDetails.class);
                Bundle extras = new Bundle();
                extras.putString("objectID", sObj.getObjectId());
                i.putExtras(extras);
                context.startActivity(i);
            }
        });


        // Get likes
        int likes = sObj.getInt(Configs.STREAMS_LIKES);
        holder.likesTxt.setText(Configs.roundThousandsIntoK(likes));

        // Show liked icon
        List<String> likedBy = sObj.getList(Configs.STREAMS_LIKED_BY);
        if (likedBy.contains(currUser.getObjectId())) {
            holder.likeButt.setBackgroundResource(R.drawable.liked_butt_small);
        } else {
            holder.likeButt.setBackgroundResource(R.drawable.like_butt_small);
        }

        // Get comments
        int comments = sObj.getInt(Configs.STREAMS_COMMENTS);
        holder.commentsTxt.setText(Configs.roundThousandsIntoK(comments));


        // Get userObj details
        Configs.getParseImage(holder.avatarImg, userObj, Configs.USER_AVATAR);

        holder.fullnameTxt.setText(userObj.getString(Configs.USER_FULLNAME));

        String sDate = Configs.timeAgoSinceDate(sObj.getCreatedAt());
        holder.usernameTimeTxt.setText(context.getString(R.string.username_formatted_with_date, userObj.getString(Configs.USER_USERNAME), sDate));



        // MARK: - LIKE STREAM BUTTON ------------------------------------
        holder.likeButt.setOnClickListener(new View.OnClickListener() {
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

                    holder.likeButt.setBackgroundResource(R.drawable.like_butt_small);
                    int likes = sObj.getInt(Configs.STREAMS_LIKES);
                    holder.likesTxt.setText(Configs.roundThousandsIntoK(likes));


                    // LIKE THIS STREAM
                } else {
                    likedBy.add(currUser.getObjectId());
                    sObj.put(Configs.STREAMS_LIKED_BY, likedBy);
                    sObj.increment(Configs.STREAMS_LIKES, 1);
                    sObj.saveInBackground();

                    holder.likeButt.setBackgroundResource(R.drawable.liked_butt_small);
                    int likes = sObj.getInt(Configs.STREAMS_LIKES);
                    holder.likesTxt.setText(Configs.roundThousandsIntoK(likes));

                    // Send push notification
                    String pushMessage = context.getString(R.string.user_liked_stream,
                            currUser.getString(Configs.USER_FULLNAME), sObj.getString(Configs.STREAMS_TEXT));
                    Configs.sendPushNotification(pushMessage, (ParseUser) userObj, context);

                    // Save Activity
                    Configs.saveActivity(currUser, sObj, pushMessage);
                }

            }
        });


        // MARK: - COMMENTS BUTTON -------------------------------------------
        holder.commentsButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(context, Comments.class);
                Bundle extras = new Bundle();
                extras.putString("objectID", sObj.getObjectId());
                i.putExtras(extras);
                context.startActivity(i);
            }
        });


        // MARK: - SHARE BUTTON ------------------------------------------------
        holder.shareButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mmp.checkPermissionForWriteExternalStorage()) {
                    mmp.requestPermissionForWriteExternalStorage();
                } else {
                    Bitmap bitmap;
                    if (sObj.getParseFile(Configs.STREAMS_IMAGE) != null) {
                        bitmap = ((BitmapDrawable) holder.streamImg.getDrawable()).getBitmap();
                    } else {
                        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.logo);
                    }
                    Uri uri = Configs.getImageUri(context, bitmap);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("image/jpeg");
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.stream_share_formatted, sObj.getString(Configs.STREAMS_TEXT), context.getString(R.string.app_name)));
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.stream_share_on)));
                }


                // Increment shares amount
                sObj.increment(Configs.STREAMS_SHARES, 1);
                sObj.saveInBackground();

            }
        });



    }

    @Override
    public int getItemCount() {
        return streamsArray.size();
    }

    public class StreamVH extends RecyclerView.ViewHolder {

        private ImageView avatarImg, streamImg;
        private TextView streamTxt, likesTxt, commentsTxt, fullnameTxt, usernameTimeTxt;
        private Button likeButt, commentsButt, shareButt;

        public StreamVH(View cell) {
            super(cell);
            avatarImg = cell.findViewById(R.id.csAvatarImg);
            streamImg = cell.findViewById(R.id.csStreamImg);
            streamTxt = cell.findViewById(R.id.csStreamTxt);
            likesTxt = cell.findViewById(R.id.csLikesTxt);
            commentsTxt = cell.findViewById(R.id.csCommentsTxt);
            fullnameTxt = cell.findViewById(R.id.csFullnameTxt);
            usernameTimeTxt = cell.findViewById(R.id.csUsernameTimeTxt);
            likeButt = cell.findViewById(R.id.csLikeButt);
            commentsButt = cell.findViewById(R.id.csCommentsButt);
            shareButt = cell.findViewById(R.id.csShareButt);
        }

    }
}
