package su.boptim.al.subjson;

import su.boptim.al.subjson.ISpeedReader;

public class StringISpeedReader implements ISpeedReader
{
    public char[] buffer;
    public int bufferIndex;
    public int bufferEnd; // Index of last element in the buffer, <= buffer.length.

    public StringISpeedReader(String s)
    {
        buffer = s.toCharArray();
        bufferIndex = 0;
        bufferEnd = buffer.length;
    }

    public char[] getBuffer()
    {
        return buffer;
    }

    public int getBufferIndex()
    {
        return bufferIndex;
    }

    public void setBufferIndex(int newBufferIndex)
    {
        bufferIndex = newBufferIndex;
    }

    public int getBufferEnd()
    {
        return bufferEnd;
    }

    public int read()
    {
        if (bufferIndex >= bufferEnd) {
            return -1;
        } else {
            final int cu = buffer[bufferIndex];
            bufferIndex += 1;
            return cu;
        }
    }

    public int fillBuffer()
    {
        return 0; // Nothing to do, always have the full buffer.
    }

    public void move(final int distance)
    {
        final int newIndex = bufferIndex + distance;

        // We check for both negative indexes and greater-than-length
        // indexes by doing an unsigned int comparison of the new distance.
        if ((newIndex & 0xffffffffL) < bufferEnd) {
            bufferIndex = newIndex;
        } else {
            throw new IndexOutOfBoundsException("String index out of range: " 
                                                + newIndex);
        }
    }
}
