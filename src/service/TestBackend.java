package service;
import model.QueryResult;
import java.util.List;
import java.util.Scanner;

public class TestBackend {

    public static void main(String[] args) {

        BackendService service = new BackendService();
        Scanner sc = new Scanner(System.in);

        while (true) {

            System.out.println("\n===== INSURANCE POLICY CLAIMS SYSTEM =====");
            System.out.println("1.  Display Customers");
            System.out.println("2.  Find Customer");
            System.out.println("3.  Add Customer");
            System.out.println("4.  Update Customer");
            System.out.println("5.  Delete Customer");
            System.out.println("6.  Display Policies");
            System.out.println("7.  Find Policy");
            System.out.println("8.  Add Policy");
            System.out.println("9.  Update Policy");
            System.out.println("10. Delete Policy");
            System.out.println("11. Display Claims");
            System.out.println("12. Get Claim Amount");
            System.out.println("13. Register Claim");
            System.out.println("14. Update Claim");
            System.out.println("15. Delete Claim");
            System.out.println("16. Display Payments");
            System.out.println("17. Make Payment");
            System.out.println("18. Update Payment");
            System.out.println("19. Delete Payment");
            System.out.println("20. Display Assets");
            System.out.println("21. Add Asset");
            System.out.println("22. Update Asset");
            System.out.println("23. Delete Asset");
            System.out.println("24. Dashboard Statistics");
            System.out.println("25. Recent Claims");
            System.out.println("26. SQL Query");
            System.out.println("27. Exit");
            System.out.print("Enter choice: ");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    List<String[]> customers = service.displayCustomers();
                    for (String[] row : customers) {
                        System.out.println(
                            row[0] + " | " + row[1] + " | " + row[2] +
                            " | " + row[3] + " | " + row[4] + " | " + row[5]
                        );
                    }
                    if (customers.isEmpty()) {
                        System.out.println("No customers found.");
                    }
                    break;

                case 2:
                    System.out.print("Enter Customer ID: ");
                    String[] found = service.findCustomer(sc.nextLine());
                    if (found != null) {
                        System.out.println(
                            found[0] + " | " + found[1] + " | " + found[2] +
                            " | " + found[3] + " | " + found[4] + " | " + found[5]
                        );
                    } else {
                        System.out.println("Customer not found.");
                    }
                    break;

                case 3:
                    System.out.print("Enter Customer ID: ");
                    String newCustomerId = sc.nextLine();
                    System.out.print("Enter First Name: ");
                    String firstName = sc.nextLine();
                    System.out.print("Enter DOB (YYYY-MM-DD): ");
                    String dob = sc.nextLine();
                    System.out.print("Enter Email: ");
                    String email = sc.nextLine();
                    System.out.print("Enter Aadhar: ");
                    String aadhar = sc.nextLine();
                    System.out.print("Enter Address: ");
                    String address = sc.nextLine();

                    boolean addResult = service.addCustomer(
                        newCustomerId, firstName, dob, email, aadhar, address
                    );
                    System.out.println(addResult ? "Customer added successfully." : "Failed to add customer.");
                    break;

                case 4:
                    System.out.print("Enter Customer ID to update: ");
                    String updateCustomerId = sc.nextLine();
                    System.out.print("Enter New First Name: ");
                    String updateFirstName = sc.nextLine();
                    System.out.print("Enter New DOB (YYYY-MM-DD): ");
                    String updateDob = sc.nextLine();
                    System.out.print("Enter New Email: ");
                    String updateEmail = sc.nextLine();
                    System.out.print("Enter New Aadhar: ");
                    String updateAadhar = sc.nextLine();
                    System.out.print("Enter New Address: ");
                    String updateAddress = sc.nextLine();

                    boolean updateResult = service.updateCustomer(
                        updateCustomerId,
                        updateFirstName,
                        updateDob,
                        updateEmail,
                        updateAadhar,
                        updateAddress
                    );
                    System.out.println(updateResult ? "Customer updated successfully." : "Failed to update customer.");
                    break;

                case 5:
                    System.out.print("Enter Customer ID to delete: ");
                    boolean delCustResult = service.deleteCustomer(sc.nextLine());
                    System.out.println(delCustResult ? "Customer deleted successfully." : "Failed to delete customer.");
                    break;

                case 6:
                    List<String[]> policies = service.displayPolicies();
                    for (String[] row : policies) {
                        System.out.println(
                            row[0] + " | " + row[1] + " | " + row[2] +
                            " | " + row[3] + " | " + row[4]
                        );
                    }
                    if (policies.isEmpty()) {
                        System.out.println("No policies found.");
                    }
                    break;

                case 7:
                    System.out.print("Enter Policy Number: ");
                    String[] foundPolicy = service.findPolicy(sc.nextLine());
                    if (foundPolicy != null) {
                        System.out.println(
                            foundPolicy[0] + " | " + foundPolicy[1] + " | " +
                            foundPolicy[2] + " | " + foundPolicy[3] + " | " +
                            foundPolicy[4]
                        );
                    } else {
                        System.out.println("Policy not found.");
                    }
                    break;

                case 8:
                    System.out.print("Enter Policy Number: ");
                    String newPolicyNo = sc.nextLine();
                    System.out.print("Enter Start Date (YYYY-MM-DD): ");
                    String newPolicyStart = sc.nextLine();
                    System.out.print("Enter Expiry Date (YYYY-MM-DD): ");
                    String newPolicyExpiry = sc.nextLine();
                    System.out.print("Enter Audit ID: ");
                    String newPolicyAudit = sc.nextLine();
                    System.out.print("Enter Premium Amount: ");
                    double newPolicyPremium = sc.nextDouble();
                    sc.nextLine();

                    boolean addPolicyResult = service.addPolicy(
                        newPolicyNo,
                        newPolicyStart,
                        newPolicyExpiry,
                        newPolicyAudit,
                        newPolicyPremium
                    );
                    System.out.println(addPolicyResult ? "Policy added successfully." : "Failed to add policy.");
                    break;

                case 9:
                    System.out.print("Enter Policy Number to update: ");
                    String updatePolicyNo = sc.nextLine();
                    System.out.print("Enter Existing Start Date (YYYY-MM-DD): ");
                    String policyStartDate = sc.nextLine();
                    System.out.print("Enter New Expiry Date (YYYY-MM-DD): ");
                    String policyExpiryDate = sc.nextLine();
                    System.out.print("Enter Audit ID: ");
                    String policyAuditId = sc.nextLine();
                    System.out.print("Enter Premium Amount: ");
                    double policyPremium = sc.nextDouble();
                    sc.nextLine();

                    boolean updatePolicyResult = service.updatePolicy(
                        updatePolicyNo,
                        policyStartDate,
                        policyExpiryDate,
                        policyAuditId,
                        policyPremium
                    );
                    System.out.println(updatePolicyResult ? "Policy updated successfully." : "Failed to update policy.");
                    break;

                case 10:
                    System.out.print("Enter Policy Number to delete: ");
                    boolean delPolResult = service.deletePolicy(sc.nextLine());
                    System.out.println(delPolResult ? "Policy deleted successfully." : "Failed to delete policy.");
                    break;

                case 11:
                    List<String[]> claims = service.displayClaims();
                    for (String[] row : claims) {
                        System.out.println(
                            row[0] + " | " + row[1] + " | " + row[2] +
                            " | " + row[3] + " | " + row[4] + " | " + row[5]
                        );
                    }
                    if (claims.isEmpty()) {
                        System.out.println("No claims found.");
                    }
                    break;

                case 12:
                    System.out.print("Enter Claim ID: ");
                    double claimAmt = service.getClaimAmount(sc.nextLine());
                    System.out.println("Claim amount: " + claimAmt);
                    break;

                case 13:
                    System.out.print("Enter Claim ID: ");
                    String newClaimId = sc.nextLine();
                    System.out.print("Enter Policy Number: ");
                    String claimPolicyNo = sc.nextLine();
                    System.out.print("Enter Incident Date (YYYY-MM-DD): ");
                    String incidentDate = sc.nextLine();
                    System.out.print("Enter Description: ");
                    String description = sc.nextLine();
                    System.out.print("Enter Status: ");
                    String status = sc.nextLine();

                    boolean regResult = service.registerClaim(
                        newClaimId,
                        claimPolicyNo,
                        incidentDate,
                        description,
                        status
                    );
                    System.out.println(regResult ? "Claim registered successfully." : "Failed to register claim.");
                    break;

                case 14:
                    System.out.print("Enter Claim ID to update: ");
                    String updateClaimId = sc.nextLine();
                    System.out.print("Enter Policy Number: ");
                    String updateClaimPolicyNo = sc.nextLine();
                    System.out.print("Enter Existing Incident Date (YYYY-MM-DD): ");
                    String updateIncidentDate = sc.nextLine();
                    System.out.print("Enter Existing Description: ");
                    String updateDescription = sc.nextLine();
                    System.out.print("Enter New Claimed Amount: ");
                    double updateClaimAmount = sc.nextDouble();
                    sc.nextLine();
                    System.out.print("Enter New Status: ");
                    String updateClaimStatus = sc.nextLine();

                    boolean updateClaimResult = service.updateClaim(
                        updateClaimId,
                        updateClaimPolicyNo,
                        updateIncidentDate,
                        updateDescription,
                        updateClaimAmount,
                        updateClaimStatus
                    );
                    System.out.println(updateClaimResult ? "Claim updated successfully." : "Failed to update claim.");
                    break;

                case 15:
                    System.out.print("Enter Claim ID to delete: ");
                    boolean delClaimResult = service.deleteClaim(sc.nextLine());
                    System.out.println(delClaimResult ? "Claim deleted successfully." : "Failed to delete claim.");
                    break;

                case 16:
                    List<String[]> payments = service.displayPayments();
                    for (String[] row : payments) {
                        System.out.println(
                            row[0] + " | " + row[1] + " | " + row[2] +
                            " | " + row[3] + " | " + row[4]
                        );
                    }
                    if (payments.isEmpty()) {
                        System.out.println("No payments found.");
                    }
                    break;

                case 17:
                    System.out.print("Enter Claim ID: ");
                    String paymentClaimId = sc.nextLine();
                    System.out.print("Enter Payment ID: ");
                    String paymentId = sc.nextLine();
                    System.out.print("Enter Payment Date (YYYY-MM-DD): ");
                    String paymentDate = sc.nextLine();
                    System.out.print("Enter Payment Method: ");
                    String method = sc.nextLine();

                    boolean payResult = service.makePayment(
                        paymentClaimId,
                        paymentId,
                        paymentDate,
                        method
                    );
                    System.out.println(payResult ? "Payment recorded successfully." : "Failed to record payment.");
                    break;

                case 18:
                    System.out.print("Enter Claim ID: ");
                    String updatePaymentClaimId = sc.nextLine();
                    System.out.print("Enter Payment ID: ");
                    String updatePaymentId = sc.nextLine();
                    System.out.print("Enter New Payment Date (YYYY-MM-DD): ");
                    String updatePaymentDate = sc.nextLine();
                    System.out.print("Enter Payment Method: ");
                    String updatePaymentMethod = sc.nextLine();
                    System.out.print("Enter New Amount Paid: ");
                    double updatePaymentAmount = sc.nextDouble();
                    sc.nextLine();

                    boolean updatePayResult = service.updatePayment(
                        updatePaymentClaimId,
                        updatePaymentId,
                        updatePaymentDate,
                        updatePaymentMethod,
                        updatePaymentAmount
                    );
                    System.out.println(updatePayResult ? "Payment updated successfully." : "Failed to update payment.");
                    break;

                case 19:
                    System.out.print("Enter Claim ID: ");
                    String deletePaymentClaimId = sc.nextLine();
                    System.out.print("Enter Payment ID: ");
                    String deletePaymentId = sc.nextLine();
                    boolean delPayResult = service.deletePayment(deletePaymentClaimId, deletePaymentId);
                    System.out.println(delPayResult ? "Payment deleted successfully." : "Failed to delete payment.");
                    break;

                case 20:
                    List<String[]> assets = service.displayAssets();
                    for (String[] row : assets) {
                        System.out.println(
                            row[0] + " | " + row[1] + " | " + row[2] +
                            " | " + row[3]
                        );
                    }
                    if (assets.isEmpty()) {
                        System.out.println("No assets found.");
                    }
                    break;

                case 21:
                    System.out.print("Enter Asset ID: ");
                    String newAssetId = sc.nextLine();
                    System.out.print("Enter Description: ");
                    String newAssetDescription = sc.nextLine();
                    System.out.print("Enter Purchase Date (YYYY-MM-DD): ");
                    String newAssetPurchaseDate = sc.nextLine();
                    System.out.print("Enter Estimated Value: ");
                    double newAssetEstimatedValue = sc.nextDouble();
                    sc.nextLine();

                    boolean addAssetResult = service.addAsset(
                        newAssetId,
                        newAssetDescription,
                        newAssetPurchaseDate,
                        newAssetEstimatedValue
                    );
                    System.out.println(addAssetResult ? "Asset added successfully." : "Failed to add asset.");
                    break;

                case 22:
                    System.out.print("Enter Asset ID to update: ");
                    String updateAssetId = sc.nextLine();
                    System.out.print("Enter Existing Description: ");
                    String updateAssetDescription = sc.nextLine();
                    System.out.print("Enter New Purchase Date (YYYY-MM-DD): ");
                    String updatePurchaseDate = sc.nextLine();
                    System.out.print("Enter New Estimated Value: ");
                    double updateEstimatedValue = sc.nextDouble();
                    sc.nextLine();

                    boolean updateAssetResult = service.updateAsset(
                        updateAssetId,
                        updateAssetDescription,
                        updatePurchaseDate,
                        updateEstimatedValue
                    );
                    System.out.println(updateAssetResult ? "Asset updated successfully." : "Failed to update asset.");
                    break;

                case 23:
                    System.out.print("Enter Asset ID to delete: ");
                    boolean delAssetResult = service.deleteAsset(sc.nextLine());
                    System.out.println(delAssetResult ? "Asset deleted successfully." : "Failed to delete asset.");
                    break;

                case 24:
                    System.out.println("\n--- Dashboard Statistics ---");
                    System.out.println("Customers:           " + service.getCustomerCount());
                    System.out.println("Policies (total):    " + service.getPolicyCount());
                    System.out.println("Policies (active):   " + service.getActivePolicyCount());
                    System.out.println("Claims (total):      " + service.getClaimCount());
                    System.out.println("Claims (this year):  " + service.getClaimsThisYearCount());
                    System.out.println("Payments:            " + service.getPaymentCount());
                    System.out.println("Assets:              " + service.getAssetCount());
                    System.out.println("Total Premium:       " + service.getTotalPremium());
                    System.out.println("Total Claimed:       " + service.getTotalClaimedAmount());
                    System.out.println("Total Paid:          " + service.getTotalAmountPaid());
                    System.out.println("Premiums Received:   " + service.getTotalPaymentsReceived());
                    break;

                case 25:
                    System.out.print("Enter number of recent claims to display: ");
                    int limit = sc.nextInt();
                    sc.nextLine();
                    List<String[]> recentClaims = service.getRecentClaims(limit);
                    for (String[] row : recentClaims) {
                        System.out.println(
                            row[0] + " | " + row[1] + " | " + row[2] +
                            " | " + row[3] + " | " + row[4] + " | " + row[5]
                        );
                    }
                    if (recentClaims.isEmpty()) {
                        System.out.println("No recent claims found.");
                    }
                    break;

                case 26:
                    System.out.print("Enter SQL query: ");
                    String sqlQuery = sc.nextLine();
                    QueryResult qr = service.executeQuery(sqlQuery);
                    if (qr.isSuccess()) {
                        // Print column headers
                        List<String> cols = qr.getColumnNames();
                        StringBuilder header = new StringBuilder();
                        for (int i = 0; i < cols.size(); i++) {
                            if (i > 0) header.append(" | ");
                            header.append(cols.get(i));
                        }
                        System.out.println(header.toString());

                        // Print rows
                        for (String[] row : qr.getRows()) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < row.length; i++) {
                                if (i > 0) sb.append(" | ");
                                sb.append(row[i]);
                            }
                            System.out.println(sb.toString());
                        }

                        System.out.println("(" + qr.getRowCount() + " rows returned)");
                    } else {
                        System.out.println("Query failed: " + qr.getErrorMessage());
                    }
                    break;

                case 27:
                    System.out.println("Exiting...");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}
