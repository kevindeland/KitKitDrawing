package com.enuma.drawingcoloring.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.enuma.drawingcoloring.activity.GleaphHolder;
import com.enuma.drawingcoloring.brush.Crayon;
import com.enuma.drawingcoloring.brush.IBrush;
import com.enuma.drawingcoloring.core.Const;
import com.enuma.drawingcoloring.files.FileObjectInterface;
import com.enuma.drawingcoloring.types.KPath;
import com.enuma.drawingcoloring.types.KPoint;
import com.enuma.drawingcoloring.types.KStroke;
import com.enuma.drawingcoloring.types.KUnitVector;
import com.enuma.drawingcoloring.utility.Log;
import com.enuma.drawingcoloring.utility.Misc;
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
 * MODE.DRAWING : 크레용 질감 표현 (Brush Image (5 * 2) 이용)
 * MODE.COLORING : 2017.09.12 DRAWING 과 동일하게 변경 (이전 일반 Path 표현)
 * 임시 작업 파일의 Save, Restore, Delete 기능 제공
 */
public class ViewDrawingColoring extends View {


    // painting is a collection of Strokes
    private List<KStroke> __thisPainting;
    // the path of last points drawn

    private KStroke __lastStroke;

    private KPath ___lastPointPath;
    private int ___lastPointCounter = 0;
    private Gson _gson = new Gson();

    ////////////////////////////////////////////////////////////////////////////////

    public enum MODE {
        DRAWING,
        COLORING
    }

    // draw single stroke, or radiate from center
    public enum RADIAL_MODE {
        SINGLE,     // single stroke
        RADIAL_2,   // two 180 degree opposites
        RADIAL_8    // 8 strokes
    }

    // when using parallel mode
    public enum PARALLEL_MODE {
        DEFAULT,    // off. Draw normal mode
        PLACE,      // place anchors
        DRAW        // draw in parallel
    }

    // when placing parallels...
    public enum VECTOR_MODE {
        JUST_TRANSLATE,        // place and draw as a Point
        VECTOR      // place and draw as a UnitVector
    }

    /**
     * Touch 의 Move Event 가 TOUCH_TOLERANCE 이내로 움직이면 무시
     */
    private static final float TOUCH_TOLERANCE = 4;

    ////////////////////////////////////////////////////////////////////////////////

    private Context mContext;

    ////////////////////////////////////////////////////////////////////////////////


    /** Double Buffer */
    private Bitmap mBitmapBuffer;
    private Canvas mCanvasBuffer;

    ////////////////////////////////////////////////////////////////////////////////

    private Callback mCallback;
    private MODE mMode = MODE.DRAWING;

    private float mTouchPosX, mTouchPosY;

    // variables for RADIAL mode
    // an array of the last drawn Points for each radial line...
    // for example, if we are drawing an 8-arm radial from 0,0... it will progress like:
    // POS0 = [( x00 = 0, y00 = 0), (0, 0)... ]
    // with 8 arms, the angle is 360 / 8 = 45...
    // let's say the distance gone each time is di
    // POS1 = [( x10 = x00 + d1*cos(0), y10 = d1*sin(0)), ...)
    // POS2 = [( x20 = x10 + d2*cos(0), y20 = d2*sin(0)), ...)
    private RADIAL_MODE mRadialMode = RADIAL_MODE.SINGLE;
    private float[] mTouchRadialPosX, mTouchRadialPosY;


    // variables for parallel mode
    private PARALLEL_MODE mParellelMode;
    private Point[] mParallelPoints; // the origins for all parallel sources...
    private int mParallelNumPoints;
    private int mParallelRootReference = -1; // that last one placed


    // variables for VectorMode
    private VECTOR_MODE mVectorMode = VECTOR_MODE.VECTOR;
    private KUnitVector mCurrentVector;
    private ArrayList<KUnitVector> mAllVectors = new ArrayList<>();
    // List of lastDrawn positions for Vectors
    private ArrayList<KPoint> mVectorPositions = new ArrayList<>();

    /** 일반적으로 사용 Paint */
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);

    private IBrush mBrush;

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

        mBrush = new Crayon(mContext, isSmallLCD);

        __thisPainting = new ArrayList<>();

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
        //mBrush.setPenColor(mCurrentColor);
    }

    public void setPenColor(int color) {
        mBrush.setPenColor(color);
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
        mParallelPoints = new Point[10]; // no reason we should limit this to ten
        mParallelNumPoints = 0;
        mParallelRootReference = -1;

        mAllVectors.clear();
        mVectorPositions.clear();
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
    }

    /* -- END PARALLEL MODE -- */

    public void clearAll() {
        mBitmapBuffer.eraseColor(Color.TRANSPARENT);
        deleteTempBitmapFile();
        invalidate();
    }

    public void saveTempBitmapFile() {
        if (mBitmapBuffer != null) {
            Util.saveImageFileFromBitmap(mBitmapBuffer, Misc.getTempFilePath(mMode), 100);
        }
    }
    ///////////////////////////////////////////////////////////
    //////////// begin TOUCH functions ////////////////////////

    public void doTouchEvent(int action, float x, float y) {
        boolean isInvalidate = false;
        if (mRect.contains((int) x, (int) y) || mRect.contains((int) mTouchPosX, (int) mTouchPosY)) {
            isInvalidate = true;
        }

        // when placing, do this thing
        if (mParellelMode == PARALLEL_MODE.PLACE) {

            if (mVectorMode == VECTOR_MODE.VECTOR)
                mPlaceVectorTouchListener.doTouchEvent(action, x, y);
            else if (mVectorMode == VECTOR_MODE.JUST_TRANSLATE) {
                mPlacePointTouchListener.doTouchEvent(action, x, y);
            }

        } else {

            mBrushTouchListener.doTouchEvent(action, x, y);

            if (isInvalidate) {
                invalidate();
            }
        }
    }

    public void undo() {
        // pop last thing
        if (__thisPainting.size() == 0) return;
        __thisPainting.remove(__thisPainting.size() - 1);
        __lastStroke = __thisPainting.get(__thisPainting.size() - 1);

        // clear the canvas
        mBitmapBuffer.eraseColor(Color.TRANSPARENT);
        deleteTempBitmapFile();
        invalidate();

        // redraw
        for (KStroke stroke : __thisPainting) {
            drawStroke(stroke);
        }

        // UNDO need to keep color to which crayon is selected
        // mBrush.setPenColor(activity.getSelectedCrayon());
    }

    private void drawStroke(KStroke stroke) {
        mBrush.setPenColor(stroke.getColor());

        for (int i = 1; i < stroke.getSize(); i++) {
            KPoint last = stroke.getPoint(i-1);
            KPoint next = stroke.getPoint(i);

            mBrush.drawLineWithBrush(mCanvasBuffer,
                    (int) (last.x) , (int) (last.y),
                    (int) (next.x), (int) (next.y));
        }
    }

    /**
     * Customizable class that listens for a Touch, and has a different behavior for:
     * TouchDown, TouchMove, and TouchUp.
     */
    private abstract class TouchEventListener {
        void doTouchEvent(int action, float x, float y) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    doTouchDown(x, y);
                    break;

                case MotionEvent.ACTION_MOVE:
                    doTouchMove(x, y);
                    break;

                case MotionEvent.ACTION_UP:
                    doTouchUp(x, y);
                    break;
            }
        }
        abstract void doTouchDown(float x, float y);
        abstract void doTouchMove(float x, float y);
        abstract void doTouchUp(float x, float y);
    }

    private BrushTouchEventListener mBrushTouchListener = new BrushTouchEventListener();

    /**
     * How does a Touch happen when you're in Brush mode???
     */
    class BrushTouchEventListener extends TouchEventListener {

        @Override
        public void doTouchDown(float x, float y) {

            // UNDO this should also know which type of Stroke it is, i.e. Radial or Parallel
            __lastStroke = new KStroke(mBrush.getPenColor());
            __lastStroke.addPoint(new KPoint((int) x, (int) y));

            ___lastPointPath = new KPath();
            ___lastPointCounter = 0;
            ___lastPointPath.addPoint(new KPoint((int) x, (int) y));

            mTouchPosX = x;
            mTouchPosY = y;

            mTouchRadialPosX = new float[]{x, x, x, x, x, x, x}; // 7 allows for 8 total
            mTouchRadialPosY = new float[]{y, y, y, y, y, y, y}; // 7 allows for 8 total

            // UNDO add stroke
            mBrush.drawLineWithBrush(mCanvasBuffer, (int) mTouchPosX, (int) mTouchPosY, (int) mTouchPosX, (int) mTouchPosY);

            if (mParellelMode == PARALLEL_MODE.DRAW && mVectorMode == VECTOR_MODE.VECTOR) {
                // for all (next)
                mVectorPositions.add(mCurrentVector.origin); // okay whatever
                // add mTouchPosX - mCurrentVector.origin.x .... minus the rootVector thingy...
                // something like that... should figure it all out
            }

            if (mCallback != null) {
                mCallback.onTouchDownForDrawing();
            }
        }

        /**
         * TODO okaaaaay this is the most complicated part of the code, I wonder if we could break
         * TODO it down and simplify
         * @param x
         * @param y
         */
        @SuppressLint("DefaultLocale")
        @Override
        public void doTouchMove(float x, float y) {
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

                __lastStroke.addPoint(new KPoint((int) x, (int) y));
                ___lastPointPath.addPoint(new KPoint((int) x, (int) y));

                Log.v(LINE_TAG, String.format("Drawing from [%d, %d] to [%d, %d]",
                        (int) mTouchPosX, (int) mTouchPosY, (int) x, (int) y));


                if (mParellelMode == PARALLEL_MODE.DRAW) {

                    doTouchMoveParallelDraw((int) x, (int) y, distance, angle);
                } else {
                    // only draw one
                    // UNDO add stroke
                    mBrush.drawLineWithBrush(mCanvasBuffer, (int) mTouchPosX, (int) mTouchPosY, (int) x, (int) y);
                }

                mTouchPosX = x;
                mTouchPosY = y;

                /////////////////////////////////////////////////////
                ///////////// begin RADIAL drawing mode /////////////

                int NUM_ANGLES = 3;

                switch(mRadialMode) {
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

                    // UNDO add stroke
                    mBrush.drawLineWithBrush(mCanvasBuffer, (int) mTouchRadialPosX[i-1], (int) mTouchRadialPosY[i-1],
                            (int) mTouchRadialPosX[i-1] + angleXEnd, (int) mTouchRadialPosY[i-1] + angleYEnd);

                    mTouchRadialPosX[i-1] = mTouchRadialPosX[i-1] + angleXEnd;
                    mTouchRadialPosY[i-1] = mTouchRadialPosY[i-1] + angleYEnd;
                }
            }

        }

        /**
         * when in parallel mode, do a TouchMove for either TRANSLATE or VECTOR mode
         * @param x
         * @param y
         * @param distance
         * @param angle
         */
        private void doTouchMoveParallelDraw(int x, int y, double distance, double angle) {
            double angleReverse;
            switch (mVectorMode) {
                case JUST_TRANSLATE:
                    Log.i("PARALLEL", String.format("NumPoints: %d; Root: %d", mParallelNumPoints, mParallelRootReference));
                    Point referenceOrigin = mParallelPoints[mParallelRootReference];
                    for (int i = 0; i < mParallelNumPoints; i++) {
                        Point localOrigin = mParallelPoints[i];
                        Log.i("PARALLEL", String.format("Index: %d; X,Y: %d,%d", i, localOrigin.x, localOrigin.y));

                        int xOffset = referenceOrigin.x - localOrigin.x;
                        int yOffset = referenceOrigin.y - localOrigin.y;

                        // UNDO add stroke
                        mBrush.drawLineWithBrush(mCanvasBuffer,
                                (int) mTouchPosX - xOffset, (int) mTouchPosY - yOffset,
                                x - xOffset, y - yOffset);
                    }
                    break;

                case VECTOR:
                    KUnitVector referenceVector = mAllVectors.get(0);
                    int angleXEnd, angleYEnd;
                    for (int i = 0; i < mAllVectors.size(); i++) {
                        KUnitVector localVector = mAllVectors.get(i);

                        angleReverse = Math.toRadians(localVector.angle - referenceVector.angle) + angle;
                        angleXEnd = (int) (distance * Math.sin(angleReverse));
                        angleYEnd = (int) (distance * Math.cos(angleReverse));

                        int xOffset = referenceVector.origin.x - localVector.origin.x;
                        int yOffset = referenceVector.origin.y - localVector.origin.y;

                        // for the first time, it's the origin
                        KPoint lastDrawn = mVectorPositions.get(i);
                        int nextX = lastDrawn.x + angleXEnd;
                        int nextY = lastDrawn.y + angleYEnd;
                        //Log.i("VECTOR", "Drawing line from (%f")
                        // UNDO add stroke
                        mBrush.drawLineWithBrush(mCanvasBuffer,
                                (int) (lastDrawn.x) , (int) (lastDrawn.y),
                                (int) (nextX), (int) (nextY));


                        mVectorPositions.set(i, new KPoint(
                                nextX, nextY
                        ));
                    }
                    break;

            }
        }

        @Override
        public void doTouchUp(float x, float y) {

            __thisPainting.add(__lastStroke);
            mTouchPosX = Float.MIN_VALUE;
            mTouchPosY = Float.MIN_VALUE;
        }
    }

    private PlaceVectorTouchEventListener mPlaceVectorTouchListener = new PlaceVectorTouchEventListener();

    /**
     * How does a Touch happen when you're in PlaceVector mode???
     */
    class PlaceVectorTouchEventListener extends TouchEventListener {

        @Override
        void doTouchDown(float x, float y) {
            // set an origin
            mCurrentVector = new KUnitVector();
            mCurrentVector.origin = new KPoint((int) x, (int) y);
            mCurrentVector.angle = null;

            if (mCallback != null) {
                mCallback.onTouchDownForDrawing();
            }
        }

        @Override
        void doTouchMove(float x, float y) {

            double angle = Util.getRadianAngleBetween2Point(
                    mCurrentVector.origin.x, mCurrentVector.origin.y,
                    (int) x, (int) y);

            mCurrentVector.angle = (int) Math.toDegrees(angle);

            mGleaphHolder.drawAngledGleaphFrame(
                    mCurrentVector.origin.x - 100,
                    mCurrentVector.origin.y - 100,
                    180 - (int) Math.toDegrees(angle));
        }

        @Override
        void doTouchUp(float x, float y) {
            double angle = Util.getRadianAngleBetween2Point(
                    mCurrentVector.origin.x, mCurrentVector.origin.y,
                    (int) x, (int) y);

            if (mCurrentVector.angle != null) {

                mCurrentVector.angle = (int) Math.toDegrees(angle);
                mAllVectors.add(mCurrentVector);
                // mVectorPositions... wtf, should this have things in it???
            }

            mGleaphHolder.saveAngledGleaphFrame(
                    mCurrentVector.origin.x - 100,
                    mCurrentVector.origin.y - 100,
                    180 - (int) Math.toDegrees(angle));

            mVectorPositions.add(new KPoint(mCurrentVector.origin.x, mCurrentVector.origin.y));
        }
    }

    private PlacePointTouchEventListener mPlacePointTouchListener = new PlacePointTouchEventListener();

    class PlacePointTouchEventListener extends TouchEventListener {

        @Override
        void doTouchDown(float x, float y) {
            // do nothing
        }

        @Override
        void doTouchMove(float x, float y) {
            // do nothing
        }

        @Override
        void doTouchUp(float x, float y) {
            placeParallelOrigin(new Point((int) x, (int) y));
            mGleaphHolder.addOneGleaphFrame((int) x, (int) y);
        }
    }

    public boolean isInit() {
        return mbInit;
    }
    ////////////////////////////////////////////////////////////////////////////////

    public interface Callback {
        void onTouchDownForDrawing();
    }

    ///////////////////////////////////////////////////////////
    //////////// begin DEBUG functions ////////////////////////


    /**
     * Should only be called after PLACE mode.
     * the path will be inserted at every parallel point
     *
     * TODO next... do this with rotations!!!
     * i.e... instead of iterating through mParallelPoints, go through the UnitVectors.
     *
     * also... might need to do some tricky rotation about the center point
     * @param path
     */
    public void insertMassGleaph(KPath path) {


        // only place at (X,Y), with no location
        if (mVectorMode == VECTOR_MODE.JUST_TRANSLATE) {
            for (int i = 0; i < mParallelNumPoints; i++) {

                int offsetX = mParallelPoints[i].x;
                int offsetY = mParallelPoints[i].y;

                for (int j = 1; j < path.getSize(); j++) {
                    KPoint lastPoint = path.getPoint(j - 1);
                    KPoint nextPoint = path.getPoint(j);
                    // UNDO add stroke
                    mBrush.drawLineWithBrush(mCanvasBuffer,
                            lastPoint.x + offsetX, lastPoint.y + offsetY,
                            nextPoint.x + offsetX, nextPoint.y + offsetY);
                }
            }
        } else if (mVectorMode == VECTOR_MODE.VECTOR) {

            int angleXEnd, angleYEnd;
            mVectorPositions.clear();

            for (int i=0; i < mAllVectors.size(); i++) {

                int offsetX = mAllVectors.get(i).origin.x;
                int offsetY = mAllVectors.get(i).origin.y;

                // start drawing from the beginning, no matter what?
                mVectorPositions.add(new KPoint(0, 0)); // this is kinda redundant lol
                mVectorPositions.set(i, mAllVectors.get(i).origin);

                for (int j = 1; j < path.getSize(); j++) {

                    KPoint lastPoint = path.getPoint(j - 1);
                    KPoint nextPoint = path.getPoint(j);

                    double distance = Util.getDistanceBetween2Point(
                            lastPoint.x, lastPoint.y, nextPoint.x, nextPoint.y
                    );
                    double angle = Util.getRadianAngleBetween2Point(
                            lastPoint.x, lastPoint.y, nextPoint.x, nextPoint.y
                    );

                    double angleReverse = Math.toRadians(mAllVectors.get(i).angle) + angle;
                    angleXEnd = (int) (distance * Math.sin(angleReverse));
                    angleYEnd = (int) (distance * Math.cos(angleReverse));


                    KPoint lastDrawn = mVectorPositions.get(i);
                    int nextX = lastDrawn.x + angleXEnd;
                    int nextY = lastDrawn.y + angleYEnd;

                    // TODO fuck... idk
                    // UNDO add stroke
                    mBrush.drawLineWithBrush(mCanvasBuffer,
                            lastDrawn.x, lastDrawn.y,
                            nextX, nextY);

                    mVectorPositions.set(i, new KPoint(nextX, nextY));
                }

            }
        }
    }

    /**
     * A debugging method for testing re-drawing the last path
     */
    public void pasteLastPathFiftyPixelsToTheRight() {

        for(int i=1; i < ___lastPointPath.getSize(); i++) {
            KPoint lastPoint = ___lastPointPath.getPoint(i-1);
            KPoint nextPoint = ___lastPointPath.getPoint(i);

            int offsetRight = 100 * (___lastPointCounter + 1);
            // UNDO add stroke
            mBrush.drawLineWithBrush(mCanvasBuffer,
                    lastPoint.x + offsetRight, lastPoint.y,
                    nextPoint.x + offsetRight, nextPoint.y);
        }

        ___lastPointCounter++;
    }

    /**
     * Takes the last single stroke drawn, and saves it to a JSON file
     */
    public void saveLastPathAsJson() {

        FileObjectInterface.savePathAsGleaphJson(___lastPointPath);
    }

    /**
     * Save the List of KStroke (__thisPainting) as a JSON value
     */
    public void savePaintingAsJson() {

        FileObjectInterface.savePaintingAsJson(__thisPainting);
    }

    /**
     * Load the List of KStroke as a JSON value and draw it
     */
    public void loadLastPaintingJson() {

        List<KStroke> strokes = FileObjectInterface.loadLastPainting();

        for (KStroke stroke : strokes) {
            drawStroke(stroke);;
        }
    }

    /**
     * Finds all JSON files in the GLEAPH folder, and renders them on the Canvas.
     */
    public void loadAndDrawAllSavedJson() {
        List<KPath> paths = FileObjectInterface.loadAllGleaphs();

        for (KPath x : paths) {
            Log.i("GSON", x.toString());

            for (KPoint k : x.getPath()) {

                Log.i("GSON", "" + k.x + ", " + k.y);
            }

            for (int i = 1; i < x.getSize(); i++) {
                KPoint lastPoint = x.getPoint(i - 1);
                KPoint nextPoint = x.getPoint(i);
                // UNDO add stroke
                mBrush.drawLineWithBrush(mCanvasBuffer,
                        lastPoint.x, lastPoint.y,
                        nextPoint.x, nextPoint.y);
            }
        }
    }
}
