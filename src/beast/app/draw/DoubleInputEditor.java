package beast.app.draw;

public class DoubleInputEditor extends InputEditor {
    private static final long serialVersionUID = 1L;

    public DoubleInputEditor() {
        super();
    }

    @Override
    public Class<?> type() {
        return Double.class;
    }
} // class DoubleInputEditor
