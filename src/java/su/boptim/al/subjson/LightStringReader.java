package su.boptim.al.subjson;

public class LightStringReader implements LightReader
{
    String _s; // The string we're gonna be reading.
    int position; // Our current position in the string.

    public LightStringReader(String s)
    {
        _s = s;
        position = 0;
    }

    public int read()
    {
        if (position >= _s.length()) {
            return -1;
        } else {
            int cp = _s.charAt(position);
            position += 1;
            return cp;
        }
    }

    public void move(int distance)
    {
        int newPosition = position + distance;

        // We check for both negative indexes and greater-than-length
        // indexes by doing an unsigned int comparison of the new distance.
        if ((newPosition & 0xffffffffL) < _s.length()) {
            position = newPosition;
        } else {
            throw new IndexOutOfBoundsException("String index out of range: " 
                                                + newPosition);
        }
    }
}
