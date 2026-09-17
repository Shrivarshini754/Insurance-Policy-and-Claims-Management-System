package model;
import java.util.List;
import java.util.ArrayList;

public class QueryResult {

    private List<String> columnNames;
    private List<String[]> rows;
    private boolean success;
    private String errorMessage;

    public QueryResult() {
        this.columnNames = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.success = false;
        this.errorMessage = null;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<String[]> getRows() {
        return rows;
    }

    public void setRows(List<String[]> rows) {
        this.rows = rows;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return columnNames.size();
    }
}
