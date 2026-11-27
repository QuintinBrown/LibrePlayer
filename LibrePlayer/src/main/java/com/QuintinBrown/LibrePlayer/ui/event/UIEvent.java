package com.QuintinBrown.LibrePlayer.ui.event;

public class UIEvent {
    private final UIEventType type;
    private final Object data;

    public UIEvent(UIEventType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public UIEventType getType(){return type;}
    public Object getData(){return data;}
}
