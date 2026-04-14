package org.onenonly.bitsandbalance.common.inventory;

/** Which inventory should be sorted when the sort button is clicked. */
public enum SortInventoryTarget {
    PLAYER(0),
    CONTAINER(1);

    private final int id;

    SortInventoryTarget(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static SortInventoryTarget fromId(int id) {
        for (SortInventoryTarget t : values()) {
            if (t.id == id) return t;
        }
        return PLAYER;
    }
}
