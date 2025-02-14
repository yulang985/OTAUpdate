package cn.wch.otalib.entry;

public enum ChipType {
    CH583("CH583"),
    CH573("CH573"),
    CH579("CH579"),
    CH32V208("CH32V208"),
    CH32F208("CH32F208"),
    UNKNOWN("unKnown");

    private final String description;

    ChipType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
