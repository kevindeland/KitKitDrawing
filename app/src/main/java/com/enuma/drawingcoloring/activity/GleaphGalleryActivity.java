package com.enuma.drawingcoloring.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.enuma.drawingcoloring.R;
import com.enuma.drawingcoloring.activity.base.BaseActivity;
import com.enuma.drawingcoloring.core.Const;
import com.enuma.drawingcoloring.view.ViewGleaphDisplay;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * drawing
 * <p>
 * Created by kevindeland on 2019-10-19.
 */
public class GleaphGalleryActivity extends BaseActivity {

    ViewGleaphDisplay mVGleaph1;
    ViewGleaphDisplay mVGleaph2;
    ViewGleaphDisplay mVGleaph3;
    ViewGleaphDisplay mVGleaph4;
    ViewGleaphDisplay mVGleaph5;
    ViewGleaphDisplay mVGleaph6;

    Button mLoadButton;

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

        mLoadButton = (Button) findViewById(R.id.v_load_button);
        mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ViewGleaphDisplay[] views = {mVGleaph1, mVGleaph2, mVGleaph3, mVGleaph4};

                try {
                    File folder = new File(Const.SAVE_GLEAPH_PATH);

                    File[] files = folder.listFiles();

                    for (int i=0; i < Math.min(files.length, views.length); i++) {

                        File file = files[i];
                        views[i].loadJsonGleaphIntoMe(file);
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
