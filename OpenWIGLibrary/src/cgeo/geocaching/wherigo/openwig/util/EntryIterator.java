package cgeo.geocaching.wherigo.openwig.util;

import java.util.Iterator;
import java.util.Map;
import cgeo.geocaching.wherigo.openwig.EventTable;

public class EntryIterator<K,V> implements Iterator<Map.Entry<K,V>> {

    private final LuaTable<K,V> table;
    private Object currentKey = null;

    public EntryIterator(final LuaTable<K,V> table) {
        this.table = table;
    }

    @Override
    public boolean hasNext() {
        return (currentKey = table.next(currentKey)) != null;
    }

    @Override
    public Map.Entry<K,V> next() {
        if (currentKey == null) {
            currentKey = table.next(null);
        }
        if (currentKey == null) {
            throw new java.util.NoSuchElementException();
        }
        final Object value = table.rawget(currentKey);
        final Map.Entry<K,V> entry = new java.util.AbstractMap.SimpleEntry<>(currentKey, value);
        currentKey = table.next(currentKey);
        return entry;
    }
}
