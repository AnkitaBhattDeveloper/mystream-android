package com.domain.mystream;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;

import com.parse.ParseUser;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_DELAY_MILLIS = 1000;
    private Handler splashHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        printKeyHash();
    }

    @Override
    protected void onResume() {
        super.onResume();

        splashHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Class clazz;
                if (ParseUser.getCurrentUser().getObjectId() != null) {
                    clazz = HomeActivity.class;
                } else {
                    clazz = Intro.class;
                }
                Intent i = new Intent(SplashScreen.this, clazz);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
            }
        }, SPLASH_DELAY_MILLIS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        splashHandler.removeCallbacksAndMessages(null);
    }

    private void printKeyHash() {
        if (!BuildConfig.DEBUG) {
            return;
        }
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("keyhash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
    }
}
