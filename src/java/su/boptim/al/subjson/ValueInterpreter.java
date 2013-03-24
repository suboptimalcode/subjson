package su.boptim.al.subjson;

import java.util.Iterator;
import java.util.Map;

public interface ValueInterpreter
{
    public enum ValueType {
        TYPE_NULL,
        TYPE_BOOLEAN,
        TYPE_STRING,
        TYPE_INTEGER,
        TYPE_REAL,
        TYPE_ARRAY,
        TYPE_OBJECT
    }

    public ValueType categorize(Object o);

    public Boolean asBoolean(Object o);
    public String asString(Object o);
    public Long asInteger(Object o);
    public Double asReal(Object o);
    
    public Iterator<Object> arrayIterator(Object o);
    public Iterator<Map.Entry<String, Object>> objectIterator(Object o);
}
