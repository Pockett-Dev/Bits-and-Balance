package org.onenonly.bitsandbalance.common.inventory;

/** Which sorting mode to apply when the sort button is clicked. */
public enum SortInventoryMode {
    DEFAULT(0, "Default"),
    DESCENDING(1, "Descending"),
    ASCENDING(2, "Ascending");

    private final int id;
    private final String displayName;

    SortInventoryMode(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public SortInventoryMode next() {
        return switch (this) {
            case DEFAULT -> DESCENDING;
            case DESCENDING -> ASCENDING;
            case ASCENDING -> DEFAULT;
        };
    }

    public static SortInventoryMode fromId(int id) {
        for (SortInventoryMode m : values()) {
            if (m.id == id) return m;
        }
        return DEFAULT;
    }
}
