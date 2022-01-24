package com.domain.mystream;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.ViewTreeObserver;

import com.domain.mystream.bottomnav.BottomNavigationAdapter;
import com.domain.mystream.bottomnav.HomeScreenPagerAdapter;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * @author sergiupuhalschi
 * @since 07/10/2018
 */

public class HomeActivity extends AppCompatActivity implements BottomNavigationAdapter.BottomNavigationListener {

    private static final int INITIAL_SELECTED_TAB_POSITION = 0;

    private ViewPager contentVP;
    private RecyclerView bottomMenuRV;
    private HomeScreenPagerAdapter homeScreenPagerAdapter;
    private BottomNavigationAdapter bottomNavigationAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setUpViews();
        registerForPushNotifications();
    }

    private void initViews() {
        contentVP = findViewById(R.id.ah_content_vp);
        bottomMenuRV = findViewById(R.id.ah_bottom_navigation_rv);
    }

    private void setUpViews() {
        setUpViewPager();
        setUpBottomNavigation();
    }

    private void setUpViewPager() {
        homeScreenPagerAdapter = new HomeScreenPagerAdapter(getSupportFragmentManager());
        contentVP.setAdapter(homeScreenPagerAdapter);
        contentVP.setOffscreenPageLimit(0);
    }

    private void setUpBottomNavigation() {
        ViewTreeObserver vto = bottomMenuRV.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setUpBottomNavigationRV();
                bottomMenuRV.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void setUpBottomNavigationRV() {
        bottomNavigationAdapter = new BottomNavigationAdapter(this, INITIAL_SELECTED_TAB_POSITION);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);
        bottomMenuRV.setAdapter(bottomNavigationAdapter);
        bottomMenuRV.setLayoutManager(layoutManager);
    }

    private void registerForPushNotifications() {
        // Register GCM Sender ID in Inatllation class
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();

        installation.put(Configs.INSTALLATION_GCM_SENDER_ID_KEY, getString(R.string.gcm_sender_id));
        installation.put(Configs.INSTALLATION_USER_ID, ParseUser.getCurrentUser().getObjectId());
        installation.put(Configs.INSTALLATION_USERNAME, ParseUser.getCurrentUser().getUsername());
        installation.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                Log.i("log-", "REGISTERED FOR PUSH NOTIFICATIONS!");
            }
        });
    }

    @Override
    public boolean onTabSelected(int pos) {
        contentVP.setCurrentItem(pos);
        return true;
    }
}
