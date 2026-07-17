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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.automatemydata.netbeans.golang.GoExecutable;

import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.base.ProcessBuilder;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ActionProgress;
import org.netbeans.spi.project.ActionProvider;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 * Runs {@code go} for the Build, Clean, Rebuild, Run and Test actions.
 *
 * <p>Output goes to the Output window, where {@link GoErrorLineConvertor} turns diagnostics into
 * links. Each action maps to the command a Go developer would type by hand — the toolchain is the
 * authority on what building and testing mean, and there is nothing IDE-specific to add.
 */
final class GoActionProvider implements ActionProvider {

    private static final String[] SUPPORTED = {
        COMMAND_BUILD,
        COMMAND_CLEAN,
        COMMAND_REBUILD,
        COMMAND_RUN,
        COMMAND_TEST,
    };

    /** How long to wait for {@code go list} when working out what Run should run. */
    private static final int LIST_TIMEOUT_SECONDS = 60;

    private final GoProject project;

    GoActionProvider(GoProject project) {
        this.project = project;
    }

    @Override
    public String[] getSupportedActions() {
        return SUPPORTED.clone();
    }

    /**
     * Every supported command is always available: a {@code go.mod} is what makes this a project,
     * and that is all any of them need. Whether there is something to run is reported by
     * {@code go} itself — deciding here would mean loading the module on every menu paint.
     */
    @Override
    public boolean isActionEnabled(String command, Lookup context) {
        return Arrays.asList(SUPPORTED).contains(command);
    }

    @Messages({
        "# {0} - project name",
        "# {1} - the action being run, e.g. build",
        "LBL_GoOutputTab={0} - go {1}",
        "ERR_GoNotFound=Could not find the go tool. Install Go from https://go.dev/dl/ and make "
                + "sure it is on your PATH."
    })
    @Override
    public void invokeAction(String command, Lookup context) {
        // The event thread: cheap work only. Everything that blocks belongs in the Callable.
        ActionProgress progress = ActionProgress.start(context);
        File dir = FileUtil.toFile(project.getProjectDirectory());
        String runArguments = COMMAND_RUN.equals(command)
                ? GoRunConfig.runArguments(project.getProjectDirectory())
                : "";
        String tab = Bundle.LBL_GoOutputTab(
                ProjectUtils.getInformation(project).getDisplayName(), command);

        // Run and Test are worth watching, so their window opens. Build and Clean have nothing to
        // say when they succeed, so they stay out of the way until something fails.
        boolean watchable = COMMAND_RUN.equals(command) || COMMAND_TEST.equals(command);

        ExecutionDescriptor descriptor = new ExecutionDescriptor()
                .controllable(true)
                .frontWindow(watchable)
                .frontWindowOnError(true)
                .showProgress(true)
                .outLineBased(true)
                .errLineBased(true)
                .inputVisible(true)
                .outConvertorFactory(() -> new GoErrorLineConvertor(dir))
                .errConvertorFactory(() -> new GoErrorLineConvertor(dir))
                .postExecution(exitCode -> progress.finished(exitCode != null && exitCode == 0));

        ExecutionService.newService(() -> startGo(dir, command, runArguments), descriptor, tab).run();
    }

    /**
     * Runs on an execution thread, so it may block. Saving first matches every other NetBeans
     * project type: what gets built is what is on disk.
     */
    private static Process startGo(File dir, String command, String runArguments) throws IOException {
        LifecycleManager.getDefault().saveAll();

        String go = GoExecutable.findGo();
        if (go == null) {
            throw new IOException(Bundle.ERR_GoNotFound());
        }

        ProcessBuilder builder = ProcessBuilder.getLocal();
        builder.setExecutable(go);
        builder.setWorkingDirectory(dir.getAbsolutePath());
        builder.setArguments(argsFor(command, go, dir, runArguments));
        return builder.call();
    }

    private static List<String> argsFor(String command, String go, File dir, String runArguments)
            throws IOException {
        switch (command) {
            case COMMAND_BUILD:
                return List.of("build", "./...");
            case COMMAND_CLEAN:
                return List.of("clean", "./...");
            case COMMAND_REBUILD:
                // -a forces a rebuild of everything, which is what Rebuild means elsewhere.
                return List.of("build", "-a", "./...");
            case COMMAND_TEST:
                return List.of("test", "./...");
            case COMMAND_RUN: {
                // Anything after the package is go's to hand on to the program untouched.
                List<String> args = new ArrayList<>(List.of("run", packageToRun(go, dir)));
                args.addAll(GoRunConfig.tokenize(runArguments));
                return args;
            }
            default:
                throw new IllegalArgumentException(command);
        }
    }

    /**
     * Works out which package Run should execute.
     *
     * <p>{@code go run .} only works when the module root is itself a {@code main} package. Plenty
     * of modules keep their commands under {@code cmd/} instead, and plenty have several
     * {@code main} packages — helper tools and generators alongside the real one. So: run the
     * module root if it is a command, otherwise run the only command there is.
     *
     * @return a package pattern for {@code go run}
     * @throws IOException if the module has several commands and no obvious one to run
     */
    @Messages({
        "# {0} - comma-separated list of package directories",
        "ERR_MultipleMains=This module has more than one main package and none at its root, so "
                + "there is no single obvious one to run: {0}. Run the one you want from a "
                + "terminal, or open it as its own project."
    })
    private static String packageToRun(String go, File dir) throws IOException {
        List<File> mains = mainPackages(go, dir);
        File root = dir.getCanonicalFile();

        for (File main : mains) {
            if (main.getCanonicalFile().equals(root)) {
                return ".";
            }
        }
        if (mains.size() == 1) {
            return relativePackage(root, mains.get(0));
        }
        if (mains.isEmpty()) {
            // Let go give its own diagnosis ("no Go files in ...", "cannot run non-main package").
            return ".";
        }
        throw new IOException(Bundle.ERR_MultipleMains(mains.stream()
                .map(m -> relativePackage(root, m))
                .collect(Collectors.joining(", "))));
    }

    /** @return the {@code main} package directories in the module, as reported by {@code go list} */
    private static List<File> mainPackages(String go, File dir) throws IOException {
        java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder(
                go, "list", "-f", "{{.Name}}|{{.Dir}}", "./...");
        pb.directory(dir);
        // Fold stderr in so a chatty module cannot fill a pipe nobody reads; its lines simply do
        // not match the "main|" prefix below.
        pb.redirectErrorStream(true);

        Process process = pb.start();
        List<File> mains = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int bar = line.indexOf('|');
                if (bar > 0 && "main".equals(line.substring(0, bar))) {
                    mains.add(new File(line.substring(bar + 1)));
                }
            }
        }
        try {
            if (!process.waitFor(LIST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        return mains;
    }

    private static String relativePackage(File root, File main) {
        String relative = root.toPath().relativize(main.toPath()).toString().replace('\\', '/');
        return relative.isEmpty() ? "." : "./" + relative;
    }
}
