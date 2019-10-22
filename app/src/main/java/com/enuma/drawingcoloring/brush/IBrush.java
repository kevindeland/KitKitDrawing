package com.enuma.drawingcoloring.brush;

import android.graphics.Canvas;

/**
 * IBrush
 * <p>Interface that can draw lines with colors</p>
 * Created by kevindeland on 2019-10-22.
 */
public interface IBrush {

    /**
     * Change pen color.
     * @param color color
     */
    void setPenColor(int color);

    /**
     * Draw a line on {@code canvas} from ({@code startX}, {@code startY}) to
     * ({@code endX}, {@code endY})
     *
     * @param canvas canvas to draw on
     * @param startX startX
     * @param startY startY
     * @param endX endX
     * @param endY endY
     */
    void drawLineWithBrush(Canvas canvas, int startX, int startY, int endX, int endY);
}
