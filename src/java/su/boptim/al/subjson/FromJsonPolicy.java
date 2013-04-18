package su.boptim.al.subjson;

/**
   The interface through which the parser will create Java objects that
   correspond to the values represented in the json it is parsing.
   <p>
   As {@link SubJson} parses, it will use the functions in the
   FromJsonPolicy it is given to build up the Java objects that
   represent the values in the json. Implementing this interface is all
   that is required to receive the results of a json parse as a
   value of the type you prefer; most likely a value made from
   language-specific types (such as Clojure maps and vectors), or
   application-specific types.
*/
public interface FromJsonPolicy
{
    //
    // Arrays
    //

    /**
       This method is called by the parser when it needs to determine
       whether the object given as an argument is an "array," however
       that may be defined for this domain. This method should return
       true if and only if the Object given is an array according to
       the logic of this policy, and false otherwise.

       @param o the object being examined
       @return true if the object is an array, and false otherwise
     */
    public boolean isArray(Object o);

    /**
       This method is called by the parser when it needs a new,
       empty array. This function should return an empty object
       of the type that will be used to represent arrays in this
       policy. An object used as an array must be mutable, in
       the sense that modifications can be made to the array with
       the same object representing the array the entire time.

       @return an object representing a new, empty array ready to
       be modified by subsequent array operations in this interface
     */
    public Object startArray();

    /**
       This method is called by the parser when it needs to append a
       json value to the object given as the array. The array argument
       is guaranteed to be an array in the sense that calling 
       {@link #isArray(Object)} on it would return true. The value argument
       is some value that has been constructed by another function in
       this interface.

       @param array an object representing an array according to this
       interface, to which the second argument should be appended
       @param value the object to insert at the end of the first
       argument
     */
    public void arrayAppend(Object array, Object value);

    /**
       This method is called by the parser to signal that it is
       finished modifying the array in the argument. This gives an
       opportunity to create a "permanent" array from what may have
       been a different data structure that was more convenient to
       build up the array with. Depending on the use case, it is quite
       possible that no work is necessary in this function, other than
       returning the argument.

       @param array the array that has been under construction, and
       which is now finished
       @return an array equivalent but not necessarily identical to
       the one given
     */
    public Object finishArray(Object array);

    //
    // Objects
    //

    /**
       This method is called by the parser when it needs to determine
       whether the object given as an argument is an "object," (in the
       json sense) however that may be defined for this domain. This 
       method should return true if and only if the Object given is a
       json object according to the logic of this policy, and false 
       otherwise.

       @param o the object being examined
       @return true if the object is a json object, and false otherwise       
     */
    public boolean isObject(Object o);

    /**
       This method is called by the parser when it needs a new,
       empty json object. This function should return an empty object
       of the type that will be used to represent json objects in this
       policy. A java object used as a json object must be mutable, in
       the sense that modifications can be made to the json object with
       the same (java) object representing the json object the entire time.

       @return an object representing a new, empty json object ready to
       be modified by subsequent object operations in this interface       
     */
    public Object startObject();

    /**
       This method is called by the parser when it needs to insert a
       json value at a given String key to the object given as the first
       argument. The obj argument is guaranteed to be a json object in 
       the sense that calling {@link #isObject(Object)} on it would return
       true. The key argument is a string (as defined by this interface),
       and the value argument is some json value that has been constructed 
       by another function in this interface.

       @param obj an object representing a json object according to this
       interface, into which the third argument should be inserted at the
       key given in the second argument
       @param key the string (as defined by this interface) key for the 
       value about to be inserted
       @param value the object to insert at the key given in the second
       argument
     */
    public void objectInsert(Object obj, Object key, Object value);

    /**
       This method is called by the parser to signal that it is
       finished modifying the json object in the argument. This gives an
       opportunity to create a "permanent" json object from what may have
       been a different data structure that was more convenient to
       build up the object with. Depending on the use case, it is quite
       possible that no work is necessary in this function, other than
       returning the argument.

       @param obj the json object that has been under construction, and
       which is now finished
       @return an object equivalent but not necessarily identical to
       the one given       
     */
    public Object finishObject(Object obj);

    //
    // Primitives
    //

    /**
       This method is called by the parser to ask for an object (reference)
       representing the null it has just parsed in the json source. It
       is perfectly valid (and likely) that you will want this
       function to simply return null, but some JVM languages might
       represent a null with some object that is more useful, and this
       is their opportunity to do so.

       @return an object reference that represents a null value
     */
    public Object makeNull();

    /**
       This method is called by the parser to ask for an object
       representing the true or false value it has just parsed. It is
       perfectly valid to simply return the argument, if your
       project or language prefers to use a {@link Boolean} as the
       representation of json booleans. 

       @param b the boolean value parsed, {@link Boolean}
       @return an object the represents the true or false value given
       in the argument
     */
    public Object makeBoolean(Boolean b);

    /**
       This method is called by the parser to ask for an object
       representing the string value it has just parsed. It is
       perfectly valid to simply return the argument, if your
       project or language prefers to use a {@link String} as the
       representation of json strings.

       @param s the string value parsed, a {@link String}
       @return an object that represents the string given in the
       argument
     */
    public Object makeString(String s);
    
    /**
       This method is called by the parser to ask for an object
       representing the number value it has just parsed. It is
       perfectly valid to simply return the argument, if your
       project or language prefers to use a {@link Long} to represent
       integers and a {@link Double} to represent floating 
       point numbers.

       @param n the number value parsed, as either a {@link Long} or
       {@link Double}
       @return an object that represents the number given in the
       argument
     */
    public Object makeNumber(Number n);
}
