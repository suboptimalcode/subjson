package su.boptim.al.subjson;

import su.boptim.al.subjson.BuildPolicy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class DefaultBuildPolicy implements BuildPolicy
{
    // Arrays
    public boolean isArray(Object o)
    {
         return o instanceof ArrayList<?>;
    }

    public Object startArray()
    {
        return new ArrayList();
    }

    @SuppressWarnings("unchecked")
    public void arrayAppend(Object a, Object value)
    {
        ArrayList<Object> arr = (ArrayList<Object>)a;
        arr.add(value);
    }

    public Object finishArray(Object array)
    {
        return array;
    }

    // Objects
    public boolean isObject(Object o)
    {
        return o instanceof HashMap<?,?>;
    }

    public Object startObject()
    {
        return new HashMap<String,Object>();
    }

    @SuppressWarnings("unchecked")
    public void objectInsert(Object o, Object key, Object value)
    {
        HashMap<String,Object> obj = (HashMap<String,Object>)o;
        obj.put((String)key, value);
    }

    public Object finishObject(Object obj)
    {
        return obj;
    }

    // Primitives
    public Object makeNull()
    {
        return null;
    }

    public Object makeBoolean(Boolean b)
    {
        return b;
    }

    public Object makeString(String s)
    {
        return s;
    }

    public Object makeNumber(Number n)
    {
        return n;
    }
}
