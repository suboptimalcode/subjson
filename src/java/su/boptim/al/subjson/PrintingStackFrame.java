package su.boptim.al.subjson;

import java.util.Iterator;

// This class will hold an iterator and a flag indicating what is being
// iterated over, to implement the non-stack-consuming print.
class PrintingStackFrame
{
    Iterator<?> it;
    ToJsonPolicy.ValueType iteratorType;
    
    public PrintingStackFrame(Iterator<?> it, ToJsonPolicy.ValueType iteratorType)
    {
        this.it = it;
        this.iteratorType = iteratorType;
    }
}
