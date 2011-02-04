package beast.app.draw;

public interface ValidateListener {
	public enum State {
		IS_VALID,
		IS_INVALID,
		HAS_INVALIDMEMBERS
	}

	void validate(State state);
}
