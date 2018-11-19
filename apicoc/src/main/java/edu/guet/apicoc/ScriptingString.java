package edu.guet.apicoc;


import android.support.annotation.NonNull;

/**
 * Created by Mr.小世界 on 2018/11/16.
 * @see ScriptRuntime#allocString(int) 构造新的实例
 * 空终止字符串的java包装器
 */

@SuppressWarnings("JniMissingFunction")
public final class ScriptingString implements Comparable<ScriptingString>
{
    private final int maxCapacity;
    private final long handler;

    ScriptingString(long handler, int maxCapacity)
    {
        this.maxCapacity = maxCapacity;
        this.handler = handler;
    }

    @Override
    public boolean equals(Object anObject)
    {
        if (this == anObject)
        {
            return true;
        }
        if (anObject instanceof ScriptingString)
        {
            ScriptingString anotherString = (ScriptingString) anObject;
            if (handler == anotherString.handler)
            {
                return true;
            }
            int n = length();
            if (n == anotherString.length())
            {
                int i = 0;
                while (n-- != 0)
                {
                    if (charAt(i) != anotherString.charAt(i))
                        return false;
                    i++;
                }
                return true;
            }
        }
        return false;
    }

    public int capacity()
    {
        return maxCapacity;
    }

    @Override
    public int hashCode()
    {
        int h = 0;
        int count = length();
        if (count > 0)
        {
            for (int i = 0; i < count; i++)
            {
                h = 31 * h + charAt(i);
            }
        }
        return h;
    }

    public native byte charAt(int index);

    public native byte setCharAt(byte ch,int index);

    public native int length();

    @Override
    public native int compareTo(@NonNull ScriptingString o);

    @Override
    public native String toString();

    public native boolean isAlive();
}
