package com.example.thermalprinter.models;

public class FacetypeModel {

    int start, end, type;

    public FacetypeModel() {
    }

    public FacetypeModel(int start, int end, int type) {
        this.start = start;
        this.end = end;
        this.type = type;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
