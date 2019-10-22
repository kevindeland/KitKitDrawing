package com.enuma.drawingcoloring.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.enuma.drawingcoloring.brush.Crayon;
import com.enuma.drawingcoloring.brush.IBrush;
import com.enuma.drawingcoloring.core.Const;
import com.enuma.drawingcoloring.types.KPoint;
import com.enuma.drawingcoloring.utility.Log;
import com.enuma.drawingcoloring.utility.Util;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * drawing
 * <p>
 * Created by kevindeland on 2019-10-17.
 */
public class ViewGleaphDisplay extends View {

    List<KPoint> _myPath;
    private Gson _gson = new Gson();

    private Context mContext;

    /** Double Buffer */
    private Bitmap mBitmapBuffer;
    private Canvas mCanvasBuffer;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Rect mRect = new Rect();
    private boolean mbInit = false;

    List<KPoint> mPath;

    IBrush mCrayon;

    public ViewGleaphDisplay(Context context) {
        super(context);
        init(context);
    }

    public ViewGleaphDisplay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ViewGleaphDisplay(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        // do a bunch of stuff
        // initialize things
        Point size = Util.getWindowSize((Activity)context);
        boolean isSmallLCD = (size.x <= 1280);
        // load a thing

        mCrayon = new Crayon(mContext, isSmallLCD);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode()) {
            Log.i("DRAWME", "isInEditMode() == true");
            return;
        }

        if (mBitmapBuffer == null) {
            mBitmapBuffer = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            mCanvasBuffer = new Canvas(mBitmapBuffer);
            mBitmapBuffer.eraseColor(Color.TRANSPARENT);
            mRect.set(0, 0, getWidth(), getHeight());
            mbInit = true;
        }

        canvas.drawBitmap(mBitmapBuffer, 0, 0, mPaint);
    }

    public void loadJsonGleaphIntoMe() {
        // look it up using the code
        // use drawLineWithBrush

        try {
            File folder = new File(Const.SAVE_GLEAPH_PATH);

            for (final File file : folder.listFiles()) {

                loadJsonGleaphIntoMe(file);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void loadJsonGleaphIntoMe(File file) throws FileNotFoundException {
        BufferedReader br = new BufferedReader(
                new FileReader(file)
        );
        List<KPoint> x = _gson.fromJson(br, new TypeToken<List<KPoint>>() {
        }.getType());


        List<KPoint> newList = scaleGleaphToZero(x);
        mPath = newList;

        Log.i("DRAWME", "drawing " + x.size() + " for " + x.toString());

        for (int i = 1; i < newList.size(); i++) {
            Log.v("DRAWME", "Drawing a thing");
            KPoint lastPoint = newList.get(i - 1);
            KPoint nextPoint = newList.get(i);
            mCrayon.drawLineWithBrush(mCanvasBuffer,
                    lastPoint.x, lastPoint.y,
                    nextPoint.x, nextPoint.y);
        }
    }

    public List<KPoint> getPath() {
        return mPath;
    }

    private List<KPoint> scaleGleaphToZero(List<KPoint> gleaph) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = 0, maxY = 0;

        for (KPoint point : gleaph) {
            if (point.x < minX) {
                minX = point.x;
            }
            if (point.y < minY) {
                minY = point.y;
            }

            if (point.x > maxX) {
                maxX = point.x;
            }
            if (point.y > maxY) {
                maxY = point.y;
            }
        }

        int X_DIFF = maxX - minX;
        int Y_DIFF = maxY - minY;


        List<KPoint> newList = new ArrayList<>();
        for (KPoint point : gleaph) {
            newList.add(new KPoint(point.x - minX, point.y - minY));
        }

        return newList;
    }
}
