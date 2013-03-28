package su.boptim.al.subjson;

public class TextUtils
{

    public static boolean isDigit(int rune) 
    {
        if (rune >= '0' && rune <= '9') return true;
        else return false;
    }

    public static boolean isHexDigit(int rune)
    {
        if ((rune >= '0' && rune <= '9') ||
            (rune >= 'a' && rune <= 'f') ||
            (rune >= 'A' && rune <= 'F')) return true;
        else return false;
    }
    
    // Returns true if the unicode codepoint argument is a control character
    // (as defined by rfc4627). That is, U+0000 through U+001F.
    public static boolean isControlCharacter(int rune) 
    {
        if (rune >= 0 && rune <= 0x1f) return true;
        else return false;
    }

    public static boolean isWhitespace(int rune)
    {
        switch (rune) {
        case 0x20:
        case 0x09:
        case 0x0A:
        case 0x0D:
            return true;
        default:
            return false;
        }
    }

    // Returns true iff this character would need to be escaped in a JSON
    // string.
    public static boolean needsEscape(char c)
    {
        // Note that '/' does not need to be escaped, even though
        // it has an escape code. So we don't.
        switch (c) {
        case '"':
        case '\\':
        case '\b':
        case '\f':
        case '\n':
        case '\r':
        case '\t':
            return true;
        default: 
            return false;
        }
    }

    // Returns a string containing the escape code for the given character in
    // a JSON string. If a character does not need escaping, it returns that
    // character in a string. 
    public static String escape(char c)
    {
        // Again, note that '/' won't be escaped, even though it does
        // have an escape code.
        switch (c) {
        case '"': return "\\\"";
        case '\\': return "\\\\";
        case '\b': return "\\b";
        case '\f': return "\\f";
        case '\n': return "\\n";
        case '\r': return "\\r";
        case '\t': return "\\t";
        default: return String.valueOf(c);
        }
    }
}
