package beagle;

/**
 * @author Andrew Rambaut
 * @author Marc Suchard
 */
public class BeagleException extends RuntimeException {
    public BeagleException(String functionName, int errCode) {
        this.functionName = functionName;
        this.errCode = errCode;        
    }

    @Override
    public String getMessage() {
        BeagleErrorCode code = null;
        for (BeagleErrorCode c : BeagleErrorCode.values()) {
            if (c.getErrCode() == errCode) {
                code = c;
            }
        }
        if (code == null) {
            return "BEAGLE function, " + functionName + ", returned error code " + errCode + " (unrecognized error code)";
        }
        return "BEAGLE function, " + functionName + ", returned error code " + errCode + " (" + code.getMeaning() + ")";
    }

    public String getFunctionName() {
        return functionName;
    }

    public int getErrCode() {
        return errCode;
    }

    private final String functionName;
    private final int errCode;
}
