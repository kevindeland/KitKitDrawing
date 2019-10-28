package com.enuma.drawingcoloring.files;

import com.enuma.drawingcoloring.core.Const;
import com.enuma.drawingcoloring.types.KPath;
import com.enuma.drawingcoloring.types.KStroke;
import com.enuma.drawingcoloring.utility.Log;
import com.enuma.drawingcoloring.utility.Util;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * FileObjectLoader
 * <p>For loading/saving JSON objects from/to files</p>
 * Created by kevindeland on 2019-10-24.
 */
public class FileObjectInterface {

    private static Gson _gson = new Gson();

    /**
     * save a path to JSON
     * @param path
     */
    public static void savePathAsGleaphJson(KPath path) {
        String writeme = _gson.toJson(path);
        Log.i("JSON", writeme);
        // SAVE ME

        FileWriter file = null;
        try {
            Calendar calendar = Calendar.getInstance();
            String TIME_FORMAT = "yyyy-MM-dd-HH-mm-ss";
            String filename = Util.getTimeFormatString(TIME_FORMAT, calendar.getTimeInMillis()) + ".json";
            file = new FileWriter(Const.SAVE_GLEAPH_PATH + "/" + filename);
            file.write(writeme);
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load all the gleaphs from the gallery
     * @return List of KPaths
     */
    public static List<KPath> loadAllGleaphs() {
        List<KPath> paths = new ArrayList<>();

        try {
            File folder = new File(Const.SAVE_GLEAPH_PATH);

            for (final File file : folder.listFiles()) {

                BufferedReader br = new BufferedReader(
                        new FileReader(file)
                );
                KPath path = _gson.fromJson(br, KPath.class);
                paths.add(path);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return paths;
    }



    /**
     * Save the List of KStroke (__thisPainting) as a JSON value
     */
    public static void savePaintingAsJson(List<KStroke> painting) {
        String writeme = _gson.toJson(painting);

        FileWriter file = null;
        try {
            Calendar calendar = Calendar.getInstance();
            String TIME_FORMAT = "yyyy-MM-dd-HH-mm-ss";
            String filename = Util.getTimeFormatString(TIME_FORMAT, calendar.getTimeInMillis()) + ".json";
            file = new FileWriter(Const.SAVE_PAINTING_PATH + "/" + filename);
            file.write(writeme);
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @return List of KStroke, which is a Painting.
     */
    public static List<KStroke> loadLastPainting() {
        try {
            File folder = new File(Const.SAVE_PAINTING_PATH);

            File[] files = folder.listFiles();
            if (files.length == 0) return null;

            File loadme = files[files.length - 1];

            BufferedReader br = new BufferedReader((new FileReader(loadme)));

            return _gson.fromJson(br, new TypeToken<List<KStroke>>() {
            }.getType());

        } catch (
                FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
