package org.slowcoders.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class RefList<T> implements Iterable<T> {

    private Entry<T> _top;
    private Entry<T> _bottom;

    public interface Visitor<T> {
        boolean visit(T obj);
    }

    private static RefCleaner refCleaner = new RefCleaner();

    public RefList() {}


    public final boolean addFirst(T item){
        return addFirst(item, 0, true);
    }

    public final boolean addFirst_asWeakRef(T item){
        return addFirst(item, 0, true);
    }

    public synchronized boolean addFirst(T item, int priority, boolean isWeakRef){
        if (this.findEntry(item, false)) {
            return false;
        }

        Entry<T> rookie = createChain_internal(item, priority, isWeakRef);
        Entry next = this._top;
        if (next == null) {
            _top = _bottom = rookie;
            return true;
        }

        if (next.priority <= priority) {
            rookie.setNextSibling(next);
            this._top = rookie;
            return true;
        }

        for (; next.priority > priority; ) {
            next = next.getNext();
            if (next == null) {
                rookie.setPrevSibling(this._bottom);
                this._bottom = rookie;
                return true;
            }
        }

        rookie.setNextSibling(next);
        rookie.setPrevSibling(next.prev);
        return true;
    }

    public final boolean addLast(T item){
        return addLast(item, 0, false);
    }

    public final boolean addLast_asWeakRef(T item){
        return addLast(item, 0, true);
    }

    public synchronized boolean addLast(T item, int priority, boolean isWeakRef){
        if (this.findEntry(item, false)) {
            return false;
        }
        Entry<T> rookie = createChain_internal(item, priority, isWeakRef);
        Entry prev = this._bottom;
        if (prev == null) {
            _top = _bottom = rookie;
            return true;
        }

        if (prev.priority >= priority) {
            rookie.setPrevSibling(prev);
            this._bottom = rookie;
            return true;
        }

        for (; prev.priority < priority; ) {
            prev = prev.getPrev();
            if (prev == null) {
                rookie.setNextSibling(this._top);
                this._top = rookie;
                return true;
            }
        }

        rookie.setNextSibling(prev.next);
        rookie.setPrevSibling(prev);
        return true;
    }

    public boolean isEmpty() {
        return _bottom == null;
    }


    private T getFirst(boolean shouldUnlink){
        Entry<T> retiree = this._top;
        if (retiree == null) {
            return null;
        }

        for (; retiree != null; retiree = retiree.getNext()) {
            T item = retiree.get();
            if (item != null) {
                if (shouldUnlink){
                    this._top = retiree.getNext();
                }
                return item;
            }
        }
        this._top = this._bottom = null;
        return null;
    }

    private T getLast(boolean shouldUnlink){
        Entry<T> retiree = this._bottom;
        if (retiree == null){
            return null;
        }

        for (; retiree != null; retiree = retiree.getPrev()){
            T item = retiree.get();
            if (item != null){
                if (shouldUnlink){
                    this._bottom = retiree.getPrev();
                }
                return item;
            }
        }
        this._top = this._bottom = null;
        return null;
    }

    /**
     * Retrieves and removes the first element of this list,
     * or returns {@code null} if this list is empty.
     */
    public synchronized T pollFirst(){
        return getFirst(true);
    }

    /**
     * Retrieves and removes the last element of this list,
     * or returns {@code null} if this list is empty.
     */
    public synchronized T pollLast(){
        return getLast(true);
    }

    /**
     * Retrieves the first element of this list,
     * or returns {@code null} if this list is empty.
     */
    public synchronized T peekFirst(){
        return getFirst(false);
    }

    /**
     * Retrieves the first element of this list,
     * or returns {@code null} if this list is empty.
     */
    public synchronized T peekLast(){
        return getLast(false);
    }


    public synchronized final Entry<T> clear() {
        Entry<T> top = this._top;
        _top = _bottom = null;
        return top;
    }


    public void forEach(Visitor<T> visitor) {
        Entry<T> prev = null, next;
        for (Entry<T> entry = getTop_internal(); entry != null; entry = next) {
            T item = entry.get();
            if (item == null || !visitor.visit(item)) {
                next = this.removeEntryAndGetNext(prev, entry);
            } else {
                prev = entry;
                next = entry.getNext();
            }
        }
    }

    public synchronized boolean remove(T retiree) {
        return findEntry(retiree, true);
    }

    public boolean contains(T target) {
        return findEntry(target, false);
    }

    protected final Entry<T> getTop_internal() {
        return _top;
    }

    protected void notifyNotEmpty() {
    }

    private boolean findEntry(T retiree, boolean doRemove) {
        Entry<T> prev = null, next;
        for (Entry<T> entry = getTop_internal(); entry != null; entry = next) {
            T item = entry.get();
            if (item == null) {
                next = this.removeEntryAndGetNext(prev, entry);
            } else if (item == retiree) {
                if (doRemove) {
                    this.removeEntryAndGetNext(prev, entry);
                }
                return true;
            } else {
                prev = entry;
                next = entry.getNext ();
            }
        }
        return false;
    }


    protected final synchronized Entry<T> removeEntryAndGetNext(Entry<T> prev, Entry<T> entry) {
        Entry<T> next = entry.getNext();
        if (prev != null) {
            prev.setNextSibling(next);
            if (next == null){
                _bottom = prev;
            }
        }
        else {
            // prev, next 가 둘 다 null 인 경우 (entry 가 하나일 때)
            // bottom 도 null 처리
            if (next == null){
                this._bottom = null;
            }
            this._top = next;
        }
        return next;
    }

    protected Entry<T> createChain_internal(T value, int priority, boolean isWeakRef) {
        Entry<T> entry = new Entry<T>(this, priority, value);

        if (!isWeakRef){
            entry.strongRef = value;
        }
        return entry;
    }

    @Override
    public Iterator<T> iterator() {
        return new EntryIterator();
    }

    protected class EntryIterator implements Iterator<T> {
        private Entry<T> curEntry;
        private T value;

        public EntryIterator() {
            setNext(getTop_internal());
        }

        private void setNext(Entry<T> entry) {
            Entry<T> prev = this.curEntry;

            this.value = null;
            while (entry != null) {
                if ((this.value = entry.get()) != null) {
                    break;
                }
                entry = removeEntryAndGetNext(prev, entry);
            }
            this.curEntry = entry;
        }

        @Override
        public boolean hasNext() {
            return value != null;
        }

        @Override
        public T next() {
            T e = this.value;
            if (e == null)
                throw new NoSuchElementException();
            setNext(curEntry.getNext());
            return e;
        }

        public void remove() {
            Debug.notImplemented();
//            if (this.curEntry == null)
//                throw new IllegalStateException();
//			this.curEntry.clear();
        }

    }


    private static class RefCleaner extends ReferenceQueue<Object> implements Runnable {

        @Override
        public void run() {
            try {
                Entry<?> ref = (Entry<?>) this.remove();
                ref.remove();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Entry<T> extends WeakReference<T>  {
        private final int priority;
        T strongRef;
        Entry<T> next;
        private boolean removed;

        RefList<T> owner;
        Entry<T> prev;

        Entry(RefList<T> owner, int priority, T subEntity) {
            super(subEntity, refCleaner);
            this.priority = priority;
            this.owner = owner;
        }

        public final Entry<T> getNext() {
            return this.next;
        }

        final void setNextSibling(Entry<T> next) {
            if (next != null) {
                next.prev = this;
            }
            this.next = next;
        }

        final boolean markRemoved() {
            if (this.removed) {
                return false;
            }
            this.removed = true;
            return true;
        }

        final Entry<T> getPrev() {
            return this.prev;
        }

        final void setPrevSibling(Entry<T> prev) {
            if (prev != null) {
                prev.next = this;
            }
            this.prev = prev;
        }

        final void remove() {
            this.owner.removeEntryAndGetNext(this.prev, this);
        }

        final boolean isRemoved() {
            return this.removed;

        }
    }

}
