package edu.guet.apicoc;
/**
 *
 * @see ScriptingIO 实现了此接口的Process对象
 * @see ScriptRuntime
 * Created by Mr.小世界 on 2018/11/10.
 */
public abstract class ScriptingIOProcess
        extends Process
        implements ScriptingIO
{
    @Override
    public void close() throws Exception
    {
        destroy();
    }
}
