package dao;
import db.DBConnection;
import model.QueryResult;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public List<String[]> displayCustomers() {

        List<String[]> results = new ArrayList<>();

        String sql = "SELECT Person_ID, F_Name, DOB, Email, Aadhar, Address FROM Customer1";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                results.add(new String[] {
                    rs.getString("Person_ID"),
                    rs.getString("F_Name"),
                    rs.getDate("DOB") != null ? rs.getDate("DOB").toString() : "",
                    rs.getString("Email"),
                    rs.getString("Aadhar"),
                    rs.getString("Address")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public boolean addCustomer(
            String personId,
            String firstName,
            String dob,
            String email,
            String aadhar,
            String address) {

        String sql = "{call add_customer(?,?,?,?,?,?)}";

        try (
            Connection con = DBConnection.getConnection();
            CallableStatement cs = con.prepareCall(sql)
        ) {

            cs.setString(1, personId);
            cs.setString(2, firstName);
            cs.setDate(3, java.sql.Date.valueOf(dob));
            cs.setString(4, email);
            cs.setString(5, aadhar);
            cs.setString(6, address);

            cs.execute();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String[] findCustomer(String personId) {

        String sql =
            "SELECT Person_ID, F_Name, DOB, Email, Aadhar, Address " +
            "FROM Customer1 WHERE Person_ID = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, personId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return new String[] {
                        rs.getString("Person_ID"),
                        rs.getString("F_Name"),
                        rs.getDate("DOB") != null ? rs.getDate("DOB").toString() : "",
                        rs.getString("Email"),
                        rs.getString("Aadhar"),
                        rs.getString("Address")
                    };
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateCustomer(
            String personId,
            String firstName,
            String dob,
            String email,
            String aadhar,
            String address) {

        String sql =
            "UPDATE Customer1 " +
            "SET F_Name = ?, DOB = ?, Email = ?, Aadhar = ?, Address = ? " +
            "WHERE Person_ID = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, firstName);
            ps.setDate(2, java.sql.Date.valueOf(dob));
            ps.setString(3, email);
            ps.setString(4, aadhar);
            ps.setString(5, address);
            ps.setString(6, personId);

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteCustomer(String personId) {

        String sql = "DELETE FROM Customer1 WHERE Person_ID = ?";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, personId);

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getCustomerCount() {

        String sql = "SELECT COUNT(*) FROM Customer1";

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
