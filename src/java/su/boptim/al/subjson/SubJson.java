package su.boptim.al.subjson;

import java.util.Stack;
import java.util.ArrayList;
import java.util.HashMap;

public class SubJson
{
    static final int STATE_OBJECT  = 0;
    static final int STATE_ARRAY   = 1;
    static final int STATE_STRING  = 2;
    static final int STATE_NUMBER  = 3;
    static final int STATE_BOOLEAN = 4;
    static final int STATE_NULL    = 5;

    static final int NEED_ANYTHING = 0;
    static final int NEED_COMMA = 1;
    static final int NEED_COLON = 2;
    
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

    public static boolean isArray(Object o)
    {
        return o instanceof ArrayList<?>;
    }

    public static boolean isObject(Object o)
    {
        return o instanceof HashMap<?,?>;
    }

    public static void startArray(Stack stack)
    {
        stack.push(new ArrayList());
    }

    public static Object[] finishArray(Stack stack)
    {
        Object[] arr = ((ArrayList)stack.pop()).toArray();
        
        if (stack.empty()) {
            // This array was the only thing we were reading, so just
            // return it.
            return arr;
        } else {
            // We need to examine the stack to see what to do.
            Object top = stack.peek();
            if (top instanceof ArrayList) {
                // There's another ArrayList below us on the stack, so we should
                // insert this array into that one.
                ((ArrayList)top).add(arr);
                return arr;
            } else if (top instanceof String) {
                // Only way a string gets on the stack is if an object is
                // being read and its key has been read but not the 
                // corresponding value. Now we have that value, so insert
                // the KV pair into the object.
                String key = (String)stack.pop();
                HashMap obj = (HashMap)stack.peek();
                obj.put(key, arr);
                return arr;
            } else {
                throw new IllegalArgumentException("Attempted to finish an array that was above an illegal type on the stack.");
            }
        }
    }

    public static void startObject(Stack stack)
    {
        stack.push(new HashMap());
    }

    public static HashMap finishObject(Stack stack)
    {
        HashMap<?,?> obj = ((HashMap<?,?>)stack.pop());

        if (stack.empty()) {
            // This object was the only thing we were reading, so just
            // return it.
            return obj;
        } else {
            // We need to examine the stack to see what to do.
            Object top = stack.peek();
            if (top instanceof ArrayList) {
                // There's an ArrayList below us on the stack, so we should
                // insert this object into that one.
                ((ArrayList)top).add(obj);
                return obj;
            } else if (top instanceof String) {
                // Only way a string gets on the stack is if an object is
                // being read and its key has been read but not the
                // corresponding value. Now we have that value, so insert
                // the KV pair into the object.
                String key = (String)stack.pop();
                HashMap stackObj = (HashMap)stack.peek();
                stackObj.put(key, obj);
                return obj;
            } else {
                throw new IllegalArgumentException("Attempted to finish an object that was above an illegal type on the stack.");
            }
        }
    }

    /* 
       Given a stack and an object, adds the object to the stack according
       to what is at the top of the stack. 

       There are three things that can go on the stack: arrays, objects (hashtables),
       and strings, and the operations are governed by three simple rules:

       * If the top of the stack is an array, stackAppend should append the
       object to the array.
       * If the top of the stack is an object, then only a string can be pushed
       onto the stack (this will be the key of the next entry in the hashtable).
       * If the top of the stack is a string, then you can push a string, array
         or object onto the stack.

       Anything else is an error, but this library will only ever give you a 
       valid object (unless it has a bug).
    */
    public static void stackPush(Stack stack, Object newObj)
    {
        Object top = stack.peek();

        if (top instanceof ArrayList) {
            ((ArrayList)top).add(newObj);
        } else if (top instanceof HashMap && newObj instanceof String) {
            stack.push(newObj); 
        } else if (top instanceof String) {
            stack.push(newObj);
        } else {
            throw new IllegalArgumentException("Could not push the object " + newObj.toString()
                                               + " onto the JSON context stack when the top of the stack is " + top.toString());
        }
    }

    /*
      Calling this function indicates that the "in-progress" object on the top
      of the stack is now "done," and should be processed in a way that
      finalizes that and removes it from the top of the stack.
     */
    /*    public static Object stackPop(Stack stack)
    {
        Object top = stack.pop();

        if (top instanceof ArrayList) {
            return ((ArrayList)top).toArray();
        } else if (top instanceof HashMap) {
            
            return top;
        } else {
            throw new IllegalArgumentException("Attempted to pop an invalid object off of the JSON context stack.");
        }
        }*/

    // jsonSrc must be pointing at the first character of a valid JSON object,
    // and the LightReader must buffer at least one character for backwards
    // movement.
    public static Object parse(LightReader jsonSrc) throws Exception
    {
        Stack stack = new Stack();

        int currRune = 0;
        boolean needCommaBeforeValue = false; // To guarantee a comma is read first
        boolean needKeyBeforeValue = false; // To guarantee a string + : is read first
        boolean needValue = false;  // Have seen a key, need a value before obj close

        // JSON can basically nest arbitrarily deeply, so only break out the
        // functions that read terminals (null/true/false, numbers, and strings)!
        // Everything else, we'll do with an explicit stack structure in this
        // loop without growing the execution stack.
        while (currRune != -1) {
            currRune = jsonSrc.read();

            // If we are expecting a specific thing, try to find it before
            // parsing anything else.
            if (needCommaBeforeValue) {
                // Skip any whitespace
                if (isWhitespace(currRune)) {
                    skipWhitespace(jsonSrc);
                    currRune = jsonSrc.read();
                }
                
                if (currRune == ',') {
                    needCommaBeforeValue = false;
                    currRune = jsonSrc.read(); // Skip the comma, it's useless.
                } else if (currRune == ']' || currRune == '}') {
                    needCommaBeforeValue = false;
                } else {
                    throw new IllegalArgumentException("Invalid input, expected a comma.");
                }
            } else if (needKeyBeforeValue) {
                // Skip any whitespace
                if (isWhitespace(currRune)) {
                    skipWhitespace(jsonSrc);
                    currRune = jsonSrc.read();
                }

                if (currRune == '"') {
                    jsonSrc.move(-1); // Go back one for the string read.
                    String key = parseString(jsonSrc);
                    skipWhitespace(jsonSrc); // Skip any whitespace after key.
                    currRune = jsonSrc.read();

                    if (currRune == ':') {
                        currRune = jsonSrc.read(); // Move to the next character
                        needValue = true;
                        needKeyBeforeValue = false;
                    } else {
                        throw new IllegalArgumentException("Invalid input, expected a key.");
                    }
                }
            }
            
            // Otherwise, we can handle pretty much any available value now.
            switch (currRune) {
                // whitespace
            case 0x20: // space
            case 0x09: // tab
            case 0x0A: // linefeed
            case 0x0D: // carriage return
                skipWhitespace(jsonSrc);
                break;
                
                // null
            case 'n': 
                jsonSrc.move(-1); // Undo the lookahead that let us know it was true.
                parseNull(jsonSrc);
                if (stack.empty()) {
                    return null;
                } else {
                    if (isArray(stack.peek())) {
                        needCommaBeforeValue = true;
                    }
                    stackPush(stack, null);
                }
                break;
                
                // true & false
            case 't':
            case 'f':
                jsonSrc.move(-1); // Undo the lookahead that let us know it was true.
                Object resultBool = parseBoolean(jsonSrc);
                if (stack.empty()) {
                    return resultBool;
                } else {
                    if (isArray(stack.peek())) {
                        needCommaBeforeValue = true;
                    }
                    stackPush(stack, (Object)resultBool);
                }
                break;
                
                // Number
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                jsonSrc.move(-1); // Undo the lookahead that told us this was a number.
                Number resultNum = parseNumber(jsonSrc);
                if (stack.empty()) {
                    return resultNum;
                } else {
                    if (isArray(stack.peek())) {
                        needCommaBeforeValue = true;
                    }
                    stackPush(stack, (Object)resultNum);
                }
                break;
                
                // String
            case '"':
                jsonSrc.move(-1);
                String resultString = parseString(jsonSrc);
                if (stack.empty()) {
                    return resultString;
                } else {
                    if (isArray(stack.peek())) {
                        needCommaBeforeValue = true;
                    } else if (isObject(stack.peek())) {
                        needCommaBeforeValue = true;
                        needKeyBeforeValue = true;
                        needValue = false;
                    }
                    stackPush(stack, (Object)resultString);
                }
                break;
                
                // Array
            case '[':
                startArray(stack);
                break;
            case ']':
                Object resultArray = finishArray(stack);
                if (stack.empty()) {
                    return resultArray;
                } else {
                    stackPush(stack, (Object)resultArray);
                }
                break;

                // Object
            case '{':
                startObject(stack);
                needKeyBeforeValue = true;
                break;
            case '}':
                if (needValue) {
                    throw new IllegalArgumentException("Encountered the end of an object, but expected a value to match a key already parsed.");
                }

                Object resultObject = finishObject(stack);
                if (stack.empty()) {
                    return resultObject;
                } else {
                    stackPush(stack, (Object)resultObject);
                }
                break;
                
            case ',':
                // We should only see a comma while reading an array or object.
                if (!isArray(stack.peek()) && !isObject(stack.peek())) {
                    throw new IllegalArgumentException("Encountered a comma, but weren't reading an object or array.");
                }
                break;
                
            default:
                throw new IllegalArgumentException("Encountered invalid character in input.");
            }
        }
        
        return stack.pop();
    }

    /*
      Given a LightReader at any point, skips past any whitespace (space, tab, CR, LF)
      so that the next character read will be something that is not whitespace (or EOF).
    */
    private static void skipWhitespace(LightReader jsonSrc)
    {
        int currRune = jsonSrc.read();
        
        while (true) {
            switch (currRune) {
            case -1: 
                return;
            case 0x20: // space
            case 0x09: // tab
            case 0x0A: // linefeed
            case 0x0D: // carriage return
                currRune = jsonSrc.read();
                break;
            default:
                jsonSrc.move(-1);  // Undo the lookahead that was not whitespace.
                return;
            }
        }
    }

    /*
      parseNull takes a LightReader that is pointing at a JSON null literal and
      advances the LightReader to the character after the end of the literal, 
      while checking that the null literal is correctly written and providing errors
      if not.
    */
    private static void parseNull(LightReader jsonSrc)
    {   
        // This loop only executes once, use it to simulate goto with a break.
        while (true) {
            int currRune = jsonSrc.read();
            if (currRune != 'n') break;
            
            currRune = jsonSrc.read();
            if (currRune != 'u') break;

            currRune = jsonSrc.read();
            if (currRune != 'l') break;

            currRune = jsonSrc.read();
            if (currRune != 'l') break;

            return;
        }

        // If we got here, it is because we had to break due to a parse error.
        throw new IllegalArgumentException("Encountered invalid input while attempting to read the null literal.");
    }

    /*
      parseBoolean takes a LightReader that is pointing at a JSON boolean literal
      and does two things:
      1) Returns the boolean value that literal represents (true or false)
      2) Advances the LightReader to the character after the end of the literal.
    */
    private static Boolean parseBoolean(LightReader jsonSrc)
    {
        int currRune = jsonSrc.read();
        switch (currRune) {
        case 't':
            // This loop only executes once, use it to simulate goto with a break.
            while (true) {
                currRune = jsonSrc.read();            
                if (currRune != 'r') break;
                
                currRune = jsonSrc.read();
                if (currRune != 'u') break;
                
                currRune = jsonSrc.read();
                if (currRune != 'e') break;
                
                return Boolean.TRUE;
            }
            
            // If we got here, it is because we had to break due to a parse error.
            throw new IllegalArgumentException("Encountered invalid input while attempting to read the boolean literal 'true'.");
        case 'f':
            // This loop only executes once, use it to simulate goto with a break.
            while (true) {
                currRune = jsonSrc.read();
                if (currRune != 'a') break;

                currRune = jsonSrc.read();
                if (currRune != 'l') break;
                
                currRune = jsonSrc.read();
                if (currRune != 's') break;
                
                currRune = jsonSrc.read();
                if (currRune != 'e') break;

                return Boolean.FALSE;
            }
            
            // If we got here, it is because we had to break due to a parse error.
            throw new IllegalArgumentException("Encountered invalid input while attempting to read the boolean literal 'false'.");
        default:
            // This code should never execute unless there is a bug; this function
            // should only be called if the next 4 or 5 characters in the LightReader
            // will be one of the two boolean literals.
            throw new IllegalArgumentException("Attempted to parse a boolean literal out of input that was not pointing at one.");
        }
    }

    /* 
       parseNumber takes a LightReader that is pointing at a JSON number literal
       and does two things: 
       1) Returns the Number that literal represents
       2) Advances the LightReader to the first non-number character in the JSON
          source (that is, a read() after this function will return the next character
          after the number literal). Basically clips the number off the front of
          the stream.
    */
    private static Number parseNumber(LightReader jsonSrc)
    {
        StringBuilder sb = new StringBuilder();
        int currRune = jsonSrc.read();

        // This while loop will only execute once, we use it
        // to get access to the break statement to jump to the
        // finishing up code from various points. Note that every
        // call to read() must handle EOF with either a thrown
        // exception or a break from the main loop.
        boolean sawDecimal = false; // Will use these to parse at the end.
        boolean sawExponent = false;
        while (true) {
            boolean sawNegation = currRune == '-' ? true : false;
            if (sawNegation) {
                // We'll append the negation to the string and move on to
                // look for the first digit.
                sb.appendCodePoint(currRune); 
                currRune = jsonSrc.read();
            }
            
            // If we saw a '-' and the next character is not a digit or is EOF, 
            // it is invalid JSON. JSON requires at least one digit before decimal,
            // exponent parts, and end-of-number.
            if (sawNegation && !isDigit(currRune)) { // Also handles EOF.
                throw new NumberFormatException("While attempting to parse a negative number, the negative sign was not followed by a digit.");
            }
            
            // A JSON number can only have a single leading 0 digit when it
            // is just before a decimal point or exponentiation.
            boolean sawLeadingZero = currRune == '0' ? true : false;
            sb.appendCodePoint(currRune);
            currRune = jsonSrc.read();
            
            if (sawLeadingZero && isDigit(currRune)) {
                throw new NumberFormatException("While attempting to parse a number, there was a leading zero not immediately followed by a decimal point or exponentiation.");
            } else if (currRune == -1) {
                break; // EOF, but enough input to parse a number.
            }
            
            // Copy as many digits as are present into the current string. Note that if
            // we already saw a '.' or 'e' (for example, this loop doesn't execute and
            // we move right on to the next test.
            while (isDigit(currRune)) {
                sb.appendCodePoint(currRune);
                currRune = jsonSrc.read();
            }
            
            if (currRune == -1) break; // EOF, but enough input to parse a number.
            
            // At this point, currRune has read a codepoint that is not the
            // unicode value for a digit. It may be a valid character for a number
            // or may indicate the number has finished.
            sawDecimal = currRune == '.' ? true : false;
            if (sawDecimal) {
                // We saw a decimal point, so add it on and continue reading digits.
                sb.appendCodePoint(currRune);
                currRune = jsonSrc.read();
                
                // We must read at least one digit before moving on.
                // Also handles EOF.
                if (!isDigit(currRune)) {
                    throw new NumberFormatException("While attempting to parse a number, there was a decimal point not immediately followed by a digit.");
                }
                
                while (isDigit(currRune)) {
                    sb.appendCodePoint(currRune);
                    currRune = jsonSrc.read();
                }
            }
            
            // When we've gotten here, we've either seen a decimal point followed by
            // at least one digit or we haven't, so there may be exponentiation or
            // the end of the number ahead. We know whatever is next is not a digit,
            // but the only thing that can keep a number going now is e/E.
            if (currRune == 'e' || currRune == 'E') {
                // Having seen an e/E, we must see at least one digit, possibly preceded
                // by + or -.
                sawExponent = true;
                sb.appendCodePoint(currRune);
                currRune = jsonSrc.read();
            } else break; // Handles EOF and non-digit, but enough to make number.
            
            // If we reach this point, then we saw e or E and did another read().
            // There might be a + or - which we will simply add to the number's
            // string and continue on to read digits and check for non-number chars.
            if (currRune == '+' || currRune == '-') {
                // Just tack it on and continue on to the next character.
                sb.appendCodePoint(currRune);
                currRune = jsonSrc.read();
            }
            
            // Now currRune is past any e/E or +/- that would be valid. currRune must
            // be either a digit, EOF, or some non-number character. If it's not the
            // first one of those, then we've reached the end of the number.
            while (isDigit(currRune)) {
                sb.appendCodePoint(currRune);
                currRune = jsonSrc.read();
            }
            
            break; // We have to break out of the infinite loop every time.
        }
        
        // Finish up the parsing of the number.
        Number retVal = null;
        if (sawDecimal || sawExponent) { 
            // If there was a decimal point or exponent, it must be floating point.
            retVal = new Double(sb.toString()); 
        } else {
            retVal = new Integer(sb.toString());
        }
        
        // We had to see a non-number character to know we were past the end of
        // the number, so if we didn't see an EOF, move back so that whoever 
        // needs it next can have it available.
        if (currRune != -1) {
            jsonSrc.move(-1); 
        }
        return retVal;
    }

    /* 
       parseString takes a LightReader that is pointing at a JSON string literal
       and does two things: 
       1) Returns the String that literal represents
       2) Advances the LightReader to the first character after the end of the 
          string in the JSON source. Basically clips the string off the front
          of the stream.
    */
    private static String parseString(LightReader jsonSrc)
    {
        StringBuilder sb = new StringBuilder();
        int currRune = jsonSrc.read();
        
        if (currRune != '"') {
            throw new IllegalArgumentException("Attempted to parse a string literal from input that was not pointing at one.");
        }

        while (true) {
            currRune = jsonSrc.read();
            
            if (isControlCharacter(currRune)) {
                throw new IllegalArgumentException("Encountered a control character while parsing a string.");
            } else {
                switch (currRune) {
                // End of input
                case -1:
                    throw new IllegalArgumentException("Encountered end of input while reading a string.");
                // End of string
                case '"':
                    return sb.toString();
                // Escape sequence. We'll handle it right here entirely.
                case '\\':
                    currRune = jsonSrc.read();
                    switch (currRune) {
                    case '"': // Escaped quotation mark
                        sb.append('"');
                        break;
                    case '\\': // Escaped reverse solidus
                        sb.append('\\');
                        break;
                    case '/': // Escaped solidus
                        sb.append('/');
                        break;
                    case 'b': // Escaped backspace
                        sb.append('\b');
                        break;
                    case 'f': // Escaped formfeed
                        sb.append('\f');
                        break;
                    case 'n': // Escaped newline
                        sb.append('\n');
                        break;
                    case 'r': // Escaped carriage return
                        sb.append('\r');
                        break;
                    case 't': // Escaped tab
                        sb.append('\t');
                        break;
                    case 'u': // Escaped Unicode character
                        StringBuilder codepoint = new StringBuilder();
                        
                        for (int i = 0; i < 4; i++) {
                            currRune = jsonSrc.read();
                            if (isHexDigit(currRune)) {
                                codepoint.appendCodePoint(currRune);
                            } else {
                                throw new IllegalArgumentException("Encountered invalid input while reading a Unicode escape sequence.");
                            }
                        }
                        
                        sb.appendCodePoint(Integer.parseInt(codepoint.toString(), 16));
                        break;
                    default:
                        throw new IllegalArgumentException("Encountered invalid input while reading an escape sequence.");
                    }
                    break;
                default:
                    // Just some regular old character.
                    sb.appendCodePoint(currRune);
                    break;
                }
            }
        }
    }
}
