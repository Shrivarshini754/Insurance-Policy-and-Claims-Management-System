package frontend;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AssetPanel extends JPanel {

    public interface AssetDataHandler {
        List<String[]> loadAllAssets();
        boolean addAsset(String assetId, String description, String purchaseDate, double estimatedValue);
        boolean updateAsset(String assetId, String description, String purchaseDate, double estimatedValue);
        boolean deleteAsset(String assetId);
    }

    private static final Color CLR_BG          = new Color(241, 245, 249);
    private static final Color CLR_CARD        = Color.WHITE;
    private static final Color CLR_BORDER      = new Color(226, 232, 240);
    private static final Color CLR_ACCENT_DARK = new Color(14,  116, 144);
    private static final Color CLR_TEXT_PRI    = new Color(15,   23,  42);
    private static final Color CLR_TEXT_SEC    = new Color(100, 116, 139);
    private static final Color CLR_BTN_FG      = Color.WHITE;
    private static final Color CLR_SEL_BG      = new Color(224, 242, 254);

    private static final Color CLR_DANGER      = new Color(220,  38,  38);
    private static final Color CLR_MUTED_FG    = new Color(100, 116, 139);
    private static final Color CLR_SUCCESS_FG  = new Color( 22, 163,  74);
    private static final Color CLR_WARNING_FG  = new Color(146,  64,  14);
    private static final Color CLR_ERROR_FG    = new Color(185,  28,  28);

    private static final String[] COLUMNS = {
        "Asset ID", "Description", "Purchase Date", "Estimated Value"
    };

    private DefaultTableModel tableModel;
    private JTable            assetTable;
    private JLabel            statusLabel;
    private JLabel            rowCountLabel;

    private JTextField fldAssetId;
    private JTextField fldDescription;
    private JTextField fldPurchaseDate;
    private JTextField fldEstimatedValue;

    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnSaveAdd;

    private boolean editMode = false;
    private AssetDataHandler dataHandler = null;

    public AssetPanel() {
        setBackground(CLR_BG);
        setLayout(new BorderLayout());
        add(buildPageHeader(), BorderLayout.NORTH);
        add(buildBody(),       BorderLayout.CENTER);
        add(buildBottomBar(),  BorderLayout.SOUTH);

        updateButtonStates();
        setStatus("Ready", StatusType.MUTED);
    }

    public void setDataHandler(AssetDataHandler handler) {
        this.dataHandler = handler;
    }

    public void setAssetData(List<String[]> data) {
        tableModel.setRowCount(0);
        if (data != null) {
            for (String[] row : data) tableModel.addRow(row);
        }
        updateRowCountLabel();
        clearForm();
    }

    public void loadAssets() {
        if (dataHandler == null) {
            setStatus("Backend integration pending.", StatusType.WARNING);
            return;
        }
        setStatus("Loading assets...", StatusType.MUTED);
        List<String[]> assets = dataHandler.loadAllAssets();
        setAssetData(assets);
        int count = tableModel.getRowCount();
        setStatus(count == 0 ? "No assets found." : count + " asset(s) loaded.", StatusType.MUTED);
    }

    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setBackground(CLR_BG);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(24, 32, 16, 32));

        JLabel title = new JLabel("Assets");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(CLR_TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Manage insured assets and properties");
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

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildTableCard(),
                buildFormCard());
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

        JButton btnAdd     = makeButton("+ Add Asset", CLR_ACCENT_DARK, CLR_BTN_FG);
        JButton btnRefresh = makeButton("Refresh", new Color(71, 85, 105), CLR_BTN_FG);

        btnAdd    .addActionListener(e -> enterAddMode());
        btnRefresh.addActionListener(e -> loadAssets());

        bar.add(btnAdd);
        bar.add(btnRefresh);

        return bar;
    }

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        cardHeader.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel cardTitle = new JLabel("Asset Records");
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
        assetTable = new JTable(tableModel);
        assetTable.setRowHeight(28);
        assetTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        assetTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        assetTable.getTableHeader().setBackground(new Color(248, 250, 252));
        assetTable.getTableHeader().setForeground(CLR_TEXT_PRI);
        assetTable.setSelectionBackground(CLR_SEL_BG);
        assetTable.setSelectionForeground(CLR_TEXT_PRI);
        assetTable.setShowVerticalLines(false);
        assetTable.setGridColor(CLR_BORDER);
        assetTable.setFillsViewportHeight(true);
        assetTable.setIntercellSpacing(new Dimension(8, 0));
        assetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int[] widths = { 100, 200, 120, 120 };
        for (int i = 0; i < widths.length; i++) {
            assetTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        assetTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onTableSelectionChanged();
        });

        JScrollPane scroll = new JScrollPane(assetTable);
        scroll.setBorder(new LineBorder(CLR_BORDER, 1, true));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(20, 20, 20, 20)));

        JLabel cardTitle = new JLabel("Asset Details");
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

        fldAssetId        = new JTextField();
        fldDescription    = new JTextField();
        fldPurchaseDate   = new JTextField();
        fldEstimatedValue = new JTextField();

        fldPurchaseDate.setToolTipText("Format: YYYY-MM-DD");

        String[]     labels = { "Asset ID *", "Description *", "Purchase Date *", "Estimated Value *" };
        JTextField[] fields = { fldAssetId, fldDescription, fldPurchaseDate, fldEstimatedValue };

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
        btnClear  .addActionListener(e -> {
            clearForm();
            setStatus("Ready", StatusType.MUTED);
        });

        panel.add(btnSaveAdd);
        panel.add(btnUpdate);
        panel.add(btnDelete);
        panel.add(btnClear);

        return panel;
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

        JLabel reqLabel = new JLabel("* Required fields");
        reqLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        reqLabel.setForeground(CLR_TEXT_SEC);
        bar.add(reqLabel, BorderLayout.EAST);
        
        return bar;
    }

    private void onTableSelectionChanged() {
        int row = assetTable.getSelectedRow();
        if (row < 0) {
            editMode = false;
            updateButtonStates();
            return;
        }
        fldAssetId       .setText(tableModel.getValueAt(row, 0).toString());
        fldDescription   .setText(tableModel.getValueAt(row, 1).toString());
        fldPurchaseDate  .setText(tableModel.getValueAt(row, 2).toString());
        fldEstimatedValue.setText(tableModel.getValueAt(row, 3).toString());

        fldAssetId.setEditable(false);
        fldAssetId.setBackground(new Color(248, 250, 252));

        editMode = true;
        updateButtonStates();
        setStatus("Asset selected. Edit fields and click Update.", StatusType.MUTED);
    }

    private void enterAddMode() {
        clearForm();
        editMode = false;
        updateButtonStates();
        fldAssetId.requestFocusInWindow();
        setStatus("Enter asset details and click Save / Add.", StatusType.MUTED);
    }

    private void handleAdd() {
        String assetId = fldAssetId.getText().trim();
        String description = fldDescription.getText().trim();
        String date = fldPurchaseDate.getText().trim();
        String estValStr = fldEstimatedValue.getText().trim();

        if (assetId.isEmpty() || description.isEmpty() || date.isEmpty() || estValStr.isEmpty()) {
            setStatus("Please fill in all required fields.", StatusType.ERROR);
            return;
        }
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            setStatus("Purchase Date must be in YYYY-MM-DD format.", StatusType.ERROR);
            return;
        }

        double estVal;
        try {
            estVal = Double.parseDouble(estValStr);
            if (estVal < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            setStatus("Estimated Value must be a valid non-negative number.", StatusType.ERROR);
            return;
        }

        if (dataHandler == null) {
            setStatus("Backend integration pending.", StatusType.WARNING);
            return;
        }

        boolean ok = dataHandler.addAsset(assetId, description, date, estVal);
        if (ok) {
            setStatus("Asset added successfully. Refreshing...", StatusType.SUCCESS);
            loadAssets();
        } else {
            setStatus("Add failed. Asset ID may exist or data is invalid.", StatusType.ERROR);
        }
    }

    private void handleUpdate() {
        if (!editMode || assetTable.getSelectedRow() < 0) return;

        String assetId = fldAssetId.getText().trim();
        String description = fldDescription.getText().trim();
        String date = fldPurchaseDate.getText().trim();
        String estValStr = fldEstimatedValue.getText().trim();

        if (assetId.isEmpty() || description.isEmpty() || date.isEmpty() || estValStr.isEmpty()) {
            setStatus("Please fill in all required fields.", StatusType.ERROR);
            return;
        }
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            setStatus("Purchase Date must be in YYYY-MM-DD format.", StatusType.ERROR);
            return;
        }

        double estVal;
        try {
            estVal = Double.parseDouble(estValStr);
            if (estVal < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            setStatus("Estimated Value must be a valid non-negative number.", StatusType.ERROR);
            return;
        }

        if (dataHandler == null) {
            setStatus("Backend integration pending.", StatusType.WARNING);
            return;
        }

        boolean ok = dataHandler.updateAsset(assetId, description, date, estVal);
        if (ok) {
            setStatus("Asset updated successfully. Refreshing...", StatusType.SUCCESS);
            loadAssets();
        } else {
            setStatus("Update failed.", StatusType.ERROR);
        }
    }

    private void handleDelete() {
        if (assetTable.getSelectedRow() < 0) return;
        String assetId = fldAssetId.getText().trim();

        int confirm = JOptionPane.showConfirmDialog(this,
            "<html><b>Are you sure you want to delete this asset?</b><br><br>Asset ID: " + assetId + "</html>",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        if (dataHandler == null) {
            setStatus("Backend integration pending.", StatusType.WARNING);
            return;
        }

        if (dataHandler.deleteAsset(assetId)) {
            setStatus("Asset deleted successfully. Refreshing...", StatusType.SUCCESS);
            loadAssets();
        } else {
            setStatus("Delete failed.", StatusType.ERROR);
        }
    }

    private void clearForm() {
        fldAssetId       .setText("");
        fldDescription   .setText("");
        fldPurchaseDate  .setText("");
        fldEstimatedValue.setText("");

        fldAssetId.setEditable(true);
        fldAssetId.setBackground(Color.WHITE);

        assetTable.clearSelection();
        editMode = false;
        updateButtonStates();
    }

    private void updateButtonStates() {
        btnUpdate .setEnabled(editMode);
        btnDelete .setEnabled(editMode);
        btnSaveAdd.setEnabled(true);
    }

    private void updateRowCountLabel() {
        int count = tableModel.getRowCount();
        rowCountLabel.setText(count + (count == 1 ? " record" : " records"));
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
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        return btn;
    }

    private JLabel makeFormLabel(String text) {
        JLabel lbl = new JLabel(text + ":");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(CLR_TEXT_SEC);
        return lbl;
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(0, 30));
        field.setBorder(new CompoundBorder(new LineBorder(CLR_BORDER, 1, true), new EmptyBorder(4, 8, 4, 8)));
    }
    
}