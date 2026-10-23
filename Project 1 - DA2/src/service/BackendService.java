package service;

import dao.*; // The * imports all your DAOs (AssetDAO, ClaimDAO, etc.)
import db.DBConnection;
import model.QueryResult;
import java.util.List;

public class BackendService {

    private CustomerDAO customerDAO;
    private PolicyDAO policyDAO;
    private ClaimDAO claimDAO;
    private PaymentDAO paymentDAO;
    private AssetDAO assetDAO;
    private QueryDAO queryDAO;

    public BackendService() {
        customerDAO = new CustomerDAO();
        policyDAO = new PolicyDAO();
        claimDAO = new ClaimDAO();
        paymentDAO = new PaymentDAO();
        assetDAO = new AssetDAO();
        queryDAO = new QueryDAO();
    }

    // ==================== Customer ====================

    public List<String[]> displayCustomers() {
        return customerDAO.displayCustomers();
    }

    public String[] findCustomer(String personId) {
        return customerDAO.findCustomer(personId);
    }

    public boolean addCustomer(
            String personId,
            String firstName,
            String dob,
            String email,
            String aadhar,
            String address) {

        return customerDAO.addCustomer(
            personId,
            firstName,
            dob,
            email,
            aadhar,
            address
        );
    }

    public boolean updateCustomer(
            String personId,
            String firstName,
            String dob,
            String email,
            String aadhar,
            String address) {

        return customerDAO.updateCustomer(
            personId,
            firstName,
            dob,
            email,
            aadhar,
            address
        );
    }

    public boolean deleteCustomer(String personId) {
        return customerDAO.deleteCustomer(personId);
    }

    // ==================== Policy ====================

    public List<String[]> displayPolicies() {
        return policyDAO.displayPolicies();
    }

    public String[] findPolicy(String policyNo) {
        return policyDAO.findPolicy(policyNo);
    }

    public boolean addPolicy(
            String policyNo,
            String startDate,
            String expiryDate,
            String auditId,
            double premiumAmount) {

        return policyDAO.addPolicy(
            policyNo,
            startDate,
            expiryDate,
            auditId,
            premiumAmount
        );
    }

    public boolean updatePolicy(
            String policyNo,
            String startDate,
            String expiryDate,
            String auditId,
            double premiumAmount) {

        return policyDAO.updatePolicy(
            policyNo,
            startDate,
            expiryDate,
            auditId,
            premiumAmount
        );
    }

    public boolean deletePolicy(String policyNo) {
        return policyDAO.deletePolicy(policyNo);
    }

    // ==================== Claim ====================

    public List<String[]> displayClaims() {
        return claimDAO.displayClaims();
    }

    public boolean registerClaim(
            String claimId,
            String policyNo,
            String incidentDate,
            String description,
            String status) {

        return claimDAO.registerClaim(
            claimId,
            policyNo,
            incidentDate,
            description,
            status
        );
    }

    public double getClaimAmount(String claimId) {
        return claimDAO.getClaimAmount(claimId);
    }

    public boolean updateClaim(
            String claimId,
            String policyNo,
            String incidentDate,
            String description,
            double claimedAmount,
            String status) {

        return claimDAO.updateClaim(
            claimId,
            policyNo,
            incidentDate,
            description,
            claimedAmount,
            status
        );
    }

    public boolean deleteClaim(String claimId) {
        return claimDAO.deleteClaim(claimId);
    }

    // ==================== Payment ====================

    public List<String[]> displayPayments() {
        return paymentDAO.displayPayments();
    }

    public boolean makePayment(
            String claimId,
            String paymentId,
            String date,
            String method) {

        return paymentDAO.makePayment(
            claimId,
            paymentId,
            date,
            method
        );
    }

    public boolean updatePayment(
            String claimId,
            String paymentId,
            String date,
            String method,
            double amountPaid) {

        return paymentDAO.updatePayment(
            claimId,
            paymentId,
            date,
            method,
            amountPaid
        );
    }

    public boolean deletePayment(String claimId, String paymentId) {
        return paymentDAO.deletePayment(claimId, paymentId);
    }

    // ==================== Asset ====================

    public List<String[]> displayAssets() {
        return assetDAO.displayAssets();
    }

    public boolean addAsset(
            String assetId,
            String description,
            String purchaseDate,
            double estimatedValue) {

        return assetDAO.addAsset(
            assetId,
            description,
            purchaseDate,
            estimatedValue
        );
    }

    public boolean updateAsset(
            String assetId,
            String description,
            String purchaseDate,
            double estimatedValue) {

        return assetDAO.updateAsset(
            assetId,
            description,
            purchaseDate,
            estimatedValue
        );
    }

    public boolean deleteAsset(String assetId) {
        return assetDAO.deleteAsset(assetId);
    }

    // ==================== Dashboard Statistics ====================

    public int getCustomerCount() {
        return customerDAO.getCustomerCount();
    }

    public int getPolicyCount() {
        return policyDAO.getPolicyCount();
    }

    public int getActivePolicyCount() {
        return policyDAO.getActivePolicyCount();
    }

    public int getClaimCount() {
        return claimDAO.getClaimCount();
    }

    public int getClaimsThisYearCount() {
        return claimDAO.getClaimsThisYearCount();
    }

    public int getPaymentCount() {
        return paymentDAO.getPaymentCount();
    }

    public int getAssetCount() {
        return assetDAO.getAssetCount();
    }

    public double getTotalPremium() {
        return policyDAO.getTotalPremium();
    }

    public double getTotalClaimedAmount() {
        return claimDAO.getTotalClaimedAmount();
    }

    public double getTotalAmountPaid() {
        return paymentDAO.getTotalAmountPaid();
    }

    // getTotalPaymentsReceived delegates to getTotalPremium because
    // the dashboard card labelled "Payments" displays "Premiums received",
    // which in insurance terminology represents premium revenue collected.
    // See the ambiguity note in the Phase 2 report.
    public double getTotalPaymentsReceived() {
        return policyDAO.getTotalPremium();
    }

    // ==================== Recent Claims ====================

    public List<String[]> getRecentClaims(int limit) {
        return claimDAO.getRecentClaims(limit);
    }

    // ==================== Generic SQL Query ====================

    public QueryResult executeQuery(String sql) {
        return queryDAO.executeQuery(sql);
    }
}
