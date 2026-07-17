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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.netbeans.api.extexecution.print.ConvertedLine;
import org.netbeans.api.extexecution.print.LineConvertor;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.text.Line;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

/**
 * Turns {@code go} diagnostics in the Output window into links that open the offending line.
 *
 * <p>Matches Go's {@code file:line:column: message} format, as emitted by {@code go build} and
 * {@code go vet}. The paths are relative to the directory the command ran in ({@code ./main.go},
 * {@code internal/lib/lib.go}), so they are resolved against that directory rather than the
 * IDE's working directory.
 */
final class GoErrorLineConvertor implements LineConvertor {

    /**
     * {@code ./main.go:7:2: declared and not used: x}. The leading group is greedy so a Windows
     * drive letter stays with the path instead of being read as the line number.
     */
    private static final Pattern ERROR_LINE = Pattern.compile("^\\s*(.+):(\\d+):(\\d+): (.*)$");

    private final File baseDir;

    GoErrorLineConvertor(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public List<ConvertedLine> convert(String line) {
        Matcher m = ERROR_LINE.matcher(line);
        if (!m.matches()) {
            return Collections.singletonList(ConvertedLine.forText(line, null));
        }

        FileObject file = resolve(m.group(1));
        if (file == null) {
            return Collections.singletonList(ConvertedLine.forText(line, null));
        }

        int lineNumber = Integer.parseInt(m.group(2)) - 1;
        int column = Integer.parseInt(m.group(3)) - 1;
        return Collections.singletonList(
                ConvertedLine.forText(line, new OpenAt(file, lineNumber, column)));
    }

    private FileObject resolve(String path) {
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(baseDir, path);
        }
        return f.isFile() ? FileUtil.toFileObject(FileUtil.normalizeFile(f)) : null;
    }

    private static final class OpenAt implements OutputListener {

        private final FileObject file;
        private final int line;
        private final int column;

        OpenAt(FileObject file, int line, int column) {
            this.file = file;
            this.line = line;
            this.column = column;
        }

        @Override
        public void outputLineAction(OutputEvent ev) {
            LineCookie cookie = file.getLookup().lookup(LineCookie.class);
            if (cookie == null) {
                return;
            }
            Line.Set lines = cookie.getLineSet();
            if (line < lines.getLines().size()) {
                lines.getCurrent(line)
                        .show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS, column);
            }
        }

        @Override
        public void outputLineSelected(OutputEvent ev) {
        }

        @Override
        public void outputLineCleared(OutputEvent ev) {
        }
    }
}
