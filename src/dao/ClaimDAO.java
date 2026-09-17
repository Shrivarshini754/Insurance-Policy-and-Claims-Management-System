package dao;
import db.DBConnection;
import model.QueryResult;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClaimDAO {

    public List<String[]> displayClaims() {

        List<String[]> results = new ArrayList<>();

        String sql =
            "SELECT A.Claim_ID, A.P_No, A.Incident_Date, " +
            "A.Descr, B.Claimed_Amt, C.Claim_Status " +
            "FROM Claim2A A " +
            "JOIN Claim2B B ON A.Descr = B.Descr " +
            "JOIN Claim1 C ON A.Incident_Date = C.Incident_Date";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                results.add(new String[] {
                    rs.getString("Claim_ID"),
                    rs.getString("P_No"),
                    rs.getDate("Incident_Date") != null ? rs.getDate("Incident_Date").toString() : "",
                    rs.getString("Descr"),
                    String.valueOf(rs.getDouble("Claimed_Amt")),
                    rs.getString("Claim_Status")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public boolean registerClaim(
            String claimId,
            String policyNo,
            String incidentDate,
            String description,
            String status) {

        String sql = "{call register_claim(?,?,?,?,?)}";

        try (
            Connection con = DBConnection.getConnection();
            CallableStatement cs = con.prepareCall(sql)
        ) {

            cs.setString(1, claimId);
            cs.setString(2, policyNo);
            cs.setDate(3, java.sql.Date.valueOf(incidentDate));
            cs.setString(4, description);
            cs.setString(5, status);

            cs.execute();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public double getClaimAmount(String claimId) {

        String sql = "{? = call get_claim_amount(?)}";

        try (
            Connection con = DBConnection.getConnection();
            CallableStatement cs = con.prepareCall(sql)
        ) {

            cs.registerOutParameter(1, java.sql.Types.NUMERIC);
            cs.setString(2, claimId);

            cs.execute();

            return cs.getDouble(1);

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    // Updates the values represented by the claim form.
    // Incident_Date and Descr are shared foreign keys, so changing their
    // associated status/amount can affect other rows that use the same key.
    public boolean updateClaim(
            String claimId,
            String policyNo,
            String incidentDate,
            String description,
            double claimedAmount,
            String status) {

        String findSql =
            "SELECT Incident_Date, Descr FROM Claim2A WHERE Claim_ID = ?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            String oldIncidentDate;
            String oldDescription;

            try (PreparedStatement ps = con.prepareStatement(findSql)) {
                ps.setString(1, claimId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    oldIncidentDate = rs.getDate("Incident_Date").toString();
                    oldDescription = rs.getString("Descr");
                }
            }

            if (!oldIncidentDate.equals(incidentDate) || !oldDescription.equals(description)) {
                con.rollback();
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE Claim2A SET P_No = ? WHERE Claim_ID = ?")) {
                ps.setString(1, policyNo);
                ps.setString(2, claimId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE Claim1 SET Claim_Status = ? WHERE Incident_Date = ?")) {
                ps.setString(1, status);
                ps.setDate(2, java.sql.Date.valueOf(incidentDate));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE Claim2B SET Claimed_Amt = ? WHERE Descr = ?")) {
                ps.setDouble(1, claimedAmount);
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

    public boolean deleteClaim(String claimId) {

        String sql = "DELETE FROM Claim2A WHERE Claim_ID = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, claimId);
            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getClaimCount() {

        String sql = "SELECT COUNT(*) FROM Claim2A";

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

    public int getClaimsThisYearCount() {

        String sql =
            "SELECT COUNT(*) FROM Claim2A " +
            "WHERE Incident_Date >= TRUNC(SYSDATE, 'YEAR')";

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

    public double getTotalClaimedAmount() {

        String sql = "SELECT NVL(SUM(Claimed_Amt), 0) FROM Claim2B";

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

    public List<String[]> getRecentClaims(int limit) {

        List<String[]> results = new ArrayList<>();

        if (limit <= 0) {
            return results;
        }

        String sql =
            "SELECT A.Claim_ID, A.P_No, A.Incident_Date, " +
            "A.Descr, B.Claimed_Amt, C.Claim_Status " +
            "FROM Claim2A A " +
            "JOIN Claim2B B ON A.Descr = B.Descr " +
            "JOIN Claim1 C ON A.Incident_Date = C.Incident_Date " +
            "ORDER BY A.Incident_Date DESC " +
            "FETCH FIRST ? ROWS ONLY";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new String[] {
                        rs.getString("Claim_ID"),
                        rs.getString("P_No"),
                        rs.getDate("Incident_Date") != null ? rs.getDate("Incident_Date").toString() : "",
                        rs.getString("Descr"),
                        String.valueOf(rs.getDouble("Claimed_Amt")),
                        rs.getString("Claim_Status")
                    });
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
