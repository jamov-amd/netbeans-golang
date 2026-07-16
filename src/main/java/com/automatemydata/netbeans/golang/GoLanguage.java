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

import org.netbeans.modules.textmate.lexer.api.GrammarRegistration;
import org.openide.filesystems.MIMEResolver;
import org.openide.util.NbBundle.Messages;

/**
 * Registers the Go file types and their TextMate grammars.
 *
 * <p>The MIME types are resolved declaratively by {@code go-mod-resolver.xml}: {@code .go} by
 * extension, and {@code go.mod} / {@code go.sum} / {@code go.work} / {@code go.work.sum} by exact
 * file name. Each resulting MIME type is then bound to a bundled TextMate grammar, which gives
 * NetBeans lexing, syntax colors and brace matching without a hand-written lexer.
 *
 * <p>This class is a registration holder only and is never instantiated.
 */
@MIMEResolver.Registration(
        displayName = "#LBL_GoFiles",
        resource = "go-mod-resolver.xml",
        showInFileChooser = {"#LBL_GoFiles"},
        position = 191919
)
@GrammarRegistration(mimeType = GoLanguage.GO_MIME_TYPE, grammar = "go.tmLanguage.json")
@GrammarRegistration(mimeType = GoLanguage.GO_MOD_MIME_TYPE, grammar = "go.mod.tmGrammar.json")
@GrammarRegistration(mimeType = GoLanguage.GO_SUM_MIME_TYPE, grammar = "go.sum.tmGrammar.json")
@Messages("LBL_GoFiles=Go Files")
public final class GoLanguage {

    /** MIME type for {@code .go} source files. */
    public static final String GO_MIME_TYPE = "text/x-go";

    /** MIME type for {@code go.mod} and {@code go.work} module files. */
    public static final String GO_MOD_MIME_TYPE = "text/x-go-mod";

    /** MIME type for {@code go.sum} and {@code go.work.sum} checksum files. */
    public static final String GO_SUM_MIME_TYPE = "text/x-go-sum";

    private GoLanguage() {
    }
}
