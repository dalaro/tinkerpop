package com.tinkerpop.gremlin.process.graph.step.sideEffect.mapreduce;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.computer.MapReduce;
import com.tinkerpop.gremlin.process.computer.traversal.TraversalVertexProgram;
import com.tinkerpop.gremlin.process.computer.util.GraphComputerHelper;
import com.tinkerpop.gremlin.process.computer.util.LambdaHolder;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.AggregateStep;
import com.tinkerpop.gremlin.process.util.BulkSet;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.commons.configuration.Configuration;
import org.javatuples.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class AggregateMapReduce implements MapReduce<MapReduce.NullObject, Object, MapReduce.NullObject, Object, Collection> {

    public static final String AGGREGATE_STEP_SIDE_EFFECT_KEY = "gremlin.aggregateStep.sideEffectKey";

    private String sideEffectKey;
    private Supplier<Collection> collectionSupplier;

    private AggregateMapReduce() {

    }

    public AggregateMapReduce(final AggregateStep step) {
        this.sideEffectKey = step.getSideEffectKey();
        this.collectionSupplier = step.getTraversal().sideEffects().<Collection>getWith(this.sideEffectKey).orElse(BulkSet::new);
    }

    @Override
    public void storeState(final Configuration configuration) {
        configuration.setProperty(AGGREGATE_STEP_SIDE_EFFECT_KEY, this.sideEffectKey);
    }

    @Override
    public void loadState(final Configuration configuration) {
        this.sideEffectKey = configuration.getString(AGGREGATE_STEP_SIDE_EFFECT_KEY);
        this.collectionSupplier = LambdaHolder.<Supplier<Traversal>>loadState(configuration, TraversalVertexProgram.TRAVERSAL_SUPPLIER).get().get().sideEffects().<Collection>getWith(this.sideEffectKey).orElse(BulkSet::new);
    }

    @Override
    public boolean doStage(final Stage stage) {
        return stage.equals(Stage.MAP);
    }

    @Override
    public void map(final Vertex vertex, final MapEmitter<NullObject, Object> emitter) {
        TraversalVertexProgram.getLocalSideEffects(vertex).<Collection<?>>orElse(this.sideEffectKey, Collections.emptyList()).forEach(object -> emitter.emit(NullObject.instance(), object));
    }

    @Override
    public Collection generateFinalResult(final Iterator<Pair<NullObject, Object>> keyValues) {
        final Collection collection = this.collectionSupplier.get();
        keyValues.forEachRemaining(pair -> collection.add(pair.getValue1()));
        return collection;
    }

    @Override
    public String getMemoryKey() {
        return this.sideEffectKey;
    }

    @Override
    public int hashCode() {
        return (this.getClass().getCanonicalName() + this.sideEffectKey).hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        return GraphComputerHelper.areEqual(this, object);
    }

    @Override
    public String toString() {
        return StringFactory.mapReduceString(this, this.sideEffectKey);
    }
}
