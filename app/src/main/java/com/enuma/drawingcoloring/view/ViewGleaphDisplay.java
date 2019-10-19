package com.enuma.drawingcoloring.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.enuma.drawingcoloring.R;
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
    /** 실제 사용하는 Brush 이미지 */
    private Bitmap mBitmapBrush;
    /** 원본 Brush Alpha 채널 이미지 */
    private Bitmap mBitmapBrushAlphaChannel;

    /** Double Buffer */
    private Bitmap mBitmapBuffer;
    private Canvas mCanvasBuffer;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Paint mPaintDrawing = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Rect mRect = new Rect();
    private boolean mbInit = false;


    /**
     * Brush 이미지의 수평 갯수
     */
    private final int BRUSH_WIDTH_COUNT = 5;

    /**
     * Brush 이미지의 수직 갯수
     */
    private final int BRUSH_HEIGHT_COUNT = 2;

    /**
     * Brush 이미지의 Brush 갯수
     */
    private final int BRUSH_COUNT = BRUSH_WIDTH_COUNT * BRUSH_HEIGHT_COUNT;

    /**
     * 하나의 Brush Image width
     */
    private int BRUSH_POINT_WIDTH = 0;

    /**
     * 하나의 Brush Image height
     */
    private int BRUSH_POINT_HEIGHT = 0;

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
        mBitmapBrushAlphaChannel = BitmapFactory.decodeResource(mContext.getResources(), isSmallLCD ? R.drawable.crayon_brush_alpha_s : R.drawable.crayon_brush_alpha);
        mBitmapBrush = Bitmap.createBitmap(mBitmapBrushAlphaChannel.getWidth(), mBitmapBrushAlphaChannel.getHeight(), Bitmap.Config.ARGB_8888);
        BRUSH_POINT_WIDTH = mBitmapBrushAlphaChannel.getWidth() / BRUSH_WIDTH_COUNT;
        BRUSH_POINT_HEIGHT = mBitmapBrushAlphaChannel.getHeight() / BRUSH_HEIGHT_COUNT;

        Log.i("DRAWME", "init -- WIDTH/HEIGHT=" + BRUSH_POINT_WIDTH + " " + BRUSH_POINT_HEIGHT);

        /// this is nuts
        mBitmapBrush.eraseColor(ContextCompat.getColor(mContext, Util.getResourceId(mContext, "color_1", "color", mContext.getPackageName())));
        Canvas canvas = new Canvas(mBitmapBrush);
        Paint paint = new Paint(mPaintDrawing);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(mBitmapBrushAlphaChannel, 0, 0, paint);
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

    private void drawLineWithBrush(Canvas canvas, int startX, int startY, int endX, int endY) {
        int distance = (int) Util.getDistanceBetween2Point(startX, startY, endX, endY);
        double angle = Util.getRadianAngleBetween2Point(startX, startY, endX, endY);

        int halfBrushWidth = BRUSH_POINT_WIDTH / 2;
        int halfBrushHeight = BRUSH_POINT_HEIGHT / 2;
        int x, y;

        int offset = halfBrushWidth / 2;

        Log.i("DRAWME", "distance=" + distance + "; offset=" + offset);
        for (int i = 0; i <= distance; i += offset) {
            Log.v("DRAWME", "Drawing a thing");
            x = (int) (startX + (Math.sin(angle) * i) - halfBrushWidth);
            y = (int) (startY + (Math.cos(angle) * i) - halfBrushHeight);
            //canvas.drawLine(startX, startY, x, y, mPaintDrawing);
            canvas.drawBitmap(getBrushPointBitmap(), x, y, mPaintDrawing);
        }
    }


    /**
     * Random 한 Brush Point Image
     *
     * @return
     */
    private Bitmap getBrushPointBitmap() {
        Bitmap result;

        int rand = (int) (Math.random() * BRUSH_COUNT);
        result = Bitmap.createBitmap(mBitmapBrush,
                (rand % BRUSH_WIDTH_COUNT) * BRUSH_POINT_WIDTH,
                (rand / BRUSH_WIDTH_COUNT) * BRUSH_POINT_HEIGHT,
                BRUSH_POINT_WIDTH,
                BRUSH_POINT_HEIGHT);

        return result;
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

        Log.i("DRAWME", "drawing " + x.size() + " for " + x.toString());

        for (int i = 1; i < newList.size(); i++) {
            Log.v("DRAWME", "Drawing a thing");
            KPoint lastPoint = newList.get(i - 1);
            KPoint nextPoint = newList.get(i);
            drawLineWithBrush(mCanvasBuffer,
                    lastPoint.x, lastPoint.y,
                    nextPoint.x, nextPoint.y);
        }
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
