package com.herfy.loyalty.objects;

public abstract class ListItem {
    public static final int TYPE_DATE = 0;
    public static final int TYPE_LOYALTY = 1;
    abstract public int getType();
}
