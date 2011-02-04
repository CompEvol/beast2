package beagle;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id$
 */
public enum BeagleErrorCode {
    NO_ERROR(0, "no error"),
    GENERAL_ERROR(-1, "general error"),
    OUT_OF_MEMORY_ERROR(-2, "out of memory error"),
    UNIDENTIFIED_EXCEPTION_ERROR(-3, "unidentified exception error"),
    UNINITIALIZED_INSTANCE_ERROR(-4, "uninitialized instance error"),
    OUT_OF_RANGE_ERROR(-5, "One of the indices specified exceeded the range of the array"),
    NO_RESOURCE_ERROR(-6, "No resource matches requirements"),
    NO_IMPLEMENTATION_ERROR(-7, "No implementation matches requirements"),
    FLOATING_POINT_ERROR(-8, "Floating-point range exceeded");

    BeagleErrorCode(int errCode, String meaning) {
        this.errCode = errCode;
        this.meaning = meaning;
    }

    public int getErrCode() {
        return errCode;
    }

    public String getMeaning() {
        return meaning;
    }

    private final int errCode;
    private final String meaning;
}

