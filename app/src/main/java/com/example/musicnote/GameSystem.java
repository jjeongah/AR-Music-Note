package com.example.musicnote;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 리듬 노드를 생성하는 GameSystem(Anchor node)
public class GameSystem extends AnchorNode{

    class NoteCreateTimer{
        float speed; // 노트의 움직이는 속도
        float scale; // 노트의 크기
        Note[] notes;

        NoteCreateTimer(Note[] notes, float speed, float scale){
            this.notes = notes;
            this.speed = speed;
            this.scale = scale;
        }

        public void setScale(float scale){
            this.scale = scale;
        }

        public void setSpeed(float speed){
            this.speed = speed;
        }

        public int getNoteTimer(int index){
            return notes[index].timer;
        }

        public int getDirection(int index){
            return notes[index].direction;
        }

        boolean isRight(int index){
            return notes[index].coordinate.x >= 0 ? true : false;
        }

        public int getNoteSize(){
            return notes.length;
        }

        public Coordinate getCoordinate(int index){
            return notes[index].coordinate;
        }
    }

    class Note{ // 시간 + 위치 + 방향을 담고 있는 클래스
        int timer;
        Coordinate coordinate;
        int direction; // 오른쪽(0), 오른쪽 아래(1), 아래(2), 왼쪽 아래(3), 왼쪽(4), 왼쪽 위(5), 위(6), 오른쪽 위(7)
        Vector3 pos; // parent(GameSystem)를 기준으로 한 로컬 벡터

        Note(){ }
        Note(int timer, Coordinate coordinate, int direction){
            this.timer = timer;
            this.coordinate = coordinate;
            this.direction = direction;
        }
    }

    final GameSystem gameSystem = this;

    ArSceneView arSceneView;

    public boolean isPlaying = false; // 게임이 진행 중인지

    MediaPlayer currentMediaPlayer; // 현재 흘러나오는 음악(터치한 오브젝트의 음악),
    // 노래 시간에 맞추기 위해서 필요? (아니면 그냥 일정 시간 마다 진행하는걸로 => 노래가 꺼져도 진행가능)
    int musicIndex = 0;
    MusicUi musicUi; // 현재 나오는 음악 정보(playing중인지) 및 index를 알기 위해

    int currentIndex = 0;
    NoteCreateTimer musicCreater = null;

    int currentScore = 0; // 현재까지 얻은 점수
    int totalScore = 0; // 전체 점수

    final float DISTANCE = 15f; // 15m (얼마나 앞에서 생성되게 할 것인지)
    final int DELAY = 3000; // 생성되고 퍼펙트 존(터치시 점수를 얻는 구역)까지 오는 데 걸리는 시간 (ms)
    final float ZONEDISTANCE = 2.75f; // 퍼펙트 존 거리
    final float SPEED = (DISTANCE - ZONEDISTANCE) * 1000 / DELAY; // 노트의 이동 속도(m/s)

    final float SCALE = 0.5f;
    final int SCORE = 50;

    final float INTERVAL = (MainActivity.getMinScreenSize() / 2160); // =>0.5

    // 터치 관련
    class Coordinate{
        Coordinate(float x, float y){
            this.x = x;
            this.y = y;
        }
        float x;
        float y;
    }
    class Touch{
        List<Coordinate> points;

        // 오른쪽(0), 오른쪽 아래(1), 아래(2), 왼쪽 아래(3), 왼쪽(4), 왼쪽 위(5), 위(6), 오른쪽 위(7)
        int direction;

        Touch(){
            points = new ArrayList<Coordinate>();
            direction = -1;
        }
    }
    Map<Integer, Touch> touchs = new HashMap<>();
    final float minDistance = MainActivity.getMinScreenSize() / 8f; // 드래그의 최소 거리

    // 점수 관련
    final TextView textView;
    final TextView textView2;

    ModelRenderable blueRenderable;
    ModelRenderable blueSlicedRenderable;
    ModelRenderable redRenderable;
    ModelRenderable redSlicedRenderable;
    ModelRenderable touchRenderable;

    MainActivity mainActivity;

    // 쓰기 쉽게 아래에 자주 쓰이는 좌표 나열
    final Coordinate RIGHT = new Coordinate(INTERVAL/2f, 0);
    final Coordinate LEFT = new Coordinate(-INTERVAL/2f, 0);
    final Coordinate RIGHTUP = new Coordinate(INTERVAL/2f, INTERVAL/3f);
    final Coordinate RIGHTDOWN = new Coordinate(INTERVAL/2f, -INTERVAL/3f);
    final Coordinate LEFTUP = new Coordinate(-INTERVAL/2f, INTERVAL/3f);
    final Coordinate LEFTDOWN = new Coordinate(-INTERVAL/2f, -INTERVAL/3f);
    final Coordinate MIDDLE = new Coordinate(0, 0);

    final int mR = 0, mRD = 1, mD = 2, mLD = 3, mL = 4, mLU = 5, mU = 6, mRU = 7;

    // 새롭게 바뀐 곡 노트 타이밍
    Note[][] NOTES = {
        // 0번째 곡
        {       new Note(1000, RIGHTUP, mU), new Note(2000, LEFTUP, mU),
                new Note(3000, RIGHTDOWN, mD), new Note(4000, LEFTDOWN, mD),
                new Note(5000, RIGHTUP, mR), new Note(6000, LEFTDOWN, mL),
                new Note(7000, RIGHTUP, mRU), new Note(8000, LEFTUP, mLU),
                new Note(9000, RIGHTDOWN, mRD), new Note(10000, LEFTDOWN, mLD),
                new Note(11000, RIGHTUP, mR), new Note(12000, LEFTUP, mL),
                new Note(13000, RIGHTUP, mR), new Note(14000, LEFTUP, mL),
                new Note(15000, RIGHTDOWN, mR), new Note(16000, LEFTDOWN, mL),
                new Note(17000, RIGHTUP, mU), new Note(18000, LEFTUP, mU),
                new Note(19000, RIGHTUP, mD), new Note(20000, LEFTUP, mD),
                new Note(21000, RIGHTDOWN, mR), new Note(22000, LEFTDOWN, mL),
                new Note(23000, RIGHTUP, mRU), new Note(24000, LEFTUP, mLU),
                new Note(25000, RIGHTDOWN, mRD), new Note(26000, LEFTDOWN, mLD),
                new Note(27000, RIGHTUP, mR), new Note(28000, LEFTUP, mL),
                new Note(29000, RIGHTDOWN, mR), new Note(30000, RIGHTDOWN, mL),
                new Note(31000, RIGHTDOWN, mR), new Note(32000, LEFTDOWN, mL),
                new Note(33000, RIGHTDOWN, mU), new Note(34000, RIGHTDOWN, mU),
                new Note(35000, RIGHTUP, mD), new Note(36000, LEFTUP, mD),
                new Note(37000, LEFTUP, mR), new Note(38000, LEFTUP, mL),
                new Note(39000, RIGHTUP, mRU), new Note(40000, LEFTUP, mLU),
                new Note(41000, RIGHTDOWN, mRD), new Note(42000, LEFTDOWN, mLD),
                new Note(43000, RIGHTUP, mR), new Note(44000, LEFTUP, mL),
                new Note(45000, LEFTUP, mR), new Note(46000, LEFTUP, mL),
                new Note(47000, RIGHTDOWN, mR), new Note(48000, LEFTDOWN, mL),
                new Note(50000, RIGHTUP, mR), new Note(50000, LEFTDOWN, mL),
                new Note(52000, RIGHTDOWN, mU), new Note(52000, LEFTUP, mU),
                new Note(54000, RIGHTDOWN, mD), new Note(54000, LEFTUP, mD),
                new Note(55000, RIGHTDOWN, mU), new Note(55000, LEFTUP, mU),
                new Note(56000, RIGHTDOWN, mD), new Note(56000, LEFTUP, mD),
                new Note(57000, RIGHTUP, mU), new Note(57000, LEFTUP, mU),
                new Note(58000, RIGHTUP, mD), new Note(58000, LEFTUP, mD),
                new Note(59000, RIGHTUP, mU), new Note(59000, LEFTUP, mU),
                new Note(60000, RIGHTUP, mD), new Note(60000, LEFTUP, mD),
                new Note(62000, RIGHTUP, mU), new Note(62000, LEFTUP, mU),
                new Note(62500, RIGHTDOWN, mU), new Note(62500, LEFTDOWN, mU),
                new Note(63000, RIGHTDOWN, mU), new Note(63000, LEFTDOWN, mU),
                new Note(65000, RIGHTDOWN, mD), new Note(65000, LEFTDOWN, mD),
                new Note(65500, RIGHTUP, mD), new Note(65500, LEFTDOWN, mD),
                new Note(66000, RIGHTUP, mD), new Note(66000, LEFTUP, mD),
                new Note(67000, RIGHTUP, mRU), new Note(67000, LEFTUP, mLU),
                new Note(68000, RIGHTDOWN, mRD), new Note(68000, LEFTDOWN, mLD)
                },
        // 1번째 곡
        {       new Note(1000, RIGHT, mU), new Note(2000, LEFT, mU),
                new Note(3000, RIGHT, mD), new Note(4000, LEFT, mD),
                new Note(5000, MIDDLE, mR), new Note(6000, MIDDLE, mL),
                new Note(7000, RIGHT, mRU), new Note(8000, LEFT, mLU),
                new Note(9000, MIDDLE, mRD), new Note(10000, MIDDLE, mLD),
                new Note(11000, RIGHT, mR), new Note(12000, LEFT, mL),
                new Note(13000, RIGHT, mR), new Note(14000, LEFT, mL),
                new Note(15000, RIGHT, mR), new Note(16000, LEFT, mL),
                new Note(17000, MIDDLE, mU), new Note(18000, MIDDLE, mU),
                new Note(19000, RIGHT, mD), new Note(20000, MIDDLE, mD),
                new Note(21000, RIGHT, mR), new Note(22000, MIDDLE, mL),
                new Note(23000, RIGHT, mRU), new Note(24000, LEFT, mLU),
                new Note(25000, RIGHT, mRD), new Note(26000, LEFT, mLD),
                new Note(27000, MIDDLE, mR), new Note(28000, LEFT, mL),
                new Note(29000, MIDDLE, mR), new Note(30000, LEFT, mL),
                new Note(31000, MIDDLE, mR), new Note(32000, MIDDLE, mL),
                new Note(33000, MIDDLE, mU), new Note(34000, LEFT, mU),
                new Note(35000, RIGHT, mD), new Note(36000, LEFT, mD),
                new Note(37000, RIGHT, mR), new Note(38000, LEFT, mL),
                new Note(39000, RIGHT, mRU), new Note(40000, LEFT, mLU),
                new Note(41000, MIDDLE, mRD), new Note(42000, LEFT, mLD),
                new Note(43000, RIGHT, mR), new Note(44000, LEFT, mL),
                new Note(45000, RIGHT, mR), new Note(46000, LEFT, mL),
                new Note(47000, RIGHT, mR), new Note(48000, LEFT, mL),
                new Note(50000, RIGHT, mR), new Note(50000, LEFT, mL),
                new Note(52000, RIGHT, mU), new Note(52000, LEFT, mU),
                new Note(54000, RIGHT, mD), new Note(54000, LEFT, mD),
                new Note(55000, MIDDLE, mU), new Note(55000, LEFT, mU),
                new Note(56000, RIGHT, mD), new Note(56000, MIDDLE, mD),
                new Note(57000, RIGHT, mU), new Note(57000, LEFT, mU),
                new Note(58000, RIGHT, mD), new Note(58000, LEFT, mD),
                new Note(59000, MIDDLE, mU), new Note(59000, LEFT, mU),
                new Note(60000, RIGHT, mD), new Note(60000, LEFT, mD),
                new Note(62000, RIGHT, mU), new Note(62000, MIDDLE, mU),
                new Note(62500, RIGHT, mU), new Note(62500, LEFT, mU),
                new Note(63000, RIGHT, mU), new Note(63000, LEFT, mU),
                new Note(65000, RIGHT, mD), new Note(65000, LEFT, mD),
                new Note(65500, MIDDLE, mD), new Note(65500, LEFT, mD),
                new Note(66000, MIDDLE, mD), new Note(66000, LEFT, mD),
                new Note(67000, MIDDLE, mRU), new Note(67000, LEFT, mLU),
                new Note(68000, RIGHT, mRD), new Note(68000, LEFT, mLD)},
        // 2번째 곡
        {       new Note(2140, RIGHTUP, mU), new Note(2140, LEFTUP, mU),
                new Note(4060, RIGHTDOWN, mD), new Note(4310, LEFTDOWN, mD),
                new Note(5990, RIGHTUP, mR), new Note(7970, LEFTDOWN, mL),
                new Note(8450, RIGHTUP, mRU), new Note(8880, LEFTUP, mLU),
                new Note(9770, RIGHTDOWN, mRD), new Note(11790, LEFTDOWN, mLD),
                new Note(12170, RIGHTUP, mR), new Note(13740, LEFTUP, mL),
                new Note(14710, RIGHTUP, mR), new Note(15630, LEFTUP, mL),
                new Note(16130, RIGHTDOWN, mR), new Note(16480, LEFTDOWN, mL),
                new Note(17620, RIGHTUP, mU), new Note(19380, LEFTUP, mU),
                new Note(21360, RIGHTUP, mD), new Note(23250, LEFTUP, mD),
                new Note(23790, RIGHTDOWN, mR), new Note(24250, LEFTDOWN, mL),
                new Note(25210, RIGHTUP, mRU), new Note(27080, LEFTUP, mLU),
                new Note(29090, RIGHTDOWN, mRD), new Note(30000, LEFTDOWN, mLD),
                new Note(30470, RIGHTUP, mR), new Note(30890, LEFTUP, mL),
                new Note(31360, RIGHTDOWN, mR), new Note(31830, RIGHTDOWN, mL),
                new Note(32910, RIGHTDOWN, mR), new Note(34830, LEFTDOWN, mL),
                new Note(35290, RIGHTDOWN, mU), new Note(35760, RIGHTDOWN, mU),
                new Note(36240, RIGHTUP, mD), new Note(38570, LEFTUP, mD),
                new Note(39060, LEFTUP, mR), new Note(39570, LEFTUP, mL),
                new Note(40030, RIGHTUP, mRU), new Note(40470, LEFTUP, mLU),
                new Note(42550, RIGHTDOWN, mRD), new Note(42960, LEFTDOWN, mLD),
                new Note(43460, RIGHTUP, mR), new Note(43910, LEFTUP, mL),
                new Note(47500, LEFTUP, mR), new Note(47700, LEFTUP, mL),
                new Note(47920, RIGHTDOWN, mR), new Note(48130, LEFTDOWN, mL),
                new Note(50060, RIGHTUP, mR), new Note(50400, LEFTDOWN, mL),
                new Note(52080, RIGHTDOWN, mU), new Note(53070, LEFTUP, mU),
                new Note(53980, RIGHTDOWN, mD), new Note(54450, LEFTUP, mD),
                new Note(54850, RIGHTDOWN, mU), new Note(55290, LEFTUP, mU),
                new Note(55470, RIGHTDOWN, mD), new Note(55620, LEFTUP, mD),
                new Note(55850, RIGHTUP, mU), new Note(57240, LEFTUP, mU),
                new Note(57570, RIGHTUP, mD), new Note(57720, LEFTUP, mD),
                new Note(57880, RIGHTUP, mU), new Note(58180, LEFTUP, mU),
                new Note(59810, RIGHTUP, mD), new Note(60720, LEFTUP, mD),
                new Note(61690, RIGHTUP, mU), new Note(62160, LEFTUP, mU),
                new Note(62590, RIGHTDOWN, mU), new Note(65100, LEFTDOWN, mU),
                new Note(65840, RIGHTDOWN, mU), new Note(66460, LEFTDOWN, mU),
                new Note(69100, RIGHTDOWN, mD), new Note(69640, LEFTDOWN, mD),
                new Note(70190, RIGHTUP, mD), new Note(71410, LEFTDOWN, mD),
                new Note(73100, RIGHTUP, mD), new Note(73530, LEFTUP, mD),
                new Note(74080, RIGHTUP, mRU), new Note(75020, LEFTUP, mLU),
                new Note(77010, RIGHTDOWN, mRD), new Note(77440, LEFTDOWN, mLD)}
    };

    // 딜레이 고려
    private float time = 0;

    // 효과음 관련
    //MediaPlayer effectSound;
    SoundPool soundPool;
    int effectSoundID;


    GameSystem(MainActivity mainActivity, ArSceneView arSceneView, MusicUi musicUi, TextView textView, TextView textView2){
        // Setting
        this.mainActivity = mainActivity;
        this.arSceneView = arSceneView;
        this.musicUi = musicUi;
        this.textView = textView;
        this.textView2 = textView2;

        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        effectSoundID = soundPool.load(mainActivity, R.raw.kick_heavy_impact_04, 1);

        //arSceneView.setOnTouchListener(this::onTouch); // 실험 -> 오 잘된다 레전드


        // Create an ARCore Anchor at the position.
        this.setParent(arSceneView.getScene());

        setUpModel();
        SetPosition();

        // 아래 내용: 이래야만 onUpdate작동하는지 확인
        this.setLocalScale(new Vector3(0.5f, 0.5f , 0.5f));

        // 오브젝트 카메라 바라보게 회전
        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();
        Vector3 objPos = this.getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, objPos).negated();
        Vector3 up = this.getUp();
        Quaternion direction = Quaternion.lookRotation(objToCam, up);
        this.setWorldRotation(direction);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

        time += frameTime.getDeltaSeconds() * 1000;

        SetPosition(); // Game System의 위치를 핸드폰 앞으로 잡기
        if (isPlaying){

            if(currentMediaPlayer == null){
                Log.e("ERROR: ", "currentMediaPlayer is null");
                return;
            }

            for(; currentIndex < musicCreater.getNoteSize() && musicCreater.getNoteTimer(currentIndex) < time; currentIndex++) {
                int direction = musicCreater.getDirection(currentIndex);
                Coordinate coordinate = musicCreater.getCoordinate(currentIndex);

                if (musicCreater.isRight(currentIndex)) {
                    GameNote note = new GameNote(arSceneView, this, blueRenderable, SPEED, DISTANCE, SCORE, coordinate, direction);
                }
                else {
                    GameNote note = new GameNote(arSceneView, this, redRenderable, SPEED, DISTANCE, SCORE, coordinate, direction);
                }
            }
        }
    }

    // 노드 생성 시작 (일정 시간 뒤에 생성되는 걸로)
    public void GameStart(){
        currentMediaPlayer = musicUi.getCurrentMediaPlayer();

        if(currentMediaPlayer == null){
            Log.e("ERROR: ", "currentMediaPlayer is null");
            return;
        }

        textView.setVisibility(View.VISIBLE);

        musicIndex = musicUi.getCurrentMediaPlayerIndex();
        currentIndex = 0;
        isPlaying = true;
        musicCreater = new NoteCreateTimer(NOTES[musicIndex], SPEED, SCALE);
        SetPosition();

        currentScore = 0;
        time = 0;
    }

    // 게임 정지
    public void GameStop(){
        textView.setVisibility(View.GONE);

        isPlaying = false;
        currentIndex = 0;
        currentScore = 0;
        musicCreater = null;
        time = 0;
    }

    // 게임 일시 정지
    public void GamePause(){
        isPlaying = !isPlaying;
    }

    // Game System(this)의 위치 조정
    public void SetPosition(){
        Camera camera = arSceneView.getScene().getCamera();

        Vector3 cameraPos = camera.getWorldPosition(); // 카메라 위치 받아옴
        Vector3 forward = camera.getForward(); // 핸드폰 앞 벡터 받아옴
        Vector3 up = this.getUp().normalized(); // 이걸로 해도 되는지 모르겠음 => 잘되네?

        // up vector를 법선벡터로 갖는 평면에 forward Vector 정사영구하기
        Vector3 upValue = new Vector3(up).scaled(Vector3.dot(up, forward));
        Vector3 systemPos = Vector3.subtract(forward, upValue).normalized().scaled(DISTANCE);
        Vector3 position = Vector3.add(cameraPos, systemPos);

        this.setWorldPosition(position); // 위치 설정

        // 오브젝트 카메라 바라보게 회전
        Vector3 objPos = this.getWorldPosition();
        Vector3 objToCam = Vector3.subtract(cameraPos, objPos).negated();
        Quaternion direction = Quaternion.lookRotation(objToCam, up);
        this.setWorldRotation(direction);
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        boolean ret = false;
        int touch_count = motionEvent.getPointerCount();
        if(touch_count > 2) touch_count = 2; // 2개 이상의 포인트를 터치했어도 2개만 본다.

        final int action = motionEvent.getAction();
        int key;
        float x,y;

        switch(action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: //한 개 포인트에 대한 DOWN을 얻을 때.
                //Log.i("디버그: ", " 터치 다운");
                x = motionEvent.getX();
                y = motionEvent.getY();
                key = motionEvent.getPointerId(0);
                touchs.put(key, new Touch());
                touchs.get(key).points.add(new Coordinate(x, y));

                ret = true;
                break;

            case MotionEvent.ACTION_POINTER_DOWN: //두 개 이상의 포인트에 대한 DOWN을 얻을 때.
                //Log.i("디버그: ", " 터치 다운 (2개)");
                for(int i = 0; i < touch_count; i++){
                    x = motionEvent.getX(i);
                    y = motionEvent.getY(i);

                    key = motionEvent.getPointerId(i);
                    touchs.put(key, new Touch());
                    touchs.get(key).points.add(new Coordinate(x, y));
                }
                ret = true;
                break;

            case MotionEvent.ACTION_MOVE:
                //Log.i("디버그: ", " 터치 무브");
                for(int i = 0; i < touch_count; i++){
                    x = motionEvent.getX(i);
                    y = motionEvent.getY(i);
                    key = motionEvent.getPointerId(i);
                    touchs.get(key).points.add(new Coordinate(x, y));
                    checkDirection(key);
                }
                ret = true;
                break;

            case MotionEvent.ACTION_UP:
                //Log.i("디버그: ", " 터치 업");
                for(int i = 0; i < touch_count; i++){
                    key = motionEvent.getPointerId(i);
                    touchs.remove(key);
                }
                break;
        }
        arSceneView.onTouchEvent(motionEvent);
        return ret;
    }

    // 해당 터치의 연속된 기록이 어떤 방향을 나타내고 있는지 => 이상하면 가장 오래된 기록 삭제
    public void checkDirection(int key){
        List<Coordinate> coordinates = touchs.get(key).points;
        int size = coordinates.size();
        Coordinate startPoint = coordinates.get(0);
        Coordinate endPoint = coordinates.get(size - 1);

        float x = endPoint.x - startPoint.x;
        float y = endPoint.y - startPoint.y;

        float distance = (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));

        if(distance >= minDistance){
            // touch의 방향 확인
            float theta = (float)Math.atan2(y, x) + MainActivity.mCurrentRoll;
            if(theta < 0) theta += (float)Math.PI * 2;

            //theta = theta % (float)(Math.PI*2);
            if(0 <= theta && theta < Math.PI/8 || Math.PI/8 + Math.PI/4 * 7 <= theta && theta < Math.PI*2){
                // 오른쪽
                touchs.get(key).direction = 0;
            }
            else if(Math.PI/8 <= theta && theta < Math.PI/8 + Math.PI/4){
                // 오른쪽 아래
                touchs.get(key).direction = 1;
            }
            else if(Math.PI/8 + Math.PI/4 <= theta && theta < Math.PI/8 + Math.PI/4*2){
                // 아래
                touchs.get(key).direction = 2;
            }
            else if(Math.PI/8 + Math.PI/4*2 <= theta && theta < Math.PI/8 + Math.PI/4*3){
                // 왼쪽 아래
                touchs.get(key).direction = 3;
            }
            else if(Math.PI/8 + Math.PI/4*3 <= theta && theta < Math.PI/8 + Math.PI/4*4){
                // 왼쪽
                touchs.get(key).direction = 4;
            }
            else if(Math.PI/8 + Math.PI/4*4 <= theta && theta < Math.PI/8 + Math.PI/4*5){
                // 왼쪽 위
                touchs.get(key).direction = 5;
            }
            else if(Math.PI/8 + Math.PI/4*5 <= theta && theta < Math.PI/8 + Math.PI/4*6){
                // 위
                touchs.get(key).direction = 6;
            }
            else if(Math.PI/8 + Math.PI/4*6 <= theta && theta < Math.PI/8 + Math.PI/4*7){
                // 오른쪽 위
                touchs.get(key).direction = 7;
            }

            checkCollision(key, startPoint, endPoint);

            while(distance >= minDistance){
                coordinates.remove(0);
                int tsize = coordinates.size();

                if(tsize == 0) break;

                Coordinate tempStart = coordinates.get(0);
                Coordinate tempEnd = coordinates.get(tsize - 1);

                float tx = tempEnd.x - tempStart.x;
                float ty = tempEnd.y - tempStart.y;

                distance = (float)Math.sqrt(Math.pow(tx, 2) + Math.pow(ty, 2));
            }
        }
        else{
            return;
        }
    }

    // 충돌 감지
    public void checkCollision(int key, Coordinate start, Coordinate end){
        Camera camera = arSceneView.getScene().getCamera();
        Vector3 forward = camera.getForward();

        // 1번째 생각 스크린 포인트를 Ray로

        Ray startRay = camera.screenPointToRay(start.x, start.y);
        Ray endRay = camera.screenPointToRay(end.x, end.y);
        boolean isCollision = false;

        for(int i = 0; i < 11; i++){
            Vector3 startPoint = startRay.getPoint(0 + 0.1f * i * (ZONEDISTANCE + 1f));
            Vector3 endPoint = endRay.getPoint(0 + 0.1f * i * (ZONEDISTANCE + 1f));
            Vector3 direction = Vector3.subtract(endPoint, startPoint).normalized();
            startPoint = Vector3.add(startPoint, direction.negated().scaled(0.1f));

            Ray ray = new Ray(startPoint, direction);
            List<HitTestResult> hits = arSceneView.getScene().hitTestAll(ray);
            for(int j = 0; j < hits.size(); j++){
                if(hits.get(j).getDistance() <= minDistance * 2f){
                    Node n = hits.get(j).getNode();
                    if(n instanceof GameNote){
                        isCollision = ((GameNote) n).getScore(touchs.get(key).direction);
                    }
                }
            }

            if(isCollision) return;

            Vector3 startPoint2 = Vector3.add(startRay.getPoint(0 + 0.1f * i * (ZONEDISTANCE + 1f)), camera.getLeft().scaled(0.25f));
            Vector3 endPoint2 = Vector3.add(endRay.getPoint(0 + 0.1f * i * (ZONEDISTANCE + 1f)), camera.getLeft().scaled(0.25f));
            Vector3 direction2 = Vector3.subtract(endPoint2, startPoint2).normalized();
            startPoint2 = Vector3.add(startPoint2, direction2.negated().scaled(0.1f));

            Ray ray2 = new Ray(startPoint2, direction2);
            List<HitTestResult> hits2 = arSceneView.getScene().hitTestAll(ray2);
            for(int j = 0; j < hits2.size(); j++){
                if(hits2.get(j).getDistance() <= minDistance * 2f){
                    Node n = hits2.get(j).getNode();
                    if(n instanceof GameNote){
                        isCollision = ((GameNote) n).getScore(touchs.get(key).direction);
                    }
                }
            }

            if(isCollision) return;

            Vector3 startPoint3 = Vector3.add(startRay.getPoint(0 + 0.1f * i * (ZONEDISTANCE + 1f)), camera.getRight().scaled(0.25f));
            Vector3 endPoint3 = Vector3.add(endRay.getPoint(0 + 0.1f * i * (ZONEDISTANCE + 1f)), camera.getRight().scaled(0.25f));
            Vector3 direction3 = Vector3.subtract(endPoint3, startPoint3).normalized();
            startPoint3 = Vector3.add(startPoint3, direction3.negated().scaled(0.1f));

            Ray ray3 = new Ray(startPoint3, direction3);
            List<HitTestResult> hits3 = arSceneView.getScene().hitTestAll(ray3);
            for(int j = 0; j < hits3.size(); j++){
                if(hits3.get(j).getDistance() <= minDistance * 2f){
                    Node n = hits3.get(j).getNode();
                    if(n instanceof GameNote){
                        isCollision = ((GameNote) n).getScore(touchs.get(key).direction);
                    }
                }
            }

            if(isCollision) return;
        }

    }

    Vector3 getPosVector(Coordinate coordinate){
        Vector3 up = gameSystem.getUp();
        Vector3 right = gameSystem.getRight();

        Vector3 pos = Vector3.add(right.scaled(coordinate.x), up.scaled(coordinate.y));

        return pos;
    }

    public void getScore(int score, Coordinate coordinate){
        currentScore += score;
        totalScore += score;
        int colorWhite = mainActivity.getResources().getColor(R.color.colorWhite);

        String scoreString = currentScore +" 점";
        int length = scoreString.length();
        SpannableStringBuilder spannable = new SpannableStringBuilder(scoreString);
        spannable.setSpan(new AbsoluteSizeSpan(60),0, length - 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan(40),length-1, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannable, TextView.BufferType.EDITABLE);

        String scoreString2 = "스코어 " + totalScore + " 점";
        int length2 = scoreString2.length();
        SpannableStringBuilder spannable2 = new SpannableStringBuilder(scoreString2);
        spannable2.setSpan(new AbsoluteSizeSpan(45),0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new AbsoluteSizeSpan(70),4, length2 - 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new AbsoluteSizeSpan(45),length2-1, length2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new ForegroundColorSpan(colorWhite),4, length2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView2.setText(spannable2, TextView.BufferType.EDITABLE);

        if(coordinate.x > 0) {
            soundPool.play(effectSoundID, 0.15f, 0.5f, 0, 0, 1.2f);
        }
        else if (coordinate.x < 0){
            soundPool.play(effectSoundID, 0.15f, 0.2f, 0, 0, 1.2f);
        }
        else{
            soundPool.play(effectSoundID, 0.15f, 0.5f, 0, 0, 1.2f);
        }

        Log.i("Time: ", currentMediaPlayer.getCurrentPosition()+"ms, "+"Time: "+time);
    }

    public int getDELAY(){
        return DELAY;
    }

    public void setUpModel(){
        ModelRenderable.builder()
                .setSource(mainActivity, R.raw.blueblock)
                .build().thenAccept(renderable -> blueRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );
        ModelRenderable.builder()
                .setSource(mainActivity, R.raw.redblock)
                .build().thenAccept(renderable -> redRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );
        ModelRenderable.builder()
                .setSource(mainActivity, R.raw.pushilin_star)
                .build().thenAccept(renderable -> blueSlicedRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );
        ModelRenderable.builder()
                .setSource(mainActivity, R.raw.pushilin_star)
                .build().thenAccept(renderable -> redSlicedRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );
        ModelRenderable.builder()
                .setSource(mainActivity, R.raw.pushilin_sun)
                .build().thenAccept(renderable -> touchRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            return null;
                        }
                );
    }


    public float getSCALE(){
        return SCALE;
    }

    public float getZONEDISTANCE(){
        return ZONEDISTANCE;
    }
}
