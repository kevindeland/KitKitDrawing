package com.enuma.drawingcoloring.types;

import android.graphics.Color;

import com.enuma.drawingcoloring.view.ViewDrawingColoring;

import java.util.Collection;
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

    // RADIAL MODE
    private ViewDrawingColoring.RADIAL_MODE radialMode;
    public ViewDrawingColoring.RADIAL_MODE getRadialMode() {
        return radialMode;
    }
    public void setRadialMode(ViewDrawingColoring.RADIAL_MODE radialMode) {
        this.radialMode = radialMode;
    }

    // PARALLEL_MODE
    private ViewDrawingColoring.VECTOR_MODE vectorMode;
    public ViewDrawingColoring.VECTOR_MODE getVectorMode() {
        return vectorMode;
    }
    public void setVectorMode(ViewDrawingColoring.VECTOR_MODE vectorMode) {
        this.vectorMode = vectorMode;
    }

    private Collection<KPoint> points;

    public Collection<KPoint> getPoints() {
        return points;
    }

    public void setPoints(Collection<KPoint> points) {
        this.points = points;
    }

    private Collection<KUnitVector> vectors;

    public Collection<KUnitVector> getVectors() {
        return vectors;
    }

    public void setVectors(Collection<KUnitVector> vectors) {
        this.vectors = vectors;
    }
}
