package dao;
import db.DBConnection;
import model.QueryResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    public List<String[]> displayPayments() {

        List<String[]> results = new ArrayList<>();

        String sql =
            "SELECT P1.Claim_ID, P1.Payment_ID, P1.P_Date, " +
            "P1.P_Method, P2.Amt_Paid " +
            "FROM Payment1 P1 " +
            "JOIN Payment2 P2 ON P1.P_Method = P2.P_Method";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                results.add(new String[] {
                    rs.getString("Claim_ID"),
                    rs.getString("Payment_ID"),
                    rs.getDate("P_Date") != null ? rs.getDate("P_Date").toString() : "",
                    rs.getString("P_Method"),
                    String.valueOf(rs.getDouble("Amt_Paid"))
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public boolean makePayment(
            String claimId,
            String paymentId,
            String date,
            String method) {

        String sql = "{call make_payment(?,?,?,?)}";

        try (
            Connection con = DBConnection.getConnection();
            java.sql.CallableStatement cs = con.prepareCall(sql)
        ) {

            cs.setString(1, claimId);
            cs.setString(2, paymentId);
            cs.setDate(3, java.sql.Date.valueOf(date));
            cs.setString(4, method);

            cs.execute();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Amount Paid is stored in Payment2 and is shared by all payments using
    // the same payment method in the supplied normalized schema.
    public boolean updatePayment(
            String claimId,
            String paymentId,
            String date,
            String method,
            double amountPaid) {

        String findSql =
            "SELECT P_Method FROM Payment1 WHERE Claim_ID = ? AND Payment_ID = ?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(findSql)) {
                ps.setString(1, claimId);
                ps.setString(2, paymentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 FROM Payment2 WHERE P_Method = ?")) {
                ps.setString(1, method);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE Payment1 SET P_Date = ?, P_Method = ? " +
                    "WHERE Claim_ID = ? AND Payment_ID = ?")) {
                ps.setDate(1, java.sql.Date.valueOf(date));
                ps.setString(2, method);
                ps.setString(3, claimId);
                ps.setString(4, paymentId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE Payment2 SET Amt_Paid = ? WHERE P_Method = ?")) {
                ps.setDouble(1, amountPaid);
                ps.setString(2, method);
                ps.executeUpdate();
            }

            con.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePayment(String claimId, String paymentId) {

        String sql =
            "DELETE FROM Payment1 WHERE Claim_ID = ? AND Payment_ID = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, claimId);
            ps.setString(2, paymentId);

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getPaymentCount() {

        String sql = "SELECT COUNT(*) FROM Payment1";

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

    public double getTotalAmountPaid() {

        String sql = "SELECT NVL(SUM(Amt_Paid), 0) FROM Payment2";

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
