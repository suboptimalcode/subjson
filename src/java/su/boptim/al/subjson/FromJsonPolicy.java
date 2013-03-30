package su.boptim.al.subjson;

public interface FromJsonPolicy
{
    // Arrays
    public boolean isArray(Object o);
    public Object startArray();
    public void arrayAppend(Object array, Object value);
    public Object finishArray(Object array);

    // Objects
    public boolean isObject(Object o);
    public Object startObject();
    public void objectInsert(Object obj, Object key, Object value);
    public Object finishObject(Object obj);

    // Primitives
    public Object makeNull();
    public Object makeBoolean(Boolean b);
    public Object makeString(String s);
    public Object makeNumber(Number n);
}
