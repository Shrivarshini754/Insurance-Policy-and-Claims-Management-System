package frontend;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

import service.BackendService;
import model.QueryResult;

import java.awt.*;
import java.util.List;

public class SQLQueryPanel extends JPanel {

    private BackendService backendService;
    
    private static final Color CLR_BG          = new Color(241, 245, 249);
    private static final Color CLR_CARD        = Color.WHITE;
    private static final Color CLR_BORDER      = new Color(226, 232, 240);
    private static final Color CLR_ACCENT_DARK = new Color(14,  116, 144);
    private static final Color CLR_TEXT_PRI    = new Color(15,   23,  42);
    private static final Color CLR_TEXT_SEC    = new Color(100, 116, 139);
    private static final Color CLR_BTN_FG      = Color.WHITE;
    private static final Color CLR_SEL_BG      = new Color(224, 242, 254);

    private static final Color CLR_MUTED_FG    = new Color(100, 116, 139);
    private static final Color CLR_SUCCESS_FG  = new Color( 22, 163,  74);
    private static final Color CLR_WARNING_FG  = new Color(146,  64,  14);
    private static final Color CLR_ERROR_FG    = new Color(185,  28,  28);

    private JTextArea         sqlArea;
    private DefaultTableModel tableModel;
    private JTable            resultsTable;
    private JLabel            statusLabel;

    public SQLQueryPanel(BackendService backendService) {
        this.backendService = backendService;
        setBackground(CLR_BG);
        setLayout(new BorderLayout());
        add(buildPageHeader(), BorderLayout.NORTH);
        add(buildBody(),       BorderLayout.CENTER);
        add(buildBottomBar(),  BorderLayout.SOUTH);

        setStatus("Ready", StatusType.MUTED);
    }

    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setBackground(CLR_BG);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(24, 32, 16, 32));

        JLabel title = new JLabel("SQL Query");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(CLR_TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Execute SQL queries against the insurance database");
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
        split.setResizeWeight(0.4);
        split.setDividerLocation(0.4);
        body.add(split, BorderLayout.CENTER);

        return body;
    }

    private JPanel buildEditorCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));

        JLabel cardTitle = new JLabel("SQL Editor");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cardTitle.setForeground(CLR_TEXT_PRI);
        cardTitle.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(cardTitle, BorderLayout.NORTH);

        sqlArea = new JTextArea();
        sqlArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        sqlArea.setMargin(new Insets(8, 8, 8, 8));
        sqlArea.setLineWrap(true);
        sqlArea.setWrapStyleWord(true);
        
        JScrollPane scroll = new JScrollPane(sqlArea);
        scroll.setBorder(new LineBorder(CLR_BORDER, 1, true));
        card.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

        JButton btnExecute = makeButton("Execute Query", CLR_ACCENT_DARK, CLR_BTN_FG);
        JButton btnClear   = makeButton("Clear", new Color(71, 85, 105), CLR_BTN_FG);

        btnExecute.addActionListener(e -> handleExecute());
        btnClear.addActionListener(e -> handleClear());

        btnPanel.add(btnClear);
        btnPanel.add(btnExecute);
        card.add(btnPanel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildResultsCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));

        JLabel cardTitle = new JLabel("Query Results");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cardTitle.setForeground(CLR_TEXT_PRI);
        cardTitle.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(cardTitle, BorderLayout.NORTH);

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

private void handleExecute() {
        String sql = sqlArea.getText().trim();
        if (sql.isEmpty()) {
            setStatus("Please enter a SQL query.", StatusType.WARNING);
            return;
        }
        if (backendService == null) {
            setStatus("Backend integration pending.", StatusType.ERROR);
            return;
        }

        setStatus("Executing query...", StatusType.MUTED);
        
        // Execute the query via BackendService
        QueryResult result = backendService.executeQuery(sql);

        if (result == null) {
            setStatus("Query failed: No result returned from backend.", StatusType.ERROR);
            return;
        }

        // Use getters instead of direct variable access
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
        sqlArea.setText("");
        tableModel.setColumnCount(0);
        tableModel.setRowCount(0);
        setStatus("Ready", StatusType.MUTED);
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