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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import com.automatemydata.netbeans.golang.GoLanguageServerProvider;
import com.automatemydata.netbeans.golang.GoOptions;

import org.openide.util.NbBundle.Messages;

/**
 * Settings panel for the gopls executable, shown under Tools | Options | Miscellaneous | Go.
 *
 * <p>Leaving the field empty is the normal case: the plugin then finds gopls the same way the Go
 * tools do. The hint under the field always says what the current setting actually resolves to,
 * so a typo or a stale path is visible here rather than only in the IDE log.
 */
final class GoOptionsPanel extends JPanel {

    private final GoOptionsPanelController controller;
    private final JTextField pathField = new JTextField();
    private final JLabel hintLabel = new JLabel();

    @Messages({
        "LBL_GoplsPath=&gopls executable:",
        "LBL_Browse=Browse...",
        "LBL_ChooseGopls=Select the gopls executable",
        "LBL_GoplsFiles=gopls executable",
        "HINT_LeaveBlank=Leave empty to detect gopls automatically (PATH, $GOBIN, $GOPATH/bin)."
    })
    GoOptionsPanel(GoOptionsPanelController controller) {
        this.controller = controller;

        JLabel label = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(label, Bundle.LBL_GoplsPath());
        label.setLabelFor(pathField);

        JButton browse = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(browse, Bundle.LBL_Browse());
        browse.addActionListener(e -> browseForGopls());

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(label, BorderLayout.WEST);
        row.add(pathField, BorderLayout.CENTER);
        row.add(browse, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JLabel blankHint = new JLabel(Bundle.HINT_LeaveBlank());
        blankHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        blankHint.setForeground(UIManager.getColor("Label.disabledForeground"));
        blankHint.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        Box content = new Box(BoxLayout.Y_AXIS);
        content.add(row);
        content.add(hintLabel);
        content.add(blankHint);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(content, BorderLayout.NORTH);

        pathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fieldChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fieldChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fieldChanged();
            }
        });
    }

    private void fieldChanged() {
        updateHint();
        controller.changed();
    }

    private void browseForGopls() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(Bundle.LBL_ChooseGopls());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().equals("gopls") || f.getName().equals("gopls.exe");
            }

            @Override
            public String getDescription() {
                return Bundle.LBL_GoplsFiles();
            }
        });

        String current = pathField.getText().trim();
        File start = current.isEmpty() ? null : new File(current).getParentFile();
        if (start != null && start.isDirectory()) {
            chooser.setCurrentDirectory(start);
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    @Messages({
        "# {0} - absolute path to the gopls executable",
        "HINT_AutoDetected=Using auto-detected gopls: {0}",
        "HINT_NotFound=gopls was not found. Install it with: " + GoLanguageServerProvider.INSTALL_COMMAND,
        "HINT_NotExecutable=Not an executable file - auto-detection will be used instead.",
        "HINT_Ok=This file will be used."
    })
    private void updateHint() {
        String configured = pathField.getText().trim();
        if (configured.isEmpty()) {
            String detected = GoLanguageServerProvider.autoDetectGopls();
            if (detected != null) {
                setHint(Bundle.HINT_AutoDetected(detected), false);
            } else {
                setHint(Bundle.HINT_NotFound(), true);
            }
        } else if (new File(configured).canExecute()) {
            setHint(Bundle.HINT_Ok(), false);
        } else {
            setHint(Bundle.HINT_NotExecutable(), true);
        }
    }

    private void setHint(String text, boolean warning) {
        hintLabel.setText(text);
        Color normal = UIManager.getColor("Label.disabledForeground");
        hintLabel.setForeground(warning ? errorColor() : normal);
    }

    private static Color errorColor() {
        Color c = UIManager.getColor("nb.errorForeground");
        return c != null ? c : Color.RED.darker();
    }

    void load() {
        pathField.setText(GoOptions.goplsPath());
        updateHint();
    }

    void store() {
        GoOptions.setGoplsPath(pathField.getText());
    }

    /**
     * @return always {@code true} — a path that does not resolve is reported in the hint and
     *         degrades to auto-detection, which is friendlier than refusing to close the dialog
     *         (the target may live on a drive that is not mounted right now)
     */
    boolean valid() {
        return true;
    }
}
