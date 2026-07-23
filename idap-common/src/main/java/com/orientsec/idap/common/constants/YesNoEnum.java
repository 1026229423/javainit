package com.orientsec.idap.common.constants;

import java.util.HashMap;
import java.util.Map;

public enum YesNoEnum {
    NIL(-1,"未知",""),
    NO(0,"NO","否"),
    Yes(1,"Yes","是"),
    ;

    int value;
    String name;
    String desc;

    YesNoEnum(int value, String name, String desc) {
        this.value = value;
        this.name = name;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    private static final Map<Integer, YesNoEnum> valueMapping = new HashMap<>();

    static {
        YesNoEnum[] enumArray = YesNoEnum.values();
        for (YesNoEnum _enum : enumArray) {
            valueMapping.put(_enum.getValue(), _enum);
        }
    }

    public static YesNoEnum getByValue(Integer value) {
        YesNoEnum valEnum = valueMapping.get(value);
        return null == valEnum ? NIL : valEnum;
    }
}
