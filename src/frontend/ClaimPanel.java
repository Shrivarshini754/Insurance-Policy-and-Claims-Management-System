package frontend;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ClaimPanel extends JPanel {

    public interface ClaimDataHandler {
        List<String[]> loadAllClaims();
        String[] findClaim(String claimId);
        double getClaimAmount(String claimId);
        boolean addClaim(String claimId, String policyNo, String incidentDate, String description, String status);
        boolean updateClaim(String claimId, String policyNo, String incidentDate, String description, double claimedAmount, String status);
        boolean deleteClaim(String claimId);
    }

    private static final Color CLR_BG          = new Color(241, 245, 249);
    private static final Color CLR_CARD        = Color.WHITE;
    private static final Color CLR_BORDER      = new Color(226, 232, 240);
    private static final Color CLR_ACCENT_DARK = new Color(14,  116, 144);
    private static final Color CLR_TEXT_PRI    = new Color(15,   23,  42);
    private static final Color CLR_TEXT_SEC    = new Color(100, 116, 139);
    private static final Color CLR_BTN_FG      = Color.WHITE;
    private static final Color CLR_SEL_BG      = new Color(224, 242, 254);
    private static final Color CLR_APPROVED_FG = new Color(22,  163,  74);
    private static final Color CLR_PENDING_FG  = new Color(180, 120,   0);
    private static final Color CLR_REJECTED_FG = new Color(185,  28,  28);

    private static final String[] COLUMNS = { "Claim ID", "Policy No.", "Incident Date", "Description", "Claimed Amount", "Status" };
    private static final String[] STATUS_OPTIONS = { "Pending", "Approved", "Rejected" };

    private DefaultTableModel tableModel;
    private JTable            claimTable;
    private JTextField        searchField;
    private JLabel            statusLabel;
    private JLabel            rowCountLabel;

    private JTextField fldClaimId;
    private JTextField fldPolicyNo;
    private JTextField fldIncidentDate;
    private JTextField fldDescription;
    private JTextField fldClaimedAmount;
    private JComboBox<String> cbStatus;

    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnSaveAdd;
    private JButton btnGetAmount;

    private boolean editMode = false;
    private ClaimDataHandler dataHandler = null;

    public ClaimPanel() {
        setBackground(CLR_BG);
        setLayout(new BorderLayout());
        add(buildPageHeader(), BorderLayout.NORTH);
        add(buildBody(),       BorderLayout.CENTER);
        add(buildBottomBar(),  BorderLayout.SOUTH);
        updateButtonStates();
        setStatus("Ready", StatusType.MUTED);
    }

    public void setDataHandler(ClaimDataHandler handler) { this.dataHandler = handler; }

    public void setClaimData(List<String[]> data) {
        tableModel.setRowCount(0);
        if (data != null) { for (String[] row : data) tableModel.addRow(row); }
        updateRowCountLabel();
        clearForm();
    }

    public void loadClaims() {
        if (dataHandler == null) return;
        List<String[]> claims = dataHandler.loadAllClaims();
        setClaimData(claims);
    }

    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setBackground(CLR_BG);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(24, 32, 16, 32));
        JLabel title = new JLabel("Claims");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(CLR_TEXT_PRI);
        header.add(title);
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
        split.setResizeWeight(0.68);
        split.setDividerLocation(0.68);
        body.add(split, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(220, 34));
        bar.add(searchField);
        JButton btnSearch  = makeButton("Search", CLR_ACCENT_DARK, CLR_BTN_FG);
        JButton btnAdd     = makeButton("+ Add Claim", CLR_ACCENT_DARK, CLR_BTN_FG);
        JButton btnRefresh = makeButton("Refresh", new Color(71, 85, 105), CLR_BTN_FG);
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadClaims(); });
        bar.add(btnSearch); bar.add(Box.createHorizontalStrut(12)); bar.add(btnAdd); bar.add(btnRefresh);
        return bar;
    }

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(new LineBorder(CLR_BORDER, 1, true), new EmptyBorder(16, 16, 16, 16)));
        tableModel = new DefaultTableModel(new Object[0][], COLUMNS) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        claimTable = new JTable(tableModel);
        claimTable.setRowHeight(28);
        claimTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) onTableSelectionChanged(); });
        card.add(new JScrollPane(claimTable), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(new LineBorder(CLR_BORDER, 1, true), new EmptyBorder(20, 20, 20, 20)));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(6, 0, 6, 8);
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        fldClaimId = new JTextField(); fldPolicyNo = new JTextField(); fldIncidentDate = new JTextField();
        fldDescription = new JTextField(); fldClaimedAmount = new JTextField(); cbStatus = new JComboBox<>(STATUS_OPTIONS);
        
        String[] labels = { "Claim ID *", "Policy No. *", "Incident Date *", "Description *", "Claimed Amt.", "Status *" };
        JComponent[] fields = { fldClaimId, fldPolicyNo, fldIncidentDate, fldDescription, fldClaimedAmount, cbStatus };
        
        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0;
            form.add(new JLabel(labels[i]), gc);
            gc.gridx = 1; gc.weightx = 1.0;
            if (fields[i] instanceof JTextField) {
                ((JTextField)fields[i]).setPreferredSize(new Dimension(0, 30));
            }
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
        btnSaveAdd = makeButton("Save / Add", CLR_ACCENT_DARK, CLR_BTN_FG);
        btnUpdate  = makeButton("Update", new Color(5, 150, 105), CLR_BTN_FG);
        btnDelete  = makeButton("Delete", new Color(220, 38, 38), CLR_BTN_FG);
        JButton btnClear = makeButton("Clear", new Color(71, 85, 105), CLR_BTN_FG);
        btnSaveAdd.addActionListener(e -> handleAdd());
        btnUpdate.addActionListener(e -> handleUpdate());
        btnDelete.addActionListener(e -> handleDelete());
        btnClear.addActionListener(e -> clearForm());
        panel.add(btnSaveAdd); panel.add(btnUpdate); panel.add(btnDelete); panel.add(btnClear);
        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(248, 250, 252));
        bar.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, CLR_BORDER), new EmptyBorder(6, 32, 6, 32)));
        statusLabel = new JLabel("Ready");
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private void onTableSelectionChanged() {
        int row = claimTable.getSelectedRow();
        if (row < 0) { editMode = false; updateButtonStates(); return; }
        fldClaimId.setText(tableModel.getValueAt(row, 0).toString());
        fldPolicyNo.setText(tableModel.getValueAt(row, 1).toString());
        fldIncidentDate.setText(tableModel.getValueAt(row, 2).toString());
        fldDescription.setText(tableModel.getValueAt(row, 3).toString());
        fldClaimedAmount.setText(tableModel.getValueAt(row, 4).toString());
        cbStatus.setSelectedItem(tableModel.getValueAt(row, 5).toString());
        fldClaimId.setEditable(false);
        editMode = true; updateButtonStates();
    }

    private void handleAdd() {
        if (dataHandler == null) return;
        boolean ok = dataHandler.addClaim(fldClaimId.getText().trim(), fldPolicyNo.getText().trim(), fldIncidentDate.getText().trim(), fldDescription.getText().trim(), cbStatus.getSelectedItem().toString());
        if (ok) loadClaims();
    }

    private void handleUpdate() {
        if (!editMode || claimTable.getSelectedRow() < 0 || dataHandler == null) return;
        boolean ok = dataHandler.updateClaim(fldClaimId.getText().trim(), fldPolicyNo.getText().trim(), fldIncidentDate.getText().trim(), fldDescription.getText().trim(), Double.parseDouble(fldClaimedAmount.getText().trim().isEmpty() ? "0" : fldClaimedAmount.getText().trim()), cbStatus.getSelectedItem().toString());
        if (ok) loadClaims();
    }

    private void handleDelete() {
        if (claimTable.getSelectedRow() < 0 || dataHandler == null) return;
        boolean ok = dataHandler.deleteClaim(fldClaimId.getText().trim());
        if (ok) loadClaims();
    }

    private void clearForm() {
        fldClaimId.setText(""); fldPolicyNo.setText(""); fldIncidentDate.setText(""); fldDescription.setText(""); fldClaimedAmount.setText(""); cbStatus.setSelectedIndex(0);
        fldClaimId.setEditable(true);
        claimTable.clearSelection();
        editMode = false; updateButtonStates();
    }

    private void updateButtonStates() {
        btnUpdate.setEnabled(editMode); btnDelete.setEnabled(editMode); btnSaveAdd.setEnabled(true);
    }
    
    private void updateRowCountLabel() { }
    private enum StatusType { MUTED, SUCCESS, WARNING, ERROR }
    private void setStatus(String message, StatusType type) { statusLabel.setText(message); }

    private JButton makeButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg); btn.setForeground(fg); btn.setOpaque(true); btn.setBorderPainted(false);
        return btn;
    }
}