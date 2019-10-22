package com.enuma.drawingcoloring.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.enuma.drawingcoloring.R;
import com.enuma.drawingcoloring.activity.base.BaseActivity;
import com.enuma.drawingcoloring.core.Const;
import com.enuma.drawingcoloring.types.KPath;
import com.enuma.drawingcoloring.types.KPoint;
import com.enuma.drawingcoloring.utility.Log;
import com.enuma.drawingcoloring.view.ViewGleaphDisplay;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * drawing
 * <p>
 * Created by kevindeland on 2019-10-19.
 */
public class GleaphGalleryActivity extends BaseActivity {

    private Gson _gson = new Gson();

    ViewGleaphDisplay mVGleaph1;
    ViewGleaphDisplay mVGleaph2;
    ViewGleaphDisplay mVGleaph3;
    ViewGleaphDisplay mVGleaph4;
    ViewGleaphDisplay mVGleaph5;
    ViewGleaphDisplay mVGleaph6;
    ViewGleaphDisplay[] mViews;

    ViewGleaphDisplay mSelected;

    Button mLoadButton;

    Button mChooseMeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gleaph_gallery);
        setupViews();
    }

    private void setupViews() {
        mVGleaph1 = (ViewGleaphDisplay) findViewById(R.id.v_gleaph_1);
        mVGleaph2 = (ViewGleaphDisplay) findViewById(R.id.v_gleaph_2);
        mVGleaph3 = (ViewGleaphDisplay) findViewById(R.id.v_gleaph_3);
        mVGleaph4 = (ViewGleaphDisplay) findViewById(R.id.v_gleaph_4);

        mViews = new ViewGleaphDisplay[]{mVGleaph1, mVGleaph2, mVGleaph3, mVGleaph4};

        GleaphClickListener click = new GleaphClickListener();

        mVGleaph1.setOnClickListener(click);
        mVGleaph2.setOnClickListener(click);
        mVGleaph3.setOnClickListener(click);
        mVGleaph4.setOnClickListener(click);

        /*View.OnClickListener disappear = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChooseMeButton != null) {
                    mChooseMeButton.setVisibility(View.INVISIBLE);
                }

                mChooseMeButton = (Button) v;
                mChooseMeButton.setVisibility(View.VISIBLE);
            }
        };*/

        /*findViewById(R.id.v_choose_1).setOnClickListener(disappear);*/

        mLoadButton = (Button) findViewById(R.id.v_load_button);
        mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                try {
                    File folder = new File(Const.SAVE_GLEAPH_PATH);

                    File[] files = folder.listFiles();

                    for (int i=0; i < Math.min(files.length, mViews.length); i++) {

                        File file = files[i];
                        mViews[i].loadJsonGleaphIntoMe(file);
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    class GleaphClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            if (mSelected != null) {
                mSelected.setBackgroundColor(getResources().getColor(R.color.bg_color_0));
            }
            mSelected = (ViewGleaphDisplay) v;
            v.setBackgroundColor(getResources().getColor(R.color.bg_color_4));


            KPath returnMe = mSelected.getPath();
            // TODO somehow pass back a KPath as Intent
            Intent gleaphIntent = new Intent();
            gleaphIntent.putExtra("GLEAPH", _gson.toJson(returnMe));
            setResult(Activity.RESULT_OK, gleaphIntent);
            Log.i("XYZ", "onClick");
            finish();
        }
    }
}
