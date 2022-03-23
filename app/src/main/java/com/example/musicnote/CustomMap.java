package com.example.musicnote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

public class CustomMap extends com.naver.maps.map.MapView{

    RectF rectF = new RectF();
    private int cornerRadiusX = 50;
    private int cornerRadiusY = 50;

    private int mapWidth = 120;
    private int mapHeight = 164;

    public CustomMap(@NonNull Context context) {
        super(context);
    }

    public CustomMap(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public CustomMap(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        rectF.set(0, 0, mapWidth, mapHeight);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        Path path = new Path();
        int count = canvas.save();

        path.addRoundRect(rectF, cornerRadiusX, cornerRadiusY, Path.Direction.CW);

        canvas.clipPath(path);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(count);
    }

}
