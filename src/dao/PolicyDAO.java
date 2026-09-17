package dao;
import db.DBConnection;
import model.QueryResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PolicyDAO {

    public List<String[]> displayPolicies() {

        List<String[]> results = new ArrayList<>();

        String sql =
            "SELECT P1.P_No, P1.Start_Date, P2.Expiry_Date, " +
            "P1.Audit_ID, P3.Premium_Amt " +
            "FROM Policy1 P1 " +
            "JOIN Policy2 P2 ON P1.Start_Date = P2.Start_Date " +
            "JOIN Policy3 P3 ON P1.Audit_ID = P3.Audit_ID";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                results.add(new String[] {
                    rs.getString("P_No"),
                    rs.getDate("Start_Date") != null ? rs.getDate("Start_Date").toString() : "",
                    rs.getDate("Expiry_Date") != null ? rs.getDate("Expiry_Date").toString() : "",
                    rs.getString("Audit_ID"),
                    String.valueOf(rs.getDouble("Premium_Amt"))
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public String[] findPolicy(String policyNo) {

        String sql =
            "SELECT P1.P_No, P1.Start_Date, P2.Expiry_Date, " +
            "P1.Audit_ID, P3.Premium_Amt " +
            "FROM Policy1 P1 " +
            "JOIN Policy2 P2 ON P1.Start_Date = P2.Start_Date " +
            "JOIN Policy3 P3 ON P1.Audit_ID = P3.Audit_ID " +
            "WHERE P1.P_No = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, policyNo);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return new String[] {
                        rs.getString("P_No"),
                        rs.getDate("Start_Date") != null ? rs.getDate("Start_Date").toString() : "",
                        rs.getDate("Expiry_Date") != null ? rs.getDate("Expiry_Date").toString() : "",
                        rs.getString("Audit_ID"),
                        String.valueOf(rs.getDouble("Premium_Amt"))
                    };
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Updates the non-primary-key values represented by the policy form.
    // Start_Date is kept unchanged because it is a primary/foreign key in Policy2.
    public boolean updatePolicy(
            String policyNo,
            String startDate,
            String expiryDate,
            String auditId,
            double premiumAmount) {

        String findSql =
            "SELECT Start_Date, Audit_ID FROM Policy1 WHERE P_No = ?";

        String updatePolicySql =
            "UPDATE Policy1 SET Audit_ID = ? WHERE P_No = ?";

        String updateExpirySql =
            "UPDATE Policy2 SET Expiry_Date = ? WHERE Start_Date = ?";

        String updatePremiumSql =
            "UPDATE Policy3 SET Premium_Amt = ? WHERE Audit_ID = ?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            String currentStartDate;

            try (PreparedStatement ps = con.prepareStatement(findSql)) {
                ps.setString(1, policyNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    currentStartDate = rs.getDate("Start_Date").toString();
                }
            }

            if (!currentStartDate.equals(startDate)) {
                con.rollback();
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 FROM Policy3 WHERE Audit_ID = ?")) {
                ps.setString(1, auditId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(updatePolicySql)) {
                ps.setString(1, auditId);
                ps.setString(2, policyNo);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(updateExpirySql)) {
                ps.setDate(1, java.sql.Date.valueOf(expiryDate));
                ps.setDate(2, java.sql.Date.valueOf(startDate));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(updatePremiumSql)) {
                ps.setDouble(1, premiumAmount);
                ps.setString(2, auditId);
                ps.executeUpdate();
            }

            con.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePolicy(String policyNo) {

        String sql = "DELETE FROM Policy1 WHERE P_No = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, policyNo);
            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addPolicy(
            String policyNo,
            String startDate,
            String expiryDate,
            String auditId,
            double premiumAmount) {

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            // Check if Policy3 record exists for this Audit_ID
            boolean policy3Exists = false;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 FROM Policy3 WHERE Audit_ID = ?")) {
                ps.setString(1, auditId);
                try (ResultSet rs = ps.executeQuery()) {
                    policy3Exists = rs.next();
                }
            }

            if (!policy3Exists) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Policy3 (Audit_ID, Premium_Amt) VALUES (?, ?)")) {
                    ps.setString(1, auditId);
                    ps.setDouble(2, premiumAmount);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Policy3 SET Premium_Amt = ? WHERE Audit_ID = ?")) {
                    ps.setDouble(1, premiumAmount);
                    ps.setString(2, auditId);
                    ps.executeUpdate();
                }
            }

            // Check if Policy2 record exists for this Start_Date
            boolean policy2Exists = false;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 FROM Policy2 WHERE Start_Date = ?")) {
                ps.setDate(1, java.sql.Date.valueOf(startDate));
                try (ResultSet rs = ps.executeQuery()) {
                    policy2Exists = rs.next();
                }
            }

            if (!policy2Exists) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO Policy2 (Start_Date, Expiry_Date) VALUES (?, ?)")) {
                    ps.setDate(1, java.sql.Date.valueOf(startDate));
                    ps.setDate(2, java.sql.Date.valueOf(expiryDate));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Policy2 SET Expiry_Date = ? WHERE Start_Date = ?")) {
                    ps.setDate(1, java.sql.Date.valueOf(expiryDate));
                    ps.setDate(2, java.sql.Date.valueOf(startDate));
                    ps.executeUpdate();
                }
            }

            // Insert the policy record into Policy1
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Policy1 (P_No, Start_Date, Audit_ID) VALUES (?, ?, ?)")) {
                ps.setString(1, policyNo);
                ps.setDate(2, java.sql.Date.valueOf(startDate));
                ps.setString(3, auditId);
                ps.executeUpdate();
            }

            con.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getPolicyCount() {

        String sql = "SELECT COUNT(*) FROM Policy1";

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

    public int getActivePolicyCount() {

        String sql =
            "SELECT COUNT(*) FROM Policy1 P1 " +
            "JOIN Policy2 P2 ON P1.Start_Date = P2.Start_Date " +
            "WHERE P2.Expiry_Date >= CURRENT_DATE";

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

    public double getTotalPremium() {

        String sql = "SELECT NVL(SUM(Premium_Amt), 0) FROM Policy3";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getDouble(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0.0;
    }
}
