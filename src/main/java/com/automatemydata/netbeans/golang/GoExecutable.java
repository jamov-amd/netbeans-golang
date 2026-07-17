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

import java.io.File;

/**
 * Finds the Go command-line tools.
 *
 * <p>{@code go} and {@code gopls} are found in different places and are looked up separately:
 * {@code go} ships in {@code GOROOT/bin} as part of the Go distribution, while {@code gopls} is
 * installed as a module and lands in {@code GOBIN} or {@code GOPATH/bin}. Both are normally on
 * the {@code PATH}, which is why that is tried first in each case.
 *
 * <p>All lookups here touch the filesystem, so call them off the event thread.
 */
public final class GoExecutable {

    private GoExecutable() {
    }

    /**
     * Locates the {@code go} tool: {@code PATH}, then {@code $GOROOT/bin}, then the default
     * install location for the platform.
     *
     * @return an absolute path to go, or {@code null} if it was not found
     */
    public static String findGo() {
        String executable = exeName("go");

        String found = onPath(executable);
        if (found != null) {
            return found;
        }

        found = executableIn(System.getenv("GOROOT"), executable);
        if (found == null) {
            found = executableIn(new File(orEmpty(System.getenv("GOROOT")), "bin").getPath(), executable);
        }
        if (found != null) {
            return found;
        }

        for (String dir : defaultGoRootBins()) {
            found = executableIn(dir, executable);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String[] defaultGoRootBins() {
        if (isWindows()) {
            return new String[]{
                "C:\\Program Files\\Go\\bin",
                "C:\\Go\\bin",
                System.getenv("LOCALAPPDATA") + "\\Programs\\Go\\bin"
            };
        }
        return new String[]{"/usr/local/go/bin", "/usr/lib/go/bin", "/opt/homebrew/bin"};
    }

    /** @return the executable's name with the platform's suffix, e.g. {@code go.exe} on Windows */
    public static String exeName(String base) {
        return isWindows() ? base + ".exe" : base;
    }

    /** @return the first directory on {@code PATH} holding {@code executable}, or {@code null} */
    public static String onPath(String executable) {
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        for (String dir : path.split(File.pathSeparator)) {
            String found = executableIn(dir, executable);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** @return the absolute path to {@code executable} in {@code dir}, or {@code null} */
    public static String executableIn(String dir, String executable) {
        if (dir == null || dir.isBlank()) {
            return null;
        }
        File candidate = new File(dir.trim(), executable);
        return candidate.isFile() && candidate.canExecute() ? candidate.getAbsolutePath() : null;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
