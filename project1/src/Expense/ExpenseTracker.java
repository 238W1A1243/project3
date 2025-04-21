package Expense;
import java.io.*;
import java.text.*;
import java.util.*;
public class ExpenseTracker {
    private static final String USERS_FILE = "users.dat";
    private static final String EXPENSE_FILE_PREFIX = "expenses_";
    private Map<String, String> userCredentials;
    private List<Expense> expenses;
    private Scanner scanner;
    private SimpleDateFormat dateFormat;
    private String currentUser;

    private static class Expense implements Serializable {
        private Date date;
        private String category;
        private double amount;
        private String description;

        public Expense(Date date, String category, double amount, String description) {
            this.date = date;
            this.category = category;
            this.amount = amount;
            this.description = description;
        }

        public String getFormattedDate() {
            return new SimpleDateFormat("yyyy-MM-dd").format(date);
        }

        @Override
        public String toString() {
            return String.format("%s | %-15s | $%-8.2f | %s",
                    getFormattedDate(), category, amount, description);
        }
    }

    public ExpenseTracker() {
        scanner = new Scanner(System.in);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        userCredentials = new HashMap<>();
        expenses = new ArrayList<>();
        loadUserCredentials();
    }

    public static void main(String[] args) {
        ExpenseTracker tracker = new ExpenseTracker();
        tracker.authenticate();
        tracker.run();
    }

    private void authenticate() {
        System.out.println("=== Expense Tracker ===");
        while (true) {
            System.out.println("\n1. Login\n2. Register\n3. Exit");
            int choice = getIntInput("Choose an option: ");

            switch (choice) {
                case 1:
                    if (loginUser()) return;
                    break;
                case 2:
                    registerUser();
                    break;
                case 3:
                    System.out.println("Goodbye!");
                    System.exit(0);
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private boolean loginUser() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        if (userCredentials.containsKey(username) && userCredentials.get(username).equals(password)) {
            currentUser = username;
            loadUserExpenses();
            return true;
        } else {
            System.out.println("Login failed. Try again.");
            return false;
        }
    }

    private void registerUser() {
        System.out.print("Choose a username: ");
        String username = scanner.nextLine().trim();
        if (userCredentials.containsKey(username)) {
            System.out.println("Username already exists.");
            return;
        }
        System.out.print("Choose a password: ");
        String password = scanner.nextLine().trim();

        userCredentials.put(username, password);
        saveUserCredentials();
        System.out.println("Registration successful!");
    }

    private void run() {
        System.out.println("\nWelcome, " + currentUser + "!");

        while (true) {
            printMenu();
            int choice = getIntInput("Enter choice: ");

            switch (choice) {
                case 1:
                    addExpense();
                    break;
                case 2:
                    viewAllExpenses();
                    break;
                case 3:
                    showCategoryTotals();
                    break;
                case 4:
                    saveUserExpenses();
                    System.out.println("Data saved. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\nMain Menu:");
        System.out.println("1. Add Expense");
        System.out.println("2. View All Expenses");
        System.out.println("3. Category Totals");
        System.out.println("4. Exit");
    }

    private void addExpense() {
        System.out.println("\nAdd New Expense:");
        Date date = getValidDate();
        String category = getStringInput("Category: ");
        double amount = getPositiveDouble("Amount: $");
        String description = getStringInput("Description (optional): ");

        expenses.add(new Expense(date, category, amount, description));
        System.out.println("Expense added!");
    }

    private void viewAllExpenses() {
        if (expenses.isEmpty()) {
            System.out.println("\nNo expenses found.");
            return;
        }

        expenses.sort((e1, e2) -> e2.date.compareTo(e1.date));
        System.out.println("\nAll Expenses:");
        System.out.println("--------------------------------------------");
        System.out.println("Date       | Category      | Amount   | Description");
        System.out.println("--------------------------------------------");
        expenses.forEach(System.out::println);

        double total = expenses.stream().mapToDouble(e -> e.amount).sum();
        System.out.printf("\nTotal Expenses: $%.2f\n", total);
    }

    private void showCategoryTotals() {
        if (expenses.isEmpty()) {
            System.out.println("\nNo expenses to summarize.");
            return;
        }

        Map<String, Double> categoryMap = new HashMap<>();
        for (Expense e : expenses) {
            categoryMap.merge(e.category, e.amount, Double::sum);
        }

        System.out.println("\nCategory Totals:");
        System.out.println("----------------------");
        categoryMap.forEach((cat, total) ->
                System.out.printf("%-15s: $%.2f\n", cat, total));
    }
    private void saveUserExpenses() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(EXPENSE_FILE_PREFIX + currentUser + ".dat"))) {
            oos.writeObject(expenses);
        } catch (IOException e) {
            System.out.println("Error saving expenses: " + e.getMessage());
        }
    }

    private void loadUserExpenses() {
        File file = new File(EXPENSE_FILE_PREFIX + currentUser + ".dat");
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(file))) {
            expenses = (List<Expense>) ois.readObject();
        } catch (Exception e) {
            System.out.println("Error loading expenses: " + e.getMessage());
        }
    }

    private void saveUserCredentials() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(userCredentials);
        } catch (IOException e) {
            System.out.println("Error saving users: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadUserCredentials() {
        File file = new File(USERS_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            userCredentials = (Map<String, String>) ois.readObject();
        } catch (Exception e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
    }
    private Date getValidDate() {
        while (true) {
            try {
                System.out.print("Date (YYYY-MM-DD): ");
                return dateFormat.parse(scanner.nextLine());
            } catch (ParseException e) {
                System.out.println("Invalid date format. Try again.");
            }
        }
    }

    private double getPositiveDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                double value = Double.parseDouble(scanner.nextLine());
                if (value > 0) return value;
                System.out.println("Amount must be positive.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number.");
            }
        }
    }

    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        }
    }
}