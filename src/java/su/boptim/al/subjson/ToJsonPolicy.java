package su.boptim.al.subjson;

import java.util.Iterator;
import java.util.Map;

/**
   The interface through which the parser will examine Java objects it
   is given, and convert them to json values it will write out.
   <p>
   When a Java object is given to {@link SubJson} to write out as json,
   it will use the functions in the ToJsonPolicy to translate the Java
   object it was given (and any sub-objects) into the json values that
   should represent them.
 */
public interface ToJsonPolicy
{
    /**
       This enum specifies the types of the json values that the
       parser knows about. In properly written code, any Java
       value that can be correctly mapped to json values will
       have a mapping (a {@link ToJsonPolicy}) to these types.
     */
    public enum ValueType {
        TYPE_NULL,
        TYPE_BOOLEAN,
        TYPE_STRING,
        TYPE_INTEGER,
        TYPE_REAL,
        TYPE_ARRAY,
        TYPE_OBJECT
    }

    /**
       This function is called by the serializer to determine what the
       json type of an object is must serialize is. As the parser
       writes a Java value to json, it must walk down the tree of
       values that represent the object, which can be either a
       primitive (null, boolean, string, or number), or a compound
       type (array or object). The function categorize takes an Object
       o, and must return the {@link ValueType} specifying the type of 
       json value this object represents. Further processing can then
       proceed by using the other functions in this interface to
       convert the domain-specific types to the ones needed by the
       serializer.  
       <p> 
       For example, in the default Java ToJsonPolicy,
       if o is a String, categorize will return TYPE_STRING, and if o
       is an ArrayList, it returns TYPE_ARRAY. Aftewards, the
       serializer will proceed by passing o to {@link #asString(Object)} 
       or {@link #arrayIterator(Object)}.

       @param o the object to categorize
       @return a ValueType corresponding to the type of json value
       this object represents
     */
    public ValueType categorize(Object o);

    /**
       This function is called by the serializer to convert a
       domain-specific boolean object to a {@link Boolean} that
       the serializer can use to write the boolean value. The
       argument o is guaranteed to be a "boolean" in the sense that
       {@link #categorize(Object)} returned TYPE_BOOLEAN when it
       was called on it.

       @param o the object to convert to a {@link Boolean}
       @return a {@link Boolean} that represents the same boolean
       value as o
     */
    public Boolean asBoolean(Object o);

    /**
       This function is called by the serializer to convert a
       domain-specific string object to a {@link String} that
       the serializer can use to write the json string value. The
       argument o is guaranteed to be a "string" in the sense that
       {@link #categorize(Object)} returned TYPE_STRING when it
       was called on it.

       @param o the object to convert to a {@link String}
       @return a {@link String} that represents the same string
       value as o
     */
    public String asString(Object o);

    /**
       This function is called by the serializer to convert a
       domain-specific integer object to a {@link Long} that
       the serializer can use to write the json integer value. The
       argument o is guaranteed to be an "integer" in the sense that
       {@link #categorize(Object)} returned TYPE_INTEGER when it
       was called on it.

       @param o the object to convert to a {@link Long}
       @return a {@link Long} that represents the same integer
       value as o
     */
    public Long asInteger(Object o);

    /**
       This function is called by the serializer to convert a
       domain-specific floating point number to a {@link Double} that
       the serializer can use to write the json floating point value. The
       argument o is guaranteed to be a "real" in the sense that
       {@link #categorize(Object)} returned TYPE_REAL when it
       was called on it.

       @param o the object to convert to a {@link Double}
       @return a {@link Double} that represents the same floating
       point value as o
     */
    public Double asReal(Object o);
    

    /**
       This function is called by the serializer to be given a way
       to iterate through the elements of a domain-specific array, so
       that it can recursively write all of its elements. The argument
       o is guaranteed to be an "array" in the sense that
       {@link #categorize(Object)} returned TYPE_ARRAY when it was
       called on it. The elements of the iterator must all be 
       domain-specific json values that {@link #categorize(Object)}
       will classify.

       @param o the array to return an {@link Iterator} on
       @return an {@link Iterator} that will iterate through the
       elements of the array
     */
    public Iterator<Object> arrayIterator(Object o);

    /**
       This function is called by the serializer to be given a way
       to iterate through the elements of a domain-specific object
       (hashtable), so that it can recursively write all of its elements.
       The argument o is guaranteed to be an "object" in the sense that
       {@link #categorize(Object)} returned TYPE_OBJECT when it was
       called on it. The elements of the iterator must be
       Map.Entry instances where the key is of type {@link String} 
       and the value is an object that {@link #categorize(Object)} will
       classify.

       @param o the object to return an {@link Iterator} on
       @return an {@link Iterator} of Map.Entry instances that
       will iterate through the key-value pairs of the object.
     */
    public Iterator<Map.Entry<String, Object>> objectIterator(Object o);
}
