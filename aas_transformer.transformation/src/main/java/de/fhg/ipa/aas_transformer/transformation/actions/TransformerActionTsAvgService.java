package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsAvg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

public class TransformerActionTsAvgService extends TransformerActionTsService {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionTsAvgService.class);

    public TransformerActionTsAvgService(
            TransformerActionTsAvg transformerAction
    ) {
        super(transformerAction);
    }

    protected List<Object> runFilter(List<Object> values) {
        int filterSize = this.transformerAction.getFilterSize();
        int halfFilterSize = (int)filterSize/2;

        List<Object> filteredValues = new ArrayList<>();
        for(int index = 0; index < values.size(); index++) {
            OptionalDouble avg = IntStream.range(index - halfFilterSize, index + halfFilterSize + 1)
                    // Avoid trying to get values out of bounds:
                    .filter(i -> i >= 0 && i < values.size())
                    .mapToObj(i -> Double.valueOf(values.get(i).toString()))
                    .mapToDouble(Double::doubleValue)
                    .average();
            filteredValues.add(avg.getAsDouble());
        }

        return filteredValues;
    }

    @Override
    public TransformerAction getTransformerAction() {
        return null;
    }
}
