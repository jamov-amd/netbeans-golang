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
package com.automatemydata.netbeans.golang.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JComponent;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 * Registers the Go settings panel under Tools | Options | Miscellaneous.
 *
 * <p>Changing the gopls path takes effect immediately: {@code GoLanguageServerProvider} listens
 * on the same preferences node and restarts the language server, so there is no need to restart
 * the IDE.
 */
@OptionsPanelController.SubRegistration(
        displayName = "#LBL_GoOptions",
        keywords = "#KW_GoOptions",
        keywordsCategory = "Advanced/Go"
)
@Messages({
    "LBL_GoOptions=Go",
    "KW_GoOptions=go, golang, gopls, language server"
})
public final class GoOptionsPanelController extends OptionsPanelController {

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private GoOptionsPanel panel;
    private boolean changed;

    @Override
    public void update() {
        getPanel().load();
        changed = false;
    }

    @Override
    public void applyChanges() {
        getPanel().store();
        changed = false;
    }

    @Override
    public void cancel() {
        // Nothing is written until applyChanges, so there is nothing to roll back.
    }

    @Override
    public boolean isValid() {
        return getPanel().valid();
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        changeSupport.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        changeSupport.removePropertyChangeListener(l);
    }

    private GoOptionsPanel getPanel() {
        if (panel == null) {
            panel = new GoOptionsPanel(this);
        }
        return panel;
    }

    /** Called by the panel whenever the user edits a field, to enable the dialog's OK button. */
    void changed() {
        if (!changed) {
            changed = true;
            changeSupport.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        changeSupport.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }
}
