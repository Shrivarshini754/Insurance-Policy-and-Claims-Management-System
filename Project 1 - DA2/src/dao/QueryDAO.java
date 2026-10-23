package dao;
import db.DBConnection;
import model.QueryResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class QueryDAO {

    public QueryResult executeQuery(String sql) {

        QueryResult result = new QueryResult();

        if (sql == null || sql.trim().isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("SQL query cannot be null or empty.");
            return result;
        }

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Read column names dynamically
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(meta.getColumnLabel(i));
            }
            result.setColumnNames(columnNames);

            // Read all rows
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String[] row = new String[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row[i - 1] = (value != null) ? value.toString() : "";
                }
                rows.add(row);
            }
            result.setRows(rows);

            result.setSuccess(true);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
}
