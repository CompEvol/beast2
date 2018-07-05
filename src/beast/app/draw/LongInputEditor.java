package beast.app.draw;

import beast.app.beauti.BeautiDoc;

public class LongInputEditor extends InputEditor.Base {
    private static final long serialVersionUID = 1L;

    public LongInputEditor(BeautiDoc doc) {
        super(doc);
    }
    //public IntegerInputEditor() {}

    @Override
    public Class<?> type() {
        return Long.class;
    }

} // class LongInputEditor
