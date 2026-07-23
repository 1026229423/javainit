package com.orientsec.idap.core.constant.bean;
import java.util.ArrayList;
import java.util.List;

public class ConstDesc {
    public String name;
    public String note;
    public List<ValueDesc> values = new ArrayList<>();
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }
    public List<ValueDesc> getValues() {
        return values;
    }
    public void setValues(List<ValueDesc> values) {
        this.values = values;
    }
    public void add(ValueDesc newVal) {
        this.values.add(newVal);
    }
    @Override
    public String toString() {
        return "ConstDesc [name=" + name + ", note=" + note + ", values=" + values + "]";
    }

}
