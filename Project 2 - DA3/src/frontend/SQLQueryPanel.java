package frontend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.ElasticsearchQueryResult;
import model.QueryResult;
import service.BackendService;
import service.ElasticsearchIndexSetup;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * SQLQueryPanel - Interactive Query Console supporting both Neo4j Cypher and Elasticsearch Query DSL.
 *
 * In Elasticsearch mode:
 *   Provides an editable Query DSL editor, index selector, endpoint selector (_search, _count),
 *   template presets, real-time JSON validation, non-blocking execution via SwingWorker,
 *   and pretty-printed JSON response inspection with hit metrics.
 *
 * In Neo4j mode:
 *   Preserves the original interactive Cypher editor and tabular JTable results display.
 */
public class SQLQueryPanel extends JPanel {

    private final BackendService backendService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // -- Styling Palette ----------------------------------------------------
    private static final Color CLR_BG          = new Color(241, 245, 249);
    private static final Color CLR_CARD        = Color.WHITE;
    private static final Color CLR_BORDER      = new Color(226, 232, 240);
    private static final Color CLR_ACCENT_DARK = new Color(14,  116, 144);
    private static final Color CLR_TEXT_PRI    = new Color(15,   23,  42);
    private static final Color CLR_TEXT_SEC    = new Color(100, 116, 139);
    private static final Color CLR_BTN_FG      = Color.WHITE;
    private static final Color CLR_SEL_BG      = new Color(224, 242, 254);

    private static final Color CLR_MUTED_FG    = new Color(100, 116, 139);
    private static final Color CLR_SUCCESS_FG  = new Color(22, 163, 74);
    private static final Color CLR_WARNING_FG  = new Color(146, 64, 14);
    private static final Color CLR_ERROR_FG    = new Color(185, 28, 28);
    private static final Color CLR_BADGE_BG    = new Color(241, 245, 249);

    // -- Common UI Components -----------------------------------------------
    private JTextArea         sqlArea;
    private JLabel            statusLabel;
    private JButton           btnExecute;
    private JButton           btnClear;

    // -- Neo4j Specific Components ------------------------------------------
    private DefaultTableModel tableModel;
    private JTable            resultsTable;

    // -- Elasticsearch Specific Components -----------------------------------
    private JComboBox<String> indexSelector;
    private JComboBox<String> endpointSelector;
    private JComboBox<String> templateSelector;
    private JTextArea         resultsArea;
    private JLabel            responseMetricsLabel;
    private JButton           btnFormatJson;
    private JButton           btnCopyResponse;

    private static final String[] ES_INDEXES = {
        ElasticsearchIndexSetup.INDEX_CUSTOMERS,
        ElasticsearchIndexSetup.INDEX_POLICIES,
        ElasticsearchIndexSetup.INDEX_CLAIMS,
        ElasticsearchIndexSetup.INDEX_PAYMENTS,
        ElasticsearchIndexSetup.INDEX_ASSETS,
        ElasticsearchIndexSetup.INDEX_PROVIDERS
    };

    private static final String[] ES_ENDPOINTS = {
        "_search",
        "_count"
    };

    private static final String[] ES_TEMPLATES = {
        "-- Example Query Templates --",
        "Search: Match All (customers)",
        "Search: Term Filter (claims by status)",
        "Search: Range Filter & Sort (claims by amount)",
        "Search: Match Query (assets description)",
        "Count: Count All (payments)",
        "Count: Filtered Range (policies by premium)"
    };

    public SQLQueryPanel(BackendService backendService) {
        this.backendService = backendService;
        setBackground(CLR_BG);
        setLayout(new BorderLayout());
        add(buildPageHeader(), BorderLayout.NORTH);
        add(buildBody(),       BorderLayout.CENTER);
        add(buildBottomBar(),  BorderLayout.SOUTH);

        if (db.DatabaseConfig.isElasticsearch()) {
            setStatus("Ready. Enter an Elasticsearch Query DSL request or pick an example template.", StatusType.MUTED);
        } else {
            setStatus("Ready. Enter a Cypher query to execute against Neo4j.", StatusType.MUTED);
        }
    }

    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setBackground(CLR_BG);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(24, 32, 16, 32));

        boolean isES = db.DatabaseConfig.isElasticsearch();
        JLabel title = new JLabel(isES ? "Elasticsearch Query Console" : "Cypher Query");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(CLR_TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(isES 
            ? "Execute read-only Elasticsearch Query DSL requests (_search, _count) against Elastic Cloud"
            : "Execute Cypher queries against the Neo4j insurance database");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(CLR_TEXT_SEC);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(subtitle);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 32, 8, 32));

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                buildEditorCard(),
                buildResultsCard());
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(8);
        split.setResizeWeight(0.45);
        split.setDividerLocation(0.45);
        body.add(split, BorderLayout.CENTER);

        return body;
    }

    private JPanel buildEditorCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));

        if (db.DatabaseConfig.isElasticsearch()) {
            // Header with title and query configuration controls
            JPanel topPanel = new JPanel();
            topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
            topPanel.setOpaque(false);
            topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

            JPanel titleRow = new JPanel(new BorderLayout());
            titleRow.setOpaque(false);
            JLabel cardTitle = new JLabel("Query Request");
            cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
            cardTitle.setForeground(CLR_TEXT_PRI);
            titleRow.add(cardTitle, BorderLayout.WEST);

            JLabel methodBadge = new JLabel(" POST ");
            methodBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            methodBadge.setForeground(Color.WHITE);
            methodBadge.setOpaque(true);
            methodBadge.setBackground(CLR_ACCENT_DARK);
            methodBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
            titleRow.add(methodBadge, BorderLayout.EAST);
            topPanel.add(titleRow);
            topPanel.add(Box.createRigidArea(new Dimension(0, 8)));

            // Controls toolbar: Index, Endpoint, Template
            JPanel controlsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            controlsBar.setOpaque(false);

            controlsBar.add(new JLabel("Index:"));
            indexSelector = new JComboBox<>(ES_INDEXES);
            indexSelector.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            indexSelector.setBackground(Color.WHITE);
            controlsBar.add(indexSelector);

            controlsBar.add(new JLabel("Endpoint:"));
            endpointSelector = new JComboBox<>(ES_ENDPOINTS);
            endpointSelector.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            endpointSelector.setBackground(Color.WHITE);
            controlsBar.add(endpointSelector);

            controlsBar.add(new JLabel("Presets:"));
            templateSelector = new JComboBox<>(ES_TEMPLATES);
            templateSelector.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            templateSelector.setBackground(Color.WHITE);
            templateSelector.addActionListener(e -> applySelectedTemplate());
            controlsBar.add(templateSelector);

            topPanel.add(controlsBar);
            card.add(topPanel, BorderLayout.NORTH);

            // Request body editor
            sqlArea = new JTextArea();
            sqlArea.setFont(new Font("Consolas", Font.PLAIN, 14));
            sqlArea.setMargin(new Insets(8, 8, 8, 8));
            sqlArea.setLineWrap(false);
            sqlArea.setText("{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 10\n}");

            // Ctrl+Enter to execute
            sqlArea.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                        handleExecute();
                    }
                }
            });

            JScrollPane scroll = new JScrollPane(sqlArea);
            scroll.setBorder(new LineBorder(CLR_BORDER, 1, true));
            card.add(scroll, BorderLayout.CENTER);

            // Bottom action buttons
            JPanel btnPanel = new JPanel(new BorderLayout());
            btnPanel.setOpaque(false);
            btnPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

            JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            leftBtns.setOpaque(false);
            btnFormatJson = makeButton("Format JSON", new Color(71, 85, 105), CLR_BTN_FG);
            btnFormatJson.addActionListener(e -> handleFormatJson());
            leftBtns.add(btnFormatJson);
            btnPanel.add(leftBtns, BorderLayout.WEST);

            JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            rightBtns.setOpaque(false);
            btnClear = makeButton("Clear", new Color(100, 116, 139), CLR_BTN_FG);
            btnExecute = makeButton("Execute Query (Ctrl+Enter)", CLR_ACCENT_DARK, CLR_BTN_FG);

            btnClear.addActionListener(e -> handleClear());
            btnExecute.addActionListener(e -> handleExecute());

            rightBtns.add(btnClear);
            rightBtns.add(btnExecute);
            btnPanel.add(rightBtns, BorderLayout.EAST);

            card.add(btnPanel, BorderLayout.SOUTH);

        } else {
            // Neo4j Cypher Editor
            JLabel cardTitle = new JLabel("Cypher Editor");
            cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
            cardTitle.setForeground(CLR_TEXT_PRI);
            cardTitle.setBorder(new EmptyBorder(0, 0, 12, 0));
            card.add(cardTitle, BorderLayout.NORTH);

            sqlArea = new JTextArea();
            sqlArea.setFont(new Font("Consolas", Font.PLAIN, 14));
            sqlArea.setMargin(new Insets(8, 8, 8, 8));
            sqlArea.setLineWrap(true);
            sqlArea.setWrapStyleWord(true);
            sqlArea.setText("MATCH (c:Customer)-[:PURCHASED]->(p:Policy)\nRETURN c.firstName, c.email, p.policyNo, p.premiumAmount\nLIMIT 10;");

            sqlArea.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                        handleExecute();
                    }
                }
            });

            JScrollPane scroll = new JScrollPane(sqlArea);
            scroll.setBorder(new LineBorder(CLR_BORDER, 1, true));
            card.add(scroll, BorderLayout.CENTER);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btnPanel.setOpaque(false);
            btnPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

            btnExecute = makeButton("Execute Query", CLR_ACCENT_DARK, CLR_BTN_FG);
            btnClear   = makeButton("Clear", new Color(71, 85, 105), CLR_BTN_FG);

            btnExecute.addActionListener(e -> handleExecute());
            btnClear.addActionListener(e -> handleClear());

            btnPanel.add(btnClear);
            btnPanel.add(btnExecute);
            card.add(btnPanel, BorderLayout.SOUTH);
        }

        return card;
    }

    private JPanel buildResultsCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel cardTitle = new JLabel("Query Results");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cardTitle.setForeground(CLR_TEXT_PRI);
        headerRow.add(cardTitle, BorderLayout.WEST);

        if (db.DatabaseConfig.isElasticsearch()) {
            JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightHeader.setOpaque(false);

            responseMetricsLabel = new JLabel("No response yet");
            responseMetricsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            responseMetricsLabel.setForeground(CLR_MUTED_FG);
            rightHeader.add(responseMetricsLabel);

            btnCopyResponse = makeButton("Copy JSON", new Color(71, 85, 105), CLR_BTN_FG);
            btnCopyResponse.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnCopyResponse.setBorder(new EmptyBorder(4, 10, 4, 10));
            btnCopyResponse.addActionListener(e -> handleCopyResponse());
            rightHeader.add(btnCopyResponse);

            headerRow.add(rightHeader, BorderLayout.EAST);
            card.add(headerRow, BorderLayout.NORTH);

            // Monospace JSON response viewer
            resultsArea = new JTextArea();
            resultsArea.setFont(new Font("Consolas", Font.PLAIN, 13));
            resultsArea.setMargin(new Insets(8, 8, 8, 8));
            resultsArea.setEditable(false);
            resultsArea.setBackground(new Color(248, 250, 252));
            resultsArea.setText("// Elasticsearch JSON response will appear here.\n// Click 'Execute Query' to send a request.");

            JScrollPane scroll = new JScrollPane(resultsArea);
            scroll.setBorder(new LineBorder(CLR_BORDER, 1, true));
            card.add(scroll, BorderLayout.CENTER);

        } else {
            card.add(headerRow, BorderLayout.NORTH);

            tableModel = new DefaultTableModel(new Object[0][], new String[0]) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            resultsTable = new JTable(tableModel);
            resultsTable.setRowHeight(28);
            resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
            resultsTable.getTableHeader().setBackground(new Color(248, 250, 252));
            resultsTable.getTableHeader().setForeground(CLR_TEXT_PRI);
            resultsTable.setSelectionBackground(CLR_SEL_BG);
            resultsTable.setSelectionForeground(CLR_TEXT_PRI);
            resultsTable.setShowVerticalLines(true);
            resultsTable.setGridColor(CLR_BORDER);
            resultsTable.setFillsViewportHeight(true);
            resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            JScrollPane scroll = new JScrollPane(resultsTable);
            scroll.setBorder(new LineBorder(CLR_BORDER, 1, true));
            card.add(scroll, BorderLayout.CENTER);
        }

        return card;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(248, 250, 252));
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, CLR_BORDER),
                new EmptyBorder(6, 32, 6, 32)));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(CLR_MUTED_FG);
        bar.add(statusLabel, BorderLayout.WEST);

        return bar;
    }

    private void applySelectedTemplate() {
        if (templateSelector == null) return;
        int idx = templateSelector.getSelectedIndex();
        if (idx <= 0) return;

        switch (idx) {
            case 1 -> { // Search: Match All (customers)
                indexSelector.setSelectedItem(ElasticsearchIndexSetup.INDEX_CUSTOMERS);
                endpointSelector.setSelectedItem("_search");
                sqlArea.setText("{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 10\n}");
            }
            case 2 -> { // Search: Term Filter (claims by status)
                indexSelector.setSelectedItem(ElasticsearchIndexSetup.INDEX_CLAIMS);
                endpointSelector.setSelectedItem("_search");
                sqlArea.setText("{\n  \"query\": {\n    \"term\": {\n      \"status\": \"APPROVED\"\n    }\n  }\n}");
            }
            case 3 -> { // Search: Range Filter & Sort (claims by amount)
                indexSelector.setSelectedItem(ElasticsearchIndexSetup.INDEX_CLAIMS);
                endpointSelector.setSelectedItem("_search");
                sqlArea.setText("{\n  \"query\": {\n    \"range\": {\n      \"claimedAmount\": {\n        \"gte\": 1000\n      }\n    }\n  },\n  \"sort\": [\n    {\n      \"claimedAmount\": \"desc\"\n    }\n  ]\n}");
            }
            case 4 -> { // Search: Match Query (assets description)
                indexSelector.setSelectedItem(ElasticsearchIndexSetup.INDEX_ASSETS);
                endpointSelector.setSelectedItem("_search");
                sqlArea.setText("{\n  \"query\": {\n    \"match\": {\n      \"description\": \"Vehicle\"\n    }\n  }\n}");
            }
            case 5 -> { // Count: Count All (payments)
                indexSelector.setSelectedItem(ElasticsearchIndexSetup.INDEX_PAYMENTS);
                endpointSelector.setSelectedItem("_count");
                sqlArea.setText("{\n  \"query\": {\n    \"match_all\": {}\n  }\n}");
            }
            case 6 -> { // Count: Filtered Range (policies by premium)
                indexSelector.setSelectedItem(ElasticsearchIndexSetup.INDEX_POLICIES);
                endpointSelector.setSelectedItem("_count");
                sqlArea.setText("{\n  \"query\": {\n    \"range\": {\n      \"premiumAmount\": {\n        \"gte\": 500\n      }\n    }\n  }\n}");
            }
        }
        setStatus("Template applied: " + ES_TEMPLATES[idx], StatusType.MUTED);
    }

    private void handleFormatJson() {
        if (sqlArea == null) return;
        String text = sqlArea.getText().trim();
        if (text.isEmpty()) {
            sqlArea.setText("{}");
            return;
        }
        try {
            JsonNode tree = OBJECT_MAPPER.readTree(text);
            String formatted = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
            sqlArea.setText(formatted);
            setStatus("JSON formatted successfully.", StatusType.SUCCESS);
        } catch (JsonProcessingException e) {
            setStatus("Invalid JSON syntax: " + e.getOriginalMessage(), StatusType.ERROR);
        }
    }

    private void handleCopyResponse() {
        if (resultsArea == null) return;
        String text = resultsArea.getText();
        if (text != null && !text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            setStatus("Response copied to clipboard.", StatusType.SUCCESS);
        }
    }

    private void handleExecute() {
        if (db.DatabaseConfig.isElasticsearch()) {
            handleExecuteElasticsearch();
        } else {
            handleExecuteCypher();
        }
    }

    private void handleExecuteElasticsearch() {
        String index = (String) indexSelector.getSelectedItem();
        String endpoint = (String) endpointSelector.getSelectedItem();
        String json = sqlArea.getText().trim();

        if (json.isEmpty()) {
            json = "{}";
            sqlArea.setText("{}");
        } else {
            // Client-side JSON pre-validation
            try {
                OBJECT_MAPPER.readTree(json);
            } catch (JsonProcessingException e) {
                setStatus("JSON Syntax Error: " + e.getOriginalMessage(), StatusType.ERROR);
                resultsArea.setText("// JSON Validation Error:\n// " + e.getMessage());
                if (responseMetricsLabel != null) {
                    responseMetricsLabel.setText("Invalid JSON");
                }
                return;
            }
        }

        final String finalJson = json;
        final String finalEndpoint = endpoint;
        final String finalIndex = index;

        btnExecute.setEnabled(false);
        setStatus("Executing POST /" + finalIndex + "/" + finalEndpoint + " ...", StatusType.MUTED);
        resultsArea.setText("// Sending request to Elasticsearch...\n// Endpoint: POST /" + finalIndex + "/" + finalEndpoint);

        // Execute non-blocking on SwingWorker
        SwingWorker<ElasticsearchQueryResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ElasticsearchQueryResult doInBackground() {
                return backendService.executeElasticsearchQuery(finalIndex, finalEndpoint, finalJson);
            }

            @Override
            protected void done() {
                btnExecute.setEnabled(true);
                try {
                    ElasticsearchQueryResult res = get();
                    if (res == null) {
                        setStatus("Query failed: No response received.", StatusType.ERROR);
                        resultsArea.setText("// Error: No response received from server.");
                        return;
                    }

                    if (res.isSuccess()) {
                        resultsArea.setText(res.getResponseBody());
                        resultsArea.setCaretPosition(0);

                        StringBuilder metrics = new StringBuilder();
                        metrics.append("HTTP ").append(res.getStatusCode()).append(" OK");
                        metrics.append(" | ").append(res.getExecutionTimeMs()).append(" ms");

                        if (res.hasTotalHits()) {
                            metrics.append(" | Total: ").append(res.getTotalHits());
                            if ("_search".equals(finalEndpoint)) {
                                metrics.append(" (hits returned: ").append(res.getReturnedHitsCount()).append(")");
                            }
                        }

                        responseMetricsLabel.setText(metrics.toString());
                        setStatus("Query completed successfully in " + res.getExecutionTimeMs() + " ms (" + metrics + ")", StatusType.SUCCESS);

                    } else {
                        if (res.getResponseBody() != null && !res.getResponseBody().isEmpty()) {
                            resultsArea.setText(res.getResponseBody());
                            resultsArea.setCaretPosition(0);
                        } else {
                            resultsArea.setText("// Query Failed\n\n" + res.getErrorMessage());
                        }

                        responseMetricsLabel.setText("HTTP " + res.getStatusCode() + " Error");
                        setStatus(res.getErrorMessage(), StatusType.ERROR);
                    }

                } catch (Exception e) {
                    btnExecute.setEnabled(true);
                    setStatus("Execution error: " + e.getMessage(), StatusType.ERROR);
                    resultsArea.setText("// Execution Error:\n" + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void handleExecuteCypher() {
        String sql = sqlArea.getText().trim();
        if (sql.isEmpty()) {
            setStatus("Please enter a Cypher query.", StatusType.WARNING);
            return;
        }
        if (backendService == null) {
            setStatus("Backend integration pending.", StatusType.ERROR);
            return;
        }

        setStatus("Executing query...", StatusType.MUTED);

        QueryResult result = backendService.executeQuery(sql);

        if (result == null) {
            setStatus("Query failed: No result returned from backend.", StatusType.ERROR);
            return;
        }

        if (!result.isSuccess()) {
            setStatus("Query failed: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error"), StatusType.ERROR);
            tableModel.setColumnCount(0);
            tableModel.setRowCount(0);
            return;
        }

        // Setup columns
        if (result.getColumnNames() != null && !result.getColumnNames().isEmpty()) {
            String[] cols = result.getColumnNames().toArray(new String[0]);
            tableModel.setColumnIdentifiers(cols);
        } else {
            tableModel.setColumnCount(0);
        }

        // Setup rows
        tableModel.setRowCount(0);
        if (result.getRows() != null) {
            for (String[] row : result.getRows()) {
                tableModel.addRow(row);
            }
        }

        // Resize columns to fit roughly
        for (int i = 0; i < resultsTable.getColumnCount(); i++) {
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(150);
        }

        int count = tableModel.getRowCount();
        if (count > 0) {
            setStatus("Query executed successfully — " + count + " rows.", StatusType.SUCCESS);
        } else {
            setStatus("No rows returned.", StatusType.SUCCESS);
        }
    }

    private void handleClear() {
        if (db.DatabaseConfig.isElasticsearch()) {
            sqlArea.setText("{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 10\n}");
            if (resultsArea != null) {
                resultsArea.setText("// Console cleared.");
            }
            if (responseMetricsLabel != null) {
                responseMetricsLabel.setText("Cleared");
            }
            setStatus("Console cleared.", StatusType.MUTED);
        } else {
            sqlArea.setText("");
            if (tableModel != null) {
                tableModel.setColumnCount(0);
                tableModel.setRowCount(0);
            }
            setStatus("Ready", StatusType.MUTED);
        }
    }

    private enum StatusType { MUTED, SUCCESS, WARNING, ERROR }

    private void setStatus(String message, StatusType type) {
        statusLabel.setText(message);
        switch (type) {
            case SUCCESS -> statusLabel.setForeground(CLR_SUCCESS_FG);
            case WARNING -> statusLabel.setForeground(CLR_WARNING_FG);
            case ERROR   -> statusLabel.setForeground(CLR_ERROR_FG);
            default      -> statusLabel.setForeground(CLR_MUTED_FG);
        }
    }

    private JButton makeButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color draw = isEnabled() ? (getModel().isRollover() ? bg.darker() : bg) : new Color(200, 200, 200);
                g2.setColor(draw);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        return btn;
    }
}
