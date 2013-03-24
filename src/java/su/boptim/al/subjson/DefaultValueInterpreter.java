package su.boptim.al.subjson;

import java.util.Iterator;
import java.util.Map;

import java.util.ArrayList;
import java.util.HashMap;

public class DefaultValueInterpreter implements ValueInterpreter
{
    public ValueType categorize(Object o)
    {
        if (o == null) return ValueType.TYPE_NULL;
        else if (o instanceof Boolean) return ValueType.TYPE_BOOLEAN;
        else if (o instanceof String) return ValueType.TYPE_STRING;
        else if (o instanceof Long) return ValueType.TYPE_INTEGER;
        else if (o instanceof Double) return ValueType.TYPE_REAL;
        else if (o instanceof ArrayList<?>) return ValueType.TYPE_ARRAY;
        else if (o instanceof HashMap<?,?>) return ValueType.TYPE_OBJECT;
        else {
            throw new IllegalArgumentException("Could not categorize the given object " 
                                               + o.toString() + " into a JSON value type.");
        }
    }

    public Boolean asBoolean(Object o)
    {
        return (Boolean)o;
    }

    public String asString(Object o)
    {
        return (String)o;
    }

    public Long asInteger(Object o)
    {
        return (Long)o;
    }

    public Double asReal(Object o)
    {
        return (Double)o;
    }
    
    public Iterator<Object> arrayIterator(Object o)
    {
        ArrayList<Object> al = (ArrayList<Object>)o;

        return al.iterator();
    }

    public Iterator<Map.Entry<String, Object>> objectIterator(Object o)
    {
        HashMap<String, Object> hm = (HashMap<String, Object>)o;

        return hm.entrySet().iterator();
    }
}
