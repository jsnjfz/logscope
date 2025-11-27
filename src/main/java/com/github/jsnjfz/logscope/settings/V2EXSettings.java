package com.github.jsnjfz.logscope.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.ui.JBColor;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores plugin-wide settings and notifies listeners about changes.
 */
@State(
        name = "com.github.jsnjfz.logscope.settings.V2EXSettings",
        storages = @Storage("logscopeSettings.xml")
)
public class V2EXSettings implements PersistentStateComponent<V2EXSettings> {

    // API Token
    public String apiToken = "";
    // Session cookie for reply actions
    public String sessionCookie = "";

    // Proxy settings
    public boolean useProxy = false;
    public String proxyHost = "127.0.0.1";
    public int proxyPort = 10808;
    public String proxyType = "SOCKS";

    // Font settings
    public String fontFamily = "Consolas";
    public int fontSize = 14;
    public Color fontColor = JBColor.BLACK;

    // Topic list limit
    public int topicDisplayLimit = 10;

    private final List<SettingsChangeListener> listeners = new ArrayList<>();

    public void addChangeListener(SettingsChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(SettingsChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifySettingsChanged() {
        for (SettingsChangeListener listener : listeners) {
            listener.onSettingsChanged();
        }
    }

    public interface SettingsChangeListener {
        void onSettingsChanged();
    }

    public static V2EXSettings getInstance() {
        return ApplicationManager.getApplication().getService(V2EXSettings.class);
    }

    @Override
    public @Nullable V2EXSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull V2EXSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}



