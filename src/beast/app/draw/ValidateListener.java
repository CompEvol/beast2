package beast.app.draw;

public interface ValidateListener {
    public enum ValidationStatus {
        IS_VALID,
        IS_INVALID,
        HAS_INVALIDMEMBERS
    }

    void validate(ValidationStatus state);
}
