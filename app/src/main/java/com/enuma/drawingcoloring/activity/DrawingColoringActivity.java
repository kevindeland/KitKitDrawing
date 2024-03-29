package com.enuma.drawingcoloring.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.enuma.drawingcoloring.R;
import com.enuma.drawingcoloring.activity.base.BaseActivity;
import com.enuma.drawingcoloring.core.Const;
import com.enuma.drawingcoloring.dialog.DialogSelectColoringBackground;
import com.enuma.drawingcoloring.dialog.DialogSelectDrawingBackground;
import com.enuma.drawingcoloring.types.KPath;
import com.enuma.drawingcoloring.utility.EffectSound;
import com.enuma.drawingcoloring.utility.Log;
import com.enuma.drawingcoloring.utility.Preference;
import com.enuma.drawingcoloring.utility.Util;
import com.enuma.drawingcoloring.view.ViewDrawingColoring;
import com.enuma.drawingcoloring.view.ViewPen;
import com.enuma.drawingcoloring.view.base.LockableScrollView;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import static com.enuma.drawingcoloring.view.ViewDrawingColoring.PARALLEL_MODE.DEFAULT;
import static com.enuma.drawingcoloring.view.ViewDrawingColoring.PARALLEL_MODE.DRAW;
import static com.enuma.drawingcoloring.view.ViewDrawingColoring.PARALLEL_MODE.PLACE;
import static com.enuma.drawingcoloring.view.ViewDrawingColoring.RADIAL_MODE.RADIAL_2;
import static com.enuma.drawingcoloring.view.ViewDrawingColoring.RADIAL_MODE.RADIAL_8;
import static com.enuma.drawingcoloring.view.ViewDrawingColoring.RADIAL_MODE.SINGLE;

public class DrawingColoringActivity extends BaseActivity implements GleaphHolder {

    private Gson _gson = new Gson();
    ////////////////////////////////////////////////////////////////////////////////

    private final int TOTAL_PEN_COLOR_COUNT = 18;
    public static final int TOTAL_BG_COLOR_COUNT = 6;
    private final String TIME_FORMAT = "yyyy-MM-dd-HH-mm-ss";
    private final long MAX_SAVE_AMOUNT = (200 * 1024 * 1024);

    ////////////////////////////////////////////////////////////////////////////////

    private BaseActivity mThisActivity;
    protected Preference mPreference;
    private EffectSound mEffectSound;
    private float mScale;
    // private User mUser;
    private String mSavePath = "";

    ////////////////////////////////////////////////////////////////////////////////

    protected ImageView mVChangeBg;
    private View mVSave;
    protected ImageView mVChangeMode;
    protected ImageView mVChangeParallel;
    private ViewGroup mLayoutDrawing;
    private ViewDrawingColoring mVDrawingColoring;

    protected ImageView mIvColoring;

    private KPath mInsertGleaph;

    private ViewPen[] mVPens = new ViewPen[TOTAL_PEN_COLOR_COUNT];
    private ViewPen[] mVSelectedPensEffect = new ViewPen[TOTAL_PEN_COLOR_COUNT];
    private ImageView mIvSaveEffect;

    ////////////////////////////////////////////////////////////////////////////////

    private int[] PEN_COLORS = new int[TOTAL_PEN_COLOR_COUNT];
    private int[] BG_COLORS = new int[TOTAL_BG_COLOR_COUNT];

    protected ArrayList<String> mImagesColoring;
    protected ArrayList<String> mThumbnailsColoring;
    protected boolean mbNeedSave = false;

    private int mTouchId;
    private float mBeforeTouchPosX, mBeforeTouchPosY;
    private boolean mbSoundingChalk = false;
    private Rect mTempRect = new Rect();

    private View mDecorView;
    private int mUiOption;
    private boolean mbSmallLCD = false;

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mThisActivity = this;
        mPreference = Preference.getInstance(mThisActivity);
        mEffectSound = EffectSound.getInstance(mThisActivity);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras != null) {
                setLanguage(extras.getString("LANGUAGE", "sw-TZ"));
            } else {
                setLanguage("sw-TZ");
            }
        }

        Point size = Util.getWindowSize(mThisActivity);
        mbSmallLCD = (size.x <= 1280);

        setContentView(mbSmallLCD ? R.layout.activity_drawing_coloring_s : R.layout.activity_drawing_coloring);
        setImmersiveMode();
        setupView();
        checkSaveButton();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVDrawingColoring.saveTempBitmapFile();
        if (mbSoundingChalk == true) {
            stopChalkingSound();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus == true) {
            mDecorView.setSystemUiVisibility(mUiOption);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getUser();

        /*if (mInsertGleaph != null) {
            mVDrawingColoring.insertMassGleaph(mInsertGleaph);
            mInsertGleaph = null;
        }*/
    }

    ////////////////////////////////////////////////////////////////////////////////

    private void setLanguage(String language) {
        if (language == null || language.isEmpty()) {
            return;
        }

        try {
            String[] splitLang = language.split("-");
            String lang = splitLang[0];
            String region = splitLang.length>1 ? splitLang[1] : "";

            Locale locale = new Locale(lang, region);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());

        } catch (Exception e) {
            Log.e("" + e);
        }
    }

    private void setImmersiveMode() {
        mDecorView = getWindow().getDecorView();
        mUiOption = getWindow().getDecorView().getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            mUiOption |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mUiOption |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mUiOption |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        mDecorView.setSystemUiVisibility(mUiOption);
    }

    private void setupView() {
        String packageName = getPackageName();

        for (int i = 0; i < TOTAL_PEN_COLOR_COUNT; ++i) {
            PEN_COLORS[i] = ContextCompat.getColor(this, Util.getResourceId(this, "color_" + i, "color", packageName));
            Log.i("COLOR", "Color " + i + " = " + PEN_COLORS[i]);
        }

        for (int i = 0; i < TOTAL_BG_COLOR_COUNT; ++i) {
            BG_COLORS[i] = ContextCompat.getColor(this, Util.getResourceId(this, "bg_color_" + i, "color", packageName));
        }

        // parent container
        findViewById(R.id.layout_root).setOnTouchListener(mOnTouchListener);

        // back button
        findViewById(R.id.v_back).setOnClickListener(mOnClickListener);

        // change background
        mVChangeBg = (ImageView) findViewById(R.id.v_bg);
        mVChangeBg.setOnClickListener(mOnClickListener);

        ///////////////////
        // for debugging
        ///////////////////
        // copy and paste the last path drawn
        findViewById(R.id.v_paste_last).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVDrawingColoring.pasteLastPathFiftyPixelsToTheRight();
            }
        });

        // save the last path drawn
        findViewById(R.id.v_save_last).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVDrawingColoring.saveLastPathAsJson();
            }
        });

        // nothing right now
        findViewById(R.id.v_gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mVDrawingColoring.loadAndDrawAllSavedJson();
                launchGleaphGallery();

            }
        });


        findViewById(R.id.v_vector_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // VECTOR_MENU show or hide menu
                findViewById(R.id.choose_vector_symbol_menu).setVisibility(View.VISIBLE);

            }
        });

        findViewById(R.id.v_undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVDrawingColoring.undo();
            }
        });

        findViewById(R.id.v_save_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVDrawingColoring.savePaintingAsJson();
            }
        });

        findViewById(R.id.v_load_all).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mVDrawingColoring.loadLastPaintingJson();
            }
        });

        //////////////////////

        // change RADIAL modes
        mVChangeMode = (ImageView) findViewById(R.id.v_mode);
        mVChangeMode.setOnClickListener(mOnClickListener);

        // change PARALLEL modes
        mVChangeParallel = (ImageView) findViewById(R.id.v_parallel);
        mVChangeParallel.setOnClickListener(mOnClickListener);

        // SAVE bitmap to file
        mVSave = findViewById(R.id.v_save);
        mVSave.setOnClickListener(mOnClickListener);

        // the drawing pad
        mLayoutDrawing = (ViewGroup) findViewById(R.id.layout_drawing);

        // the ViewDrawingColoring, which contains the canvas
        mVDrawingColoring = (ViewDrawingColoring) findViewById(R.id.v_drawing_coloring);
        mVDrawingColoring.setCallback(mViewDrawingCoCallback);
        mVDrawingColoring.setGleaphHolder(this);

        // ImageView for Coloring, which contains the ColoringBook image to be displayed
        mIvColoring = (ImageView) findViewById(R.id.iv_coloring);

        // Layer for drawing things like GleaphFrame reference points
        mVSupportLayer = (RelativeLayout) findViewById(R.id.v_support_layer);

        Bitmap bitmapPen = BitmapFactory.decodeResource(getResources(), R.drawable.drawingpad_crayon_1_white);

        // scale the PenSize to fit within the screen
        int layoutPenHeight = 1800 - (mbSmallLCD ? 121 : 242);
        if (layoutPenHeight < bitmapPen.getHeight() * (TOTAL_PEN_COLOR_COUNT + 1)) {
            float scale = (float) layoutPenHeight / (bitmapPen.getHeight() * (TOTAL_PEN_COLOR_COUNT + 1));
            View layoutPenGroup = findViewById(R.id.layout_pen_group);
            layoutPenGroup.setPivotX(0);
            layoutPenGroup.setPivotY(0);
            layoutPenGroup.setScaleX(scale);
            layoutPenGroup.setScaleY(scale);
        }

        ((LockableScrollView) findViewById(R.id.scrollView)).setScrollingEnabled(false);

        final int[] RESOURCE_PEN = {
                R.drawable.drawingpad_crayon_1_white,
                R.drawable.drawingpad_crayon_2_black,
                R.drawable.drawingpad_crayon_3_gray,
                R.drawable.drawingpad_crayon_4_brown,
                R.drawable.drawingpad_crayon_5_red,
                R.drawable.drawingpad_crayon_6_orange,
                R.drawable.drawingpad_crayon_7_yelloworange,
                R.drawable.drawingpad_crayon_8_yellow,
                R.drawable.drawingpad_crayon_9_lime,
                R.drawable.drawingpad_crayon_10_green,
                R.drawable.drawingpad_crayon_11_emeraldgreen,
                R.drawable.drawingpad_crayon_12_aqua,
                R.drawable.drawingpad_crayon_13_lightblue,
                R.drawable.drawingpad_crayon_14_blue,
                R.drawable.drawingpad_crayon_15purpleblue,
                R.drawable.drawingpad_crayon_16_magentapurple,
                R.drawable.drawingpad_crayon_17_hotpink,
                R.drawable.drawingpad_crayon_18_lightpink
        };

        final int[] RESOURCE_PEN_S = {
                R.drawable.drawingpad_crayon_1_white_s,
                R.drawable.drawingpad_crayon_2_black_s,
                R.drawable.drawingpad_crayon_3_gray_s,
                R.drawable.drawingpad_crayon_4_brown_s,
                R.drawable.drawingpad_crayon_5_red_s,
                R.drawable.drawingpad_crayon_6_orange_s,
                R.drawable.drawingpad_crayon_7_yelloworange_s,
                R.drawable.drawingpad_crayon_8_yellow_s,
                R.drawable.drawingpad_crayon_9_lime_s,
                R.drawable.drawingpad_crayon_10_green_s,
                R.drawable.drawingpad_crayon_11_emeraldgreen_s,
                R.drawable.drawingpad_crayon_12_aqua_s,
                R.drawable.drawingpad_crayon_13_lightblue_s,
                R.drawable.drawingpad_crayon_14_blue_s,
                R.drawable.drawingpad_crayon_15purpleblue_s,
                R.drawable.drawingpad_crayon_16_magentapurple_s,
                R.drawable.drawingpad_crayon_17_hotpink_s,
                R.drawable.drawingpad_crayon_18_lightpink_s
        };

        Bitmap bitmapPenSelected = BitmapFactory.decodeResource(getResources(), (mbSmallLCD ? R.drawable.drawingpad_crayon_0_highlight_s : R.drawable.drawingpad_crayon_0_highlight));
        int marginTop = (int) getResources().getDimension(mbSmallLCD ? R.dimen.pen_normal_margin_top_s : R.dimen.pen_normal_margin_top);

        LinearLayout layoutPen = (LinearLayout) findViewById(R.id.layout_pen);

        // set up all of the pens
        for (int i = 0; i < TOTAL_PEN_COLOR_COUNT; ++i) {
            mVPens[i] = new ViewPen(this);
            mVPens[i].setPenImage(mbSmallLCD ? RESOURCE_PEN_S[i] : RESOURCE_PEN[i], null);
            mVPens[i].setTag(new Integer(i));
//            mVPens[i].setOnClickListener(mOnClickListener);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, bitmapPenSelected.getHeight());
            if (i != 0) {
                params.setMargins(0, -marginTop * 2, 0, 0);
            }

            layoutPen.addView(mVPens[i], params);
        }

        LinearLayout layoutSelectedPenEffect = (LinearLayout) findViewById(R.id.layout_select_pen_effect);
        for (int i = 0; i < TOTAL_PEN_COLOR_COUNT; ++i) {
            mVSelectedPensEffect[i] = new ViewPen(this);
            mVSelectedPensEffect[i].setPenImage(-1, bitmapPenSelected);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, bitmapPenSelected.getHeight());
            if (i != 0) {
                params.setMargins(0, -marginTop * 2, 0, 0);
            }

            layoutSelectedPenEffect.addView(mVSelectedPensEffect[i], params);
        }

        mIvSaveEffect = (ImageView) findViewById(R.id.iv_save_effect);

        mScale = Util.setScale(mThisActivity, findViewById(R.id.layout_root), false);
    }

    private void launchGleaphGallery() {
        Intent intent = new Intent(mThisActivity, GleaphGalleryActivity.class);
        startActivityForResult(intent, Const.REQUEST_GLEAPH_CODE);
        // TODO somehow wait for a selected Gleaph and load it into parallel things

        //findViewById(R.id.choose_vector_symbol_menu).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null) return;
        Log.i("XYZ", "onActivityResult");
        String xyz = data.getStringExtra("GLEAPH");
        Log.i("XYZ", xyz);
        KPath x = _gson.fromJson(xyz, KPath.class);

        // TODO place things...
        mVDrawingColoring.insertMassGleaph(x);

    }

    ////////////////////////////////////
    ////// Begin GleaphHolder
    ////////////////////////////////////

    private RelativeLayout mVSupportLayer; // the layer that draws these GleaphFrame views
    private Collection<ImageView> mSupportVectors; // why is this not used???s
    private ImageView mCurrentVector;

    @Override
    public void addOneGleaphFrame(int left, int top) {
        // could make this a new type of view, with its own methods
        ImageView testFrame = new ImageView(this);
        testFrame.setImageDrawable(getResources().getDrawable(R.drawable.frame));
        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageParams.leftMargin = left;
        imageParams.topMargin = top;
        testFrame.setLayoutParams(imageParams);
        mVSupportLayer.addView(testFrame);
    }

    @Override
    public void removeAllGleaphFrames() {
        mVSupportLayer.removeAllViews();
    }

    @Override
    public void drawAngledGleaphFrame(int left, int top, int angle) {
        // remove the "current vector" because it's the one we're redrawing
        if (mCurrentVector != null) {
            mVSupportLayer.removeView(mCurrentVector);
        }

        // create new ImageView with an arrow to point in the angled direction
        mCurrentVector = new ImageView(this);
        mCurrentVector.setImageDrawable(getResources().getDrawable(
                mVDrawingColoring.getCurrentSymbolVector().getDrawable()));
        // put it at the correct (x,y) and (angle)
        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageParams.leftMargin = left;
        imageParams.topMargin = top;
        mCurrentVector.setLayoutParams(imageParams);
        mCurrentVector.setRotation(angle);

        // add to SupportLayer
        mVSupportLayer.addView(mCurrentVector);

    }

    @Override
    public void saveAngledGleaphFrame(int left, int top, int angle) {
        this.drawAngledGleaphFrame(left, top, angle);
        if (mSupportVectors == null) {
            mSupportVectors = new ArrayList<ImageView>();
        }
        mSupportVectors.add(mCurrentVector);
        mCurrentVector = null;
    }

    ////////////////////////////////////
    ////// End GleaphHolder
    ////////////////////////////////////

    private void setNeedSave(boolean bNeedSave) {
        mbNeedSave = bNeedSave;
        checkSaveButton();
    }



    private void startSaveAnimation(String bitmapFilePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(bitmapFilePath);
        mIvSaveEffect.setImageBitmap(bitmap);

        Rect rectDrawView = new Rect();
        Rect rectSaveButton = new Rect();
        mLayoutDrawing.getGlobalVisibleRect(rectDrawView);
        rectDrawView.left = (int) (rectDrawView.left / mScale);
        rectDrawView.top = (int) (rectDrawView.top / mScale);
        rectDrawView.right = (int) (rectDrawView.right / mScale);
        rectDrawView.bottom = (int) (rectDrawView.bottom / mScale);

        mVSave.getGlobalVisibleRect(rectSaveButton);
        rectSaveButton.left = (int) (rectSaveButton.left / mScale);
        rectSaveButton.top = (int) (rectSaveButton.top / mScale);
        rectSaveButton.right = (int) (rectSaveButton.right / mScale);
        rectSaveButton.bottom = (int) (rectSaveButton.bottom / mScale);

        int offsetX = rectSaveButton.centerX() - rectDrawView.centerX();
        int offsetY = rectSaveButton.centerY() - rectDrawView.centerY();

        AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(mThisActivity, R.anim.save);

        TranslateAnimation translateAnimation = new TranslateAnimation(0, offsetX, 0, offsetY);
        animationSet.addAnimation(translateAnimation);
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mEffectSound.startSoundPool(EffectSound.SOUND_SAVE_BOARD, 0);
                mIvSaveEffect.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mIvSaveEffect.setVisibility(View.VISIBLE);
        mIvSaveEffect.startAnimation(animationSet);
    }

    private void startChalkingSound() {
        mbSoundingChalk = true;
        //mEffectSound.startSoundPool(EffectSound.SOUND_CHALK, -1);
    }

    private void stopChalkingSound() {
        mbSoundingChalk = false;
        //mEffectSound.stopSoundPool(EffectSound.SOUND_CHALK);
    }

    private void getUser() {
        // mUser = mKitkitDBHandler.getCurrentUser();

        //if (mUser == null) {
            mSavePath = Const.SAVE_PATH + File.separator;

        //} else {
            // mSavePath = Const.SAVE_PATH + File.separator + mUser.getUserName() + File.separator;

        //}
    }

    private void processSaveAmount() {
        Util.setMarkTime();
        File saveFolder = new File(mSavePath);

        if (saveFolder.exists() == true) {
            File[] filesArray = saveFolder.listFiles();

            if (filesArray != null) {
                ArrayList<File> files = new ArrayList<>(Arrays.asList(filesArray));

                long folderSize = 0L;
                for (int i = files.size() - 1; i >= 0; --i) {
                    if (files.get(i).isDirectory() == true) {
                        files.remove(i);
                    } else {
                        folderSize += files.get(i).length();
                    }
                }

                Log.i("saveFolder Size : " + folderSize);
                if (folderSize > MAX_SAVE_AMOUNT) {
                    Collections.sort(files, new Comparator<File>() {
                        @Override
                        public int compare(File o1, File o2) {
                            return (int) (o2.lastModified() - o1.lastModified());
                        }
                    });

                    folderSize = 0L;
                    boolean bExcessAmount = false;
                    for (int i = 0; i < files.size(); ++i) {
                        folderSize += files.get(i).length();
                        if (bExcessAmount == false && folderSize > MAX_SAVE_AMOUNT) {
                            bExcessAmount = true;
                        }

                        if (bExcessAmount == true) {
                            if (files.get(i).exists() == true) {
                                Log.i("delete file : " + files.get(i).getName());
                                files.get(i).delete();
                            }
                        }
                    }
                }
            }
        }

        Util.getElapsedTime("processSaveAmount");
    }

    ////////////////////////////////////////////////////////////////////////////////

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            Integer colorIndexTag = (Integer) view.getTag();
            if (colorIndexTag != null) {
//                setPenColor(colorIndexTag.intValue());

            } else {

                switch(id) {
                    case R.id.v_back:
                        finish();
                        break;

                    case R.id.v_bg:
                        if (mVDrawingColoring.getMode() == ViewDrawingColoring.MODE.DRAWING) {
                            DialogSelectDrawingBackground dialogSelectDrawingBackground = new DialogSelectDrawingBackground(mThisActivity, mSelectDrawingBgCallback);
                            dialogSelectDrawingBackground.show();

                        } else {
                            DialogSelectColoringBackground dialogSelectColoringBackground = new DialogSelectColoringBackground(mThisActivity, mThumbnailsColoring, mSelectColoringBgCallback);
                            dialogSelectColoringBackground.show();

                        }
                        break;

                    case R.id.v_save:
                        Calendar calendar = Calendar.getInstance();
                        String saveFileName = mSavePath + Util.getTimeFormatString(TIME_FORMAT, calendar.getTimeInMillis()) + ".jpg";

                        if (Util.saveImageFileFromView(mLayoutDrawing, saveFileName, null, 70)) {
                            processSaveAmount();
                            setNeedSave(false);
                            startSaveAnimation(saveFileName);
                            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + saveFileName)));
                        }
                        break;

                    case R.id.v_mode:
                        changeRadialMode();
                        break;


                    case R.id.v_parallel:

                        changeParallelMode();
                }
            }
        }

        /**
         * cycle between parallel modes
         */
        private void changeParallelMode() {
            ViewDrawingColoring.PARALLEL_MODE nextParallelMode;
            int nextParallelDraw;
            switch(mVDrawingColoring.getParellelMode()) {
                case DEFAULT:
                    nextParallelMode = PLACE;
                    nextParallelDraw = R.drawable.selector_parallel_place;
                    mVDrawingColoring.resetParallelOrigins();
                    break;

                case PLACE:
                    nextParallelMode = DRAW;
                    nextParallelDraw = R.drawable.selector_parallel_draw;
                    break;

                case DRAW:
                default:
                    nextParallelMode = DEFAULT;
                    nextParallelDraw = R.drawable.selector_parallel_off;
                    removeAllGleaphFrames();
                    break;
            }
            mVDrawingColoring.setParellelMode(nextParallelMode);
            mVChangeParallel.setImageDrawable(getResources().getDrawable(nextParallelDraw));
            // change parallel mode and change image (still need icons
        }

        /**
         * cycle between radial modes
         */
        private void changeRadialMode() {
            // change mode, and change image
            ViewDrawingColoring.RADIAL_MODE nextRadialMode;
            int nextRadialDraw;
            switch(mVDrawingColoring.getRadialMode()) {
                case SINGLE:
                    nextRadialMode = RADIAL_2;
                    nextRadialDraw = R.drawable.selector_mode_radial2;
                    break;

                case RADIAL_2:
                    nextRadialMode = RADIAL_8;
                    nextRadialDraw = R.drawable.selector_mode_radial8;
                    break;

                case RADIAL_8:
                default:
                    nextRadialMode = SINGLE;
                    nextRadialDraw = R.drawable.selector_mode_straight;

            }
            mVDrawingColoring.setRadialMode(nextRadialMode);

            mVChangeMode.setImageDrawable(getResources().getDrawable(nextRadialDraw));
        }
    };

    private DialogSelectDrawingBackground.Callback mSelectDrawingBgCallback = new DialogSelectDrawingBackground.Callback() {
        @Override
        public void onSelectBGColorIndex(int colorIndex) {
            setBGColor(colorIndex, true);

            final int BLACK_COLOR_INDEX = 5;

            if (BLACK_COLOR_INDEX == colorIndex) {
                setPenColor(0, false);

            } else {
                setPenColor(1, false);

            }

            mVDrawingColoring.clearAll();
            setNeedSave(false);
        }
    };

    private DialogSelectColoringBackground.Callback mSelectColoringBgCallback = new DialogSelectColoringBackground.Callback() {
        @Override
        public void onSelectBGImageIndex(int bgIndex) {
            setColoringImage(bgIndex, true);
            setPenColor(1, false);

            mVDrawingColoring.clearAll();
            setNeedSave(false);
        }
    };

    private ViewDrawingColoring.Callback mViewDrawingCoCallback = new ViewDrawingColoring.Callback() {
        @Override
        public void onTouchDownForDrawing() {
            setNeedSave(true);
        }
    };

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @SuppressLint("DefaultLocale")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!mVDrawingColoring.isInit()) {
                Log.i("not ready");
                return false;
            }


            float x = 0;
            float y = 0;

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    mTouchId = event.getPointerId(0);
                    x = event.getX(event.findPointerIndex(mTouchId));
                    y = event.getY(event.findPointerIndex(mTouchId));
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (event.findPointerIndex(mTouchId) != -1) {
                        x = event.getX(event.findPointerIndex(mTouchId));
                        y = event.getY(event.findPointerIndex(mTouchId));
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (event.findPointerIndex(mTouchId) != -1) {
                        x = event.getX(event.findPointerIndex(mTouchId));
                        y = event.getY(event.findPointerIndex(mTouchId));
                    }
                    break;
            }

            if (event.findPointerIndex(mTouchId) != -1) {
                for (int i = 0; i < mVPens.length; ++i) {
                    mVPens[i].getGlobalVisibleRect(mTempRect);
                    mTempRect.left = (int) (mTempRect.left / mScale);
                    mTempRect.top = (int) (mTempRect.top / mScale);
                    mTempRect.right = (int) (mTempRect.right / mScale);
                    mTempRect.bottom = (int) (mTempRect.bottom / mScale);

                    if (mTempRect.contains((int) x, (int) y)) {
                        setPenColor(i, true);
                        break;
                    }
                }

                // just checks if it's inside of the coloring box
                mVDrawingColoring.getGlobalVisibleRect(mTempRect);
                mTempRect.left = (int) (mTempRect.left / mScale);
                mTempRect.top = (int) (mTempRect.top / mScale);
                mTempRect.right = (int) (mTempRect.right / mScale);
                mTempRect.bottom = (int) (mTempRect.bottom / mScale);

                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    stopChalkingSound();

                } else {
                    final float TOUCH_TOLERANCE = 4;

                    if (mTempRect.contains((int) x, (int) y)) {
                        if (Math.abs(x - mBeforeTouchPosX) < TOUCH_TOLERANCE &&
                                Math.abs(y - mBeforeTouchPosY) < TOUCH_TOLERANCE) {
                            stopChalkingSound();

                        } else {
                            if (!mbSoundingChalk) {
                                startChalkingSound();

                            }
                        }
                    } else {
                        stopChalkingSound();
                    }
                }

//                if (mbSoundingChalk == true) {
//                    mEffectSound.setVolume(EffectSound.SOUND_CHALK, (float)Math.random());
//                }


                // this must be where the important stuff happens...
                mVDrawingColoring.doTouchEvent(event.getAction() & MotionEvent.ACTION_MASK, x - mTempRect.left, y - mTempRect.top);
            }

            mBeforeTouchPosX = x;
            mBeforeTouchPosY = y;
            return true;
        }
    };

    public void placeVectors(View v) {
        String tag = (String) v.getTag();
        Log.i("PLACE_VECTOR", tag);

        // hide it
        findViewById(R.id.choose_vector_symbol_menu).setVisibility(View.INVISIBLE);

        // VECTOR_MENU... also set the
        mVDrawingColoring.setParellelMode(PLACE);
        mVDrawingColoring.setCurrentSymbolVector(Integer.parseInt(tag.substring(1)) - 1); // this is terrible
    }

    public void populateVectors(View v) {
        String tag = (String) v.getTag();
        Log.i("POPULATE_VECTOR", tag);

        // hide it
        findViewById(R.id.choose_vector_symbol_menu).setVisibility(View.INVISIBLE);

        // VECTOR_MENU
        mVDrawingColoring.setCurrentSymbolVector(Integer.parseInt(tag.substring(1)) - 1); // this is terrible

        launchGleaphGallery();
    }

    ////////////////////////////////////////////////////////////////////////////////

    protected void setMode(ViewDrawingColoring.MODE mode) {
        mVDrawingColoring.setMode(mode);

        if (mode == ViewDrawingColoring.MODE.COLORING) {
            mVChangeBg.setImageResource(mbSmallLCD ? R.drawable.selector_coloring_bg_select_s : R.drawable.selector_coloring_bg_select);

            mImagesColoring = new ArrayList<>();
            mThumbnailsColoring = new ArrayList<>();

            String[] list = Util.getFileListFromAssets(mThisActivity, Const.COLORING_IMAGE_PATH_FROM_ASSETS);
            if (list != null) {
                for (int i = 0; i < list.length; ++i) {
                    if (list[i].contains("_thumb")) {
                        mThumbnailsColoring.add(list[i]);

                    } else {
                        mImagesColoring.add(list[i]);
                    }
                }
            }

            Collections.sort(mImagesColoring, new Comparator<String>() {
                @Override
                public int compare(String s, String t1) {
                    return s.compareTo(t1);
                }
            });

            Collections.sort(mThumbnailsColoring, new Comparator<String>() {
                @Override
                public int compare(String s, String t1) {
                    return s.compareTo(t1);
                }
            });

        } else {
            mVChangeBg.setImageResource(mbSmallLCD ? R.drawable.selector_drawing_bg_select_s : R.drawable.selector_drawing_bg_select);

        }
    }

    /**
     * Pen Color 선택
     *
     * @param colorIndex
     */
    protected void setPenColor(int colorIndex, boolean bEffectSound) {
        if (mVPens[colorIndex].isSelected() == true) {
            return;
        }

        for (int i = 0; i < TOTAL_PEN_COLOR_COUNT; ++i) {
            mVPens[i].setSelected(false);
            mVSelectedPensEffect[i].setSelected(false);
        }

        mVPens[colorIndex].setSelected(true);
        mVSelectedPensEffect[colorIndex].setSelected(true);

        mVDrawingColoring.setPenColor(PEN_COLORS[colorIndex]);

        if (mVDrawingColoring.getMode() == ViewDrawingColoring.MODE.DRAWING) {
            mPreference.setConfig(Const.PARAM_PEN_COLOR_INDEX_IN_DRAWING, colorIndex);

        } else {
            mPreference.setConfig(Const.PARAM_PEN_COLOR_INDEX_IN_COLORING, colorIndex);

        }

        if (bEffectSound == true) {
            mEffectSound.startSoundPool(EffectSound.SOUND_PICK_CRAYON, 0);
        }
    }

    /**
     * Background Color 선택
     *
     * @param colorIndex
     */
    protected void setBGColor(int colorIndex, boolean bEffectSound) {
        mVDrawingColoring.setBackgroundColor(BG_COLORS[colorIndex]);

        if (mVDrawingColoring.getMode() == ViewDrawingColoring.MODE.DRAWING) {
            mPreference.setConfig(Const.PARAM_BG_COLOR_INDEX, colorIndex);
        }

        if (bEffectSound == true) {
            mEffectSound.startSoundPool(EffectSound.SOUND_NEW_BOARD, 0);
        }
    }

    /**
     * Coloring 에서 이미지 선택
     *
     * @param coloringImageIndex
     */
    protected void setColoringImage(int coloringImageIndex, boolean bEffectSound) {
        Bitmap bitmap = Util.getBitmapFromAssets(mThisActivity, Const.COLORING_IMAGE_PATH_FROM_ASSETS + File.separator + mImagesColoring.get(coloringImageIndex));
        if (bitmap != null) {
            mIvColoring.setImageBitmap(bitmap);
            mPreference.setConfig(Const.PARAM_BG_IMAGE_INDEX, coloringImageIndex);

            if (bEffectSound == true) {
                mEffectSound.startSoundPool(EffectSound.SOUND_NEW_BOARD, 0);
            }
        }
    }

    protected void checkSaveButton() {
        mVSave.setEnabled(mbNeedSave);
    }
}
