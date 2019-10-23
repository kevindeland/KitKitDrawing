package com.enuma.drawingcoloring.types;

import android.graphics.Color;

import java.util.List;

/**
 * KStroke
 * <p>A colored path.</p>
 * Created by kevindeland on 2019-10-15.
 */
public class KStroke {

    private int _color;
    private KPath _path;

    // UNDO this should have a type of Stroke, e.g. Radial(1,2,8) or Parallel(P,V)

    public KStroke(int color) {
        _color = color;
        _path = new KPath();
    }

    public int getColor() {
        return _color;
    }

    public void setColor(int color) {
        _color = color;
    }

    public void addPoint(KPoint point) {
        _path.addPoint(point);
    }

    public int getSize() {
        return _path.getSize();
    }

    public KPoint getPoint(int i) {
        return _path.getPoint(i);
    }

    public KPath getPath() {
        return _path;
    }
}
