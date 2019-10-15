package com.enuma.drawingcoloring.activity;

/**
 * drawing
 * <p>
 * Created by kevindeland on 2019-10-14.
 */
public interface GleaphHolder {

    void addOneGleaphFrame(int left, int top);

    void removeAllGleaphFrames();

    void drawAngledGleaphFrame(int left, int top, int angle);

    void saveAngledGleaphFrame(int left, int top, int angle);
}
