package com.example.musicnote;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class PopupActivity extends AppCompatActivity {

    Button startBtn;
    public static PopupActivity pa;

    SoundPool soundPool;
    int effectSoundID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.howtoplay);

      /* getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
               WindowManager.LayoutParams.FLAG_FULLSCREEN);*/
        //  getWindow().setBackgroundDrawable(new ColorDrawable(0xCC000000));//배경 투명하게

        soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        effectSoundID = soundPool.load(this, R.raw.ui_menu_button_confirm_03, 1);

        startBtn = (Button)findViewById(R.id.checkBtn);

        startBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
                soundPool.play(effectSoundID, 0.75f, 0.75f, 0, 0, 1f);
                startActivityForResult(intent, 1);
                finish();
            }
        });

    }
}