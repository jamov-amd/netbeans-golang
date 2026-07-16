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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.modules.lsp.client.spi.LanguageServerProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;

/**
 * Starts <a href="https://go.dev/gopls/">gopls</a>, the official Go language server, and hands its
 * stdio streams to the NetBeans LSP client.
 *
 * <p>Everything the plugin offers beyond syntax coloring — completion, hover, go to declaration,
 * find usages, diagnostics and formatting — comes from gopls through this provider.
 *
 * <p>gopls is not bundled; the user installs it with
 * {@code go install golang.org/x/tools/gopls@latest}. See {@link #findGopls()} for how it is
 * located.
 */
@MimeRegistrations({
    @MimeRegistration(mimeType = GoLanguage.GO_MIME_TYPE, service = LanguageServerProvider.class),
    @MimeRegistration(mimeType = GoLanguage.GO_MOD_MIME_TYPE, service = LanguageServerProvider.class)
})
public final class GoLanguageServerProvider implements LanguageServerProvider {

    private static final Logger LOG = Logger.getLogger(GoLanguageServerProvider.class.getName());

    /** Preferences key holding a user-configured absolute path to the gopls executable. */
    public static final String PREF_GOPLS_PATH = "goplsPath";

    private static final String INSTALL_COMMAND = "go install golang.org/x/tools/gopls@latest";

    /** Guards the "gopls not found" dialog so it appears at most once per IDE session. */
    private static final AtomicBoolean MISSING_REPORTED = new AtomicBoolean();

    @Override
    public LanguageServerDescription startServer(Lookup lookup) {
        String gopls = findGopls();
        if (gopls == null) {
            reportMissing();
            return null;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(gopls, "serve");
            Process process = pb.start();
            drainStderr(process);
            LOG.log(Level.INFO, "Started gopls: {0}", gopls);
            return LanguageServerDescription.create(
                    process.getInputStream(), process.getOutputStream(), process);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Could not start gopls from " + gopls, ex);
            return null;
        }
    }

    /**
     * Locates the gopls executable, in order of preference:
     * <ol>
     *   <li>an explicit path configured in the plugin preferences,</li>
     *   <li>{@code gopls} on the {@code PATH},</li>
     *   <li>{@code $GOBIN},</li>
     *   <li>{@code $GOPATH/bin} (first entry, if {@code GOPATH} is set),</li>
     *   <li>the default module bin directory, {@code ~/go/bin}.</li>
     * </ol>
     *
     * @return an absolute path to an executable gopls, or {@code null} if none was found
     */
    static String findGopls() {
        String configured = NbPreferences.forModule(GoLanguageServerProvider.class)
                .get(PREF_GOPLS_PATH, null);
        if (configured != null && !configured.isBlank()) {
            File f = new File(configured.trim());
            if (f.canExecute()) {
                return f.getAbsolutePath();
            }
            LOG.log(Level.WARNING, "Configured gopls path is not executable: {0}", configured);
        }

        String executable = isWindows() ? "gopls.exe" : "gopls";

        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                String found = executableIn(dir, executable);
                if (found != null) {
                    return found;
                }
            }
        }

        String found = executableIn(System.getenv("GOBIN"), executable);
        if (found != null) {
            return found;
        }

        String gopath = System.getenv("GOPATH");
        if (gopath != null && !gopath.isBlank()) {
            // GOPATH may list several roots; go install writes to the first one.
            String first = gopath.split(File.pathSeparator)[0];
            found = executableIn(new File(first, "bin").getPath(), executable);
            if (found != null) {
                return found;
            }
        }

        String home = System.getProperty("user.home");
        if (home != null) {
            found = executableIn(new File(home, "go" + File.separator + "bin").getPath(), executable);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static String executableIn(String dir, String executable) {
        if (dir == null || dir.isBlank()) {
            return null;
        }
        File candidate = new File(dir.trim(), executable);
        return candidate.isFile() && candidate.canExecute() ? candidate.getAbsolutePath() : null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    /**
     * Consumes the server's stderr on a daemon thread. Without this the pipe fills up and gopls
     * blocks; the output is worth keeping for diagnosis, so it goes to the IDE log.
     */
    private static void drainStderr(Process process) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    LOG.log(Level.FINE, "gopls: {0}", line);
                }
            } catch (IOException ex) {
                LOG.log(Level.FINE, "Reading gopls stderr failed", ex);
            }
        }, "gopls stderr reader");
        t.setDaemon(true);
        t.start();
    }

    @Messages({
        "MSG_GoplsNotFound=The Go language server (gopls) could not be found, so code completion, "
                + "diagnostics and navigation are unavailable. Syntax highlighting still works.\n\n"
                + "Install it with:\n\n    " + INSTALL_COMMAND + "\n\n"
                + "The plugin looks for gopls on your PATH, in $GOBIN, and in $GOPATH/bin."
    })
    private static void reportMissing() {
        LOG.log(Level.WARNING, "gopls not found on PATH, GOBIN, GOPATH/bin or ~/go/bin");
        if (MISSING_REPORTED.compareAndSet(false, true)) {
            DialogDisplayer.getDefault().notifyLater(
                    new NotifyDescriptor.Message(
                            Bundle.MSG_GoplsNotFound(), NotifyDescriptor.WARNING_MESSAGE));
        }
    }
}
