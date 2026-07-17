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

import java.awt.Image;

import javax.swing.Action;

import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 * Shows the module in the Projects tab.
 *
 * <p>The view is the project folder's own node wrapped in a {@link FilterNode}, so the real
 * directory tree shows through as-is. A Go module's layout is meaningful on disk — there are no
 * synthetic "Source Packages" groupings to invent.
 */
final class GoLogicalViewProvider implements LogicalViewProvider {

    private final Project project;

    GoLogicalViewProvider(Project project) {
        this.project = project;
    }

    @Override
    public Node createLogicalView() {
        try {
            DataObject projectFolder = DataObject.find(project.getProjectDirectory());
            return new RootNode(projectFolder.getNodeDelegate());
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
            return new AbstractRootNode();
        }
    }

    @Override
    public Node findPath(Node root, Object target) {
        return null;
    }

    /**
     * The project folder's node, re-badged with the Go icon and the project's own actions. Its
     * lookup also carries the {@link Project}, which is how the Projects tab and the common
     * project actions find it.
     */
    private final class RootNode extends FilterNode {

        RootNode(Node delegate) {
            super(delegate, null,
                    new ProxyLookup(delegate.getLookup(), Lookups.fixed(project)));
        }

        @Override
        public Image getIcon(int type) {
            return ImageUtilities.loadImage(GoProject.ICON);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public String getDisplayName() {
            return project.getProjectDirectory().getNameExt();
        }

        @Override
        public Action[] getActions(boolean context) {
            return CommonProjectActions.forType(GoProject.TYPE);
        }
    }

    /** Fallback shown only if the project folder cannot be resolved to a node. */
    private static final class AbstractRootNode extends FilterNode {

        AbstractRootNode() {
            super(Node.EMPTY);
        }
    }
}
