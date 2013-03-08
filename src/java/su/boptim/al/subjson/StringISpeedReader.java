package su.boptim.al.subjson;

import su.boptim.al.subjson.ISpeedReader;

public class StringISpeedReader implements ISpeedReader
{
    final String s; // The string we're gonna be reading.
    int position; // Our current position in the string.

    int recordingStart; // Index of current recording start, 
                        // or negative if none in progress.

    public StringISpeedReader(String s)
    {
        this.s = s;
        position = 0;
        recordingStart = -1;
    }

    public int read()
    {
        if (position >= s.length()) {
            return -1;
        } else {
            final int cu = s.charAt(position);
            position += 1;
            return cu;
        }
    }

    public void move(final int distance)
    {
        final int newPosition = position + distance;

        // We check for both negative indexes and greater-than-length
        // indexes by doing an unsigned int comparison of the new distance.
        if ((newPosition & 0xffffffffL) <= s.length()) {
            position = newPosition;
        } else {
            throw new IndexOutOfBoundsException("String index out of range: " 
                                                + newPosition);
        }
    }

    public void setMinimumMemory(int newMinimum)
    {
        // We have infinite memory, so nothing to do.
    }

    public void startRecording()
    {
        recordingStart = position;
    }

    public boolean isRecording()
    {
        if (recordingStart >= 0) {
            return true;
        } else {
            return false;
        }
    }

    public String copyRecording()
    {
        return new String(s.substring(recordingStart, position));
    }

    public String endRecording()
    {
        String recordedString = s.substring(recordingStart, position);
        recordingStart = -1; // End recording.

        // We return a copy of the string. This constructor will use
        // System.arraycopy under the hood to be fast, and by not reusing
        // the storage of the input string, the input string won't be pinned
        // into memory because it is needed as the backing storage for a
        // small string we parsed.
        return new String(recordedString);
    }
}
