package su.boptim.al.subjson;

import java.io.Reader;
import java.io.IOException;
import java.lang.UnsupportedOperationException;

/*
  This class is an implementation of java.io.Reader for Strings, but it
  does a few things differently from StringReader. First, it is not
  synchronized, which gives measurable speed benefits. Second, it does not
  support the close() function, which again, gives a speed boost to the
  other operations, which do not have to check if the stream is still open.

  So, unless you know that you aren't going to need synchronization and close(),
  you should probably not use this. However, SubJson.parse() really doesn't use
  those, so when given a String, it can be advantageous to use this Reader.
 */
public class UnsynchronizedStringReader extends Reader
{
    String string;
    int next;
    int mark;

    public UnsynchronizedStringReader(String s)
    {
        string = s;
        next = 0;
        mark = -1;
    }

    public int read(char[] cbuf, int off, int len) throws IOException
    {
        int numChars = Math.min(len, string.length() - next);
        string.getChars(next, next+numChars, cbuf, off);
        next += numChars;

        return numChars;
    }

    public int read(char[] cbuf) throws IOException
    {
        return read(cbuf, 0, cbuf.length);
    }

    public int read() throws IOException
    {
        if (next >= string.length()) {
            return -1;
        } else {
            int cu = string.charAt(next);
            next += 1;
            return cu;
        }
    }

    public long skip(long n) throws IOException
    {
        if (n < 0L) {
            throw new IllegalArgumentException("skip value is negative");
        }

        next += n;

        return n;
    }

    public boolean ready() throws IOException
    {
        return true;
    }

    public boolean markSupported()
    {
        return true;
    }

    public void mark(int readAheadLimit) throws IOException
    {
        mark = next;
    }

    public void reset() throws IOException
    {
        next = mark;
    }

    public void close()
    {
        string = null;
        
        throw new UnsupportedOperationException("Reader cannot be closed.");
    }
}
