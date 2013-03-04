package subjson.perftests;

public abstract class SpeedReader
{
    public char[] buffer;
    public int bufferIndex;
    public int bufferEnd; // Index of last element in the buffer, <= buffer.length.

    // Number of elements we can move backward in the stream, assuming that
    // there is room (ie, it wouldn't take us before the beginning or past
    // the end).
    protected int maxMemory;
               

    public abstract int fillBuffer();

    /* Read the next character as a Unicode code point. Returns -1 when
       the end of input has been reached. */
    public abstract int read();

    /* Move distance codepoints forward or backward (negative values). 
       Movement backwards must be less than the character memory limit. */
    public abstract void move(int distance);
}
