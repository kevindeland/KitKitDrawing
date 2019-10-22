package com.enuma.drawingcoloring.brush;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import com.enuma.drawingcoloring.R;
import com.enuma.drawingcoloring.utility.Util;

/**
 * Crayon
 * <p>Any code related to the actual drawing of a brush/line has been separated into this class.</p>
 * Created by kevindeland on 2019-10-22.
 */
public class Crayon implements IBrush {

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

    /** 원본 Brush Alpha 채널 이미지 */
    private Bitmap mBitmapBrushAlphaChannel;

    /** 실제 사용하는 Brush 이미지 */
    private Bitmap mBitmapBrush;

    private int mCurrentColor;

    private Paint mPaintDrawing = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Paint mPaintColoring = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);


    public Crayon(Context context, boolean isSmallLCD) {
        mBitmapBrushAlphaChannel = BitmapFactory.decodeResource(context.getResources(), isSmallLCD ? R.drawable.crayon_brush_alpha_s : R.drawable.crayon_brush_alpha);
        mBitmapBrush = Bitmap.createBitmap(mBitmapBrushAlphaChannel.getWidth(), mBitmapBrushAlphaChannel.getHeight(), Bitmap.Config.ARGB_8888);
        BRUSH_POINT_WIDTH = mBitmapBrushAlphaChannel.getWidth() / BRUSH_WIDTH_COUNT;
        BRUSH_POINT_HEIGHT = mBitmapBrushAlphaChannel.getHeight() / BRUSH_HEIGHT_COUNT;

        mPaintColoring.setStyle(Paint.Style.STROKE);
        mPaintColoring.setStrokeJoin(Paint.Join.ROUND);
        mPaintColoring.setStrokeCap(Paint.Cap.ROUND);
        mPaintColoring.setStrokeWidth(BRUSH_POINT_WIDTH / 3.0f * 2);
    }

    public void setPenColor(int color) {
        mCurrentColor = color;

//        if (mMode == MODE.DRAWING) {
//            mBitmapBrush.eraseColor(color);
//            Canvas canvas = new Canvas(mBitmapBrush);
//            Paint paint = new Paint(mPaintDrawing);
//            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
//            canvas.drawBitmap(mBitmapBrushAlphaChannel, 0, 0, paint);
//
//        } else {
//            mPaintColoring.setColor(color);
//
//        }

        mBitmapBrush.eraseColor(color);
        Canvas canvas = new Canvas(mBitmapBrush);
        Paint paint = new Paint(mPaintDrawing);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(mBitmapBrushAlphaChannel, 0, 0, paint);
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


    /**
     * 크레용 질감의 Line Drawing
     *
     * draw line between [startX, startY], and [endX, endY]
     */
    public void drawLineWithBrush(Canvas canvas, int startX, int startY, int endX, int endY) {
        int distance = (int) Util.getDistanceBetween2Point(startX, startY, endX, endY);
        double angle = Util.getRadianAngleBetween2Point(startX, startY, endX, endY);
        double angleReverse = angle - Math.toRadians(180); // duh...

        int halfBrushWidth = BRUSH_POINT_WIDTH / 2;
        int halfBrushHeight = BRUSH_POINT_WIDTH / 2;
        int x, y;

        int offset = halfBrushWidth / 2;

        for (int i = 0; i <= distance; i += offset) {
            x = (int) (startX + (Math.sin(angle) * i) - halfBrushWidth);
            y = (int) (startY + (Math.cos(angle) * i) - halfBrushHeight);
            canvas.drawBitmap(getBrushPointBitmap(), x, y, mPaintDrawing);
        }
    }

}
