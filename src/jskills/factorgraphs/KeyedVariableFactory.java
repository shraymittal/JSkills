package jskills.factorgraphs;

public class KeyedVariableFactory<TKey, TValue> extends VariableFactory<TValue> {

    public KeyedVariableFactory(Func<TValue> variablePriorInitializer) {
        super(variablePriorInitializer);
    }

}
