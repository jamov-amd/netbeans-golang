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
package com.automatemydata.netbeans.golang.project;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openide.filesystems.FileObject;
import org.openide.util.NbPreferences;

/**
 * Per-project Run settings.
 *
 * <p>Kept in the IDE's preferences, keyed by project directory, rather than in a file inside the
 * project: arguments tend to name paths and servers on one developer's machine, and a Go module
 * should not grow IDE-specific files just because it was opened here.
 */
final class GoRunConfig {

    private static final String KEY_RUN_ARGUMENTS = "runArguments";

    /** {@code "quoted string"} or a bare run of non-space characters. */
    private static final Pattern TOKEN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    private GoRunConfig() {
    }

    static Preferences prefs(FileObject projectDir) {
        return NbPreferences.forModule(GoProject.class)
                .node("projects")
                .node(nodeName(projectDir));
    }

    /** @return the raw argument string the user typed, or an empty string */
    static String runArguments(FileObject projectDir) {
        return prefs(projectDir).get(KEY_RUN_ARGUMENTS, "");
    }

    static void setRunArguments(FileObject projectDir, String arguments) {
        if (arguments == null || arguments.isBlank()) {
            prefs(projectDir).remove(KEY_RUN_ARGUMENTS);
        } else {
            prefs(projectDir).put(KEY_RUN_ARGUMENTS, arguments.trim());
        }
    }

    /**
     * Splits an argument string into individual arguments, keeping {@code "quoted sections"}
     * together so paths with spaces survive.
     */
    static List<String> tokenize(String arguments) {
        List<String> tokens = new ArrayList<>();
        if (arguments == null || arguments.isBlank()) {
            return tokens;
        }
        Matcher m = TOKEN.matcher(arguments);
        while (m.find()) {
            tokens.add(m.group(1) != null ? m.group(1) : m.group(2));
        }
        return tokens;
    }

    /**
     * Preferences node names are limited to 80 characters and cannot contain a slash, so a full
     * project path will not do. The directory name keeps the node recognisable when browsing
     * preferences; the hash is what actually distinguishes two projects of the same name.
     */
    private static String nodeName(FileObject projectDir) {
        String name = projectDir.getNameExt().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.length() > 40) {
            name = name.substring(0, 40);
        }
        return name + "-" + Integer.toHexString(projectDir.getPath().hashCode());
    }
}
