package su.boptim.al.subjson;

public interface ISpeedReader
{
    /* Read the next character as a Unicode code point. Returns -1 when
       the end of input has been reached. */
    public int read();

    /* Move distance codepoints forward or backward (negative values). 
       Movement backwards must be less than the character memory limit. */
    public void move(int distance);

    /* Instruct this reader to keep at least newMinimum characters of
       memory *from now on*. The reader may decide to keep more than
       the amount you set, but you can only count on the amount you set,
       and only for positions in the stream at least newMinimum characters
       from the point this was called. */
    public void setMinimumMemory(int newMinimum);

    /* Instruct this reader to start recording the characters read from
       this point onwards, until the recording is stopped. Obviously,
       *at least* the corresponding amount of memory is used up while
       recording, so be mindful that you only record exactly what you
       need to prevent the entire stream from being held in memory. 
       
       Also note that although the recording can get indefinitely long, and
       in particular longer than the memory being saved, having recorded
       longer than the reader's memory does not guarantee you will be able
       to do a move back farther than the reader's memory! 
       
       If a recording was already in progress, this will discard the recording
       up to the current point. */
    public void startRecording();

    /* Returns true if there is a recording in progress, false otherwise. */
    public boolean isRecording();

    /* Return a string containing what has been recorded so far. Does not
       end the recording. */
    public String copyRecording();

    /* Finish the recording. The stream will no longer be considered to be
       recording and any memory that was being kept to support the recording
       can be freed. */
    public void endRecording();

}
