package com.enuma.drawingcoloring.types;

/**
 * drawing
 * <p>
 * Created by kevindeland on 2019-10-28.
 */
public class KSymbolVector {

    int id;
    String name;
    int drawable;

    public KSymbolVector(int id, String name, int drawable) {
        this.id = id;
        this.name = name;
        this.drawable = drawable;
    }

    public String getName() {
        return name;
    }

    public int getDrawable() {
        return drawable;
    }

    /**
     * I know, this is terrible.
     * @return just return the id
     */
    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KSymbolVector other = (KSymbolVector) obj;
        return id == other.id;
    }
}
