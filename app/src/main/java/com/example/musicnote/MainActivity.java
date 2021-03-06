package com.example.musicnote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.MonthDisplayHelper;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewManager;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.CameraUtils;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.widget.CompassView;
import com.naver.maps.map.widget.LocationButtonView;
import com.naver.maps.map.widget.ScaleBarView;
import com.naver.maps.map.widget.ZoomControlView;
import com.ssomai.android.scalablelayout.ScalableLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static android.hardware.SensorManager.AXIS_X;
import static android.hardware.SensorManager.AXIS_Z;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        SensorEventListener{

    private static final String TAG = "MainActivity";

    private FrameLayout popupLayout;

    // ????????? ?????? ??????
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
    };

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    // ?????? ?????? ??????
    boolean isFullScreen = false;
    float mapWidth = 0;
    float mapHeight = 0;
    boolean isThreadRunning = false;
    PathOverlay path;
    List<Marker> markerList;
    int musicMaxIndex = -1;

    // ?????? ??????
    public Location mCurrentLocation;

    // ?????? ??????
    private List<Location> markers = new ArrayList<Location>();
    private Location logoLocation;

    // ar ??????
    private View mLayout;  // Snackbar ???????????? ???????????? View??? ???????????????.
    // (????????? Toast????????? Context??? ??????????????????.)

    // ????????? ArCamera??? ?????? ?????? ??????
    private ArFragment arFragment;
    private static ArSceneView arSceneView;
    private AnchorNode[] mAnchorNode = new AnchorNode[3];
    private AnchorNode logoAnchor;

    private ModelRenderable bofLogoRenderable;
    private ModelRenderable[] musicNotes = new ModelRenderable[2];

    private ModelRenderable[] albumRenderable = new ModelRenderable[3]; // album Object???

    // Device Orientation ??????
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    public static float mCurrentAzim = 0f; // ?????????
    public static float mCurrentPitch = 0f; // ??????
    public static float mCurrentRoll = 0f; // ???

    // UI
    private TextView musicTitle;
    private ImageView play;
    private ProgressBar musicBar;
    private MusicUi musicUiclass;
    private ScalableLayout musicUi;
    private ImageView album;

    // ?????? ??????
    private GameSystem gameSystem;
    public static MainActivity ma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ma = this;

        // Devicd Orientation ??????
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // ???????????? ????????????
        mLayout = findViewById(R.id.layout_main);

        // ????????????
        // ????????? ??????
        markers.add(new Location("point 0"));
        markers.get(0).setLatitude(35.163372);
        markers.get(0).setLongitude(129.159100);

        // ????????? ??????
        markers.add(new Location("point 1"));
        markers.get(1).setLatitude(35.162959);
        markers.get(1).setLongitude(129.159446);

        // ????????? ??????
        markers.add(new Location("point 2"));
        markers.get(2).setLatitude(35.162545);
        markers.get(2).setLongitude(129.159794);

        // ????????? ??????
        markers.add(new Location("point 3"));
        markers.get(3).setLatitude(35.162131);
        markers.get(3).setLongitude(129.160141);

        // ???????????? ??????
        markers.add(new Location("point 4"));
        markers.get(4).setLatitude(35.161710);
        markers.get(4).setLongitude(129.160484);

        // ???????????? ??????
        markers.add(new Location("point 5"));
        markers.get(5).setLatitude(35.161295);
        markers.get(5).setLongitude(129.160839);

        // ???????????? ??????
        markers.add(new Location("point 6"));
        markers.get(6).setLatitude(35.160891);
        markers.get(6).setLongitude(129.161202);

        // ???????????? ??????
        markers.add(new Location("point 7"));
        markers.get(7).setLatitude(35.160473);
        markers.get(7).setLongitude(129.161549);

        // ???????????? ??????
        markers.add(new Location("point 8"));
        markers.get(8).setLatitude(35.160061);
        markers.get(8).setLongitude(129.161892);

        // ?????? ??????
        logoLocation = new Location("BOF LOGO");
        logoLocation.setLatitude(35.159715);
        logoLocation.setLongitude(129.162171);

        /* ?????????
        // ????????? ??????//ddddd
        markers[0] = new Location("point A");
        markers[0].setLatitude(37.284677);
        markers[0].setLongitude(127.053124);
        // ????????? ??????
        markers[1] = new Location("point B");
        markers[1].setLatitude(37.284457);
        markers[1].setLongitude(127.053318);
        // ????????? ??????
        markers[2] = new Location("point C");
        markers[2].setLatitude(37.284161);
        markers[2].setLongitude(127.053781);

        // ?????? ??????
        logoLocation = new Location("BOF LOGO");
        logoLocation.setLatitude(37.283827);
        logoLocation.setLongitude(127.054332);
         */


        // ??????????????? ?????? ????????? ????????? ??????
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // ?????? ui ???????????? ????????????
        // ???????????? ?????? ??????
        FrameLayout gameLayout = (FrameLayout) inflater.inflate(R.layout.game_ui, null);
        // ???????????? ?????? ????????? ??????
        gameLayout.setBackgroundColor(Color.parseColor("#00000000"));
        // ???????????? ?????? ?????????
        FrameLayout.LayoutParams paramll2 = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        );
        addContentView(gameLayout, paramll2);

        // ???????????? ?????? ??????
        LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.navermap, null);
        // ???????????? ?????? ????????? ??????
        ll.setBackgroundColor(Color.parseColor("#00000000"));
        // ???????????? ?????? ?????????
        LinearLayout.LayoutParams paramll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addContentView(ll, paramll);

        // '????????? ?????? ???' ????????? ????????????
        popupLayout = (FrameLayout)inflater.inflate(R.layout.activity_popup, null);
        popupLayout.setBackgroundColor(Color.parseColor("#CC000000"));
        FrameLayout.LayoutParams popParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        );
        addContentView(popupLayout, popParams);

        // ?????? ?????? ??????
        musicUi = (ScalableLayout) findViewById(R.id.musicUi);

        musicTitle = (TextView) findViewById(R.id.musicTitle);
        play = (ImageView) findViewById(R.id.play);
        play.setImageResource(R.drawable.ic_media_stop);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicUiclass.getCurrentMediaPlayer().isPlaying()) { // ????????? ???????????? ?????? ??? => ????????? ????????????
                    musicUiclass.musicPause();
                } else { // ????????? ???????????? ??? => ????????? ???????????????
                    musicUiclass.musicPlay();
                }
            }
        });
        musicBar = (ProgressBar) findViewById(R.id.musicBar);
        //album = (ImageView) findViewById(R.id.album);
        musicUiclass = new MusicUi(this, this, musicBar, musicTitle, play);

        TextView score = findViewById(R.id.score);
        SpannableStringBuilder spannable = new SpannableStringBuilder(score.getText());
        spannable.setSpan(new AbsoluteSizeSpan(60),0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan(40), 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        score.setText(spannable, TextView.BufferType.EDITABLE);

        int colorWhite = getResources().getColor(R.color.colorWhite);
        TextView score2 = findViewById(R.id.scoreText);
        SpannableStringBuilder spannable2 = new SpannableStringBuilder(score2.getText());
        spannable2.setSpan(new AbsoluteSizeSpan(45),0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new AbsoluteSizeSpan(70),4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new AbsoluteSizeSpan(45),6, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable2.setSpan(new ForegroundColorSpan(colorWhite),4, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        score2.setText(spannable2, TextView.BufferType.EDITABLE);

        // ?????? ?????? ??????
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        // getMapAsync??? ???????????? ???????????? onMapReady ?????? ????????? ??????
        // onMapReady?????? NaverMap ????????? ??????
        mapFragment.getMapAsync(this);

        // ????????? ???????????? ???????????? FusedLocationSource ??????
        mLocationSource =
                new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        // ar ??????
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arCamera);
        arSceneView = arFragment.getArSceneView();
        setUpModel();

        arFragment.getArSceneView().getScene().setOnUpdateListener(this::onSceneUpdate);
        arSceneView.setOnTouchListener((view, motionEvent)->{
            final int action = motionEvent.getAction();
            if((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP){
                if(isFullScreen){
                    popDownScrollUI();
                }
            }
            return gameSystem.onTouch(view, motionEvent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);

        arFragment.onResume();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        arFragment.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //arFragment.onDestroy();
    }

    @Override
    public void onBackPressed(){
        if(isFullScreen){
            popDownScrollUI();
        }
        else{
            super.onBackPressed();
        }
    }

    public void popDownScrollUI(){
        if(isThreadRunning) isThreadRunning = false;

        Activity a = this;
        View mapOverlay = (View)findViewById(R.id.map);
        View mapBorder = (View)findViewById(R.id.mapBorder);
        View mapImg = (View)findViewById(R.id.mapImg);
        View gameUI = (View)findViewById(R.id.game_ui);
        View panel = (View)findViewById(R.id.panel);

        gameUI.setVisibility(View.VISIBLE);

        mapImg.setVisibility(View.VISIBLE);
        mapOverlay.setVisibility(View.INVISIBLE);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() { // Thread ??? ????????? ????????? ??????
                final int deltaSecond = 5;
                final int duration = 75;
                int time = 0;
                final int height = 1740;

                while(time < duration){
                    try {
                        Thread.sleep(deltaSecond);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    time += deltaSecond;

                    int finalTime = time;
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            panel.setLayoutParams(new ScalableLayout.LayoutParams((-250 + 75)/duration * finalTime - 75, 0, 200, 1850));
                            mapImg.setLayoutParams(new ScalableLayout.LayoutParams(35, 35, (290 - (1000-70))/duration * finalTime + (1000-70), (390 - height)/duration * finalTime + height));
                            mapBorder.setLayoutParams(new ScalableLayout.LayoutParams(30, 30, (300 - (1000-60))/duration * finalTime + (1000-60), (400 - (height+10))/duration * finalTime + (height+10)));
                        }
                    });
                }
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        panel.setLayoutParams(new ScalableLayout.LayoutParams(-250, 0, 200, 1850));
                        mapImg.setVisibility(View.INVISIBLE);
                        mapOverlay.setLayoutParams(new ScalableLayout.LayoutParams(35, 35, 290, 390));
                        mapOverlay.setVisibility(View.VISIBLE);
                        mapBorder.setLayoutParams(new ScalableLayout.LayoutParams(30, 30, 300, 400));
                        path.setMap(null);
                        CameraUpdate cameraUpdate = CameraUpdate.zoomTo(15);
                        mNaverMap.moveCamera(cameraUpdate);

                        for(Marker m : markerList){
                            m.setCaptionText("");
                            m.setSubCaptionText("");
                        }
                    }
                });
            }
        });
        thread.start();

        isFullScreen = false;
    }

    private void setUpModel() {

        ModelRenderable.builder()
                .setSource(this, R.raw.boflogo)
                .build().thenAccept(renderable -> bofLogoRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load bof logo model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

       /*
        ModelRenderable.builder()
                .setSource(this, R.raw.orange_note)
                .build().thenAccept(renderable -> musicNotes[0] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load orange note model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.red_note)
                .build().thenAccept(renderable -> musicNotes[1] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load red note model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );
     */
        ModelRenderable.builder()
                .setSource(this, R.raw.exo_album)
                .build().thenAccept(renderable -> albumRenderable[0] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load albumRenderable 1 model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.nct_model)
                .build().thenAccept(renderable -> albumRenderable[1] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load albumRenderable 2 model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );

        ModelRenderable.builder()
                .setSource(this, R.raw.redvelvet_album)
                .build().thenAccept(renderable -> albumRenderable[2] = renderable)
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load albumRenderable 3 model", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                );
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        // NaverMap ?????? ????????? NaverMap ????????? ?????? ?????? ??????
        mNaverMap = naverMap;

        mNaverMap.setLocationSource(mLocationSource);

        Log.d(TAG, "onMapReady");
        markerList = new ArrayList<Marker>();

        // ?????? ??????
        Marker marker1 = new Marker();
        marker1.setPosition(new LatLng(markers.get(0).getLatitude(), markers.get(0).getLongitude()));
        marker1.setIcon(OverlayImage.fromResource(R.drawable.exologo));
        markerList.add(marker1);

        Marker marker2 = new Marker();
        marker2.setPosition(new LatLng(markers.get(1).getLatitude(), markers.get(1).getLongitude()));
        marker2.setIcon(OverlayImage.fromResource(R.drawable.nctlogo));
        markerList.add(marker2);

        Marker marker3 = new Marker();
        marker3.setPosition(new LatLng(markers.get(2).getLatitude(), markers.get(2).getLongitude()));
        marker3.setIcon(OverlayImage.fromResource(R.drawable.redlogo));
        markerList.add(marker3);

        // ?????? ????????? ??????
        Marker marker4 = new Marker();
        marker4.setPosition(new LatLng(markers.get(3).getLatitude(), markers.get(3).getLongitude()));
        marker4.setIcon(OverlayImage.fromResource(R.drawable.blacklogo));
        markerList.add(marker4);

        Marker marker5 = new Marker();
        marker5.setPosition(new LatLng(markers.get(4).getLatitude(), markers.get(4).getLongitude()));
        marker5.setIcon(OverlayImage.fromResource(R.drawable.btslogo));
        markerList.add(marker5);

        Marker marker6 = new Marker();
        marker6.setPosition(new LatLng(markers.get(5).getLatitude(), markers.get(5).getLongitude()));
        marker6.setIcon(OverlayImage.fromResource(R.drawable.exologo));
        markerList.add(marker6);

        Marker marker7 = new Marker();
        marker7.setPosition(new LatLng(markers.get(6).getLatitude(), markers.get(6).getLongitude()));
        marker7.setIcon(OverlayImage.fromResource(R.drawable.nctlogo));
        markerList.add(marker7);

        Marker marker8 = new Marker();
        marker8.setPosition(new LatLng(markers.get(7).getLatitude(), markers.get(7).getLongitude()));
        marker8.setIcon(OverlayImage.fromResource(R.drawable.redlogo));
        markerList.add(marker8);

        Marker marker9 = new Marker();
        marker9.setPosition(new LatLng(markers.get(8).getLatitude(), markers.get(8).getLongitude()));
        marker9.setIcon(OverlayImage.fromResource(R.drawable.btslogo));
        markerList.add(marker9);

        for(Marker m : markerList){
            m.setHeight(70);
            m.setWidth(60);
            m.setAnchor(new PointF(0.5f, 1));
            m.setIconPerspectiveEnabled(true);
            m.setHideCollidedSymbols(true);
            m.setCaptionMinZoom(14);
            m.setCaptionMaxZoom(17);
            m.setCaptionTextSize(13);
            m.setSubCaptionColor(Color.DKGRAY);
            m.setSubCaptionHaloColor(Color.WHITE);
            m.setSubCaptionTextSize(10);
            m.setMap(naverMap);
        }

        Marker logo = new Marker();
        logo.setPosition(new LatLng(logoLocation.getLatitude(), logoLocation.getLongitude()));
        logo.setHeight(60);
        logo.setWidth(70);
        logo.setIcon(OverlayImage.fromResource(R.drawable.boflogo));
        logo.setAnchor(new PointF(0.5f, 0.5f));
        logo.setIconPerspectiveEnabled(true);
        logo.setCaptionText("?????????");
        logo.setCaptionTextSize(15);
        logo.setMap(naverMap);

        // UI ????????? ?????????
        UiSettings uiSettings = mNaverMap.getUiSettings();
        uiSettings.setCompassEnabled(false); // ????????? : true
        uiSettings.setScaleBarEnabled(false); // ????????? : true
        uiSettings.setZoomControlEnabled(false); // ????????? : true
        uiSettings.setLocationButtonEnabled(false); // ????????? : false
        uiSettings.setLogoGravity(Gravity.LEFT | Gravity.BOTTOM);
        uiSettings.setLogoMargin(0, 0, 0, -5);

        CameraUpdate cameraUpdate = CameraUpdate.zoomTo(15);
        mNaverMap.moveCamera(cameraUpdate);
        mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        mNaverMap.setSymbolScale(0.75f);


        LocationOverlay locationOverlay = mNaverMap.getLocationOverlay();

        locationOverlay.setIconWidth(40);
        locationOverlay.setIconHeight(40);

        locationOverlay.setSubIconWidth(40);
        locationOverlay.setSubIconHeight(40);
        locationOverlay.setSubAnchor(new PointF(0.5f, 0.9f));

        mNaverMap.addOnLocationChangeListener(location ->{
                    mCurrentLocation = location;
                });

        mNaverMap.addOnCameraIdleListener(()->{
            if(!isFullScreen && (mNaverMap.getLocationTrackingMode() == LocationTrackingMode.NoFollow ||
            mNaverMap.getLocationTrackingMode() == LocationTrackingMode.None)){
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        });

        List<ImageView> imageViews = new ArrayList<ImageView>();
        imageViews.add(findViewById(R.id.marker1));
        imageViews.add(findViewById(R.id.marker2));
        imageViews.add(findViewById(R.id.marker3));
        imageViews.add(findViewById(R.id.marker4));
        imageViews.add(findViewById(R.id.marker5));
        imageViews.add(findViewById(R.id.marker6));
        imageViews.add(findViewById(R.id.marker7));
        imageViews.add(findViewById(R.id.marker8));
        imageViews.add(findViewById(R.id.marker9));

        List<ImageView> lines = new ArrayList<ImageView>();
        lines.add(findViewById(R.id.line1));
        lines.add(findViewById(R.id.line2));
        lines.add(findViewById(R.id.line3));
        lines.add(findViewById(R.id.line4));
        lines.add(findViewById(R.id.line5));
        lines.add(findViewById(R.id.line6));
        lines.add(findViewById(R.id.line7));
        lines.add(findViewById(R.id.line8));
        lines.add(findViewById(R.id.line9));

        mNaverMap.setOnMapClickListener((point, coord)->{
            if(!isFullScreen) {
                View mapOverlay = (View) findViewById(R.id.map);
                View mapBorder = (View) findViewById(R.id.mapBorder);
                View mapImg = (View) findViewById(R.id.mapImg);
                View gameUi = (View)findViewById(R.id.game_ui);
                View panel = (View)findViewById(R.id.panel);

                mapWidth = 290;
                mapHeight = 390;
                float borderWidth = 300;
                float borderHeight = 400;
                final float width = 1000 - 70;
                final float height = 1740;
                isFullScreen = true;
                Activity a = this;
                isThreadRunning = true;
                mapImg.setVisibility(View.VISIBLE);
                mapOverlay.setVisibility(View.INVISIBLE);
                mapOverlay.setLayoutParams(new ScalableLayout.LayoutParams(35, 35, width, height));

                int index = musicUiclass.getCurrentMediaPlayerIndex();

                if(index != -1) {
                    ImageView checkCircle = (ImageView) findViewById(R.id.checkCircle);
                    // ?????? ?????? ?????? ?????? ????????????????????? ??????!!
                    float x = imageViews.get(index).getX();
                    float y = imageViews.get(index).getY();
                    float ImgWidth = imageViews.get(index).getWidth();
                    float ImgHeight = imageViews.get(index).getHeight();
                    float widthCircle = checkCircle.getWidth();
                    float heightCircle = checkCircle.getHeight();
                    checkCircle.setX(x + ImgWidth / 2 - widthCircle / 2);
                    checkCircle.setY(y + ImgHeight / 2 - heightCircle / 2);
                    checkCircle.setVisibility(View.VISIBLE);

                    if (index > musicMaxIndex) musicMaxIndex = index;

                    for (int i = 0; i < musicMaxIndex + 1; i++) {
                        if (imageViews.get(i).getColorFilter() != null) {
                            imageViews.get(i).setColorFilter(null);
                            imageViews.get(i).setImageAlpha(256);
                        }
                    }

                    for(int i = 0; i <musicMaxIndex; i++){
                        Drawable d = getResources().getDrawable(R.drawable.line);
                        lines.get(i).setBackground(d);
                    }
                }

                // ????????? ?????? ????????? GRAYSCALE ??????
                for (int i = musicMaxIndex + 1; i < imageViews.size(); i++) {
                    if (imageViews.get(i).getColorFilter() == null) {
                        ColorMatrix matrix = new ColorMatrix();
                        matrix.setSaturation(0);
                        ColorFilter colorFilter = new ColorMatrixColorFilter(matrix);
                        imageViews.get(i).setColorFilter(colorFilter);
                        imageViews.get(i).setImageAlpha(180);
                    }
                }

                // ?????? ?????? ???????????????
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() { // Thread ??? ????????? ????????? ??????
                        final int deltaSecond = 5;
                        final int duration = 75;
                        int time = 0;

                        while(isThreadRunning && time < duration){
                            try {
                                Thread.sleep(deltaSecond);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            time += deltaSecond;

                            int finalTime = time;
                            a.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mapImg.setLayoutParams(new ScalableLayout.LayoutParams(35, 35, (width - mapWidth)/duration * finalTime + mapWidth, (height - mapHeight)/duration * finalTime + mapHeight));
                                    mapBorder.setLayoutParams(new ScalableLayout.LayoutParams(30, 30, (width - borderWidth)/duration * finalTime + borderWidth, (height - borderHeight)/duration * finalTime + borderHeight));
                                    panel.setLayoutParams(new ScalableLayout.LayoutParams((-75 + 250)/duration * finalTime - 250, 0, 200, 1850));
                                }
                            });
                        }

                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                gameUi.setVisibility(View.INVISIBLE);
                                mapBorder.setLayoutParams(new ScalableLayout.LayoutParams(30, 30, width + 10, height + 10));
                                panel.setLayoutParams(new ScalableLayout.LayoutParams(-75, 0, 200, 1850));
                                mapOverlay.setVisibility(View.VISIBLE);
                                mapImg.setVisibility(View.INVISIBLE);

                                path = new PathOverlay();
                                path.setCoords(Arrays.asList(
                                        new LatLng(markers.get(0).getLatitude(), markers.get(0).getLongitude()),
                                        new LatLng(logoLocation.getLatitude(), logoLocation.getLongitude())
                                ));
                                //path.setProgress(0.5);
                                path.setPatternImage(OverlayImage.fromResource(R.drawable.arrow_pattern));
                                path.setPatternInterval(75);
                                path.setColor(Color.parseColor("#3B7BF9"));
                                //path.setPassedColor(Color.GRAY);
                                path.setWidth(20);
                                path.setOutlineWidth(3);
                                path.setOutlineColor(Color.parseColor("#828282"));
                                path.setMap(mNaverMap);

                                mNaverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);

                                CameraPosition cameraPosition = new CameraPosition(
                                        new LatLng(35.161521, 129.160587),
                                        16.2,
                                        0,
                                        -12
                                );
                                mNaverMap.setCameraPosition(cameraPosition);

                                mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, false);

                                marker1.setCaptionText("EXO");
                                marker1.setSubCaptionText("Love Shot");

                                marker2.setCaptionText("NCT127");
                                marker2.setSubCaptionText("Hero");

                                marker3.setCaptionText("????????????");
                                marker3.setSubCaptionText("?????????");

                                marker4.setCaptionText("????????????");
                                marker4.setSubCaptionText("How You Like That");

                                marker5.setCaptionText("???????????????");
                                marker5.setSubCaptionText("DNA");

                                marker6.setCaptionText("EXO");
                                marker6.setSubCaptionText("Love Shot");

                                marker7.setCaptionText("NCT127");
                                marker7.setSubCaptionText("Hero");

                                marker8.setCaptionText("????????????");
                                marker8.setSubCaptionText("?????????");

                                marker9.setCaptionText("???????????????");
                                marker9.setSubCaptionText("DNA");
                            }
                        });
                        isThreadRunning = false;
                    }
                });
                thread.start();

                /*
                Thread cameraThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(isFullScreen){
                            try {
                                Thread.sleep(1);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            a.runOnUiThread(new Runnable(){
                                @Override
                                public void run() {
                                    CameraPosition cameraPosition = new CameraPosition(
                                            new LatLng(35.161521, 129.160587),
                                            16.2,
                                            0,
                                            -12
                                    );
                                    mNaverMap.setCameraPosition(cameraPosition);
                                }
                            });
                        }
                    }
                });
                cameraThread.start();
                 */
            }
    });

        for(int i = 0; i < imageViews.size(); i++){

            int finalI = i;
            imageViews.get(i).setOnClickListener((view)->{
                if(finalI > musicMaxIndex) return; // ?????? ?????? ??????????????? ??????

                ImageView checkCircle = (ImageView)findViewById(R.id.checkCircle);

                // ?????? ?????? ?????? ?????? ????????????????????? ??????!!
                float x = imageViews.get(finalI).getX();
                float y = imageViews.get(finalI).getY();
                float width = imageViews.get(finalI).getWidth();
                float height = imageViews.get(finalI).getHeight();
                float widthCircle = checkCircle.getWidth();
                float heightCircle = checkCircle.getHeight();
                checkCircle.setX(x + width/2 - widthCircle/2);
                checkCircle.setY(y + height/2 - heightCircle/2);
                checkCircle.setVisibility(View.VISIBLE);

                if(musicUiclass.getMediaPlayer(finalI) == null) return; // ????????? ??????

                if (musicUi.getVisibility() == View.INVISIBLE || musicUi.getVisibility() == View.GONE) {
                    musicUi.setVisibility(View.VISIBLE);
                }
                if (musicUiclass.isPlaying(finalI)) {
                    musicUiclass.musicStop();
                }
                else {
                    musicUiclass.musicStop();
                    musicUiclass.setMediaPlayer(finalI);
                    musicUiclass.musicPlay();
                }
            });
        }

        // ????????????. ????????? onRequestPermissionsResult ?????? ????????? ??????
        //ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (mLocationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!mLocationSource.isActivated()) { // ?????? ?????????
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }

            Log.i("?????????: ", " ????????? ??????");
            return;
        }

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrix(rotationMatrix, null, mLastAccelerometer, mLastMagnetometer);

            float[] adjustedRotationMatrix = new float[9];
            SensorManager.remapCoordinateSystem(rotationMatrix, AXIS_X, AXIS_Z, adjustedRotationMatrix);
            float[] orientation = new float[3];
            SensorManager.getOrientation(adjustedRotationMatrix, orientation);

            mCurrentAzim = orientation[0]; // ????????? (?????????)
            mCurrentPitch = orientation[1]; // ??????
            mCurrentRoll = orientation[2]; // ???
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void onSceneUpdate(FrameTime frameTime) {
        // ?????? ????????? ???????????? ???
        if(logoAnchor != null) {
            if (logoAnchor.getAnchor().getTrackingState() != TrackingState.TRACKING
                    && arSceneView.getArFrame().getCamera().getTrackingState() == TrackingState.TRACKING) {
                // Detach the old anchor
                List<Node> children = new ArrayList<>(logoAnchor.getChildren());
                for (Node n : children) {
                    Log.d(TAG, "find node list");
                    if (n instanceof BofLogo) {
                        Log.d(TAG, "removed");
                        logoAnchor.removeChild(n);
                        n.setParent(null);
                    }
                }
                arSceneView.getScene().removeChild(logoAnchor);
                logoAnchor.getAnchor().detach();
                logoAnchor.setParent(null);
                logoAnchor = null;
            }
        }

        for (int i = 0; i < 3; i++) {
            if (mAnchorNode[i] != null) {
                // ???????????? ??????????????? ??????????????? (????????? ????????? ???????????????)
                if (mAnchorNode[i].getAnchor().getTrackingState() != TrackingState.TRACKING
                        && arSceneView.getArFrame().getCamera().getTrackingState() == TrackingState.TRACKING) {
                    // Detach the old anchor
                    List<Node> children = new ArrayList<>(mAnchorNode[i].getChildren());
                    for (Node n : children) {
                        Log.d(TAG, "find node list");
                        if (n instanceof AlbumNode) {
                            Log.d(TAG, "removed");
                            mAnchorNode[i].removeChild(n);
                            n.setParent(null);
                        }
                    }
                    arSceneView.getScene().removeChild(mAnchorNode[i]);
                    mAnchorNode[i].getAnchor().detach();
                    mAnchorNode[i].setParent(null);
                    mAnchorNode[i] = null;
                }
            }
        }

        if(gameSystem != null && logoAnchor != null && mAnchorNode[0] != null && mAnchorNode[1] != null && mAnchorNode[2] != null){
            return;
        }

        if (mCurrentLocation == null) {
            Log.d(TAG, "Location is null");
            return;
        }
        else{
            if(popupLayout == null || popupLayout.getParent() != null)
                ((ViewManager)popupLayout.getParent()).removeView(popupLayout);
        }

        if (bofLogoRenderable == null) {
            Log.d(TAG, "onUpdate: bof logo Renderable is null");
            return;
        }

        /*
        for (ModelRenderable m : musicNotes) {
            if (m == null) {
                Log.d(TAG, "onUpdate: musicNotes Renderable is null");
                return;
            }
        }
         */

        for (ModelRenderable m : albumRenderable) {
            if (m == null) {
                Log.d(TAG, "onUpdate: album Renderable is null");
                return;
            }
        }

        if (arSceneView.getArFrame() == null) {
            Log.d(TAG, "onUpdate: No frame available");
            // No frame available
            return;
        }

        if (arSceneView.getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
            Log.d(TAG, "onUpdate: Tracking not started yet");
            // Tracking not started yet
            return;
        }

        // ???????????? ??????!
        for (int i = 0; i < 3; i++) {
            if (mAnchorNode[i] == null) {
                //Log.d(TAG, "onUpdate: mAnchorNode["+ i +"] is null");
                // ??????????????? ??? gps??? ?????? ??????, ???????????? ?????? ????????? ???????????? ???????????? Pose??? ????????????!
                if (createNode(i) == false) continue;
            }
        }
        // BOF ?????? ???????????? ??????
        if(logoAnchor == null){
            createLogo();
        }

        if(gameSystem == null) {
            // ?????? ????????? ??????
            gameSystem = new GameSystem(this, arSceneView, musicUiclass, findViewById(R.id.score), findViewById(R.id.scoreText));
            musicUiclass.setGameSystem(gameSystem);
            Log.i("GameSystem create: ", "true");
        }
    }

    public void createLogo(){
        float dLatitude = (float) (logoLocation.getLatitude() - mCurrentLocation.getLatitude()) * 110900f;
        float dLongitude = (float) (logoLocation.getLongitude() - mCurrentLocation.getLongitude()) * 88400f;

        // ????????? ??????


        dLatitude = 20f;
        dLongitude = 0f;


        float distance = (float) Math.sqrt((dLongitude * dLongitude) + (dLatitude * dLatitude));

        if (distance > 25) { // 25m?????? ?????? ???????????? ??????X
            return;
        }

        float height = 5f;
        Vector3 objVec = new Vector3(dLongitude, dLatitude, height);

        Vector3 xUnitVec;
        Vector3 yUnitVec;
        Vector3 zUnitVec;

        zUnitVec = new Vector3((float) (Math.cos(mCurrentPitch) * Math.sin(mCurrentAzim)), (float) (Math.cos(mCurrentPitch) * Math.cos(mCurrentAzim)), (float) (-Math.sin(mCurrentPitch)));
        zUnitVec = zUnitVec.normalized().negated();

        yUnitVec = new Vector3((float) (Math.sin(mCurrentPitch) * Math.sin(mCurrentAzim)), (float) (Math.sin(mCurrentPitch) * Math.cos(mCurrentAzim)), (float) (Math.cos(mCurrentPitch))).normalized();

        float wx = zUnitVec.x;
        float wy = zUnitVec.y;
        float wz = zUnitVec.z;

        float yx = yUnitVec.x;
        float yy = yUnitVec.y;
        float yz = yUnitVec.z;

        float t = 1 - (float) Math.cos(mCurrentRoll);
        float s = (float) Math.sin(mCurrentRoll);
        float c = (float) Math.cos(mCurrentRoll);

        float[][] rotMat = {{wx * wx * t + c, wx * wy * t + wz * s, wx * wz * t - wy * s},
                {wy * wx * t - wz * s, wy * wy * t + c, wy * wz * t + wx * s},
                {wz * wx * t + wy * s, wz * wy * t - wx * s, wz * wz * t + c}};

        yUnitVec = new Vector3(yx * rotMat[0][0] + yy * rotMat[0][1] + yz * rotMat[0][2],
                yx * rotMat[1][0] + yy * rotMat[1][1] + yz * rotMat[1][2],
                yx * rotMat[2][0] + yy * rotMat[2][1] + yz * rotMat[2][2]).normalized();


        xUnitVec = Vector3.cross(yUnitVec, zUnitVec).normalized();

        float xPos = Vector3.dot(objVec, xUnitVec);
        float yPos = Vector3.dot(objVec, yUnitVec);
        float zPos = Vector3.dot(objVec, zUnitVec);

        Vector3 xAxis = arSceneView.getScene().getCamera().getRight().normalized().scaled(xPos);
        Vector3 yAxis = arSceneView.getScene().getCamera().getUp().normalized().scaled(yPos);
        Vector3 zAxis = arSceneView.getScene().getCamera().getBack().normalized().scaled(zPos);
        Vector3 objectPos = new Vector3(xAxis.x + yAxis.x + zAxis.x, xAxis.y + yAxis.y + zAxis.y, xAxis.z + yAxis.z + zAxis.z);
        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();

        Vector3 position = Vector3.add(cameraPos, objectPos);

        // Create an ARCore Anchor at the position.
        Pose pose = Pose.makeTranslation(position.x, position.y, position.z);
        Anchor anchor = arSceneView.getSession().createAnchor(pose);

        logoAnchor = new AnchorNode(anchor);
        logoAnchor.setParent(arSceneView.getScene());

        // ???????????? ????????? ????????????
        Vector3 v = new Vector3(0f, 0f, 1f);
        xPos = Vector3.dot(v, xUnitVec);
        yPos = Vector3.dot(v, yUnitVec);
        zPos = Vector3.dot(v, zUnitVec);

        xAxis = arSceneView.getScene().getCamera().getRight().normalized().scaled(xPos);
        yAxis = arSceneView.getScene().getCamera().getUp().normalized().scaled(yPos);
        zAxis = arSceneView.getScene().getCamera().getBack().normalized().scaled(zPos);

        Vector3 up = new Vector3(xAxis.x + yAxis.x + zAxis.x, xAxis.y + yAxis.y + zAxis.y, xAxis.z + yAxis.z + zAxis.z).normalized();


        BofLogo bofLogo = new BofLogo(logoAnchor, bofLogoRenderable, arSceneView);


        bofLogo.setOnTapListener(new Node.OnTapListener() {
            @Override
            public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                Intent intent = new Intent(getApplicationContext(), PopupActivity2.class);
                String scoreString2 = gameSystem.currentScore + " ???";
                intent.putExtra("Score", scoreString2);
                startActivity(intent);
            }
        });



        Snackbar.make(mLayout, "????????? ????????? ?????????????????? (distance: " + distance + "m)", Snackbar.LENGTH_SHORT).show();
    }

    // ?????? ?????? ??????~!
    public boolean createNode(int i) {
        float dLatitude = (float) (markers.get(i).getLatitude() - mCurrentLocation.getLatitude()) * 110900f;
        float dLongitude = (float) (markers.get(i).getLongitude() - mCurrentLocation.getLongitude()) * 88400f;

        // ????????? ??????

        if( i == 0 ) {
            dLatitude = 3f;
            dLongitude = 0f;
        }
        else if ( i == 1 ){
            dLatitude = -3f;
            dLongitude = 0f;
        }
        else{
            dLatitude = 0f;
            dLongitude = 3f;
        }

        float distance = (float) Math.sqrt((dLongitude * dLongitude) + (dLatitude * dLatitude));

        if (distance > 15) { // 15m?????? ?????? ???????????? ??????X
            return false;
        }
        float height = -0.5f;
        Vector3 objVec = new Vector3(dLongitude, dLatitude, height);

        Vector3 xUnitVec;
        Vector3 yUnitVec;
        Vector3 zUnitVec;

        zUnitVec = new Vector3((float) (Math.cos(mCurrentPitch) * Math.sin(mCurrentAzim)), (float) (Math.cos(mCurrentPitch) * Math.cos(mCurrentAzim)), (float) (-Math.sin(mCurrentPitch)));
        zUnitVec = zUnitVec.normalized().negated();

        yUnitVec = new Vector3((float) (Math.sin(mCurrentPitch) * Math.sin(mCurrentAzim)), (float) (Math.sin(mCurrentPitch) * Math.cos(mCurrentAzim)), (float) (Math.cos(mCurrentPitch))).normalized();

        float wx = zUnitVec.x;
        float wy = zUnitVec.y;
        float wz = zUnitVec.z;

        float yx = yUnitVec.x;
        float yy = yUnitVec.y;
        float yz = yUnitVec.z;

        float t = 1 - (float) Math.cos(mCurrentRoll);
        float s = (float) Math.sin(mCurrentRoll);
        float c = (float) Math.cos(mCurrentRoll);

        float[][] rotMat = {{wx * wx * t + c, wx * wy * t + wz * s, wx * wz * t - wy * s},
                {wy * wx * t - wz * s, wy * wy * t + c, wy * wz * t + wx * s},
                {wz * wx * t + wy * s, wz * wy * t - wx * s, wz * wz * t + c}};

        yUnitVec = new Vector3(yx * rotMat[0][0] + yy * rotMat[0][1] + yz * rotMat[0][2],
                yx * rotMat[1][0] + yy * rotMat[1][1] + yz * rotMat[1][2],
                yx * rotMat[2][0] + yy * rotMat[2][1] + yz * rotMat[2][2]).normalized();


        xUnitVec = Vector3.cross(yUnitVec, zUnitVec).normalized();

        float xPos = Vector3.dot(objVec, xUnitVec);
        float yPos = Vector3.dot(objVec, yUnitVec);
        float zPos = Vector3.dot(objVec, zUnitVec);

        Vector3 xAxis = arSceneView.getScene().getCamera().getRight().normalized().scaled(xPos);
        Vector3 yAxis = arSceneView.getScene().getCamera().getUp().normalized().scaled(yPos);
        Vector3 zAxis = arSceneView.getScene().getCamera().getBack().normalized().scaled(zPos);
        Vector3 objectPos = new Vector3(xAxis.x + yAxis.x + zAxis.x, xAxis.y + yAxis.y + zAxis.y, xAxis.z + yAxis.z + zAxis.z);
        Vector3 cameraPos = arSceneView.getScene().getCamera().getWorldPosition();

        Vector3 position = Vector3.add(cameraPos, objectPos);

        // Create an ARCore Anchor at the position.
        Pose pose = Pose.makeTranslation(position.x, position.y, position.z);
        Anchor anchor = arSceneView.getSession().createAnchor(pose);

        mAnchorNode[i] = new AnchorNode(anchor);
        mAnchorNode[i].setParent(arSceneView.getScene());

        // ???????????? ????????? ????????????
        Vector3 v = new Vector3(0f, 0f, 1f);
        xPos = Vector3.dot(v, xUnitVec);
        yPos = Vector3.dot(v, yUnitVec);
        zPos = Vector3.dot(v, zUnitVec);

        xAxis = arSceneView.getScene().getCamera().getRight().normalized().scaled(xPos);
        yAxis = arSceneView.getScene().getCamera().getUp().normalized().scaled(yPos);
        zAxis = arSceneView.getScene().getCamera().getBack().normalized().scaled(zPos);

        Vector3 up = new Vector3(xAxis.x + yAxis.x + zAxis.x, xAxis.y + yAxis.y + zAxis.y, xAxis.z + yAxis.z + zAxis.z).normalized();

        AlbumNode albumNode = new AlbumNode(mAnchorNode[i], albumRenderable[i],
                musicUiclass.getMediaPlayer(i), arSceneView);
        music(albumNode, i);

        Snackbar.make(mLayout, "????????? ????????? ???????????? (distance: " + distance + "m)", Snackbar.LENGTH_SHORT).show();

        return true;
    }

    public void music(AlbumNode albumNode, int i) {
        Context c = this;

        albumNode.setOnTapListener((v, event) -> {
            /* gps??? ????????? ??????
            float dLatitude = (float) (markers[i].getLatitude() - mCurrentLocation.getLatitude()) * 110900f;
            float dLongitude = (float) (markers[i].getLongitude() - mCurrentLocation.getLongitude()) * 88400f;
            float distance = (float) Math.sqrt((dLongitude * dLongitude) + (dLatitude * dLatitude));
            */

            // AR????????? world position??? ????????? ??????
            Vector3 vec = Vector3.subtract(albumNode.getWorldPosition(), arSceneView.getScene().getCamera().getWorldPosition());
            float distance = (float) Math.sqrt(Vector3.dot(vec, vec));

            // ????????? ?????????????????? ????????? 20m?????? ????????? ?????? ??????
            if (distance <= 20f) {
                if (musicUi.getVisibility() == View.INVISIBLE || musicUi.getVisibility() == View.GONE) {
                    musicUi.setVisibility(View.VISIBLE);
                }

                if (musicUiclass.isPlaying(i)) {
                    musicUiclass.musicStop();
                    albumNode.stopGame();
                    Snackbar.make(mLayout, "music stop (??????: " + distance + "m)", Snackbar.LENGTH_SHORT).show();
                }
                else {
                    musicUiclass.musicStop();
                    musicUiclass.setMediaPlayer(i);
                    musicUiclass.musicPlay();
                    albumNode.startGame();
                    Snackbar.make(mLayout, "music start (??????: " + distance + "m)", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static float getMinScreenSize(){
        return Math.min(arSceneView.getWidth(), arSceneView.getHeight());
    }

    public static float getWidth(){
        return arSceneView.getWidth();
    }

    public static float getHeight(){
        return arSceneView.getHeight();
    }
}