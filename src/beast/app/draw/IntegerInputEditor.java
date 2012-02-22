package beast.app.draw;

import beast.app.beauti.BeautiDoc;

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
