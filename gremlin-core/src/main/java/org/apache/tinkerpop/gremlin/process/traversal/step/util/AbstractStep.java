/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.process.traversal.step.util;

import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.util.EmptyTraversal;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class AbstractStep<S, E> implements Step<S, E> {

    protected Set<String> labels = new LinkedHashSet<>();
    protected String id = Traverser.Admin.HALT;
    protected Traversal.Admin traversal;
    protected ExpandableStepIterator<S> starts;
    protected Traverser.Admin<E> nextEnd = null;
    protected boolean traverserStepIdAndLabelsSetByChild = false;

    protected Step<?, S> previousStep = EmptyStep.instance();
    protected Step<E, ?> nextStep = EmptyStep.instance();

    public AbstractStep(final Traversal.Admin traversal) {
        this.traversal = traversal;
        this.starts = new ExpandableStepIterator<>(this);
    }

    @Override
    public void setId(final String id) {
        Objects.nonNull(id);
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void addLabel(final String label) {
        this.labels.add(label);
    }

    @Override
    public void removeLabel(final String label) {
        this.labels.remove(label);
    }

    @Override
    public Set<String> getLabels() {
        return Collections.unmodifiableSet(this.labels);
    }

    @Override
    public void reset() {
        this.starts.clear();
        this.nextEnd = null;
    }

    @Override
    public void addStarts(final Iterator<Traverser<S>> starts) {
        this.starts.add((Iterator) starts);
    }

    @Override
    public void addStart(final Traverser<S> start) {
        this.starts.add((Traverser.Admin<S>) start);
    }

    @Override
    public void setPreviousStep(final Step<?, S> step) {
        this.previousStep = step;
    }

    @Override
    public Step<?, S> getPreviousStep() {
        return this.previousStep;
    }

    @Override
    public void setNextStep(final Step<E, ?> step) {
        this.nextStep = step;
    }

    @Override
    public Step<E, ?> getNextStep() {
        return this.nextStep;
    }

    @Override
    public Traverser<E> next() {
        if (null != this.nextEnd) {
            try {
                return this.prepareTraversalForNextStep(this.nextEnd);
            } finally {
                this.nextEnd = null;
            }
        } else {
            while (true) {
                final Traverser.Admin<E> traverser = (Traverser.Admin<E>) this.processNextStart();
                if (null != traverser.get() && 0 != traverser.bulk())
                    return this.prepareTraversalForNextStep(traverser);
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (null != this.nextEnd)
            return true;
        else {
            try {
                while (true) {
                    this.nextEnd = (Traverser.Admin<E>) this.processNextStart();
                    if (null != this.nextEnd.get() && 0 != this.nextEnd.bulk())
                        return true;
                    else
                        this.nextEnd = null;
                }
            } catch (final NoSuchElementException e) {
                return false;
            }
        }
    }

    @Override
    public <A, B> Traversal.Admin<A, B> getTraversal() {
        return this.traversal;
    }

    @Override
    public void setTraversal(final Traversal.Admin<?, ?> traversal) {
        this.traversal = traversal;
    }

    protected abstract Traverser<E> processNextStart() throws NoSuchElementException;

    @Override
    public String toString() {
        return StringFactory.stepString(this);
    }

    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public AbstractStep<S, E> clone() {
        try {
            final AbstractStep<S, E> clone = (AbstractStep<S, E>) super.clone();
            clone.starts = new ExpandableStepIterator<>(clone);
            clone.previousStep = EmptyStep.instance();
            clone.nextStep = EmptyStep.instance();
            clone.nextEnd = null;
            clone.traversal = EmptyTraversal.instance();
            clone.reset();
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public boolean equals(final Object other) {
        return other != null && other.getClass().equals(this.getClass()) && this.hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        int result = this.getClass().hashCode();
        for (final String label : this.getLabels()) {
            result ^= label.hashCode();
        }
        return result;
    }

    private final Traverser.Admin<E> prepareTraversalForNextStep(final Traverser.Admin<E> traverser) {
        if (!this.traverserStepIdAndLabelsSetByChild) {
            traverser.setStepId(this.nextStep.getId());
            traverser.addLabels(this.labels);
        }
        return traverser;
    }

}
