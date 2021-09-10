package beast.app.beauti;

import java.lang.reflect.InvocationTargetException;

import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.InputEditor;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.tree.coalescent.ConstantPopulation;

public class ConstantPopulationInputEditor extends InputEditor.Base {
	private static final long serialVersionUID = 1L;

	public ConstantPopulationInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
	@Override
	public Class<?> type() {
		return ConstantPopulation.class;
	}
	
	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
			boolean addButtons) {
		ConstantPopulation population = (ConstantPopulation) input.get();
		try {
			InputEditor editor = doc.inputEditorFactory.createInputEditor(population.popSizeParameter, population, doc);
			add(editor.getComponent());
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		//super.init(input, beastObject, itemNr, isExpandOption, addButtons);
	}

}
