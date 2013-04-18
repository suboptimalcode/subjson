package subjson.perftests;

import java.io.Reader;

public class ParseToQuotes
{
    // return the index of the endquote, -1 if not found.
    public static int findQuote(String s)
    {
        char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '"')
                return i;
        }
        
        return -1;
    }

    public static int findQuote(char[] chars)
    {
        final int len = chars.length;
        for (int i = 0; i < len; i++) {
            if (chars[i] == '"')
                return i;
        }
        
        return -1;
    }

    public static int findQuote(Reader s)
    {
        int currRune = -1;
        int idx = 0; 

        try { // Fuck checked exceptions.
            while (true) {
                currRune = s.read();

                if (currRune == -1) return -1;
                else if (currRune == '"') return idx;
                else {
                    idx++;
                }
            }
        } catch (Exception e) {
            // Go kill yourself.
        }

        return -1;
    }
}

