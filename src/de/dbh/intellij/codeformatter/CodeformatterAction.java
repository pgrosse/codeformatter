package de.dbh.intellij.codeformatter;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

/**
 * User: pgr
 * Date: 07.05.2014
 */
public class CodeformatterAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        new CodeformatterWriteCommandAction(editor).execute();
    }

    private static class CodeformatterWriteCommandAction extends WriteCommandAction {

        private Editor editor;


        public CodeformatterWriteCommandAction(Editor editor) {
            super(editor.getProject(), "Codeformatter", "Codeformatter");
            this.editor = editor;
        }


        @Override
        protected void run(@NotNull Result result) throws Throwable {
            String selectedText = editor.getSelectionModel().getSelectedText();
            String formattedText = new Codeformatter().format(selectedText);

            editor.getDocument().setReadOnly(false);
            editor.getDocument().replaceString(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd(), formattedText);
        }

    }

}
