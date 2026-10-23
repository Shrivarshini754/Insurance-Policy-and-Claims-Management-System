package dao;
import db.DBConnection;
import model.QueryResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AssetDAO {

    public List<String[]> displayAssets() {

        List<String[]> results = new ArrayList<>();

        String sql =
            "SELECT A1.Asset_ID, A1.Descr, A1.P_Date, A2.Est_Value " +
            "FROM Asset1 A1 " +
            "JOIN Asset2 A2 ON A1.Descr = A2.Descr";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                results.add(new String[] {
                    rs.getString("Asset_ID"),
                    rs.getString("Descr"),
                    rs.getDate("P_Date") != null ? rs.getDate("P_Date").toString() : "",
                    String.valueOf(rs.getDouble("Est_Value"))
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    // Description and Estimated Value are stored through Asset2 in the
    // normalized schema. Changing them can affect other assets sharing the
    // same Description, so the method preserves the existing Description key.
    public boolean updateAsset(
            String assetId,
            String description,
            String purchaseDate,
            double estimatedValue) {

        String findSql =
            "SELECT Descr FROM Asset1 WHERE Asset_ID = ?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            String oldDescription;

            try (PreparedStatement ps = con.prepareStatement(findSql)) {
                ps.setString(1, assetId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    oldDescription = rs.getString("Descr");
                }
            }

            if (!oldDescription.equals(description)) {
                con.rollback();
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE Asset1 SET P_Date = ? WHERE Asset_ID = ?")) {
                ps.setDate(1, java.sql.Date.valueOf(purchaseDate));
                ps.setString(2, assetId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE Asset2 SET Est_Value = ? WHERE Descr = ?")) {
                ps.setDouble(1, estimatedValue);
                ps.setString(2, description);
                ps.executeUpdate();
            }

            con.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteAsset(String assetId) {

        String sql = "DELETE FROM Asset1 WHERE Asset_ID = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, assetId);

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addAsset(
            String assetId,
            String description,
            String purchaseDate,
            double estimatedValue) {

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            // Check if Asset2 record exists for this Description
            boolean asset2Exists = false;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 FROM Asset2 WHERE Descr = ?")) {
                ps.setString(1, description);
                try (ResultSet rs = ps.executeQuery()) {
                    asset2Exists = rs.next();
                }
            }

            if (!asset2Exists) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Asset2 (Descr, Est_Value) VALUES (?, ?)")) {
                    ps.setString(1, description);
                    ps.setDouble(2, estimatedValue);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Asset2 SET Est_Value = ? WHERE Descr = ?")) {
                    ps.setDouble(1, estimatedValue);
                    ps.setString(2, description);
                    ps.executeUpdate();
                }
            }

            // Insert the asset record into Asset1
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Asset1 (Asset_ID, Descr, P_Date) VALUES (?, ?, ?)")) {
                ps.setString(1, assetId);
                ps.setString(2, description);
                ps.setDate(3, java.sql.Date.valueOf(purchaseDate));
                ps.executeUpdate();
            }

            con.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getAssetCount() {

        String sql = "SELECT COUNT(*) FROM Asset1";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
