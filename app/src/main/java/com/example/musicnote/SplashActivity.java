package com.example.musicnote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    ProgressBar loadingbar;
    boolean isStart = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        loadingbar = (ProgressBar)findViewById(R.id.loadingbar);
        loadingbar.setMax(200);
        Activity activity = this;

        Thread musicThread = new Thread(new Runnable() {
            @Override
            public void run() { // Thread 로 작업할 내용을 구현

                int progress = 0;
                while(true) {
                    try {
                        Thread.sleep(10);
                        progress++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(!isStart && progress > 75){
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        isStart = true;
                        finish();
                    }

                    int finalProgress = progress;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingbar.setProgress(finalProgress);
                        }
                    });
                }
            }
        });
        musicThread.start(); // 쓰레드 시작

    }
}
