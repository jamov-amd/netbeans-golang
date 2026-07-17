/*
 * Copyright 2026 Automate My Data, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.automatemydata.netbeans.golang;

import java.util.prefs.Preferences;

import org.openide.util.NbPreferences;

/**
 * The plugin's stored settings.
 *
 * <p>{@code NbPreferences.forModule} keys the node by the module's code name base rather than by
 * the given class's package, so every caller here shares one node regardless of which class it
 * passes. Routing all access through this class keeps that guarantee obvious rather than
 * incidental.
 */
public final class GoOptions {

    /** Preferences key holding a user-configured absolute path to the gopls executable. */
    public static final String PREF_GOPLS_PATH = "goplsPath";

    private GoOptions() {
    }

    public static Preferences prefs() {
        return NbPreferences.forModule(GoOptions.class);
    }

    /**
     * @return the configured gopls path, or an empty string when the user has not set one and
     *         gopls should be auto-detected
     */
    public static String goplsPath() {
        return prefs().get(PREF_GOPLS_PATH, "");
    }

    /**
     * Stores the gopls path. A blank value removes the setting, restoring auto-detection.
     */
    public static void setGoplsPath(String path) {
        if (path == null || path.isBlank()) {
            prefs().remove(PREF_GOPLS_PATH);
        } else {
            prefs().put(PREF_GOPLS_PATH, path.trim());
        }
    }
}
