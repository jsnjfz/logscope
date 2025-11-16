package com.github.jsnjfz.logscope.settings;

import com.intellij.ui.components.JBTextField;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Text field that masks tokens while preserving the real value.
 */
public class MaskedTokenField extends JBTextField {
    private static final int VISIBLE_CHARS = 4;
    private MaskedDocument maskedDocument;
    private String pendingText;

    public MaskedTokenField(String text) {
        pendingText = normalize(text);
        initMaskedDocument();
    }

    @Override
    public String getText() {
        if (maskedDocument != null) {
            return maskedDocument.getActualText();
        }
        return safePendingText();
    }

    @Override
    public void setText(String t) {
        if (maskedDocument == null) {
            pendingText = normalize(t);
            return;
        }
        maskedDocument.setActualText(normalize(t));
    }

    private void initMaskedDocument() {
        maskedDocument = new MaskedDocument();
        setDocument(maskedDocument);
        maskedDocument.setActualText(pendingText);
        pendingText = "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    private String safePendingText() {
        return pendingText == null ? "" : pendingText;
    }

    private class MaskedDocument extends PlainDocument {
        private String actualText = "";

        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            actualText = new StringBuilder(actualText).insert(offs, str).toString();
            super.remove(0, getLength());
            super.insertString(0, getMaskedText(), a);
        }

        @Override
        public void remove(int offs, int len) throws BadLocationException {
            actualText = new StringBuilder(actualText).delete(offs, offs + len).toString();
            super.remove(0, getLength());
            super.insertString(0, getMaskedText(), null);
        }

        private String getMaskedText() {
            if (actualText.isEmpty()) {
                return "";
            }
            if (actualText.length() <= VISIBLE_CHARS * 2) {
                return actualText;
            }

            return actualText.substring(0, VISIBLE_CHARS)
                    + "*".repeat(actualText.length() - VISIBLE_CHARS * 2)
                    + actualText.substring(actualText.length() - VISIBLE_CHARS);
        }

        public String getActualText() {
            return actualText;
        }

        public void setActualText(String text) {
            try {
                actualText = text;
                super.remove(0, getLength());
                super.insertString(0, getMaskedText(), null);
            } catch (BadLocationException e) {
                // ignore
            }
        }
    }
}
