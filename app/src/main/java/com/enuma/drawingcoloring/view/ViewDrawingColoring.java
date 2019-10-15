package com.enuma.drawingcoloring.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.enuma.drawingcoloring.R;
import com.enuma.drawingcoloring.activity.GleaphHolder;
import com.enuma.drawingcoloring.types.UnitVector;
import com.enuma.drawingcoloring.utility.Log;
import com.enuma.drawingcoloring.utility.Misc;
import com.enuma.drawingcoloring.utility.Util;

import java.io.File;
import java.util.ArrayList;

/**
 * MODE.DRAWING : 크레용 질감 표현 (Brush Image (5 * 2) 이용)
 * MODE.COLORING : 2017.09.12 DRAWING 과 동일하게 변경 (이전 일반 Path 표현)
 * 임시 작업 파일의 Save, Restore, Delete 기능 제공
 */
public class ViewDrawingColoring extends View {

    ////////////////////////////////////////////////////////////////////////////////

    public enum MODE {
        DRAWING,
        COLORING
    }

    public enum RADIAL_MODE {
        SINGLE,
        RADIAL_2,
        RADIAL_8
    }

    public enum PARALLEL_MODE {
        DEFAULT,
        PLACE,
        DRAW
    }

    public enum VECTOR_MODE {
        OFF,
        VECTOR
    }

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

    /**
     * Touch 의 Move Event 가 TOUCH_TOLERANCE 이내로 움직이면 무시
     */
    private static final float TOUCH_TOLERANCE = 4;

    ////////////////////////////////////////////////////////////////////////////////

    private Context mContext;

    ////////////////////////////////////////////////////////////////////////////////

    /** 원본 Brush Alpha 채널 이미지 */
    private Bitmap mBitmapBrushAlphaChannel;

    /** 실제 사용하는 Brush 이미지 */
    private Bitmap mBitmapBrush;

    /** Double Buffer */
    private Bitmap mBitmapBuffer;
    private Canvas mCanvasBuffer;

    ////////////////////////////////////////////////////////////////////////////////

    private Callback mCallback;
    private MODE mMode = MODE.DRAWING;
    private RADIAL_MODE mRadialMode = RADIAL_MODE.SINGLE;
    private int mCurrentColor;
    private int mTouchId;
    private float mTouchOriginX, mTouchOriginY;
    private float mTouchPosX, mTouchPosY;
    private float mTouchRevX, mTouchRevY;
    private float[] mTouchAngleX, mTouchAngleY;


    // variables for parallel mode
    private PARALLEL_MODE mParellelMode;
    private Point[] mParallelPoints; // the origins for all parallel sources...
    private int mParallelNumPoints;
    private int mParallelRootReference = -1; // that last one placed


    // variables for VectorMode
    private VECTOR_MODE mVectorMode = VECTOR_MODE.VECTOR;
    private UnitVector mCurrentVector;
    private ArrayList<UnitVector> mAllVectors = new ArrayList<>();

    /** 일반적으로 사용 Paint */
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);

    private Paint mPaintDrawing = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Paint mPaintColoring = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);

    private Path mPathColoring = new Path();
    private Rect mRect = new Rect();
    private boolean mbInit = false;
    ////////////////////////////////////////////////////////////////////////////////

    private GleaphHolder mGleaphHolder;
    ////////////////////////////////////////////////////

    public ViewDrawingColoring(Context context) {
        super(context);
        init(context);
    }

    public ViewDrawingColoring(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ViewDrawingColoring(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mTouchPosX = Float.MIN_VALUE;
        mTouchPosY = Float.MIN_VALUE;

        Point size = Util.getWindowSize((Activity)context);
        boolean isSmallLCD = (size.x <= 1280);

        mBitmapBrushAlphaChannel = BitmapFactory.decodeResource(mContext.getResources(), isSmallLCD ? R.drawable.crayon_brush_alpha_s : R.drawable.crayon_brush_alpha);
        mBitmapBrush = Bitmap.createBitmap(mBitmapBrushAlphaChannel.getWidth(), mBitmapBrushAlphaChannel.getHeight(), Bitmap.Config.ARGB_8888);
        BRUSH_POINT_WIDTH = mBitmapBrushAlphaChannel.getWidth() / BRUSH_WIDTH_COUNT;
        BRUSH_POINT_HEIGHT = mBitmapBrushAlphaChannel.getHeight() / BRUSH_HEIGHT_COUNT;

        mPaintColoring.setStyle(Paint.Style.STROKE);
        mPaintColoring.setStrokeJoin(Paint.Join.ROUND);
        mPaintColoring.setStrokeCap(Paint.Cap.ROUND);
        mPaintColoring.setStrokeWidth(BRUSH_POINT_WIDTH / 3.0f * 2);


        // MODE_PARALLEL initialize defaults for testing
        mParellelMode = PARALLEL_MODE.DEFAULT;
        mParallelPoints = new Point[10];
    }

    public void setGleaphHolder (GleaphHolder holder)  {
        mGleaphHolder = holder;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode() == true) {
            return;
        }

        if (mBitmapBuffer == null) {
            mBitmapBuffer = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            mCanvasBuffer = new Canvas(mBitmapBuffer);
            mBitmapBuffer.eraseColor(Color.TRANSPARENT);
            mRect.set(0, 0, getWidth(), getHeight());
            restoreTempBitmapFile();
            mbInit = true;
        }

        canvas.drawBitmap(mBitmapBuffer, 0, 0, mPaint);

//        if (mMode == MODE.COLORING) {
//            if (mPathColoring.isEmpty() == false) {
//                canvas.drawPath(mPathColoring, mPaintColoring);
//            }
//        }
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        float x;
//        float y;
//
//        switch (event.getAction() & MotionEvent.ACTION_MASK) {
//            case MotionEvent.ACTION_DOWN:
//                mTouchId = event.getPointerId(0);
//                x = event.getX(event.findPointerIndex(mTouchId));
//                y = event.getY(event.findPointerIndex(mTouchId));
//
//                doTouchDownBrush(x, y);
//                invalidate();
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                if (event.findPointerIndex(mTouchId) != -1) {
//                    x = event.getX(event.findPointerIndex(mTouchId));
//                    y = event.getY(event.findPointerIndex(mTouchId));
//                    doTouchMoveBrush(x, y);
//                }
//                invalidate();
//                break;
//
//            case MotionEvent.ACTION_UP:
//                if (event.findPointerIndex(mTouchId) != -1) {
//                    x = event.getX(event.findPointerIndex(mTouchId));
//                    y = event.getY(event.findPointerIndex(mTouchId));
//                    doTouchUpBrush(x, y);
//                }
//                invalidate();
//                break;
//        }
//
//        return true;
//    }

    ////////////////////////////////////////////////////////////////////////////////


    private void doTouchDownBrush(float x, float y) {
        mTouchPosX = x;
        mTouchPosY = y;

        mTouchOriginX = x;
        mTouchOriginY = y;

        mTouchRevX =x;
        mTouchRevY = y;

        mTouchAngleX = new float[]{x, x, x, x, x, x, x}; // 7 allows for 8 total
        mTouchAngleY = new float[]{y, y, y, y, y, y, y}; // 7 allows for 8 total

//        if (mMode == MODE.DRAWING) {
//            drawLineWithBrush(mCanvasBuffer, (int) mTouchPosX, (int) mTouchPosY, (int) mTouchPosX, (int) mTouchPosY);
//
//        } else {
//            mPathColoring.moveTo(mTouchPosX, mTouchPosY);
//
//        }

        drawLineWithBrush(mCanvasBuffer, (int) mTouchPosX, (int) mTouchPosY, (int) mTouchPosX, (int) mTouchPosY);

        if (mCallback != null) {
            mCallback.onTouchDownForDrawing();
        }
    }

    @SuppressLint("DefaultLocale")
    private void doTouchMoveBrush(float x, float y) {
        if (mTouchPosX == Float.MIN_VALUE || mTouchPosY == Float.MIN_VALUE) {
            return;
        }

        String LINE_TAG = "WALK_LINE";

        float vdx = Math.abs(x - mTouchPosX); // how much x has changed since last event
        float vdy = Math.abs(y - mTouchPosY); // how much y has changed since last event

        float dx = x - mTouchPosX; // need these for symmetry
        float dy = y - mTouchPosY;

        double distance = Util.getDistanceBetween2Point((int) mTouchPosX, (int) mTouchPosY, (int) x, (int) y);
        double angle = Util.getRadianAngleBetween2Point((int) (mTouchPosX * 1000),
                (int) (mTouchPosY * 1000), (int) (x * 1000), (int) (y * 1000));
        double angleReverse = angle - Math.toRadians(180);

        // This was useful for debugging
        Log.v(LINE_TAG, String.format("Ø=%f; dx=%f; dy=%f; rcosØ=%f; rsinØ=%f", angle, dx, dy, distance*Math.sin(angle), distance*Math.cos(angle)));
        Log.v(LINE_TAG, String.format("mTouchPosX=%f; mTouchPosY=%f; x=%f; y=%f", mTouchPosX, mTouchPosY, x, y));
        Log.v(LINE_TAG, String.format("dx=%f; dy=%f", dx, dy));
        Log.v(LINE_TAG, String.format("Ø=%f, -dx=%f; -dy=%f; rcos(Ø-180)=%f; rsin(Ø-180)=%f", angleReverse, -dx, -dy,
                distance*Math.sin(angleReverse), distance*Math.cos(angleReverse)));


        if (vdx >= TOUCH_TOLERANCE || vdy >= TOUCH_TOLERANCE) {

//            if (mMode == MODE.DRAWING) {
//                drawLineWithBrush(mCanvasBuffer, (int) mTouchPosX, (int) mTouchPosY, (int) x, (int) y);
//
//            } else {
//                mPathColoring.quadTo(mTouchPosX, mTouchPosY, (x + mTouchPosX) / 2, (y + mTouchPosY) / 2);
//
//            }

            Log.v(LINE_TAG, String.format("Drawing from [%d, %d] to [%d, %d]",
                    (int) mTouchPosX, (int) mTouchPosY, (int) x, (int) y));


            if (mParellelMode == PARALLEL_MODE.DRAW) {

                if (mVectorMode == VECTOR_MODE.OFF) {
                    Log.i("PARALLEL", String.format("NumPoints: %d; Root: %d", mParallelNumPoints, mParallelRootReference));
                    Point referenceOrigin = mParallelPoints[mParallelRootReference];
                    for (int i = 0; i < mParallelNumPoints; i++) {
                        Point localOrigin = mParallelPoints[i];
                        Log.i("PARALLEL", String.format("Index: %d; X,Y: %d,%d", i, localOrigin.x, localOrigin.y));

                        int xOffset = referenceOrigin.x - localOrigin.x;
                        int yOffset = referenceOrigin.y - localOrigin.y;

                        drawLineWithBrush(mCanvasBuffer,
                                (int) mTouchPosX - xOffset, (int) mTouchPosY - yOffset,
                                (int) x - xOffset, (int) y - yOffset);
                    }
                } else if (mVectorMode == VECTOR_MODE.VECTOR) {

                    UnitVector referenceVector = mAllVectors.get(0);
                    for (int i = 0; i < mAllVectors.size(); i++) {
                        UnitVector localVector = mAllVectors.get(i);

                        int xOffset = referenceVector.origin.x - localVector.origin.x;
                        int yOffset = referenceVector.origin.y - localVector.origin.y;

                        drawLineWithBrush(mCanvasBuffer,
                                (int) mTouchPosX - xOffset, (int) mTouchPosY - yOffset,
                                (int) x - xOffset, (int) y - yOffset);
                    }
                }
            } else {
                // only draw one
                drawLineWithBrush(mCanvasBuffer, (int) mTouchPosX, (int) mTouchPosY, (int) x, (int) y);
            }

            mTouchPosX = x;
            mTouchPosY = y;

            int NUM_ANGLES = 3;

            switch(getRadialMode()) {
                case SINGLE:
                    return; // don't do extra drawing... this could be refactored

                case RADIAL_2:
                    NUM_ANGLES = 2;
                    break;

                case RADIAL_8:
                    NUM_ANGLES = 8;
            }

            // between N lines, draw lines for 1, N-1 (0 and N are excluded, as they're drawn above)
            int angleDivisor = 360 / NUM_ANGLES;
            int angleXEnd, angleYEnd;
            for (int i=1; i<NUM_ANGLES; i++) {
                angleReverse = angle - Math.toRadians(i* angleDivisor);
                Log.i(LINE_TAG, String.format("i=%d; angle=%f; angleReverse=%f", i, angle, angleReverse));
                angleXEnd = (int) (distance * (float) Math.sin(angleReverse));
                angleYEnd = (int) (distance * (float) Math.cos(angleReverse));

                drawLineWithBrush(mCanvasBuffer, (int) mTouchAngleX[i-1], (int) mTouchAngleY[i-1],
                        (int) mTouchAngleX[i-1] + angleXEnd, (int) mTouchAngleY[i-1] + angleYEnd);

                mTouchAngleX[i-1] = mTouchAngleX[i-1] + angleXEnd;
                mTouchAngleY[i-1] = mTouchAngleY[i-1] + angleYEnd;
            }
        }
    }

    private void doTouchUpBrush(float x, float y) {
//        if (mMode == MODE.COLORING) {
//            if (x == mTouchPosX && y == mTouchPosY) {
//                mPathColoring.quadTo(x, y, (x + mTouchPosX) / 2 + 1, (y + mTouchPosY) / 2 + 1);
//
//            } else {
//                mPathColoring.quadTo(x, y, (x + mTouchPosX) / 2, (y + mTouchPosY) / 2);
//
//            }
//
//            mCanvasBuffer.drawPath(mPathColoring, mPaintColoring);
//            mPathColoring.reset();
//        }

        mTouchPosX = Float.MIN_VALUE;
        mTouchPosY = Float.MIN_VALUE;
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
     * @param canvas
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    @SuppressLint("DefaultLocale")
    private void drawLineWithBrush(Canvas canvas, int startX, int startY, int endX, int endY) {
        // MAGIC here is where it is!!!!
        int distance = (int) Util.getDistanceBetween2Point(startX, startY, endX, endY);
        double angle = Util.getRadianAngleBetween2Point(startX, startY, endX, endY);
        double angleReverse = angle - Math.toRadians(180); // duh...

        Log.i("MAGIC", String.format("distance=%d, angle=%f", distance, angle));

        int halfBrushWidth = BRUSH_POINT_WIDTH / 2;
        int halfBrushHeight = BRUSH_POINT_WIDTH / 2;
        int x, y;
        int xRev, yRev;

        int offset = halfBrushWidth / 2;

        for (int i = 0; i <= distance; i += offset) {
            x = (int) (startX + (Math.sin(angle) * i) - halfBrushWidth);
            y = (int) (startY + (Math.cos(angle) * i) - halfBrushHeight);
            canvas.drawBitmap(getBrushPointBitmap(), x, y, mPaintDrawing);

            // try this :)
            // weird almost perpendicular thing
            /* xRev = (int) (startX + (Math.sin(angleReverse) * i) - halfBrushWidth);
            yRev = (int) (startY + (Math.cos(angleReverse) * i) - halfBrushHeight);
            canvas.drawBitmap(getBrushPointBitmap(), xRev, yRev, mPaintDrawing); */

        }
    }

    /**
     * 임시 파일 삭제
     */
    private void deleteTempBitmapFile() {
        File file = new File(Misc.getTempFilePath(mMode));
        if (file.exists() == true) {
            file.delete();
        }
    }

    /**
     * 임시파일을 Double Buffer 로 적용
     */
    private void restoreTempBitmapFile() {
        File tempFile = new File(Misc.getTempFilePath(mMode));
        if (tempFile.exists() == true) {
            Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath());
            if (bitmap != null && mBitmapBuffer != null) {
                mCanvasBuffer.drawBitmap(bitmap, 0, 0, mPaint);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setMode(MODE mode) {
        mMode = mode;
        setPenColor(mCurrentColor);
    }

    public MODE getMode() {
        return mMode;
    }

    public void setRadialMode(RADIAL_MODE mode) {
        mRadialMode = mode;
    }
    public RADIAL_MODE getRadialMode() {
        return mRadialMode;
    }

    /* -- PARALLEL MODE -- */
    public void setParellelMode(PARALLEL_MODE mParellelMode) {
        this.mParellelMode = mParellelMode;
    }

    public PARALLEL_MODE getParellelMode() {
        return mParellelMode;
    }

    public void resetParallelOrigins() {
        mParallelPoints = new Point[10];
        mParallelNumPoints = 0;
        mParallelRootReference = -1;

        mAllVectors.clear();
    }

    public void placeParallelOrigin(Point origin) {
        // update pointer counter
        mParallelRootReference++;
        if (mParallelNumPoints < 10)
            mParallelNumPoints++;
        // if more than 10, reset last one
        if (mParallelRootReference == 9)
            mParallelRootReference = 0; // reset!
        // add to array
        mParallelPoints[mParallelRootReference] = origin;


        // also later... remove views
    }

    /*public void placeVectorOrigin()*/

    /* -- END PARALLEL MODE -- */

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

    public void clearAll() {
        mBitmapBuffer.eraseColor(Color.TRANSPARENT);
        mPathColoring.reset();
        deleteTempBitmapFile();
        invalidate();
    }

    public void saveTempBitmapFile() {
        if (mBitmapBuffer != null) {
            Util.saveImageFileFromBitmap(mBitmapBuffer, Misc.getTempFilePath(mMode), 100);
        }
    }

    public void doTouchEvent(int action, float x, float y) {
        boolean isInvalidate = false;
        if (mRect.contains((int) x, (int) y) || mRect.contains((int) mTouchPosX, (int) mTouchPosY)) {
            isInvalidate = true;
        }

        // when placing, do this thing
        if (getParellelMode() == PARALLEL_MODE.PLACE) {
            // PARALLEL next --- do this!!! place Vectors instead of points
            // follow the logic in the SupportLayer...
            // -- onDown, start a new one
            // -- onMove, redraw
            // -- onUp, add to the list
            /*if (action == MotionEvent.ACTION_UP) {
                placeParallelOrigin(new Point((int) x, (int) y));
                mGleaphHolder.addOneGleaphFrame((int) x, (int) y);
            }*/
            doTouchEventVector(action, x, y);

        } else {

            doTouchEventBrush(action, x, y);

            if (isInvalidate) {
                invalidate();
            }
        }
    }

    /**
     *
     * @param action
     * @param x
     * @param y
     */
    private void doTouchEventBrush(int action, float x, float y) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                doTouchDownBrush(x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                doTouchMoveBrush(x, y);
                break;

            case MotionEvent.ACTION_UP:
                doTouchUpBrush(x, y);
                break;
        }
    }

    /**
     * Perform a touch event when in UnitVector drawing mode.
     * @param action DOWN, MOVE, UP
     * @param x touch.x
     * @param y touch.y
     */
    private void doTouchEventVector(int action, float x, float y) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                doTouchDownVector(x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                doTouchMoveVector(x, y);
                break;

            case MotionEvent.ACTION_UP:
                doTouchUpVector(x, y);
                break;
        }
    }

    /**
     * Set an origin
     * @param x touch.x
     * @param y touch.y
     */
    private void doTouchDownVector(float x, float y) {
        // set an origin
        mCurrentVector = new UnitVector();
        mCurrentVector.origin = new com.enuma.drawingcoloring.types.Point();
        mCurrentVector.origin.x = (int) x;
        mCurrentVector.origin.y = (int) y;
        mCurrentVector.angle = null;

        if (mCallback != null) {
            mCallback.onTouchDownForDrawing();
        }

    }

    /**
     * Redraw line from origin to current point
     * @param x touch.x
     * @param y touch.y
     */
    private void doTouchMoveVector(float x, float y) {
        // redraw the thing
        /*mCanvasBuffer.drawLine(
                mCurrentVector.origin.x, mCurrentVector.origin.y,
                x, y, new Paint());*/

        double angle = Util.getRadianAngleBetween2Point(
                mCurrentVector.origin.x, mCurrentVector.origin.y,
                (int) x, (int) y);

        mCurrentVector.angle = (int) angle;

        mGleaphHolder.drawAngledGleaphFrame(
                mCurrentVector.origin.x - 100,
                mCurrentVector.origin.y - 100,
                180 - (int) Math.toDegrees(angle));

    }

    /**
     * Add vector to list of vectors
     * @param x touch.x
     * @param y touch.y
     */
    private void doTouchUpVector(float x, float y) {

        double angle = Util.getRadianAngleBetween2Point(
                mCurrentVector.origin.x, mCurrentVector.origin.y,
                (int) x, (int) y);

        if (mCurrentVector.angle != null) {
            mCurrentVector.angle = (int) angle;
            mAllVectors.add(mCurrentVector);
        }

        mGleaphHolder.saveAngledGleaphFrame(
                mCurrentVector.origin.x - 100,
                mCurrentVector.origin.y - 100,
                180 - (int) Math.toDegrees(angle));
        mCurrentVector = null;

    }

    public boolean isInit() {
        return mbInit;
    }
    ////////////////////////////////////////////////////////////////////////////////

    public interface Callback {
        void onTouchDownForDrawing();
    }
}
