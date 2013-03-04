package su.boptim.al.subjson;

public interface ISpeedReader
{
    /*
    public char[] buffer;
    public int bufferIndex;
    public int bufferEnd; // Index of last element in the buffer, <= buffer.length.
    */

    public char[] getBuffer();
    public int getBufferIndex();
    public void setBufferIndex(int newBufferIndex);
    public int getBufferEnd();

    // Number of elements we can move backward in the stream, assuming that
    // there is room (ie, it wouldn't take us before the beginning or past
    // the end).
    //protected int maxMemory;
               
    public int fillBuffer();

    /* Read the next character as a Unicode code point. Returns -1 when
       the end of input has been reached. */
    public int read();

    /* Move distance codepoints forward or backward (negative values). 
       Movement backwards must be less than the character memory limit. */
    public void move(int distance);
}
