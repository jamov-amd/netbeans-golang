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

import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.Icon;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 * A Go module opened as a NetBeans project.
 *
 * <p>Any directory holding a {@code go.mod} (or {@code go.work}) is a project — Go already
 * describes its own module layout, so there is nothing for the IDE to add and no wizard needed.
 *
 * <p>Recognising the module matters beyond the Projects tab: the LSP client asks
 * {@code FileOwnerQuery} who owns a file and passes that project's directory to gopls as the
 * workspace root. Without a project it falls back to the file's own parent directory. gopls
 * still finds {@code go.mod} by walking up, so navigation works either way — but the client
 * caches a server per root, so browsing files across a module without a project starts one gopls
 * per directory instead of one per module.
 */
@ActionReferences(value = {
    @ActionReference(id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.BuildProject"),
            path = GoProject.ACTIONS_PATH, position = 100),
    @ActionReference(id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.RebuildProject"),
            path = GoProject.ACTIONS_PATH, position = 200),
    @ActionReference(id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.CleanProject"),
            path = GoProject.ACTIONS_PATH, position = 300, separatorAfter = 350),
    @ActionReference(id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.RunProject"),
            path = GoProject.ACTIONS_PATH, position = 400),
    @ActionReference(id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.TestProject"),
            path = GoProject.ACTIONS_PATH, position = 500, separatorAfter = 550),
    @ActionReference(id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.SetMainProject"),
            path = GoProject.ACTIONS_PATH, position = 600),
    @ActionReference(id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.CloseProject"),
            path = GoProject.ACTIONS_PATH, position = 700, separatorAfter = 750),
    @ActionReference(id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.CustomizeProject"),
            path = GoProject.ACTIONS_PATH, position = 800)
})
public final class GoProject implements Project {

    static final String ICON = "com/automatemydata/netbeans/golang/project/go-project.png";

    /** Project type token; the logical view's actions are read from this type's layer folder. */
    static final String TYPE = "org-automatemydata-netbeans-golang";

    /** Where the Projects tab looks for this project's context-menu actions. */
    static final String ACTIONS_PATH = "Projects/" + TYPE + "/Actions";

    private final FileObject projectDir;
    private final Lookup lookup;

    GoProject(FileObject projectDir) {
        this.projectDir = projectDir;
        this.lookup = Lookups.fixed(this, new ProjectInfo(), new GoLogicalViewProvider(this),
                new GoActionProvider(this), new GoCustomizerProvider(this));
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDir;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public String toString() {
        return "GoProject[" + projectDir.getPath() + "]";
    }

    static Icon icon() {
        return ImageUtilities.image2Icon(ImageUtilities.loadImage(ICON));
    }

    /**
     * Recognises a Go module by its {@code go.mod}. {@code go.work} counts too, so a multi-module
     * workspace opens at the workspace root rather than as several unrelated projects.
     */
    @ServiceProvider(service = ProjectFactory.class)
    public static final class FactoryImpl implements ProjectFactory2 {

        @Override
        public ProjectManager.Result isProject2(FileObject projectDirectory) {
            return isProject(projectDirectory) ? new ProjectManager.Result(icon()) : null;
        }

        @Override
        public boolean isProject(FileObject projectDirectory) {
            return projectDirectory.getFileObject("go.mod") != null
                    || projectDirectory.getFileObject("go.work") != null;
        }

        @Override
        public Project loadProject(FileObject projectDirectory, ProjectState state) throws IOException {
            return isProject(projectDirectory) ? new GoProject(projectDirectory) : null;
        }

        @Override
        public void saveProject(Project project) {
            // Nothing is stored outside go.mod, which the Go tooling owns.
        }
    }

    /** The project's name and icon, as shown in the Projects tab. */
    private final class ProjectInfo implements ProjectInformation {

        @Override
        public String getName() {
            return projectDir.getNameExt();
        }

        @Override
        public String getDisplayName() {
            return projectDir.getNameExt();
        }

        @Override
        public Icon getIcon() {
            return icon();
        }

        @Override
        public Project getProject() {
            return GoProject.this;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            // Name and icon are derived from the directory and never change while open.
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }
    }
}
