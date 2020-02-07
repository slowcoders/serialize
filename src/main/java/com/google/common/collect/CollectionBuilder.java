package com.google.common.collect;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public class CollectionBuilder<E> extends ImmutableCollection.Builder<E> {

    E[] contents;
    private int size;
    private boolean forceCopy;

    public CollectionBuilder() {
        this(4);
    }

    public CollectionBuilder(int capacity) {
        this.contents = (E[])new Object[capacity];
        this.size = 0;
    }

    private void getReadyToExpandTo(int minCapacity) {
        if (this.contents.length < minCapacity) {
            this.contents = Arrays.copyOf(this.contents, expandedCapacity(this.contents.length, minCapacity));
            this.forceCopy = false;
        } else if (this.forceCopy) {
            this.contents = Arrays.copyOf(this.contents, this.contents.length);
            this.forceCopy = false;
        }

    }

    public CollectionBuilder<E> add(E element) {
//        Preconditions.checkNotNull(element);
        this.getReadyToExpandTo(this.size + 1);
        this.contents[this.size++] = element;
        return this;
    }

    public CollectionBuilder<E> add(E... elements) {
        ObjectArrays.checkElementsNotNull(elements);
        this.add(elements, elements.length);
        return this;
    }

    private void add(Object[] elements, int n) {
        this.getReadyToExpandTo(this.size + n);
        System.arraycopy(elements, 0, this.contents, this.size, n);
        this.size += n;
    }

    public CollectionBuilder<E> addAll(Iterable<? extends E> elements) {
        Preconditions.checkNotNull(elements);
        if (elements instanceof Collection) {
            Collection<?> collection = (Collection)elements;
            this.getReadyToExpandTo(this.size + collection.size());
            if (collection instanceof ImmutableCollection) {
                ImmutableCollection<?> immutableCollection = (ImmutableCollection)collection;
                this.size = immutableCollection.copyIntoArray(this.contents, this.size);
                return this;
            }
        }

        super.addAll(elements);
        return this;
    }

    public CollectionBuilder<E> addAll(Iterator<? extends E> elements) {
        super.addAll(elements);
        return this;
    }

    CollectionBuilder<E> combine(CollectionBuilder<E> builder) {
        Preconditions.checkNotNull(builder);
        this.add(builder.contents, builder.size);
        return this;
    }

    public ImmutableList<E> build() {
        this.forceCopy = true;
        return ImmutableList.asImmutableList(this.contents, this.size);
    }

    public ImmutableSet<E> buildSet() {
        E[] items = this.contents;
        if (this.size != items.length) {
            items = Arrays.copyOf(contents, this.size);
        }
        return ImmutableSet.copyOf(items);
    }

    public ImmutableSortedSet<E> buildSortedSet(Comparator<? super E> comparator) {
        this.sortAndDedup(comparator);
        if (this.size == 0) {
            return ImmutableSortedSet.emptySet(comparator);
        } else {
            this.forceCopy = true;
            return new RegularImmutableSortedSet(build(), comparator);
        }
    }

    private void sortAndDedup(Comparator<? super E> comparator) {
        if (this.size != 0) {
            Arrays.sort(this.contents, 0, this.size, comparator);
            int unique = 1;

            for(int i = 1; i < this.size; ++i) {
                int cmp = comparator.compare(this.contents[unique - 1], this.contents[i]);
                if (cmp < 0) {
                    this.contents[unique++] = this.contents[i];
                } else if (cmp > 0) {
                    throw new AssertionError("Comparator " + comparator + " compare method violates its contract");
                }
            }

            Arrays.fill(this.contents, unique, this.size, (Object)null);
            this.size = unique;
        }
    }

    public final int size() {
        return this.size;
    }

    public final E get(int idx) {
        if (idx < 0 || idx >= this.size) {
            throw new ArrayIndexOutOfBoundsException(idx);
        }
        return contents[idx];
    }

    public final Object[] toArray(String arrayType) throws Exception {
        Object[] array = null;
        if (arrayType != null) {
            array = (Object[]) Class.forName(arrayType).newInstance();
        } else {
            array = new Object[this.size];
        }
        System.arraycopy(this.contents, 0, array, 0, this.size);
        return array;
    }

}
