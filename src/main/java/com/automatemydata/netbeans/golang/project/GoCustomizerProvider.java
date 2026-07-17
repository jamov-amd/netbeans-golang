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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.netbeans.spi.project.ui.CustomizerProvider;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle.Messages;

/**
 * The project's Properties dialog: one field, for the arguments passed to the program when you
 * Run it.
 *
 * <p>A plain dialog rather than the tabbed project customizer — there is one setting, and Go
 * projects have little else to configure that {@code go.mod} does not already say.
 */
final class GoCustomizerProvider implements CustomizerProvider {

    private final GoProject project;

    GoCustomizerProvider(GoProject project) {
        this.project = project;
    }

    @Messages({
        "# {0} - project name",
        "LBL_GoProperties={0} - Properties"
    })
    @Override
    public void showCustomizer() {
        RunPanel panel = new RunPanel();
        panel.load();

        DialogDescriptor descriptor = new DialogDescriptor(panel,
                Bundle.LBL_GoProperties(project.getProjectDirectory().getNameExt()));
        if (DialogDisplayer.getDefault().notify(descriptor) == DialogDescriptor.OK_OPTION) {
            panel.store();
        }
    }

    private final class RunPanel extends JPanel {

        private final JTextField argumentsField = new JTextField();

        @Messages({
            "LBL_RunArguments=&Run arguments:",
            "HINT_RunArguments=<html>Passed to your program when you Run the project, after the "
                    + "package &mdash; the same as typing them after <code>go run .</code><br>"
                    + "Relative paths resolve against the project directory. Use \"quotes\" around "
                    + "an argument containing spaces.</html>"
        })
        RunPanel() {
            JLabel label = new JLabel();
            org.openide.awt.Mnemonics.setLocalizedText(label, Bundle.LBL_RunArguments());
            label.setLabelFor(argumentsField);

            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.add(label, BorderLayout.WEST);
            row.add(argumentsField, BorderLayout.CENTER);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel hint = new JLabel(Bundle.HINT_RunArguments());
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            hint.setForeground(UIManager.getColor("Label.disabledForeground"));
            hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

            Box content = new Box(BoxLayout.Y_AXIS);
            content.add(row);
            content.add(hint);

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setPreferredSize(new Dimension(560, 130));
            add(content, BorderLayout.NORTH);
        }

        void load() {
            argumentsField.setText(GoRunConfig.runArguments(project.getProjectDirectory()));
        }

        void store() {
            GoRunConfig.setRunArguments(project.getProjectDirectory(), argumentsField.getText());
        }
    }
}
