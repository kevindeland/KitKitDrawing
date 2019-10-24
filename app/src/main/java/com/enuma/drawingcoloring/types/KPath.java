package com.enuma.drawingcoloring.types;

import java.util.ArrayList;
import java.util.List;

/**
 * KPath
 * <p>An ordered List of points.</p>
 * Created by kevindeland on 2019-10-15.
 */
public class KPath {

    private List<KPoint> path;

    public KPath() {
        path = new ArrayList<>();
    }

    public void addPoint(KPoint point) {
        path.add(point);
    }

    public int getSize() {
        return path.size();
    }

    public KPoint getPoint(int i) {
        return path.get(i);
    }

    public List<KPoint> getPath() {
        return path;
    }

    public void setPath(List<KPoint> path) {
        this.path = path;
    }
}
