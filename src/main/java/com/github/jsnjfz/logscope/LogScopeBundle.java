package com.github.jsnjfz.logscope;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

/**
 * Central access point for localized strings.
 */
public final class LogScopeBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "messages.LogScopeBundle";

    private static final LogScopeBundle INSTANCE = new LogScopeBundle();

    private LogScopeBundle() {
        super(BUNDLE);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
