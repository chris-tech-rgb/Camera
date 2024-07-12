package com.example.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class CropImageView extends View {
    private Paint paint;
    private Rect rect;
    private Point startPoint, endPoint;

    public CropImageView(Context context) {
        super(context);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(android.graphics.Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        rect = new Rect();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rect, paint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startPoint = new Point((int) event.getX(), (int) event.getY());
                endPoint = new Point((int) event.getX(), (int) event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                endPoint = new Point((int) event.getX(), (int) event.getY());
                updateRect();
                invalidate();
                break;
        }
        return true;
    }

    private void updateRect() {
        rect.left = Math.min(startPoint.x, endPoint.x);
        rect.top = Math.min(startPoint.y, endPoint.y);
        rect.right = Math.max(startPoint.x, endPoint.x);
        rect.bottom = Math.max(startPoint.y, endPoint.y);
    }

    public Rect getSelectedRect() {
        return rect;
    }

    public void clearSelection() {
        rect.setEmpty(); // Reset the rectangle
        invalidate(); // Redraw the view to clear the selection
    }
}
