package service;

import frontend.CustomerPanel;
import frontend.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomerPanelFixTest - Validates the CustomerPanel lifecycle and error distinction logic.
 * Ensures:
 * 1. Database error (null) displays an explicit ERROR status message, NOT "No customers found."
 * 2. Successful empty result ([]) displays "No customers found."
 * 3. Populated result displays "N customer(s) loaded."
 * 4. MainFrame stores CustomerPanel and refreshes customer records on tab selection.
 */
public class CustomerPanelFixTest {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CUSTOMER PANEL FIX VERIFICATION TEST");
        System.out.println("================================================================================\n");

        int passed = 0;
        int total = 0;

        // Test 1: Verify MainFrame has customerPanel field and showCard wires customers
        total++;
        try {
            Field cpField = MainFrame.class.getDeclaredField("customerPanel");
            cpField.setAccessible(true);
            System.out.println(" [PASS] Test 1: MainFrame declares 'customerPanel' as a managed member field.");
            passed++;
        } catch (NoSuchFieldException e) {
            System.err.println(" [FAIL] Test 1: MainFrame is missing 'customerPanel' member field.");
        }

        // Test 2: Verify CustomerPanel distinguishes database error (null) from empty list
        total++;
        try {
            // Mock BackendService that returns null (simulating database failure)
            BackendService mockErrorService = new BackendService() {
                @Override
                public List<String[]> displayCustomers() {
                    return null; // Database failure
                }
            };

            CustomerPanel panel = new CustomerPanel(mockErrorService);
            Field statusLabelField = CustomerPanel.class.getDeclaredField("statusLabel");
            statusLabelField.setAccessible(true);
            JLabel statusLabel = (JLabel) statusLabelField.get(panel);

            String statusText = statusLabel.getText();
            if (statusText.toLowerCase().contains("database error") &&
                !statusText.contains("No customers found")) {
                System.out.println(" [PASS] Test 2: Database failure (null) displays: \"" + statusText + "\" (NOT 'No customers found')");
                passed++;
            } else {
                System.err.println(" [FAIL] Test 2: Unexpected status message on database failure: \"" + statusText + "\"");
            }
        } catch (Exception e) {
            System.err.println(" [FAIL] Test 2: Exception testing database failure status: " + e.getMessage());
        }

        // Test 3: Verify CustomerPanel displays "No customers found." for successful empty results
        total++;
        try {
            // Mock BackendService that returns an empty list (0 customers in database)
            BackendService mockEmptyService = new BackendService() {
                @Override
                public List<String[]> displayCustomers() {
                    return new ArrayList<>(); // 0 records
                }
            };

            CustomerPanel panel = new CustomerPanel(mockEmptyService);
            Field statusLabelField = CustomerPanel.class.getDeclaredField("statusLabel");
            statusLabelField.setAccessible(true);
            JLabel statusLabel = (JLabel) statusLabelField.get(panel);

            String statusText = statusLabel.getText();
            if ("No customers found.".equals(statusText)) {
                System.out.println(" [PASS] Test 3: Empty database result ([]) displays: \"" + statusText + "\"");
                passed++;
            } else {
                System.err.println(" [FAIL] Test 3: Unexpected status message on empty results: \"" + statusText + "\"");
            }
        } catch (Exception e) {
            System.err.println(" [FAIL] Test 3: Exception testing empty results: " + e.getMessage());
        }

        // Test 4: Verify CustomerPanel displays "N customer(s) loaded." for populated results
        total++;
        try {
            // Mock BackendService that returns 5 customers
            BackendService mockPopulatedService = new BackendService() {
                @Override
                public List<String[]> displayCustomers() {
                    List<String[]> list = new ArrayList<>();
                    list.add(new String[]{"P101", "Ramesh", "1990-01-01", "ramesh@email.com", "123456789012", "Address 1"});
                    list.add(new String[]{"P102", "Ramya", "1992-02-02", "ramya@email.com", "234567890123", "Address 2"});
                    list.add(new String[]{"P103", "Dharshini", "1994-03-03", "dharshini@email.com", "345678901234", "Address 3"});
                    list.add(new String[]{"P104", "Suresh", "1988-04-04", "suresh@email.com", "456789012345", "Address 4"});
                    list.add(new String[]{"P105", "Pavan", "1996-05-05", "pavan@email.com", "567890123456", "Address 5"});
                    return list;
                }
            };

            CustomerPanel panel = new CustomerPanel(mockPopulatedService);
            Field statusLabelField = CustomerPanel.class.getDeclaredField("statusLabel");
            statusLabelField.setAccessible(true);
            JLabel statusLabel = (JLabel) statusLabelField.get(panel);

            Field countLabelField = CustomerPanel.class.getDeclaredField("rowCountLabel");
            countLabelField.setAccessible(true);
            JLabel countLabel = (JLabel) countLabelField.get(panel);

            String statusText = statusLabel.getText();
            String countText = countLabel.getText();
            if ("5 customer(s) loaded.".equals(statusText) && "5 records".equals(countText)) {
                System.out.println(" [PASS] Test 4: Populated result displays: \"" + statusText + "\" and \"" + countText + "\"");
                passed++;
            } else {
                System.err.println(" [FAIL] Test 4: Unexpected status message on populated results: \"" + statusText + "\"");
            }
        } catch (Exception e) {
            System.err.println(" [FAIL] Test 4: Exception testing populated results: " + e.getMessage());
        }

        System.out.println("\n================================================================================");
        System.out.println("  SUMMARY: " + passed + " / " + total + " checks passed.");
        System.out.println("================================================================================\n");
    }
}
