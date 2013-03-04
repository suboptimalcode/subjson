package subjson.perftests;

import subjson.perftests.SpeedReader;
import java.lang.reflect.Field;

public class StringSpeedReader extends SpeedReader
{
    private static final Field field;
    static {
        field = initializeField();
    }

    private static Field initializeField()
    {
        try {
            Field field = String.class.getDeclaredField("value");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            // Convert this godawful checked exception into a less awful unchecked runtime one.
            throw new RuntimeException(e);
        }
    }

    public StringSpeedReader(String s)
    {
        try {
            buffer = (char[]) field.get(s);
        } catch (Exception e) {
            System.out.println("Wow, that really should have never happened.");
        }
        bufferIndex = 0;
        bufferEnd = buffer.length;
    }

    public int read()
    {
        if (bufferIndex >= bufferEnd) {
            return -1;
        } else {
            int cu = buffer[bufferIndex];
            bufferIndex += 1;
            return cu;
        }
    }

    public int fillBuffer()
    {
        return 0; // Nothing to do, always have the full buffer.
    }

    public void move(int distance)
    {
        int newIndex = bufferIndex + distance;

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
