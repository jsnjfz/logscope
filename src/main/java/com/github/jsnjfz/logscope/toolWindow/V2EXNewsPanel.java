package com.github.jsnjfz.logscope.toolWindow;

import com.github.jsnjfz.logscope.LogScopeBundle;
import com.github.jsnjfz.logscope.settings.V2EXSettings;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class V2EXNewsPanel implements V2EXSettings.SettingsChangeListener {
    private static final String CARD_CONTENT = "content";
    private static final String CARD_STEALTH = "stealth";
    private final Project project;
    private final JPanel mainPanel;
    private final JPanel contentPanel;
    private final JTextPane contentArea;
    private final JPanel displayCardPanel;
    private final CardLayout displayCardLayout;
    private final JPanel stealthPanel;

    private JTextArea replyInputArea;
    private JButton replySendButton;
    private JLabel replyStatusLabel;
    private JPanel replySectionPanel;
    private JButton replyPanelButton;
    private boolean replyPanelVisible = false;

    private JToggleButton stealthToggle;
    private JComboBox<NodeOption> nodeSelector;
    private boolean updatingNodeSelector = false;
    private boolean stealthMode = false;

    private final List<TopicInfo> currentTopics = new ArrayList<>();
    private boolean isShowingList = true;
    private int currentPage = 1;
    private int totalReplies = 0;
    private int currentTopicId = 0;
    private String currentNode = "hot";

    private final OkHttpClient.Builder clientBuilder;

    private static final String FONT_SAMPLE_TEXT = "SAMPLE LOG TEXT 0123456789 检测中文字体";
    private static final int REPLIES_PER_PAGE = 20;
    private static final int IMAGE_URL_LENGTH_THRESHOLD = 60;
    private static final int IMAGE_URL_PREFIX_LENGTH = 25;
    private static final int IMAGE_URL_SUFFIX_LENGTH = 10;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
            "(https?://\\S+?\\.(?:png|jpe?g|gif|webp|bmp)(?:\\?\\S*)?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TOPIC_ID_PATTERN = Pattern.compile("/t/(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONCE_PATTERN = Pattern.compile("name=\"once\" value=\"(\\d+)\"");
    private static final String WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private JButton prevButton;
    private JButton nextButton;

    private static final NodeOption[] NODE_OPTIONS = new NodeOption[]{
            new NodeOption("tech", "filter.node.tech"),
            new NodeOption("creative", "filter.node.creative", EndpointType.TAB_PAGE, "creative"),
            new NodeOption("play", "filter.node.play", EndpointType.TAB_PAGE, "play"),
            new NodeOption("apple", "filter.node.apple"),
            new NodeOption("jobs", "filter.node.jobs"),
            new NodeOption("deals", "filter.node.deals"),
            new NodeOption("city", "filter.node.city", EndpointType.TAB_PAGE, "city"),
            new NodeOption("qna", "filter.node.qna"),
            new NodeOption("hot", "filter.node.hot", "hot.json"),
            new NodeOption("all", "filter.node.all", "latest.json"),
            new NodeOption("r2", "filter.node.r2", EndpointType.TAB_PAGE, "r2"),
            new NodeOption("nodes", "filter.node.nodes", EndpointType.TAB_PAGE, "nodes")
    };

    private enum EndpointType {
        NODE_SHOW,
        DIRECT_JSON,
        TAB_PAGE
    }

    private static class TopicInfo {
        final int id;
        final String title;
        final int replies;
        final String nodeTitle;
        final String author;

        TopicInfo(int id, String title, int replies, String nodeTitle, String author) {
            this.id = id;
            this.title = title;
            this.replies = replies;
            this.nodeTitle = nodeTitle;
            this.author = author;
        }
    }

    private static class NodeOption {
        final String key;
        final String labelKey;
        final EndpointType type;
        final String customPath;

        NodeOption(String key, String labelKey) {
            this(key, labelKey, EndpointType.NODE_SHOW, null);
        }

        NodeOption(String key, String labelKey, String apiPath) {
            this(key, labelKey, EndpointType.DIRECT_JSON, apiPath);
        }

        NodeOption(String key, String labelKey, EndpointType type, String customPath) {
            this.key = key;
            this.labelKey = labelKey;
            this.type = type;
            this.customPath = customPath;
        }

        @Override
        public String toString() {
            return LogScopeBundle.message(labelKey);
        }
    }

    private enum ReplyFeedback {
        INFO,
        SUCCESS,
        ERROR
    }

    private static class ReplyResult {
        final boolean success;
        final String message;

        private ReplyResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static ReplyResult success(String message) {
            return new ReplyResult(true, message);
        }

        static ReplyResult failure(String message) {
            return new ReplyResult(false, message);
        }
    }

    /**
     */
    public V2EXNewsPanel(Project project) {
        this.project = project;

        this.clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30));

        mainPanel = new JPanel(new BorderLayout());
        contentPanel = new JPanel(new BorderLayout());
        displayCardLayout = new CardLayout();
        displayCardPanel = new JPanel(displayCardLayout);
        contentArea = new JTextPane();
        contentArea.setEditable(false);
        contentArea.setMargin(JBUI.insets(8));
        contentArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        applyFontSettings();

        stealthPanel = createStealthPanel();

        JPanel toolbar = createToolbar();
        mainPanel.add(toolbar, BorderLayout.NORTH);

        JBScrollPane scrollPane = new JBScrollPane(contentArea);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty());
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        displayCardPanel.add(contentPanel, CARD_CONTENT);
        displayCardPanel.add(stealthPanel, CARD_STEALTH);
        mainPanel.add(displayCardPanel, BorderLayout.CENTER);

        V2EXSettings.getInstance().addChangeListener(this);

        refreshContent(null);
    }

    /**
     */
    public static Proxy getProxy(V2EXSettings settings) {
        if (!settings.useProxy || settings.proxyHost.isEmpty()) {
            return Proxy.NO_PROXY;
        }

        Proxy.Type proxyType = "SOCKS".equals(settings.proxyType)
                ? Proxy.Type.SOCKS
                : Proxy.Type.HTTP;

        return new Proxy(
                proxyType,
                new InetSocketAddress(settings.proxyHost, settings.proxyPort)
        );
    }

    /**
     */
    @Override
    public void onSettingsChanged() {
        SwingUtilities.invokeLater(this::applyFontSettings);
    }

    /**
     */
    private void applyFontSettings() {
        V2EXSettings settings = V2EXSettings.getInstance();
        Font font = getDisplayFont(settings);
        contentArea.setFont(font);
        contentArea.setForeground(normalizeBodyTextColor(settings.fontColor));

        contentArea.revalidate();
        contentArea.repaint();
        contentPanel.revalidate();
        contentPanel.repaint();
        mainPanel.revalidate();
        mainPanel.repaint();

        if (isShowingList) {
            renderTopicCards();
        }
    }

    /**
     */
    private JPanel createToolbar() {
        JPanel strip = new JPanel(new BorderLayout());
        strip.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(6, 10)
        ));
        strip.setOpaque(true);
        strip.setBackground(UIManager.getColor("Panel.background"));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel scopeLabel = new JLabel(LogScopeBundle.message("filter.scope"));
        scopeLabel.setForeground(JBColor.GRAY);
        scopeLabel.setFont(scopeLabel.getFont().deriveFont(Font.BOLD));
        scopeLabel.setBorder(JBUI.Borders.emptyRight(6));

        nodeSelector = new JComboBox<>(NODE_OPTIONS);
        nodeSelector.setFocusable(false);
        nodeSelector.addActionListener(e -> {
            if (updatingNodeSelector) {
                return;
            }
            NodeOption option = (NodeOption) nodeSelector.getSelectedItem();
            if (option != null && !option.key.equals(currentNode)) {
                currentNode = option.key;
                refreshContent(null);
            }
        });
        updateNodeSelectorSelection();

        stealthToggle = new JToggleButton();
        stealthToggle.setMargin(JBUI.insets(2, 8));
        stealthToggle.setFocusPainted(false);
        stealthToggle.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        stealthToggle.setForeground(JBColor.GRAY);
        stealthToggle.addItemListener(e -> setStealthMode(e.getStateChange() == ItemEvent.SELECTED));
        updateStealthToggleText(false);

        JButton refreshButton = new JButton(LogScopeBundle.message("action.sync.logs"));
        refreshButton.addActionListener(this::refreshContent);
        styleMinimalButton(refreshButton);

        JButton backButton = new JButton(LogScopeBundle.message("action.view.index"));
        backButton.addActionListener(e -> showTopicList());
        styleMinimalButton(backButton);

        prevButton = new JButton(LogScopeBundle.message("action.prev.page"));
        nextButton = new JButton(LogScopeBundle.message("action.next.page"));
        prevButton.addActionListener(e -> showPreviousPage());
        nextButton.addActionListener(e -> showNextPage());
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
        styleMinimalButton(prevButton);
        styleMinimalButton(nextButton);

        replyPanelButton = new JButton(LogScopeBundle.message("action.reply.open"));
        replyPanelButton.addActionListener(e -> toggleReplyPanel());
        replyPanelButton.setEnabled(false);
        styleMinimalButton(replyPanelButton);

        left.add(scopeLabel);
        left.add(nodeSelector);
        left.add(stealthToggle);
        left.add(Box.createHorizontalStrut(8));
        left.add(refreshButton);
        left.add(backButton);
        left.add(prevButton);
        left.add(nextButton);
        left.add(replyPanelButton);

        strip.add(left, BorderLayout.WEST);
        return strip;
    }

    private void styleMinimalButton(AbstractButton button) {
        button.setFocusPainted(false);
        button.setMargin(JBUI.insets(2, 6));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        button.setContentAreaFilled(false);
        button.setBorder(JBUI.Borders.empty());
        button.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        button.setForeground(JBColor.GRAY);
    }

    private void updateNodeSelectorSelection() {
        if (nodeSelector == null) {
            return;
        }
        updatingNodeSelector = true;
        try {
            for (int i = 0; i < nodeSelector.getItemCount(); i++) {
                NodeOption option = nodeSelector.getItemAt(i);
                if (option.key.equals(currentNode)) {
                    nodeSelector.setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            updatingNodeSelector = false;
        }
    }

    private JPanel createStealthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(12));
        JTextArea fakeOutput = new JTextArea();
        fakeOutput.setEditable(false);
        fakeOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        fakeOutput.setLineWrap(true);
        fakeOutput.setWrapStyleWord(true);
        fakeOutput.setText(LogScopeBundle.message("stealth.placeholder.log"));
        fakeOutput.setBackground(UIManager.getColor("Panel.background"));
        fakeOutput.setForeground(JBColor.GRAY);
        fakeOutput.setMargin(JBUI.insets(8));
        fakeOutput.setCaretPosition(0);

        JBScrollPane scrollPane = new JBScrollPane(fakeOutput);
        scrollPane.setBorder(JBUI.Borders.customLine(JBColor.border(), 1));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);

        String hint = LogScopeBundle.message("stealth.placeholder.hint",
                LogScopeBundle.message("action.stealth.disable"));
        if (hint != null && !hint.isBlank()) {
            JLabel hintLabel = new JLabel(hint);
            hintLabel.setForeground(JBColor.GRAY);
            hintLabel.setBorder(JBUI.Borders.emptyTop(8));
            panel.add(hintLabel, BorderLayout.SOUTH);
        }
        return panel;
    }

    private void setStealthMode(boolean enabled) {
        stealthMode = enabled;
        if (enabled) {
            displayCardLayout.show(displayCardPanel, CARD_STEALTH);
        } else {
            displayCardLayout.show(displayCardPanel, CARD_CONTENT);
        }
        updateStealthToggleText(enabled);
    }

    private void updateStealthToggleText(boolean enabled) {
        if (stealthToggle != null) {
            String key = enabled ? "action.stealth.disable" : "action.stealth.enable";
            stealthToggle.setText(LogScopeBundle.message(key));
            stealthToggle.setToolTipText(LogScopeBundle.message("tooltip.stealth.toggle"));
        }
    }

    private void ensureContentVisible() {
        if (!stealthMode) {
            displayCardLayout.show(displayCardPanel, CARD_CONTENT);
        }
    }

    /**
     */
    public JComponent getContent() {
        return mainPanel;
    }

    /**
     */
    private void refreshContent(ActionEvent e) {
        currentTopics.clear();
        isShowingList = true;

        V2EXSettings settings = V2EXSettings.getInstance();
        String token = settings.apiToken;
        if (token.isEmpty()) {
            showNoTokenWarning();
            return;
        }

        NodeOption option = getCurrentOption();

        showLoadingState();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                if (option == null) {
                    return LogScopeBundle.message("error.loading", "invalid node");
                }
                OkHttpClient client = clientBuilder.proxy(getProxy(settings)).build();
                try {
                    if (option.type == EndpointType.TAB_PAGE) {
                        String tabUrl = getTabUrl(option);
                        Request request = new Request.Builder()
                                .url(tabUrl)
                                .build();
                        try (Response response = client.newCall(request).execute()) {
                            if (!response.isSuccessful()) {
                                return LogScopeBundle.message("error.request",
                                        response.code() + " " + response.message());
                            }
                            return formatTabTopicList(response.body().string(), option);
                        }
                    }

                    String apiUrl = getNodeApiUrl(option);
                    Request request = new Request.Builder()
                            .url(apiUrl)
                            .header("Authorization", "Bearer " + token)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            return LogScopeBundle.message("error.request",
                                    response.code() + " " + response.message());
                        }

                        JSONArray topics = new JSONArray(response.body().string());
                        return formatTopicList(topics);
                    }
                } catch (IOException ex) {
                    return LogScopeBundle.message("error.loading", ex.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    updateContent(get());
                } catch (Exception ex) {
                    updateContent(LogScopeBundle.message("error.loading", ex.getMessage()));
                }
            }
        };

        worker.execute();
    }

    private void handleReplySubmit() {
        if (replyInputArea == null || replySendButton == null) {
            return;
        }
        String text = replyInputArea.getText() == null ? "" : replyInputArea.getText().trim();
        if (text.isEmpty()) {
            updateReplyStatus(LogScopeBundle.message("error.reply.empty"), ReplyFeedback.ERROR);
            return;
        }
        if (currentTopicId <= 0) {
            updateReplyStatus(LogScopeBundle.message("error.topic.not.found"), ReplyFeedback.ERROR);
            return;
        }
        V2EXSettings settings = V2EXSettings.getInstance();
        if (settings.apiToken.isEmpty()) {
            updateReplyStatus(LogScopeBundle.message("error.no.token"), ReplyFeedback.ERROR);
            return;
        }

        replySendButton.setEnabled(false);
        updateReplyStatus(LogScopeBundle.message("reply.status.posting"), ReplyFeedback.INFO);

        SwingWorker<ReplyResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ReplyResult doInBackground() {
                return submitReply(settings, text);
            }

            @Override
            protected void done() {
                try {
                    ReplyResult result = get();
                    updateReplyStatus(result.message, result.success ? ReplyFeedback.SUCCESS : ReplyFeedback.ERROR);
                    replySendButton.setEnabled(true);
                    if (result.success) {
                        replyInputArea.setText("");
                        showTopicContent(currentTopicId);
                    }
                } catch (Exception ex) {
                    updateReplyStatus(LogScopeBundle.message("reply.status.failure", ex.getMessage()), ReplyFeedback.ERROR);
                    replySendButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void toggleReplyPanel() {
        if (isShowingList) {
            return;
        }
        replyPanelVisible = !replyPanelVisible;
        JPanel topicPanel = getCurrentTopicPanel();
        if (topicPanel != null) {
            if (replyPanelVisible) {
                topicPanel.add(getReplySection(), BorderLayout.SOUTH);
            } else if (replySectionPanel != null && replySectionPanel.getParent() != null) {
                topicPanel.remove(replySectionPanel);
            }
            topicPanel.revalidate();
            topicPanel.repaint();
        }
        updateReplyButtonState();
    }

    /**
     */
    private void showLoadingState() {
        ensureContentVisible();
        contentPanel.removeAll();
        JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setBorder(JBUI.Borders.empty(40, 12));
        JLabel loadingLabel = new JLabel(LogScopeBundle.message("loading"), SwingConstants.CENTER);
        loadingLabel.setFont(loadingLabel.getFont().deriveFont(Font.BOLD, loadingLabel.getFont().getSize() + 1f));
        loadingLabel.setForeground(JBColor.GRAY);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorder(JBUI.Borders.emptyTop(12));
        loadingPanel.add(loadingLabel, BorderLayout.CENTER);
        loadingPanel.add(progressBar, BorderLayout.SOUTH);
        contentPanel.add(loadingPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     */
    private String formatTopicList(JSONArray topics) {
        if (topics == null || topics.length() == 0) {
            return LogScopeBundle.message("error.loading", "no topic list");
        }

        StringBuilder content = new StringBuilder();
        currentTopics.clear();
        V2EXSettings settings = V2EXSettings.getInstance();
        int displayLimit = Math.max(1, settings.topicDisplayLimit);
        int count = Math.min(displayLimit, topics.length());

        for (int i = 0; i < count; i++) {
            JSONObject topic = topics.getJSONObject(i);
            int id = topic.getInt("id");
            String title = topic.getString("title");
            int replies = topic.getInt("replies");
            JSONObject node = topic.optJSONObject("node");
            JSONObject member = topic.optJSONObject("member");
            String nodeTitle = node != null ? node.optString("title", "") : "";
            String author = member != null ? member.optString("username", "") : "";

            currentTopics.add(new TopicInfo(id, title, replies, nodeTitle, author));
        }

        return content.toString();
    }

    /**
     */
    private void showTopicList() {
        if (!isShowingList) {
            isShowingList = true;
            replyPanelVisible = false;
            updateContent("");
        }
    }

    /**
     */
    private void updateContent(String text) {
        ensureContentVisible();
        if (isShowingList) {
            replyPanelVisible = false;
            renderTopicCards();
            updatePaginationButtons();
            updateReplyButtonState();
            return;
        }

        contentPanel.removeAll();
        contentArea.setText(text);
        applyFontSettings();
        contentArea.setForeground(normalizeBodyTextColor(contentArea.getForeground()));
        JBScrollPane scrollPane = new JBScrollPane(contentArea);
        scrollPane.setBorder(JBUI.Borders.empty(5));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        JPanel topicPanel = new JPanel(new BorderLayout());
        topicPanel.add(scrollPane, BorderLayout.CENTER);
        if (replyPanelVisible) {
            topicPanel.add(getReplySection(), BorderLayout.SOUTH);
        }
        contentPanel.add(topicPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
        updatePaginationButtons();
        updateReplyButtonState();
    }

    private JPanel getReplySection() {
        if (replySectionPanel == null) {
            replySectionPanel = buildReplySection();
        }
        Container parent = replySectionPanel.getParent();
        if (parent != null) {
            parent.remove(replySectionPanel);
        }
        return replySectionPanel;
    }

    private JPanel buildReplySection() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(JBUI.Borders.empty(8, 12, 12, 12));

        JPanel editorContainer = new JPanel(new BorderLayout());
        editorContainer.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(4)
        ));

        replyInputArea = new JTextArea();
        replyInputArea.setLineWrap(true);
        replyInputArea.setWrapStyleWord(true);
        replyInputArea.setRows(4);
        replyInputArea.setMargin(JBUI.insets(4));
        replyInputArea.setFont(getDisplayFont(V2EXSettings.getInstance()));
        replyInputArea.setToolTipText(LogScopeBundle.message("reply.placeholder"));
        replyInputArea.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "sendReply");
        replyInputArea.getActionMap().put("sendReply", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleReplySubmit();
            }
        });

        JBScrollPane replyScrollPane = new JBScrollPane(replyInputArea);
        replyScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        replyScrollPane.getVerticalScrollBar().setUnitIncrement(12);
        replyScrollPane.setBorder(JBUI.Borders.empty());
        editorContainer.add(replyScrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.setBorder(JBUI.Borders.emptyTop(6));
        replyStatusLabel = new JLabel(LogScopeBundle.message("reply.status.helper"));
        replyStatusLabel.setForeground(JBColor.GRAY);
        replySendButton = new JButton(LogScopeBundle.message("action.reply.send"));
        replySendButton.addActionListener(e -> handleReplySubmit());
        replySendButton.setMargin(JBUI.insets(2, 12));
        actionPanel.add(replyStatusLabel, BorderLayout.CENTER);
        actionPanel.add(replySendButton, BorderLayout.EAST);

        editorContainer.add(actionPanel, BorderLayout.SOUTH);
        wrapper.add(editorContainer, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     */
    private void renderTopicCards() {
        ensureContentVisible();
        contentPanel.removeAll();

        if (currentTopics.isEmpty()) {
            JLabel emptyLabel = new JLabel(LogScopeBundle.message("topic.card.empty"), SwingConstants.CENTER);
            emptyLabel.setBorder(JBUI.Borders.empty(24));
            emptyLabel.setForeground(JBColor.GRAY);
            contentPanel.add(emptyLabel, BorderLayout.CENTER);
            contentPanel.revalidate();
            contentPanel.repaint();
            return;
        }

        V2EXSettings settings = V2EXSettings.getInstance();
        Font baseFont = getDisplayFont(settings);
        Font monoFont = new Font(Font.MONOSPACED, Font.PLAIN, Math.max(12, baseFont.getSize()));
        Color textColor = normalizeBodyTextColor(settings.fontColor);
        Color secondaryColor = JBColor.GRAY;
        Color baseBackground = JBColor.namedColor("EditorPane.background", UIManager.getColor("Panel.background"));
        Color hoverBackground = JBColor.namedColor("EditorPane.inactiveBackground", new Color(0xF0F0F0));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(JBUI.Borders.empty(6, 10));
        listPanel.setOpaque(false);

        for (TopicInfo topic : currentTopics) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(true);
            row.setBackground(baseBackground);
            row.setBorder(JBUI.Borders.empty(2, 4));

            String nodeName = topic.nodeTitle == null || topic.nodeTitle.isEmpty() ? currentNode : topic.nodeTitle;
            String logLine = LogScopeBundle.message("log.row.summary",
                    TIME_FORMATTER.format(LocalTime.now()),
                    nodeName,
                    topic.replies,
                    topic.title);
            JLabel mainLabel = new JLabel(logLine);
            mainLabel.setFont(monoFont);
            mainLabel.setForeground(textColor);

            String metaLine = LogScopeBundle.message("log.row.meta", topic.author, topic.id);
            JLabel metaLabel = new JLabel(metaLine);
            metaLabel.setFont(monoFont.deriveFont(monoFont.getSize2D() - 1));
            metaLabel.setForeground(secondaryColor);

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.add(mainLabel);
            textPanel.add(metaLabel);

            row.add(textPanel, BorderLayout.CENTER);

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showTopicContent(topic.id);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    row.setBackground(hoverBackground);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    row.setBackground(baseBackground);
                }
            };

            row.addMouseListener(adapter);
            mainLabel.addMouseListener(adapter);
            metaLabel.addMouseListener(adapter);
            row.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(4));
        }

        JBScrollPane scrollPane = new JBScrollPane(listPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     */
    private void showPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            showTopicContent(currentTopicId);
        }
    }

    /**
     */
    private void showNextPage() {
        if ((currentPage * REPLIES_PER_PAGE) < totalReplies) {
            currentPage++;
            showTopicContent(currentTopicId);
        }
    }

    /**
     */
    private void showTopicContent(int topicId) {
        if (currentTopicId != topicId) {
            currentPage = 1;
        }
        currentTopicId = topicId;
        isShowingList = false;
        replyPanelVisible = false;
        if (replySectionPanel != null && replySectionPanel.getParent() != null) {
            Container parent = replySectionPanel.getParent();
            parent.remove(replySectionPanel);
        }

        showLoadingState();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                V2EXSettings settings = V2EXSettings.getInstance();
                String token = settings.apiToken;
                if (token.isEmpty()) {
                    return LogScopeBundle.message("error.no.token");
                }

                OkHttpClient client = clientBuilder.proxy(getProxy(settings)).build();

                Request topicRequest = new Request.Builder()
                        .url("https://www.v2ex.com/api/topics/show.json?id=" + topicId)
                        .header("Authorization", "Bearer " + token)
                        .build();

                String topicContent;
                try (Response response = client.newCall(topicRequest).execute()) {
                    if (!response.isSuccessful()) {
                        return LogScopeBundle.message("error.request",
                                response.code() + " " + response.message());
                    }

                    JSONArray topics = new JSONArray(response.body().string());
                    if (topics.length() == 0) {
                        return LogScopeBundle.message("error.topic.not.found");
                    }

                    JSONObject topic = topics.getJSONObject(0);
                    totalReplies = topic.getInt("replies");

                    StringBuilder content = new StringBuilder();
                    content.append(topic.getString("title")).append("\n\n");
                    content.append(shortenImageUrls(topic.optString("content", ""))).append("\n\n");
                    content.append("-------------------\n\n");
                    topicContent = content.toString();
                }

                Request repliesRequest = new Request.Builder()
                        .url(String.format("https://www.v2ex.com/api/replies/show.json?topic_id=%d&p=%d",
                                topicId, currentPage))
                        .header("Authorization", "Bearer " + token)
                        .build();

                try (Response response = client.newCall(repliesRequest).execute()) {
                    if (!response.isSuccessful()) {
                        return topicContent + LogScopeBundle.message("error.replies.failed", response.code());
                    }

                    JSONArray replies = new JSONArray(response.body().string());
                    StringBuilder repliesContent = new StringBuilder(topicContent);

                    int totalPages = (totalReplies + REPLIES_PER_PAGE - 1) / REPLIES_PER_PAGE;
                    repliesContent.append(LogScopeBundle.message("page.info", currentPage, totalPages)).append("\n\n");

                    for (int i = 0; i < replies.length(); i++) {
                        JSONObject reply = replies.getJSONObject(i);
                        repliesContent.append(String.format("#%d %s:\n",
                                (currentPage - 1) * REPLIES_PER_PAGE + i + 1,
                                reply.getJSONObject("member").getString("username")
                        ));
                        repliesContent.append(shortenImageUrls(reply.optString("content", ""))).append("\n\n");
                        if (i < replies.length() - 1) {
                            repliesContent.append("-------------------\n\n");
                        }
                    }

                    return repliesContent.toString();
                }
            }

            @Override
            protected void done() {
                try {
                    updateContent(get());
                } catch (Exception ex) {
                    updateContent(LogScopeBundle.message("error.loading", ex.getMessage()));
                }
            }
        };

        worker.execute();
    }

    /**
     */
    private void updatePaginationButtons() {
        prevButton.setEnabled(!isShowingList && currentPage > 1);
        nextButton.setEnabled(!isShowingList && (currentPage * REPLIES_PER_PAGE) < totalReplies);
    }

    private void updateReplyButtonState() {
        if (replyPanelButton == null) {
            return;
        }
        replyPanelButton.setEnabled(!isShowingList);
        String key = replyPanelVisible ? "action.reply.hide" : "action.reply.open";
        replyPanelButton.setText(LogScopeBundle.message(key));
    }

    private JPanel getCurrentTopicPanel() {
        if (contentPanel.getComponentCount() == 0) {
            return null;
        }
        Component component = contentPanel.getComponent(0);
        return component instanceof JPanel ? (JPanel) component : null;
    }

    private ReplyResult submitReply(V2EXSettings settings, String content) {
        String cookie = settings.sessionCookie == null ? "" : settings.sessionCookie.trim();
        if (cookie.isEmpty()) {
            return ReplyResult.failure(LogScopeBundle.message("error.reply.cookie"));
        }
        OkHttpClient client = clientBuilder.proxy(getProxy(settings)).build();
        try {
            String once = fetchReplyOnceToken(client, cookie, currentTopicId);
            return postReplyViaWeb(client, cookie, once, content, currentTopicId);
        } catch (ReplySubmitException ex) {
            return ReplyResult.failure(ex.getMessage());
        }
    }

    private ReplyResult postReplyViaWeb(OkHttpClient client, String cookie, String once, String content, int topicId) {
        RequestBody formBody = new FormBody.Builder()
                .add("content", content)
                .add("once", once)
                .build();
        Request request = new Request.Builder()
                .url("https://www.v2ex.com/t/" + topicId)
                .header("Cookie", cookie)
                .header("User-Agent", WEB_USER_AGENT)
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.request().url().encodedPath().startsWith("/signin")) {
                return ReplyResult.failure(LogScopeBundle.message("error.reply.login"));
            }
            if (response.isSuccessful()) {
                return ReplyResult.success(LogScopeBundle.message("reply.status.success"));
            }
            String payload = response.body() != null ? response.body().string() : "";
            String failureMessage = payload.isBlank() ? response.message() : payload;
            return ReplyResult.failure(LogScopeBundle.message("reply.status.failure",
                    response.code() + " " + failureMessage));
        } catch (IOException ex) {
            return ReplyResult.failure(LogScopeBundle.message("reply.status.failure", ex.getMessage()));
        }
    }

    private String fetchReplyOnceToken(OkHttpClient client, String cookie, int topicId) throws ReplySubmitException {
        Request request = new Request.Builder()
                .url("https://www.v2ex.com/t/" + topicId)
                .header("Cookie", cookie)
                .header("User-Agent", WEB_USER_AGENT)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.request().url().encodedPath().startsWith("/signin")) {
                throw new ReplySubmitException(LogScopeBundle.message("error.reply.login"));
            }
            if (!response.isSuccessful()) {
                throw new ReplySubmitException(LogScopeBundle.message("reply.status.failure",
                        response.code() + " " + response.message()));
            }
            String html = response.body() != null ? response.body().string() : "";
            Matcher matcher = ONCE_PATTERN.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
            throw new ReplySubmitException(LogScopeBundle.message("error.reply.once"));
        } catch (IOException ex) {
            throw new ReplySubmitException(LogScopeBundle.message("reply.status.failure", ex.getMessage()));
        }
    }

    private void updateReplyStatus(String message, ReplyFeedback feedback) {
        if (replyStatusLabel == null) {
            return;
        }
        replyStatusLabel.setText(message);
        Color color = switch (feedback) {
            case SUCCESS -> JBColor.GREEN;
            case ERROR -> JBColor.RED;
            default -> JBColor.GRAY;
        };
        replyStatusLabel.setForeground(color);
    }

    private static class ReplySubmitException extends Exception {
        ReplySubmitException(String message) {
            super(message);
        }
    }

    private Font getDisplayFont(V2EXSettings settings) {
        Font configured = new Font(settings.fontFamily, Font.PLAIN, settings.fontSize);
        if (configured.canDisplayUpTo(FONT_SAMPLE_TEXT) == -1) {
            return configured;
        }
        Font fallback = UIManager.getFont("Label.font");
        if (fallback == null) {
            fallback = new JLabel().getFont();
        }
        return fallback.deriveFont((float) settings.fontSize);
    }

    private Color normalizeBodyTextColor(Color color) {
        if (color == null || Color.BLACK.equals(color)) {
            return new Color(0x66, 0x66, 0x66);
        }
        return color;
    }

    private String shortenImageUrls(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Matcher matcher = IMAGE_URL_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        boolean replaced = false;
        while (matcher.find()) {
            replaced = true;
            String url = matcher.group(1);
            String replacement = url.length() > IMAGE_URL_LENGTH_THRESHOLD ? abbreviateUrl(url) : url;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        if (!replaced) {
            return text;
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String abbreviateUrl(String url) {
        if (url.length() <= IMAGE_URL_LENGTH_THRESHOLD) {
            return url;
        }
        int prefixLength = Math.min(IMAGE_URL_PREFIX_LENGTH, url.length());
        int suffixLength = Math.min(IMAGE_URL_SUFFIX_LENGTH, url.length() - prefixLength);
        if (suffixLength <= 0) {
            return url.substring(0, prefixLength) + "...";
        }
        return url.substring(0, prefixLength) + "..." + url.substring(url.length() - suffixLength);
    }

    /**
     */
    private String getNodeApiUrl(NodeOption option) {
        String baseUrl = "https://www.v2ex.com/api/topics/";
        return switch (option.type) {
            case DIRECT_JSON -> baseUrl + option.customPath;
            case NODE_SHOW -> baseUrl + "show.json?node_name=" + option.key;
            default -> baseUrl + "hot.json";
        };
    }

    private String getTabUrl(NodeOption option) {
        String tabKey = option.customPath != null ? option.customPath : option.key;
        return "https://www.v2ex.com/?tab=" + tabKey;
    }

    private NodeOption getCurrentOption() {
        for (NodeOption option : NODE_OPTIONS) {
            if (option.key.equals(currentNode)) {
                return option;
            }
        }
        return NODE_OPTIONS.length > 0 ? NODE_OPTIONS[0] : null;
    }

    private String formatTabTopicList(String html, NodeOption option) {
        currentTopics.clear();
        if (html == null || html.isEmpty()) {
            return LogScopeBundle.message("error.loading", "empty tab page");
        }

        Document document = Jsoup.parse(html);
        Elements items = document.select("div.cell.item");
        if (items.isEmpty()) {
            return LogScopeBundle.message("error.loading", "no tab topics");
        }

        V2EXSettings settings = V2EXSettings.getInstance();
        int displayLimit = Math.max(1, settings.topicDisplayLimit);

        for (Element item : items) {
            Element titleAnchor = item.selectFirst(".item_title a[href^=/t/]");
            if (titleAnchor == null) {
                continue;
            }
            int id = extractTopicId(titleAnchor.attr("href"));
            if (id <= 0) {
                continue;
            }
            String title = titleAnchor.text();

            Element repliesEl = item.selectFirst(".count_livid, .count_orange");
            int replies = parseReplies(repliesEl != null ? repliesEl.text() : null);

            Element nodeEl = item.selectFirst("a.node");
            String nodeTitle = nodeEl != null ? nodeEl.text() : option.key;

            Element authorEl = item.selectFirst(".small.fade a[href^=/member/]");
            String author = authorEl != null ? authorEl.text() : "";

            currentTopics.add(new TopicInfo(id, title, replies, nodeTitle, author));
            if (currentTopics.size() >= displayLimit) {
                break;
            }
        }

        if (currentTopics.isEmpty()) {
            return LogScopeBundle.message("error.loading", "no tab topics");
        }
        return "";
    }

    private int extractTopicId(String href) {
        if (href == null) {
            return -1;
        }
        Matcher matcher = TOPIC_ID_PATTERN.matcher(href);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private int parseReplies(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void showNoTokenWarning() {
        ensureContentVisible();
        contentPanel.removeAll();
        JPanel warningPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel iconLabel = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        gbc.gridy = 0;
        warningPanel.add(iconLabel, gbc);

        String tip = LogScopeBundle.message("error.no.token").replace("\n", "<br>");
        JLabel warningLabel = new JLabel("<html><div style='text-align: center;'><b><font color='#FF4444' size='+1'>"
                + LogScopeBundle.message("error.no.token.title")
                + "</font></b><br>" + tip + "</div></html>", SwingConstants.CENTER);
        gbc.gridy = 1;
        warningPanel.add(warningLabel, gbc);

        ActionLink settingsLink = new ActionLink(
                LogScopeBundle.message("action.open.settings"),
                (ActionListener) e -> ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, "V2EX LogScope")
        );
        gbc.gridy = 2;
        warningPanel.add(settingsLink, gbc);

        contentPanel.add(warningPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
} 






