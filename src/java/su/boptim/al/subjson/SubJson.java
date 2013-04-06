package su.boptim.al.subjson;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Iterator;
import java.io.Reader;
import java.io.IOException;


/**
   This class is the main API to the JSON reader and writer. The main
   entrypoints are the static methods read() and write(). The read()
   method takes a {@link Reader}, reads a json object from it, and
   returns it. There are also variants to take a String argument and
   provide an object that directs how the object is constructed in
   memory. The write() method takes a {@link Appendable} and writes the
   json object to it, either pretty-printed or with minimal whitespace.
   This method also has variants that take an object to direct how
   in-memory objects are mapped to json values, as well as the
   writeToString() method for convenience.
 */
public class SubJson
{
    // JUMP POINTS -- see big comment in read()
    private static final int LBL_READ_VALUE = 0;
    private static final int LBL_READ_ARRAY = 1;
    private static final int LBL_PA_STARTVALUE = 2;
    private static final int LBL_PA_HAVEREADVALUE = 3;
    private static final int LBL_READ_OBJECT = 4;
    private static final int LBL_PO_STARTKV = 5;
    private static final int LBL_PO_HAVEREADKV = 6;
    private static final int LBL_ROUTE_VALUE = 7;

    private static final int LBL_PRINT_VALUE = 0;
    private static final int LBL_CHECK_STACK_OR_FINISH = 1;
    private static final int LBL_PRINT_ARRAY_ELEMENT = 2;
    private static final int LBL_PRINT_ARRAY_CONTINUE = 3;
    private static final int LBL_PRINT_ARRAY_FINISH = 4;
    private static final int LBL_PRINT_OBJECT_ELEMENT = 5;
    private static final int LBL_PRINT_OBJECT_CONTINUE = 6;
    private static final int LBL_PRINT_OBJECT_FINISH = 7;
    
    private static final FromJsonPolicy defaultFromJP = new DefaultFromJsonPolicy();
    private static final ToJsonPolicy defaultToJP = new DefaultToJsonPolicy();

    /*
      Takes a Reader and returns what read() will return,
      but without actually moving the stream forward. The Reader
      must return true when markSupported() is called.
     */
    private static int peek(Reader r) throws IOException
    {
        r.mark(1);
        int retVal = r.read();
        r.reset();
        return retVal;
    }

    
    // Convenience function for Strings.
    /**
       Reads a json value from the jsonSrc argument, a {@link String},
       and builds a corresponding java object according to the
       default {@link FromJsonPolicy}. The parser will only read as
       much input as it needs to read a single json value, so the
       jsonSrc argument can contain any data after a valid json value.

       @param jsonSrc a {@link String} to read a json value from
       @return the in-memory java object it was directed to construct
       by the {@link FromJsonPolicy}
       @see #read(Reader, FromJsonPolicy)
     */
    public static Object read(String jsonSrc)
        throws Exception, IOException
    {
        return read(new UnsynchronizedStringReader(jsonSrc));
    }

    /**
       Reads a json value from the jsonSrc argument, a {@link String},
       and builds a corresponding java object according to the fjp
       argument, {@link FromJsonPolicy}. The parser will only read as
       much input as it needs to read a single json value, so the
       jsonSrc argument can contain any data after a valid json value.

       @param jsonSrc a {@link String} to read a json value from
       @param fjp a {@link FromJsonPolicy} to use when mapping the json
       values to in-memory java objects
       @return the in-memory java object it was directed to construct
       by the {@link FromJsonPolicy}
       @see #read(Reader, FromJsonPolicy)
     */
    public static Object read(String jsonSrc, FromJsonPolicy fjp)
        throws Exception, IOException
    {
        return read(new UnsynchronizedStringReader(jsonSrc), fjp);
    }

    /**
       Reads a json value from the jsonSrc argument, a {@link Reader}, and
       builds a corresponding java object. This function is equivalent to
       calling {@link #read(Reader, FromJsonPolicy)} with a 
       DefaultFromJsonPolicy object. The {@link Reader} must return
       true when markSupported() is called.

       @param jsonSrc a {@link Reader} to read a json value from
       @return the in-memory java object parsed
       @see #read(Reader, FromJsonPolicy)
     */
    public static Object read(Reader jsonSrc) 
        throws Exception, IOException
    {
        return read(jsonSrc, defaultFromJP);
    }

    // jsonSrc must be pointing at the first character of a valid JSON object,
    // and the Reader must return true when markSupported() is called.
    /**
       Reads a json value from the jsonSrc argument, a {@link Reader}, and 
       builds a corresponding java object according to the fjp argument, a 
       {@link FromJsonPolicy}. The parser will only read as much input as
       it needs to read a single json value, leaving the {@link Reader} so
       that the next character read() from it will be the first character after
       the end of the json value. It will read the longest single json value
       possible; given "12345", there are 5 valid json values you could read
       off the front of that, but it will pick 12345. The {@link Reader} will
       not be buffered or wrapped, so if performance is important, pick a
       Reader that makes sense for your use case. The {@link Reader} must
       return true when markSupported() is called.

       @param jsonSrc a {@link Reader} to read a json value from
       @param fjp a {@link FromJsonPolicy} to use when mapping the json
       values to in-memory java objects
       @return the in-memory java object it was directed to construct
       by the {@link FromJsonPolicy}
     */
    public static Object read(Reader jsonSrc, FromJsonPolicy fjp) 
        throws Exception, IOException
    {
        ArrayDeque<Object> valueStack = new ArrayDeque<Object>();
        ArrayDeque<Object> keyStack = new ArrayDeque<Object>(); // For parsing KV pairs in objects.
        int currState = LBL_READ_VALUE; 

        int currRune = 0;
        
        // Although null is a value we care about, we have written this so
        // that if there is no valid value read, it will have errored, so
        // (hopefully) latestValue is always the correct latest value.
        Object latestValue = null;

        // JSON can basically nest arbitrarily deeply, so only break out the
        // functions that read terminals (null/true/false, numbers, and strings)!
        // Everything else, we'll do with an explicit stack structure in this
        // loop without growing the execution stack.

        /*
          Basically, we wish we could write the following code (more or less):

          readValue()                            // LBL_READ_VALUE:
              case '[': readArray();
              case '{': readObject();
              default: return readPrimitive();
          
          readArray()                            // LBL_READ_ARRAY:
              read('[');
              startvalue:                         // LBL_PA_STARTVALUE:
              readValue();
              if (lookahead == ',')               // LBL_PA_READDVALUE:
                  read(',');
                  goto startvalue;
              else
                  read(']');
                  return newArray;

           readObject()                          // LBL_READ_OBJECT:
               read('{');
               startkv:                           // LBL_PO_STARTKV:
               readString();
               read(':');
               readValue();
               if (lookahead == ',')
                   read(',');
                   goto startkv;
               else
                   read('}');
                   return newObject;

           However, Java, of course, does not allow us to use goto, nor does it
           let us have arbitrarily looping recursion (like the calls to 
           readValue() in readArray() and readObject()). Thus, we must 
           manually manage the stack and simulate the gotos, and we do this in 
           the big while loop with nested switch statements. This interpretation
           should help reason about the loop; the outer switch is any point you
           might jump to in the pseudocode above. Pushing something onto the stack
           and 'break dispatch'ing is a function call. Popping off the stack and 
           setting a new state is like a function return. 

           I refer to these "set currState + break dispatch" as "calling" the 
           "function" they are jumping to, to help annotate the code below.
         */
        while (currRune != -1) {
            dispatch:
            switch (currState) {
            case LBL_READ_VALUE:
                currRune = peek(jsonSrc);

                switch (currRune) {
                    // whitespace
                case 0x20: // space
                case 0x09: // tab
                case 0x0A: // linefeed
                case 0x0D: // carriage return
                    skipWhitespace(jsonSrc);
                    break dispatch; // Skip checking for value to insert.
                    
                    // null
                case 'n': 
                    readNull(jsonSrc);

                    latestValue = fjp.makeNull();
                    break; // Jump to cleanup code after inner switch.
                    
                    // true & false
                case 't':
                case 'f':
                    latestValue = fjp.makeBoolean(readBoolean(jsonSrc));
                    break; // Jump to cleanup code after inner switch.
                    
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
                    latestValue = fjp.makeNumber(readNumber(jsonSrc));
                    break; // Jump to cleanup code after inner switch
                    
                    // String
                case '"':
                    latestValue = fjp.makeString(readString(jsonSrc));
                    break; // Jump to cleanup code after inner switch
                    
                    // Array
                case '[':
                    currState = LBL_READ_ARRAY;
                    break dispatch; // "Call" "readArray()"
                    
                    // Object
                case '{':
                    currState = LBL_READ_OBJECT;
                    break dispatch; // "Call" "readObject()"

                default:
                    throw new IllegalArgumentException("Encountered unexpected character '"
                                                       + (char)currRune + "' in input.");
                }

                // Fall through to route_value() to finish the value...
            case LBL_ROUTE_VALUE:
                // Having read a value, we need to figure out where to store it and
                // where to "return" to. If the value stack is empty, we are done and
                // the value should be returned. Otherwise, we need to insert it on
                // the value stack, depending on what is on top of that, and "return" to
                // the "function" that was building what was on top of the stack.
                if (valueStack.isEmpty()) {
                    return latestValue;
                } else {
                    Object valueStackTop = valueStack.peek();
                    if (fjp.isArray(valueStackTop)) {
                        // We had to read a value while parsing an array
                        fjp.arrayAppend(valueStackTop, latestValue);
                        currState = LBL_PA_HAVEREADVALUE;
                    } else if (fjp.isObject(valueStackTop)) {
                        fjp.objectInsert(valueStackTop, keyStack.pop(), latestValue);
                        currState = LBL_PO_HAVEREADKV;
                    }
                    break dispatch;
                }
                // Can't fall through to here.

                // "readArray()" (see comment above)
            case LBL_READ_ARRAY:
                readChar(jsonSrc, '[');
                valueStack.push(fjp.startArray());
            case LBL_PA_STARTVALUE: // Note: Falls through from LBL_READ_ARRAY!
                skipWhitespace(jsonSrc);
                currRune = peek(jsonSrc);

                // Need to check for empty array, where an attempt to read a 
                // value would fail.
                if (currRune == -1) {
                    throw new IllegalArgumentException("Reached EOF while parsing an array.");
                } else if (currRune != ']') {
                    currState = LBL_READ_VALUE; // "Call" "readValue()"
                    break dispatch;
                }                     
                // currRune == ']', so fall through to continue/finish array
            case LBL_PA_HAVEREADVALUE:         // ... which will know to return here from stack top.
                skipWhitespace(jsonSrc);
                currRune = peek(jsonSrc);
                if (currRune == ',') {
                    readChar(jsonSrc, ',');
                    currState = LBL_PA_STARTVALUE;
                    break dispatch;
                } else {
                    readChar(jsonSrc, ']');
                    latestValue = fjp.finishArray(valueStack.pop());
                    // Now we need to check stack to figure out where to return to.
                    currState = LBL_ROUTE_VALUE; // "call" "route_value()"
                    break dispatch;
                }

                // "readObject()" (see comment above)
            case LBL_READ_OBJECT:
                readChar(jsonSrc, '{');
                valueStack.push(fjp.startObject());
            case LBL_PO_STARTKV: // Note: Falls through from LBL_READ_OBJECT!
                skipWhitespace(jsonSrc);
                currRune = peek(jsonSrc);

                // Need to check for '}' in case of empty object. If so,
                // fall through to READDKV to finish object.
                if (currRune == -1) {
                    throw new IllegalArgumentException("Reached EOF while parsing an object.");
                } else if (currRune != '}') {
                    keyStack.push(fjp.makeString(readString(jsonSrc)));
                    skipWhitespace(jsonSrc);
                    readChar(jsonSrc, ':');
                    skipWhitespace(jsonSrc);
                    currState = LBL_READ_VALUE; // "Call" "readValue()"
                    break dispatch;
                }
                // currRune == '}' so fall through to finish object
            case LBL_PO_HAVEREADKV:            // ... which will know to return here from stack top.
                skipWhitespace(jsonSrc);
                currRune = peek(jsonSrc);
                if (currRune == ',') {
                    readChar(jsonSrc, ',');
                    currState = LBL_PO_STARTKV;
                    break dispatch;
                } else {
                    readChar(jsonSrc, '}');
                    latestValue = fjp.finishObject(valueStack.pop());
                    // Now we need to check stack to figure out where to return to.
                    currState = LBL_ROUTE_VALUE; // "call" "route_value()"
                    break dispatch;
                }
            }
        }   
        return valueStack.pop(); // No idea how we'd get here.
    }

    /*
      Given a Reader at any point, skips past any whitespace (space, tab, CR, LF)
      so that the next character read will be something that is not whitespace (or EOF).
    */
    private static void skipWhitespace(Reader jsonSrc) throws IOException
    {
        int currRune = peek(jsonSrc);
        
        while (true) {
            // We can eliminate whitespace+EOF with one quick check: is the rune
            // <= 0x20 (ASCII space). If it is, we can do more checks to determine
            // whether it is should indeed be skipped or not. Because often there
            // is no whitespace where some is allowed, this creates a fast path
            // to bail out quickly, which registers a speed improvement when
            // measured.
            if (currRune <= 0x20) {
                switch (currRune) {
                case 0x20: // space
                case 0x09: // tab
                case 0x0a: // linefeed
                case 0x0d: // carriage return
                    jsonSrc.read();
                    currRune = peek(jsonSrc);
                    break;
                default:
                    // There are characters below 0x20 that aren't valid whitespace
                    // but they are not valid in a JSON stream, so we simply
                    // assume that whatever will try to read something next (in
                    // the main read loop) will report the error if the next rune
                    // would cause an error, should reading need to continue.
                    return;
                }
            } else {
                // Peeked character is definitely not whitespace, so we've fulfilled
                // our mission and just return.
                return;
            }
        }
    }

    /*
      Given a Reader, attempts to read the next character from it and check that
      it is the character given as the second argument. If it is, it simply returns
      and the Reader will be on the next character after the one just read. 
      Otherwise, throws a descriptive error.
     */
    private static void readChar(Reader jsonSrc, char theChar) throws IOException
    {
        int currRune = jsonSrc.read();
        if (currRune == theChar) {
            return;
        } else if (currRune == -1) {
            throw new IllegalArgumentException("Read EOF when " + theChar 
                                               + " was expected.");
        } else {
            throw new IllegalArgumentException("Read " + (char)currRune + " when "
                                               + theChar + " was expected.");
        }
    }

    /*
      readNull takes a Reader that is pointing at a JSON null literal and
      advances the Reader to the character after the end of the literal, 
      while checking that the null literal is correctly written and providing errors
      if not.
    */
    private static void readNull(Reader jsonSrc) throws IOException
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

        // If we got here, it is because we had to break due to a read error.
        throw new IllegalArgumentException("Encountered invalid input while attempting to read the null literal.");
    }

    /*
      readBoolean takes an Reader that is pointing at a JSON boolean literal
      and does two things:
      1) Returns the boolean value that literal represents (true or false)
      2) Advances the Reader to the character after the end of the literal.
    */
    private static Boolean readBoolean(Reader jsonSrc) throws IOException
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
            
            // If we got here, it is because we had to break due to a read error.
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
            
            // If we got here, it is because we had to break due to a read error.
            throw new IllegalArgumentException("Encountered invalid input while attempting to read the boolean literal 'false'.");
        default:
            // This code should never execute unless there is a bug; this function
            // should only be called if the next 4 or 5 characters in the Reader
            // will be one of the two boolean literals.
            throw new IllegalArgumentException("Attempted to read a boolean literal out of input that was not pointing at one.");
        }
    }

    /* 
       readNumber takes an Reader that is pointing at a JSON number literal
       and does two things: 
       1) Returns the Number that literal represents
       2) Advances the Reader to the first non-number character in the JSON
          source (that is, a read() after this function will return the next character
          after the number literal). Basically clips the number off the front of
          the stream.
    */
    private static Number readNumber(Reader jsonSrc) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        int currRune = peek(jsonSrc);

        // This while loop will only execute once, we use it
        // to get access to the break statement to jump to the
        // finishing up code from various points. Note that every
        // call to read() must handle EOF with either a thrown
        // exception or a break from the main loop.
        boolean sawDecimal = false; // Will use these to read at the end.
        boolean sawExponent = false;
        while (true) {
            boolean sawNegation = currRune == '-' ? true : false;
            if (sawNegation) {
                // We'll append the negation to the string and move on to
                // look for the first digit.
                sb.appendCodePoint(currRune); 
                jsonSrc.skip(1);
                currRune = peek(jsonSrc);
            }
            
            // If we saw a '-' and the next character is not a digit or is EOF, 
            // it is invalid JSON. JSON requires at least one digit before decimal,
            // exponent parts, and end-of-number.
            if (sawNegation && !TextUtils.isDigit(currRune)) { // Also handles EOF.
                throw new NumberFormatException("While attempting to read a negative number, the negative sign was not followed by a digit.");
            }
            
            // A JSON number can only have a single leading 0 digit when it
            // is just before a decimal point or exponentiation.
            boolean sawLeadingZero = currRune == '0' ? true : false;
            sb.appendCodePoint(currRune);
            jsonSrc.skip(1);
            currRune = peek(jsonSrc);
            
            if (sawLeadingZero && TextUtils.isDigit(currRune)) {
                throw new NumberFormatException("While attempting to read a number, there was a leading zero not immediately followed by a decimal point or exponentiation.");
            } else if (currRune == -1) {
                break; // EOF, but enough input to read a number.
            }
            
            // Copy as many digits as are present into the current string. Note that if
            // we already saw a '.' or 'e' (for example, this loop doesn't execute and
            // we move right on to the next test.
            while (TextUtils.isDigit(currRune)) {
                sb.appendCodePoint(currRune);
                jsonSrc.skip(1);
                currRune = peek(jsonSrc);
            }
            
            if (currRune == -1) break; // EOF, but enough input to read a number.
            
            // At this point, currRune has read a codepoint that is not the
            // unicode value for a digit. It may be a valid character for a number
            // or may indicate the number has finished.
            sawDecimal = currRune == '.' ? true : false;
            if (sawDecimal) {
                // We saw a decimal point, so add it on and continue reading digits.
                sb.appendCodePoint(currRune);
                jsonSrc.skip(1);
                currRune = peek(jsonSrc);
                
                // We must read at least one digit before moving on.
                // Also handles EOF.
                if (!TextUtils.isDigit(currRune)) {
                    throw new NumberFormatException("While attempting to read a number, there was a decimal point not immediately followed by a digit.");
                }
                
                while (TextUtils.isDigit(currRune)) {
                    sb.appendCodePoint(currRune);
                    jsonSrc.skip(1);
                    currRune = peek(jsonSrc);
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
                jsonSrc.skip(1);
                currRune = peek(jsonSrc);
            } else break; // Handles EOF and non-digit, but enough to make number.
            
            // If we reach this point, then we saw e or E and did another read().
            // There might be a + or - which we will simply add to the number's
            // string and continue on to read digits and check for non-number chars.
            if (currRune == '+' || currRune == '-') {
                // Just tack it on and continue on to the next character.
                sb.appendCodePoint(currRune);
                jsonSrc.skip(1);
                currRune = peek(jsonSrc);
            }
            
            // Now currRune is past any e/E or +/- that would be valid. currRune must
            // be either a digit, EOF, or some non-number character. If it's not the
            // first one of those, then we've reached the end of the number.
            while (TextUtils.isDigit(currRune)) {
                sb.appendCodePoint(currRune);
                jsonSrc.skip(1);
                currRune = peek(jsonSrc);
            }
            
            break; // We have to break out of the infinite loop every time.
        }
        
        // Finish up the parsing of the number.
        Number retVal = null;
        if (sawDecimal || sawExponent) { 
            // If there was a decimal point or exponent, it must be floating point.
            retVal = new Double(sb.toString()); 
        } else {
            retVal = new Long(sb.toString());
        }
        
        return retVal;
    }

    /* 
       readString takes a Reader that is pointing at a JSON string literal
       and does two things: 
       1) Returns the String that literal represents
       2) Advances the Reader to the first character after the end of the 
          string in the JSON source. Basically clips the string off the front
          of the stream.
    */
    private static String readString(Reader jsonSrc) throws IOException
    {
        // There's a measurable performance benefit to building a
        // string out of chunks, instead of char by char, so we'll use
        // this buffer to copy into from the Reader and append to the
        // StringBuilder whenever it is full.  Some care must be taken
        // to append its contents when character substitutions are
        // made due to escaping. This is also why any Reader we parse
        // from must return true from markSupported().
        //
        // Note that the buffer is only copied into when we've seen as
        // many characters as we said the mark should buffer. bufferedCount
        // is used to help us keep track of when that is, so we can do
        // the copy.
        final int BUFFER_SIZE = 32;
        final char[] cbuf = new char[BUFFER_SIZE];
        int bufferedCount = 0;

        StringBuilder sb = new StringBuilder();
        int currRune = jsonSrc.read();

        if (currRune != '"') {
            throw new IllegalArgumentException("Attempted to parse a string literal from input that was not pointing at one.");
        }

        jsonSrc.mark(BUFFER_SIZE);
        while (true) {    
            // Need to check if our buffer filled up, and if so copy it off
            // to the StringBuilder.
            if (bufferedCount == BUFFER_SIZE) {
                jsonSrc.reset();
                jsonSrc.read(cbuf, 0, bufferedCount);
                sb.append(cbuf, 0, bufferedCount);

                // Restart the buffering.
                jsonSrc.mark(BUFFER_SIZE);
                bufferedCount = 0;
            }
        
            currRune = jsonSrc.read();
            bufferedCount++;

            // It turns out that almost all of the "special handling" values we can
            // receive from read() are below the '"' character in ascii. Since we
            // expect most of these values to be fairly rare in valid JSON, we can
            // check for them all at once by first checking if they are less than '"'
            // and only if so figure out exactly which situation we are in. This
            // measures as somewhat faster at runtime. Alas, the '\' character is
            // fairly high in the ASCII range, above most letters, numbers and symbols.
            if (currRune <= '"') {
                if (currRune == '"') {
                    jsonSrc.reset(); // Go back to beginning of last "buffer" to copy.
                    // What would we even do if return != bufferedCount-1?
                    jsonSrc.read(cbuf, 0, bufferedCount-1); // Don't include the '"'
                    jsonSrc.skip(1); // Skip past the '"' we saw.
                    
                    // If we haven't had to copy to the builder yet, just return
                    // the string without doing extraneous copies through the
                    // StringBuilder.
                    if (sb.length() == 0) {
                        return new String(cbuf, 0, bufferedCount-1);
                    } else {
                        sb.append(cbuf, 0, bufferedCount-1);
                        return sb.toString();
                    }
                } else if (TextUtils.isControlCharacter(currRune)) {
                    throw new IllegalArgumentException("Encountered a control character while parsing a string.");
                } else if (currRune == -1) {
                    throw new IllegalArgumentException("Encountered end of input while reading a string.");
                } else {
                    // There are a few valid characters below '"' in ascii
                    // which we simply move past and will copy on the next
                    // buffer spill.
                }
            } else {
                switch (currRune) {
                // Escape sequence. We'll handle it right here entirely.
                case '\\':
                    // Escape characters means we can't just copy from the
                    // string source to the returned string, so we stop
                    // the buffering, copy what we've parsed so far to the
                    // stringbuilder, append the escaped character to the
                    // stringbuilder, and then resume buffering anew after.
 
                    jsonSrc.reset();
                    jsonSrc.read(cbuf, 0, bufferedCount-1);
                    sb.append(cbuf, 0, bufferedCount-1);
                    jsonSrc.skip(1); // Skip past the '/' we already saw.

                    // Now we decode the escape sequence.
                    currRune = jsonSrc.read();
                    switch (currRune) {
                    case '"': // Escaped quotation mark
                        sb.append('\"');
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
                        int cp = 0;
                        
                        for (int i = 0; i < 4; i++) {
                            currRune = jsonSrc.read();

                            // Note: '0'-'9' are 0x30-0x39
                            //       'a'-'f' are 0x61-0x66
                            //       'A'-'F' are 0x41-0x46
                            // So lowest 4 bits of ascii code for a hex digit
                            // are the digit's value, as long as you add 9 for
                            // values in the 0x41-0x66 range.
                            if (TextUtils.isHexDigit(currRune)) {
                                cp = (cp << 4) | ((0xf & currRune) + (currRune <= '9' ? 0 : 9));
                            } else {
                                throw new IllegalArgumentException("Encountered invalid input while reading a Unicode escape sequence.");
                            }
                        }
                        
                        sb.appendCodePoint(cp);
                        break;
                    default:
                        throw new IllegalArgumentException("Encountered invalid input while reading an escape sequence.");
                    }
                    
                    // Finally, restart the buffering.
                    jsonSrc.mark(BUFFER_SIZE);
                    bufferedCount = 0;
                    break;
                default:
                    // Just some regular old character, just move past it so it
                    // will get copied on the next buffer spill.
                    break;
                }
            }
        }
    }

    /**
       Returns a String containing the pretty-printed serialization of jsonValue.

       @param jsonValue the value to pretty-print to json
     */
    public static String writeToString(Object jsonValue)
        throws IOException
    {
        return writeToString(jsonValue, true);
    }

    /**
       Returns a String containing the serialization of jsonValue, according
       to the default {@link ToJsonPolicy}. If pretty is true, the output will
       be pretty-printed, and if it is false, the output will be printed
       in a compact style with minimal whitespace.

       @param jsonValue the value to serialize to json
       @param pretty pass true to emite pretty-printed json, false for
       compact json
     */
    public static String writeToString(Object jsonValue, boolean pretty)
        throws IOException
    {
        return writeToString(jsonValue, pretty, defaultToJP);
    }

    /**
       Returns a String containing the serialization of jsonValue, according
       to tjp, a {@link ToJsonPolicy}. If pretty is true, the output will
       be pretty-printed, and if it is false, the output will be printed
       in a compact style with minimal whitespace.

       @param jsonValue the value to serialize to json
       @param pretty pass true to emit pretty-printed json, false for
       compact json
       @param tjp a ToJsonPolicy to translate the java value to json values
     */
    public static String writeToString(Object jsonValue, boolean pretty,
                                       ToJsonPolicy tjp)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();
        write(sb, jsonValue, pretty, tjp);
        return sb.toString();
    }

    /**
       Pretty-print the java object jsonValue to the {@link Appendable} out, 
       using the default ToJsonPolicy.

       @param out the {@link Appendable} to pretty-print the value to
       @param jsonValue the value to pretty-print to json
     */
    public static void write(Appendable out, Object jsonValue)
        throws IOException
    {
        write(out, jsonValue, true, defaultToJP);
    }

    /**
       Write the java object jsonValue to the {@link Appendable} out, using
       the default ToJsonPolicy. If pretty is true, the output will
       be pretty-printed, and if it is false, the output will be printed
       in a compact style with minimal whitespace.

       @param out the {@link Appendable} to serialize the value to
       @param jsonValue the value to serialize to json
       @param pretty pass true to emit pretty-printed json, false for
       compact json
     */
    public static void write(Appendable out, Object jsonValue, boolean pretty)
        throws IOException
    {
        write(out, jsonValue, pretty, defaultToJP);
    }

    /**
       Writes the java object jsonValue to the {@link Appendable} out according
       to tjp, a {@link ToJsonPolicy}. If pretty is true, the output will
       be pretty-printed, and if it is false, the output will be printed
       in a compact style with minimal whitespace.

       @param out the writer to serialize the value to
       @param jsonValue the value to serialize to json
       @param pretty pass true to emit pretty-printed json, false for
       compact json
       @param tjp a ToJsonPolicy to translate the java value to json values 
     */
    public static void write(Appendable out, Object jsonValue, boolean pretty, 
                             ToJsonPolicy tjp)
        throws IOException
    {
        ArrayDeque<PrintingStackFrame> inProgressStack 
            = new ArrayDeque<PrintingStackFrame>();
        
        int currState = LBL_PRINT_VALUE;
        Object currValue = jsonValue;

        // Choose characters for either pretty or compact printing.
        String NL, COMMA, COLON, TAB;
        if (pretty) {
            NL = "\n";
            COMMA = "," + NL;
            COLON = ": ";
            TAB = "    ";
        } else {
            NL = "";
            COMMA = ",";
            COLON = ":";
            TAB = "";
        }
        
        // We'll use a StringBuilder as a mutable CharSequence to
        // hold the current characters used for indentation. An
        // indentation is 4 space characters.
        StringBuilder indentation = new StringBuilder();

        /*
          Here's the (pseudo-)code we wish we could write 
          (see comment in read() above):

          write(jsonVal):
              if (primitive(jsonVal)) writePrimitive(jsonVal);
              else if (isArray(jsonVal)):
                  writeArray(jsonVal);
              else if (isObject(jsonVal)):
                  writeObject(jsonVal);
                  
          writeArray(jsonVal):
              out.append("[");
              write_array_elt:
              if (jsonVal.hasNextElement()):
                  v = getNextArrayElement(jsonVal);
                  write(v);
                  if (jsonVal.hasNextElement()):
                      out.append(",");
                      goto write_array_elt;
              out.append("]");

          writeObject(jsonVal):
              out.append("{");
              write_object_elt:
              writeString(kv.key());
              out.append(":");
              write(kv.val());
              if (jsonVal.hasNextElement()):
                  out.append(",");
                  goto write_object_elt;
              out.append("}");

          The code is a little different in flow compared to the read
          case because we must deal with the API provided by the
          Iterator interface here, instead of being in charge of the
          parsing right from the input.
        */
        while (true) {
            dispatch:
            switch (currState) {
            case LBL_PRINT_VALUE:
                ToJsonPolicy.ValueType currType = tjp.categorize(currValue);
                
                switch (currType) {
                case TYPE_NULL:
                    out.append("null");
                    break;
                case TYPE_BOOLEAN:
                    if (((Boolean)currValue).booleanValue() == true) {
                        out.append("true");
                    } else {
                        out.append("false");
                    }
                    break;
                case TYPE_STRING:
                    // Wrong, obviously, needs escaping
                    writeString(out, (String)currValue);
                    break;
                case TYPE_INTEGER:
                    out.append(currValue.toString());
                    break;
                case TYPE_REAL:
                    out.append(currValue.toString());
                    break;
                case TYPE_ARRAY:
                    {
                        PrintingStackFrame psf = 
                            new PrintingStackFrame(tjp.arrayIterator(currValue),
                                                   ToJsonPolicy.ValueType.TYPE_ARRAY);

                        // Pretty printing has an exception for empty values (empty array 
                        // = "[]", no whitespace), so handle it specially here. Works
                        // fine for both pretty and compact styles.
                        if (!psf.it.hasNext()) {
                            out.append("[]");
                            
                            currState = LBL_CHECK_STACK_OR_FINISH;
                        } else {
                            inProgressStack.push(psf);
                            indentation.append(TAB);
                            
                            out.append("[");

                            // We need attempt to print an array element first, because jumping to
                            // LBL_PRINT_ARRAY_CONTINUE assumes at least one has been printed.
                            // PRINT_ARRAY_ELEMENT will handle empty array check.            
                            out.append(NL);
                            currState = LBL_PRINT_ARRAY_ELEMENT;
                        }
                        break dispatch;
                    }
                case TYPE_OBJECT:
                    {
                        PrintingStackFrame psf =
                            new PrintingStackFrame(tjp.objectIterator(currValue),
                                                   ToJsonPolicy.ValueType.TYPE_OBJECT);

                        // Pretty printing has an exception for empty object, so handle
                        // it specially here. Works fine for both pretty and compact styles.
                        if (!psf.it.hasNext()) {
                            out.append("{}");

                            currState = LBL_CHECK_STACK_OR_FINISH;
                        } else {
                            inProgressStack.push(psf);
                            indentation.append(TAB);
                            
                            out.append("{");

                            // We know the object is non-empty since we are in the else.
                            out.append(NL);
                            currState = LBL_PRINT_OBJECT_ELEMENT;
                        }
                        break dispatch;
                    }
                default:
                    
                    break;
                }

                // Note that "print-value" falls through to here.
            case LBL_CHECK_STACK_OR_FINISH:
                if (inProgressStack.isEmpty()) {
                    return;
                } else {
                    PrintingStackFrame psf = inProgressStack.peek();
                    if (psf.iteratorType == ToJsonPolicy.ValueType.TYPE_ARRAY) {
                        currState = LBL_PRINT_ARRAY_CONTINUE;
                        break dispatch;
                    } else if (psf.iteratorType == ToJsonPolicy.ValueType.TYPE_OBJECT) {
                        currState = LBL_PRINT_OBJECT_CONTINUE;
                        break dispatch;
                    } else {
                    }
                }
                
            case LBL_PRINT_ARRAY_ELEMENT:
                {
                    PrintingStackFrame psf = inProgressStack.peek();
                    Iterator<Object> it = (Iterator<Object>)psf.it;
                    if (it.hasNext()) {
                        out.append(indentation);
                        currValue = it.next();
                        currState = LBL_PRINT_VALUE;
                    } else {
                        currState = LBL_PRINT_ARRAY_FINISH;
                    }
                }
                break dispatch;

            case LBL_PRINT_ARRAY_CONTINUE:
                {
                    PrintingStackFrame psf = inProgressStack.peek();
                    Iterator<Object> it = (Iterator<Object>)psf.it;
                    if (it.hasNext()) {
                        out.append(COMMA);
                        currState = LBL_PRINT_ARRAY_ELEMENT;
                    } else {
                        currState = LBL_PRINT_ARRAY_FINISH;
                    }
                }
                break dispatch;
                
            case LBL_PRINT_ARRAY_FINISH:
                // Finish printing the array and "return."
                inProgressStack.pop();
                indentation.delete(indentation.length() - TAB.length(),
                                   indentation.length());
                
                out.append(NL); // Need newline after last item for pretty
                out.append(indentation);
                out.append("]");

                currState = LBL_CHECK_STACK_OR_FINISH;
                break dispatch;

            case LBL_PRINT_OBJECT_ELEMENT:
                {
                    PrintingStackFrame psf = inProgressStack.peek();
                    Iterator<Map.Entry<String, Object>> it = 
                        (Iterator<Map.Entry<String, Object>>)psf.it;
                    if (it.hasNext()) {
                        out.append(indentation);
                        Map.Entry<String, Object> me = (Map.Entry<String, Object>)it.next();
                        writeString(out, me.getKey());
                        out.append(COLON);
                        currValue = me.getValue();
                        currState = LBL_PRINT_VALUE;
                    } else {
                        currState = LBL_PRINT_OBJECT_FINISH;
                    }
                }
                break dispatch;

            case LBL_PRINT_OBJECT_CONTINUE:
                {
                    PrintingStackFrame psf = inProgressStack.peek();
                    Iterator<Map.Entry<String, Object>> it =
                        (Iterator<Map.Entry<String, Object>>)psf.it;
                    if (it.hasNext()) {
                        out.append(COMMA);
                        currState = LBL_PRINT_OBJECT_ELEMENT;
                    } else {
                        currState = LBL_PRINT_OBJECT_FINISH;
                    }
                }
                break dispatch;

            case LBL_PRINT_OBJECT_FINISH:
                // Finish printing the object and "return."
                inProgressStack.pop();
                indentation.delete(indentation.length() - TAB.length(),
                                   indentation.length());

                out.append(NL); // Need newline after last item for pretty.
                out.append(indentation);
                out.append("}");

                currState = LBL_CHECK_STACK_OR_FINISH;
                break dispatch;
            }
        }
    }

    private static void writeString(Appendable out, String str)
        throws IOException
    {
        int segStart = 0;
        int i = 0;

        out.append("\"");
        while (i < str.length()) {
            char currChar = str.charAt(i);

            // Check if currChar requires escaping.
            if (TextUtils.needsEscape(currChar)) {
                // Write out the segment we've been scanning so far,
                // then write out the escaped character and restart
                // the scanning for the next segment.
                out.append(str.substring(segStart, i));
                out.append(TextUtils.escape(currChar));
                i++;
                segStart = i;
            } else {
                i++; // Just move on to the next char.
            }
        }
        
        // Need to print the final segment.
        out.append(str.substring(segStart, i));
        out.append("\"");
    }
}
