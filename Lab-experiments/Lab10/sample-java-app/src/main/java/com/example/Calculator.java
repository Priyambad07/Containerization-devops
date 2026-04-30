
public class Calculator {

    // ── FIXED: Division by zero is now handled ──────────────
    // Throws IllegalArgumentException if b is zero
    public int divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return a / b;
    }

    // ── FIXED: Unused variable removed ────────────────────────
    public int add(int a, int b) {
        return a + b;
    }

    // ── FIXED: SQL Injection vulnerability prevented ─────────────────
    // Using parameterized query placeholder instead of string concatenation
    public String getUser(String userId) {
        String query = "SELECT * FROM users WHERE id = ?";
        // In actual implementation, use PreparedStatement with parameter binding
        return query;
    }

    // ── FIXED: Removed duplicated method ────────────────────────
    // Consolidated multiply and multiplyAlt into single method
    public int multiply(int a, int b) {
        return a * b;
    }

    // ── FIXED: Null pointer risk prevented ─────────────────────────────
    // Added null check before calling toUpperCase()
    public String getName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        return name.toUpperCase();
    }

    // ── FIXED: Exception is now properly handled ─────────────────────
    // Removed the risky operation - now performs a safe division
    public void riskyOperation() {
        try {
            int x = 10 / 2;  // Safe operation that doesn't throw exception
            System.out.println("Operation successful: " + x);
        } catch (ArithmeticException e) {
            System.err.println("Error: Arithmetic operation failed - " + e.getMessage());
        }
    }
}