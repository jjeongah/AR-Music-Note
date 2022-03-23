package com.example.musicnote;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class InfoActivity extends AppCompatActivity {
    private String TAG = "InfoActivity";

    private static final int PERMISSION_REQUEST_CODE = 1000;
    int PERMISSION_ALL = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE
    };

    private Context c;
    private Activity a;

    SoundPool soundPool;
    int effectSoundID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Button start_btn = (Button) findViewById(R.id.start_btn);
        ImageView imageView = (ImageView)findViewById(R.id.illust_image);

        soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        effectSoundID = soundPool.load(this, R.raw.ui_menu_button_confirm_03, 1);




        c = this;
        a = this;

        start_btn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                soundPool.play(effectSoundID, 0.75f, 0.75f, 0, 0, 1f);


                // 권한이 허용되어있지않다면 권한요청
                if(!hasPermissions(c, PERMISSIONS)){
                    ActivityCompat.requestPermissions(a, PERMISSIONS, PERMISSION_ALL);

                    if(hasPermissions(c, PERMISSIONS)){
                        Intent intent = new Intent(getApplicationContext(), PopupActivity.class);
                        startActivityForResult(intent,1);
                        finish();
                    }
                }
                // 권한이 허용되어있다면 다음 화면 진행
                else {
                    Intent intent = new Intent(getApplicationContext(), PopupActivity.class);
                    startActivityForResult(intent,1);
                    finish();
                }
            }

        });
    }

    public boolean hasPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getPermission(){

        ActivityCompat.requestPermissions(this,
                PERMISSIONS,
                1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= 23) {

            // requestPermission의 배열의 index가 아래 grantResults index와 매칭
            // 퍼미션이 승인되면
            if(grantResults.length > 0  && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                Log.d(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);

                // TODO : 퍼미션이 승인되는 경우에 대한 코드

            }
            // 퍼미션이 승인 거부되면
            else {
                Log.d(TAG,"Permission denied");

                // TODO : 퍼미션이 거부되는 경우에 대한 코드
            }
        }
    }
}

