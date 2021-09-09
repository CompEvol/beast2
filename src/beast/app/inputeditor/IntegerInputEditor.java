package beast.app.inputeditor;

public class IntegerInputEditor extends InputEditor.Base {
    private static final long serialVersionUID = 1L;

    public IntegerInputEditor(BeautiDoc doc) {
        super(doc);
    }
    //public IntegerInputEditor() {}

    @Override
    public Class<?> type() {
        return Integer.class;
    }

} // class IntegerInputEditor
