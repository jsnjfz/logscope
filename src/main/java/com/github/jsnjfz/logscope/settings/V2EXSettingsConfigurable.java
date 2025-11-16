package com.github.jsnjfz.logscope.settings;

import com.github.jsnjfz.logscope.LogScopeBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.ColorPanel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.ActionListener;

/**
 * Provides the settings UI for configuring LogScope.
 */
public class V2EXSettingsConfigurable implements Configurable {

    private MaskedTokenField tokenField;
    private JComboBox<String> fontFamilyCombo;
    private JBIntSpinner fontSizeSpinner;
    private JBIntSpinner topicLimitSpinner;
    private ColorPanel fontColorPanel;
    private JBCheckBox useProxyCheckBox;
    private JBTextField proxyHostField;
    private JBTextField proxyPortField;
    private JBRadioButton httpProxyRadio;
    private JBRadioButton socksProxyRadio;

    private JPanel mainPanel;

    private final V2EXSettings settings = V2EXSettings.getInstance();

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "V2EX LogScope";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (mainPanel == null) {
            mainPanel = buildMainPanel();
        }
        return mainPanel;
    }

    private JPanel buildMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(6);

        c.gridy = 0;
        panel.add(createTokenPanel(), c);

        c.gridy++;
        panel.add(createFontPanel(), c);

        c.gridy++;
        panel.add(createProxyPanel(), c);

        return panel;
    }

        private JPanel createTokenPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(IdeBorderFactory.createTitledBorder("API Token Settings"));

        GridBagConstraints c = baseConstraints();

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel(LogScopeBundle.message("settings.token") + ":"), c);

        tokenField = new MaskedTokenField(settings.apiToken);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(tokenField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        ActionLink link = new ActionLink(LogScopeBundle.message("settings.token.get"),
                (ActionListener) e -> BrowserUtil.browse("https://v2ex.com/settings/tokens"));
        panel.add(link, c);
        return panel;
    }

        private JPanel createFontPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(IdeBorderFactory.createTitledBorder("Font and List"));

        GridBagConstraints c = baseConstraints();

        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Font:"), c);

        fontFamilyCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontFamilyCombo.setSelectedItem(settings.fontFamily);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(fontFamilyCombo, c);

        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel("Font Size:"), c);

        fontSizeSpinner = new JBIntSpinner(settings.fontSize, 8, 72, 1);
        c.gridx = 1;
        panel.add(fontSizeSpinner, c);

        c.gridy = 2;
        c.gridx = 0;
        panel.add(new JLabel("List Limit:"), c);

        topicLimitSpinner = new JBIntSpinner(settings.topicDisplayLimit, 5, 50, 1);
        c.gridx = 1;
        panel.add(topicLimitSpinner, c);

        c.gridy = 3;
        c.gridx = 0;
        panel.add(new JLabel("Font Color:"), c);

        fontColorPanel = new ColorPanel();
        fontColorPanel.setSelectedColor(settings.fontColor);
        c.gridx = 1;
        panel.add(fontColorPanel, c);

        return panel;
    }

    private JPanel createProxyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(IdeBorderFactory.createTitledBorder(LogScopeBundle.message("settings.proxy")));

        GridBagConstraints c = baseConstraints();

        useProxyCheckBox = new JBCheckBox(LogScopeBundle.message("settings.proxy.use"), settings.useProxy);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        panel.add(useProxyCheckBox, c);

        c.gridwidth = 1;
        c.gridy = 1;
        panel.add(new JLabel(LogScopeBundle.message("settings.proxy.host") + ":"), c);

        proxyHostField = new JBTextField(settings.proxyHost);
        c.gridx = 1;
        panel.add(proxyHostField, c);

        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel(LogScopeBundle.message("settings.proxy.port") + ":"), c);

        proxyPortField = new JBTextField(String.valueOf(settings.proxyPort));
        c.gridx = 1;
        panel.add(proxyPortField, c);

        c.gridx = 0;
        c.gridy = 3;
        panel.add(new JLabel(LogScopeBundle.message("settings.proxy.type") + ":"), c);

        JPanel typePanel = new JPanel();
        ButtonGroup group = new ButtonGroup();
        httpProxyRadio = new JBRadioButton("HTTP");
        socksProxyRadio = new JBRadioButton("SOCKS");
        group.add(httpProxyRadio);
        group.add(socksProxyRadio);
        typePanel.add(httpProxyRadio);
        typePanel.add(socksProxyRadio);
        selectProxyType(settings.proxyType);
        c.gridx = 1;
        panel.add(typePanel, c);

        return panel;
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        return c;
    }

    @Override
    public boolean isModified() {
        if (tokenField == null) {
            return false;
        }
        boolean tokenChanged = !tokenField.getText().equals(settings.apiToken);
        boolean fontChanged = !fontFamilyCombo.getSelectedItem().equals(settings.fontFamily)
                || spinnerValue(fontSizeSpinner) != settings.fontSize
                || spinnerValue(topicLimitSpinner) != settings.topicDisplayLimit
                || !fontColorPanel.getSelectedColor().equals(settings.fontColor);
        boolean proxyChanged = useProxyCheckBox.isSelected() != settings.useProxy
                || !proxyHostField.getText().equals(settings.proxyHost)
                || parsePort(proxyPortField.getText()) != settings.proxyPort
                || !getSelectedProxyType().equalsIgnoreCase(settings.proxyType);
        return tokenChanged || fontChanged || proxyChanged;
    }

    @Override
    public void apply() {
        settings.apiToken = tokenField.getText();
        settings.fontFamily = (String) fontFamilyCombo.getSelectedItem();
        settings.fontSize = spinnerValue(fontSizeSpinner);
        settings.topicDisplayLimit = spinnerValue(topicLimitSpinner);
        settings.fontColor = fontColorPanel.getSelectedColor();

        settings.useProxy = useProxyCheckBox.isSelected();
        settings.proxyHost = proxyHostField.getText();
        settings.proxyPort = parsePort(proxyPortField.getText());
        settings.proxyType = getSelectedProxyType();

        settings.notifySettingsChanged();
    }

    @Override
    public void reset() {
        if (tokenField == null) {
            return;
        }
        tokenField.setText(settings.apiToken);
        fontFamilyCombo.setSelectedItem(settings.fontFamily);
        setSpinnerValue(fontSizeSpinner, settings.fontSize);
        setSpinnerValue(topicLimitSpinner, settings.topicDisplayLimit);
        fontColorPanel.setSelectedColor(settings.fontColor);

        useProxyCheckBox.setSelected(settings.useProxy);
        proxyHostField.setText(settings.proxyHost);
        proxyPortField.setText(String.valueOf(settings.proxyPort));
        selectProxyType(settings.proxyType);
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        tokenField = null;
        fontFamilyCombo = null;
        fontSizeSpinner = null;
        topicLimitSpinner = null;
        fontColorPanel = null;
        useProxyCheckBox = null;
        proxyHostField = null;
        proxyPortField = null;
        httpProxyRadio = null;
        socksProxyRadio = null;
    }

    private int parsePort(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return settings.proxyPort;
        }
    }

    private String getSelectedProxyType() {
        return httpProxyRadio.isSelected() ? "HTTP" : "SOCKS";
    }

    private void selectProxyType(String type) {
        if ("HTTP".equalsIgnoreCase(type)) {
            httpProxyRadio.setSelected(true);
        } else {
            socksProxyRadio.setSelected(true);
        }
    }

    private int spinnerValue(JBIntSpinner spinner) {
        Object value = spinner.getValue();
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private void setSpinnerValue(JBIntSpinner spinner, int value) {
        spinner.setValue(value);
    }
}




