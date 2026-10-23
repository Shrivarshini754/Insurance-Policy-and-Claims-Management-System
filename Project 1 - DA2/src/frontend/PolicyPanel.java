package frontend;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PolicyPanel extends JPanel {

    public interface PolicyDataHandler {
        List<String[]> loadAllPolicies();
        String[] findPolicy(String policyNo);
        boolean addPolicy(String policyNo, String startDate, String expiryDate, String auditId, double premiumAmount);
        boolean updatePolicy(String policyNo, String startDate, String expiryDate, String auditId, double premiumAmount);
        boolean deletePolicy(String policyNo);
    }

    private static final Color CLR_BG          = new Color(241, 245, 249);
    private static final Color CLR_CARD        = Color.WHITE;
    private static final Color CLR_BORDER      = new Color(226, 232, 240);
    private static final Color CLR_ACCENT_DARK = new Color(14,  116, 144);
    private static final Color CLR_TEXT_PRI    = new Color(15,   23,  42);
    private static final Color CLR_TEXT_SEC    = new Color(100, 116, 139);
    private static final Color CLR_BTN_FG      = Color.WHITE;
    private static final Color CLR_SEL_BG      = new Color(224, 242, 254);
    private static final Color CLR_GREEN       = new Color( 22, 163,  74);
    private static final Color CLR_GREEN_BG    = new Color(220, 252, 231);
    private static final Color CLR_AMBER       = new Color(180, 120,   0);
    private static final Color CLR_AMBER_BG    = new Color(254, 243, 199);
    private static final Color CLR_RED         = new Color(185,  28,  28);
    private static final Color CLR_RED_BG      = new Color(254, 226, 226);
    private static final Color CLR_DANGER      = new Color(220,  38,  38);
    private static final Color CLR_MUTED_FG    = new Color(100, 116, 139);
    private static final Color CLR_SUCCESS_FG  = new Color( 22, 163,  74);
    private static final Color CLR_WARNING_FG  = new Color(146,  64,  14);
    private static final Color CLR_ERROR_FG    = new Color(185,  28,  28);

    private static final String[] COLUMNS = { "Policy No.", "Start Date", "Expiry Date", "Audit ID", "Premium Amount" };

    private DefaultTableModel tableModel;
    private JTable            policyTable;
    private JTextField        searchField;
    private JLabel            statusLabel;
    private JLabel            rowCountLabel;

    private JLabel lblActive;
    private JLabel lblExpiringSoon;
    private JLabel lblExpired;

    private JTextField fldPolicyNo;
    private JTextField fldStartDate;
    private JTextField fldExpiryDate;
    private JTextField fldAuditId;
    private JTextField fldPremium;

    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnSaveAdd;

    private boolean editMode = false;
    private PolicyDataHandler dataHandler = null;

    public PolicyPanel() {
        setBackground(CLR_BG);
        setLayout(new BorderLayout());
        add(buildPageHeader(), BorderLayout.NORTH);
        add(buildBody(),       BorderLayout.CENTER);
        add(buildBottomBar(),  BorderLayout.SOUTH);
        updateButtonStates();
        setStatus("Ready", StatusType.MUTED);
    }

    public void setDataHandler(PolicyDataHandler handler) { this.dataHandler = handler; }

    public void setPolicyData(List<String[]> data) {
        tableModel.setRowCount(0);
        if (data != null) { for (String[] row : data) tableModel.addRow(row); }
        updateRowCountLabel();
        refreshStatusCounts();
        clearForm();
    }

    public void loadPolicies() {
        if (dataHandler == null) { setStatus("Backend integration pending.", StatusType.WARNING); return; }
        setStatus("Loading policies...", StatusType.MUTED);
        List<String[]> policies = dataHandler.loadAllPolicies();
        setPolicyData(policies);
        int count = tableModel.getRowCount();
        setStatus(count == 0 ? "No policies found." : count + " policy record(s) loaded.", StatusType.MUTED);
    }

    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setBackground(CLR_BG);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(24, 32, 16, 32));
        JLabel title = new JLabel("Policies");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(CLR_TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Manage insurance policies and coverage details");
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
        body.add(buildToolbar(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTableCard(), buildFormCard());
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(8);
        split.setResizeWeight(0.65);
        split.setDividerLocation(0.65);
        body.add(split, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(220, 34));
        searchField.setBorder(new CompoundBorder(new LineBorder(CLR_BORDER, 1, true), new EmptyBorder(4, 10, 4, 10)));
        searchField.addActionListener(e -> handleSearch());
        bar.add(searchField);
        JButton btnSearch  = makeButton("Search", CLR_ACCENT_DARK, CLR_BTN_FG);
        JButton btnAdd     = makeButton("+ Add Policy", CLR_ACCENT_DARK, CLR_BTN_FG);
        JButton btnRefresh = makeButton("Refresh", new Color(71, 85, 105), CLR_BTN_FG);
        btnSearch .addActionListener(e -> handleSearch());
        btnAdd    .addActionListener(e -> enterAddMode());
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadPolicies(); });
        bar.add(btnSearch);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(btnAdd);
        bar.add(btnRefresh);
        return bar;
    }

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(new LineBorder(CLR_BORDER, 1, true), new EmptyBorder(16, 16, 16, 16)));
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        cardHeader.setBorder(new EmptyBorder(0, 0, 12, 0));
        JLabel cardTitle = new JLabel("Policy Records");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cardTitle.setForeground(CLR_TEXT_PRI);
        cardHeader.add(cardTitle, BorderLayout.WEST);
        rowCountLabel = new JLabel("0 records");
        rowCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rowCountLabel.setForeground(CLR_TEXT_SEC);
        cardHeader.add(rowCountLabel, BorderLayout.EAST);
        card.add(cardHeader, BorderLayout.NORTH);
        tableModel = new DefaultTableModel(new Object[0][], COLUMNS) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        policyTable = new JTable(tableModel);
        policyTable.setRowHeight(28);
        policyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        policyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        policyTable.getTableHeader().setBackground(new Color(248, 250, 252));
        policyTable.getTableHeader().setForeground(CLR_TEXT_PRI);
        policyTable.setSelectionBackground(CLR_SEL_BG);
        policyTable.setSelectionForeground(CLR_TEXT_PRI);
        policyTable.setShowVerticalLines(false);
        policyTable.setGridColor(CLR_BORDER);
        policyTable.setFillsViewportHeight(true);
        policyTable.setIntercellSpacing(new Dimension(8, 0));
        policyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = { 90, 100, 110, 90, 120 };
        for (int i = 0; i < widths.length; i++) { policyTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]); }
        policyTable.getColumnModel().getColumn(2).setCellRenderer(new ExpiryDateRenderer());
        policyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onTableSelectionChanged();
        });
        JScrollPane scroll = new JScrollPane(policyTable);
        scroll.setBorder(new LineBorder(CLR_BORDER, 1, true));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(new LineBorder(CLR_BORDER, 1, true), new EmptyBorder(20, 20, 20, 20)));
        JLabel cardTitle = new JLabel("Policy Details");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        cardTitle.setForeground(CLR_TEXT_PRI);
        cardTitle.setBorder(new EmptyBorder(0, 0, 16, 0));
        card.add(cardTitle, BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(6, 0, 6, 8);
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        fldPolicyNo   = new JTextField();
        fldStartDate  = new JTextField();
        fldExpiryDate = new JTextField();
        fldAuditId    = new JTextField();
        fldPremium    = new JTextField();
        String[]     labels = { "Policy No. *", "Start Date *", "Expiry Date *", "Audit ID *", "Premium Amount *" };
        JTextField[] fields = { fldPolicyNo, fldStartDate, fldExpiryDate, fldAuditId, fldPremium };
        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0;
            form.add(makeFormLabel(labels[i]), gc);
            gc.gridx = 1; gc.weightx = 1.0;
            styleField(fields[i]);
            form.add(fields[i], gc);
        }
        card.add(form, BorderLayout.CENTER);
        card.add(buildFormButtons(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildFormButtons() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));
        btnSaveAdd   = makeButton("Save / Add", CLR_ACCENT_DARK,            CLR_BTN_FG);
        btnUpdate    = makeButton("Update",     new Color(  5, 150, 105),   CLR_BTN_FG);
        btnDelete    = makeButton("Delete",     CLR_DANGER,                 CLR_BTN_FG);
        JButton btnClear = makeButton("Clear",  new Color( 71,  85, 105),   CLR_BTN_FG);
        btnSaveAdd.addActionListener(e -> handleAdd());
        btnUpdate .addActionListener(e -> handleUpdate());
        btnDelete .addActionListener(e -> handleDelete());
        btnClear  .addActionListener(e -> { clearForm(); setStatus("Ready", StatusType.MUTED); });
        panel.add(btnSaveAdd);
        panel.add(btnUpdate);
        panel.add(btnDelete);
        panel.add(btnClear);
        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(248, 250, 252));
        bar.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, CLR_BORDER), new EmptyBorder(6, 32, 6, 32)));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(CLR_MUTED_FG);
        bar.add(statusLabel, BorderLayout.WEST);
        JPanel pills = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pills.setOpaque(false);
        lblActive       = makeStatusPill("Active: 0",        CLR_GREEN, CLR_GREEN_BG);
        lblExpiringSoon = makeStatusPill("Expiring Soon: 0", CLR_AMBER, CLR_AMBER_BG);
        lblExpired      = makeStatusPill("Expired: 0",       CLR_RED,   CLR_RED_BG);
        pills.add(lblActive);
        pills.add(lblExpiringSoon);
        pills.add(lblExpired);
        bar.add(pills, BorderLayout.EAST);
        return bar;
    }

    private JLabel makeStatusPill(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(fg);
        lbl.setBackground(bg);
        lbl.setOpaque(false);
        lbl.setBorder(new EmptyBorder(4, 12, 4, 12));
        return lbl;
    }

    private void refreshStatusCounts() {
        LocalDate today = LocalDate.now();
        LocalDate soonThreshold = today.plusDays(90);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int active = 0, soon = 0, expired = 0;
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Object val = tableModel.getValueAt(r, 2);
            if (val == null) { active++; continue; }
            try {
                LocalDate expiry = LocalDate.parse(val.toString().trim(), fmt);
                if (expiry.isBefore(today)) { expired++; } else if (!expiry.isAfter(soonThreshold)) { soon++; } else { active++; }
            } catch (Exception ignored) { active++; }
        }
        lblActive      .setText("Active: "        + active);
        lblExpiringSoon.setText("Expiring Soon: " + soon);
        lblExpired     .setText("Expired: "       + expired);
    }

    private void onTableSelectionChanged() {
        int row = policyTable.getSelectedRow();
        if (row < 0) { editMode = false; updateButtonStates(); return; }
        fldPolicyNo  .setText(tableModel.getValueAt(row, 0).toString());
        fldStartDate .setText(tableModel.getValueAt(row, 1).toString());
        fldExpiryDate.setText(tableModel.getValueAt(row, 2).toString());
        fldAuditId   .setText(tableModel.getValueAt(row, 3).toString());
        fldPremium   .setText(tableModel.getValueAt(row, 4).toString());
        fldPolicyNo.setEditable(false);
        fldPolicyNo.setBackground(new Color(248, 250, 252));
        editMode = true;
        updateButtonStates();
        setStatus("Policy selected. Edit fields and click Update, or click Delete to remove.", StatusType.MUTED);
    }

    private void enterAddMode() {
        clearForm();
        editMode = false;
        updateButtonStates();
        fldPolicyNo.requestFocusInWindow();
        setStatus("Enter policy details and click Save / Add.", StatusType.MUTED);
    }

    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) { setStatus("Please enter a Policy No. to search.", StatusType.WARNING); return; }
        if (dataHandler == null) { setStatus("Backend integration pending.", StatusType.WARNING); return; }
        String[] result = dataHandler.findPolicy(query);
        if (result == null) { setStatus("No policy found with Policy No.: " + query, StatusType.WARNING); clearForm(); return; }
        tableModel.setRowCount(0);
        tableModel.addRow(result);
        updateRowCountLabel();
        refreshStatusCounts();
        policyTable.setRowSelectionInterval(0, 0);
        setStatus("Policy found.", StatusType.SUCCESS);
    }

    private void handleAdd() {
        double[] premium = validateAndCollect();
        if (premium == null) return;
        if (dataHandler == null) { setStatus("Backend integration pending.", StatusType.WARNING); return; }
        boolean ok = dataHandler.addPolicy(fldPolicyNo.getText().trim(), fldStartDate.getText().trim(), fldExpiryDate.getText().trim(), fldAuditId.getText().trim(), premium[0]);
        if (ok) { setStatus("Policy added successfully.", StatusType.SUCCESS); loadPolicies(); } 
        else { setStatus("Add failed.", StatusType.ERROR); }
    }

    private void handleUpdate() {
        if (!editMode || policyTable.getSelectedRow() < 0) return;
        double[] premium = validateAndCollect();
        if (premium == null) return;
        if (dataHandler == null) { setStatus("Backend integration pending.", StatusType.WARNING); return; }
        boolean ok = dataHandler.updatePolicy(fldPolicyNo.getText().trim(), fldStartDate.getText().trim(), fldExpiryDate.getText().trim(), fldAuditId.getText().trim(), premium[0]);
        if (ok) { setStatus("Policy updated successfully.", StatusType.SUCCESS); loadPolicies(); } 
        else { setStatus("Update failed.", StatusType.ERROR); }
    }

    private void handleDelete() {
        if (policyTable.getSelectedRow() < 0) return;
        String policyNo  = tableModel.getValueAt(policyTable.getSelectedRow(), 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete policy " + policyNo + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        if (dataHandler == null) return;
        if (dataHandler.deletePolicy(policyNo)) { setStatus("Policy deleted.", StatusType.SUCCESS); loadPolicies(); } 
        else { setStatus("Delete failed.", StatusType.ERROR); }
    }

    private double[] validateAndCollect() {
        String policyNo = fldPolicyNo.getText().trim(), startDate = fldStartDate.getText().trim();
        String expiryDate = fldExpiryDate.getText().trim(), auditId = fldAuditId.getText().trim(), premiumStr = fldPremium.getText().trim();
        if (policyNo.isEmpty() || startDate.isEmpty() || expiryDate.isEmpty() || auditId.isEmpty() || premiumStr.isEmpty()) {
            setStatus("All fields are required.", StatusType.ERROR); return null;
        }
        try {
            return new double[]{ Double.parseDouble(premiumStr) };
        } catch (NumberFormatException ex) {
            setStatus("Premium must be numeric.", StatusType.ERROR); return null;
        }
    }

    private void clearForm() {
        fldPolicyNo.setText(""); fldStartDate.setText(""); fldExpiryDate.setText(""); fldAuditId.setText(""); fldPremium.setText("");
        fldPolicyNo.setEditable(true); fldPolicyNo.setBackground(Color.WHITE);
        policyTable.clearSelection(); editMode = false; updateButtonStates();
    }

    private void updateButtonStates() { btnUpdate.setEnabled(editMode); btnDelete.setEnabled(editMode); btnSaveAdd.setEnabled(true); }
    private void updateRowCountLabel() { int c = tableModel.getRowCount(); rowCountLabel.setText(c + (c == 1 ? " record" : " records")); }

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

    private class ExpiryDateRenderer extends DefaultTableCellRenderer {
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private final LocalDate today = LocalDate.now(), soon = today.plusDays(90);
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, s, f, r, c);
            if (!s && v != null) {
                try {
                    LocalDate expiry = LocalDate.parse(v.toString().trim(), fmt);
                    if (expiry.isBefore(today)) setForeground(CLR_RED);
                    else if (!expiry.isAfter(soon)) setForeground(CLR_AMBER);
                    else setForeground(CLR_GREEN);
                } catch (Exception e) { setForeground(CLR_TEXT_PRI); }
            } else setForeground(CLR_TEXT_PRI);
            return this;
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
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(fg); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false); btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        return btn;
    }

    private JLabel makeFormLabel(String text) {
        JLabel lbl = new JLabel(text + ":"); lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13)); lbl.setForeground(CLR_TEXT_SEC); return lbl;
    }
    private void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13)); field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(0, 30)); field.setBorder(new CompoundBorder(new LineBorder(CLR_BORDER, 1, true), new EmptyBorder(4, 8, 4, 8)));
    }
}