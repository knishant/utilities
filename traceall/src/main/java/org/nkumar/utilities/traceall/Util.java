package org.nkumar.utilities.traceall;

/**
 * Just a collection of all the utility methods that did not fit anywhere else.
 */
public final class Util
{
    private static final String[] PADDING = new String[16];

    private static final String UNIT_PADDING = "  ";

    static
    {
        PADDING[0] = "";
        for (int i = 1; i < PADDING.length; i++)
        {
            PADDING[i] = PADDING[i - 1] + UNIT_PADDING;
        }
    }

    private static final String[] SINGLE_PADDING = new String[16];

    static
    {
        SINGLE_PADDING[0] = "";
        for (int i = 1; i < SINGLE_PADDING.length; i++)
        {
            SINGLE_PADDING[i] = SINGLE_PADDING[i - 1] + "0";
        }
    }

    private Util()
    {
    }

    /**
     * Returns a string where UNIT_PADDING is repeated size number of times.
     * @param size
     * @return a string where UNIT_PADDING is repeated size number of times
     */
    public static String padding(final int size)
    {
        if (size < PADDING.length)
        {
            return PADDING[size];
        }
        else
        {
            final int lengthMinusOne = PADDING.length - 1;
            return padding(size - lengthMinusOne) + PADDING[lengthMinusOne];
        }
    }


    public static void pad(long value, int size, StringBuilder builder)
    {
        String val = String.valueOf(value);
        if (size > val.length())
        {
            builder.append(SINGLE_PADDING[size - val.length()]);
        }
        builder.append(val);
    }

    /**
     * Returns true is the string is null or the length of the trimmed string is 0.
     * @param str
     * @return true is the string is null or the length of the trimmed string is 0
     */
    public static boolean isEmptyString(final String str)
    {
        return str == null || str.trim().isEmpty();
    }

}
