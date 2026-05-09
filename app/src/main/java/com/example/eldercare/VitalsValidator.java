package com.example.eldercare;

public class VitalsValidator {

    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }

    public static ValidationResult validateSystolic(String input) {
        try {
            int value = Integer.parseInt(input);
            if (value >= 60 && value <= 250) {
                return new ValidationResult(true, null);
            } else {
                return new ValidationResult(false, "Range: 60 - 250 mmHg");
            }
        } catch (NumberFormatException e) {
            return new ValidationResult(false, "Invalid number");
        }
    }

    public static ValidationResult validateDiastolic(String input) {
        try {
            int value = Integer.parseInt(input);
            if (value >= 40 && value <= 150) {
                return new ValidationResult(true, null);
            } else {
                return new ValidationResult(false, "Range: 40 - 150 mmHg");
            }
        } catch (NumberFormatException e) {
            return new ValidationResult(false, "Invalid number");
        }
    }

    public static ValidationResult validateGlucose(String input) {
        try {
            double value = Double.parseDouble(input);
            if (value >= 1.0 && value <= 50.0) {
                return new ValidationResult(true, null);
            } else {
                return new ValidationResult(false, "Range: 1.0 - 50.0 mmol/L");
            }
        } catch (NumberFormatException e) {
            return new ValidationResult(false, "Invalid number");
        }
    }
}
