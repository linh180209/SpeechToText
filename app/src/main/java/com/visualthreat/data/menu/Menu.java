package com.visualthreat.data.menu;

import android.util.Log;

import com.visualthreat.data.activity.EventPressMenu;

import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by USER on 1/3/2017.
 */

public class Menu implements RadialMenuWidget.RadialMenuEntry {
    private String name;
    private String lable;
    private int icon;
    private EventPressMenu mEventPressMenu;

    public void SetEventPressMenu(EventPressMenu eventPressMenu){
        mEventPressMenu = eventPressMenu;
    }
    public Menu(String name, String lable, int icon){
        this.name = name;
        this.lable = lable;
        this.icon = icon;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLable() {
        return lable;
    }

    public void setLable(String lable) {
        this.lable = lable;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLabel() {
        return lable;
    }

    @Override
    public int getIcon() {
        return icon;
    }

    @Override
    public List<RadialMenuWidget.RadialMenuEntry> getChildren() {
        return null;
    }

    @Override
    public void menuActiviated(String name) {
        mEventPressMenu.OnClickMenu(name);
    }
}
