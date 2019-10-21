package com.enuma.drawingcoloring.activity;

/**
 * drawing
 * <p>
 * Created by kevindeland on 2019-10-14.
 */
public interface GleaphHolder {

    /**
     * Add a GleaphFrame translated to position (left, top).
     *
     * @param left x
     * @param top y
     */
    void addOneGleaphFrame(int left, int top);

    /**
     * Remove all translated GleaphFrames.
     */
    void removeAllGleaphFrames();

    /**
     * Draw a GleaphFrame translated to position (left, top) and rotated to angle (angle).
     * These angled frames must be drawn constantly while the user selects its position and angle,
     * so this method is only for drawing, and not for saving.
     *
     * @param left x
     * @param top y
     * @param angle theta
     */
    void drawAngledGleaphFrame(int left, int top, int angle);

    /**
     * Add an angled GleaphFrame to the list of angled GleaphFrames.
     *
     * @param left x
     * @param top y
     * @param angle theta
     */
    void saveAngledGleaphFrame(int left, int top, int angle);
}
