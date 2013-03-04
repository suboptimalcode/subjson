package subjson.perftests;

public class StringCopies
{
    public static String copyStringSBChars(String s)
    {
        final int len = s.length();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < len; i++) {
            sb.append(s.charAt(i));
        }

        return sb.toString();
    }

    public static String copyStringSBStr(String s)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(s);

        return sb.toString();
    }

    public static String copyStringCharArray(char[] s)
    {
        return new String(s);
    }
}
