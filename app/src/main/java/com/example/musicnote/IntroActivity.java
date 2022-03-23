package com.example.musicnote;

import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    SoundPool soundPool;
    int effectSoundID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        effectSoundID = soundPool.load(this, R.raw.ui_menu_button_click_07, 1);

        ImageButton musicnote_btn = (ImageButton) findViewById(R.id.musicnote_btn);
        TextView bof_text = (TextView)findViewById(R.id.textView);
        TextView btn_text = (TextView)findViewById(R.id.musicnote_btn_text);
        TextView gameBtnText = (TextView)findViewById(R.id.game_btn_text);
        int color = getResources().getColor(R.color.colorDefault);

        SpannableStringBuilder spannable = new SpannableStringBuilder("#두근두근_행사장 가는 길\nAR 음악노트");
        spannable.setSpan(new AbsoluteSizeSpan(45),0, 14, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan(70),15, 22, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        btn_text.setText(spannable, TextView.BufferType.EDITABLE);

        SpannableStringBuilder spannable3 = new SpannableStringBuilder("#몸은_멀리_마음은_가까이\nAR 겜미팅");
        spannable3.setSpan(new AbsoluteSizeSpan(45),0, 14, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable3.setSpan(new AbsoluteSizeSpan(70),15, 21, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        gameBtnText.setText(spannable3, TextView.BufferType.EDITABLE);

        SpannableStringBuilder spannable2 = new SpannableStringBuilder("AR로 즐기는\n부산원아시아\n페스티벌");
        spannable2.setSpan(new StyleSpan(Typeface.BOLD),0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new StyleSpan(Typeface.NORMAL),3,7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new StyleSpan(Typeface.BOLD),8, 19, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new ForegroundColorSpan(color),8, 19, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        bof_text.setText(spannable2, TextView.BufferType.EDITABLE);

        musicnote_btn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                soundPool.play(effectSoundID, 0.75f, 0.75f, 0, 0, 1f);
                startActivity(intent);
            }
        });
    }
}
