package su.boptim.al.subjson;


// This interface implements a lighter-weight version of Java's Reader 
// abstract class. It lets you read one character at a time using read()
// and returns -1 when the end of the input has been reached. Unlike Reader,
// this abstract requires the ability to move both forwards and backwards into
// the stream. The amount of backwards movement possible is up to the specific
// implementation (and might be zero).
public interface LightReader
{
    /* Read the next character as a Unicode code point. Returns -1 when
       the end of input has been reached. */
    public int read();

    /* Move distance codepoints forward or backward (negative values). 
       Movement backwards must be less than the character memory limit. */
    public void move(int distance);
}
