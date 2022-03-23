package com.example.musicnote;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.media.MediaPlayer;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class MusicUi{
    private MediaPlayer currentMediaPlayer;
    private ProgressBar musicBar;
    private TextView titleText;
    private ImageView playBtn;
    //private ImageView album;
    String[] title = {"Love Shot - EXO", "HERO - NCT127","빨간맛 - 레드벨벳"};
    //final int[] FILEROOT = {R.drawable.blackpink_howyoulikethat, R.drawable.bts_dna, R.drawable.redvelvet_redflavor};
    final int[] MEDIAROOT = {R.raw.exo_loveshot, R.raw.nct127_hero, R.raw.red_velvet};
    //MediaPlayer[] mediaPlayer = new MediaPlayer[3];
    List<MediaPlayer> mediaPlayers = new ArrayList<>(3);

    private final Activity mActivity;
    private GameSystem gameSystem;

    MusicUi(Activity mActivity, Context context, ProgressBar musicBar, TextView titleText, ImageView playBtn){
        this.musicBar = musicBar;
        this.titleText = titleText;
        this.playBtn = playBtn;

        for(int r : MEDIAROOT){
            mediaPlayers.add(MediaPlayer.create(context, r));
        }

        this.mActivity = mActivity;
    }

    public void setGameSystem(GameSystem gameSystem){
        this.gameSystem = gameSystem;
    }

    public void setMediaPlayer(int number){
        this.currentMediaPlayer = mediaPlayers.get(number);
        currentMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                musicStop();
            }
        });
        musicBar.setMax(currentMediaPlayer.getDuration());
        musicBar.setProgress(currentMediaPlayer.getCurrentPosition());
        titleText.setText(title[number]);
        //album.setImageResource(FILEROOT[number]);
    }

    public void musicPlay(){
        if(currentMediaPlayer != null) {

            gameSystem.GameStart();
            currentMediaPlayer.start();
            Thread musicThread = new Thread(new Runnable() {
                @Override
                public void run() { // Thread 로 작업할 내용을 구현
                    while(currentMediaPlayer.isPlaying()){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                musicBar.setProgress(currentMediaPlayer.getCurrentPosition());
                            }
                        });
                    }
                }
            });
            musicThread.start(); // 쓰레드 시작
            playBtn.setImageResource(R.drawable.ic_media_stop);
        }
    }

    public void musicPause(){
        if(currentMediaPlayer != null) {
            currentMediaPlayer.pause();
            gameSystem.GamePause();
            playBtn.setImageResource(R.drawable.ic_media_play);
        }
    }

    public void musicStop(){
        if(currentMediaPlayer != null) {
            currentMediaPlayer.pause();
            currentMediaPlayer.seekTo(0);
            gameSystem.GameStop();
            playBtn.setImageResource(R.drawable.ic_media_play);
            musicBar.setProgress(0);
        }
    }

    public MediaPlayer getCurrentMediaPlayer(){
        return currentMediaPlayer;
    }

    public MediaPlayer getMediaPlayer(int i){return mediaPlayers.get(i);}

    public boolean isPlaying(int i){
        return mediaPlayers.get(i).isPlaying();
    }

    public int getCurrentMediaPlayerIndex(){
        return mediaPlayers.indexOf(currentMediaPlayer);
    }
}
