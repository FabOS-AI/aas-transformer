package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionTsMdn;
import org.eclipse.digitaltwin.aas4j.v3.model.DataTypeDefXsd;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TransformerActionTsMdnService extends TransformerActionTsService {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionTsMdnService.class);

    public TransformerActionTsMdnService(
            TransformerActionTsMdn transformerActionTsMdn
    ) {
        super(transformerActionTsMdn);
    }

    protected List<Object> runFilter(List<Object> values) {
        int filterSize = this.transformerAction.getFilterSize();
        int halfFilterSize = (int)filterSize/2;

        List<Object> filteredValues = new ArrayList<>();
        for(int index = 0; index < values.size(); index++) {
            List<Double> sortedValues = IntStream.range(index - halfFilterSize, index + halfFilterSize + 1)
                    // Avoid trying to get values out of bounds:
                    .filter(i -> i >= 0 && i < values.size())
                    .mapToObj(i -> Double.valueOf(values.get(i).toString()))
                    .sorted()
                    .collect(Collectors.toList());
            filteredValues.add(getMedianOfList(sortedValues));
        }

        return filteredValues;
    }

    private Object getMedianOfList(List<Double> sortedValues) {
        int size = sortedValues.size();
        if(size == 0) {
            LOG.error("Can not calculate median of empty list");
            return 0;
        }
        if(size % 2 == 0) {
            return (sortedValues.get(size/2) + sortedValues.get(size/2 - 1)) / 2;
        } else {
            return sortedValues.get(size/2);
        }
    }
}
