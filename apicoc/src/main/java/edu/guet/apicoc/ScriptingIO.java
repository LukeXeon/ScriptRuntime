package edu.guet.apicoc;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @see ScriptRuntime
 * @see ScriptingIOProcess
 * Created by Mr.小世界 on 2018/11/6.
 */

public interface ScriptingIO
{
    OutputStream getOutputStream();

    InputStream getInputStream();

    InputStream getErrorStream();

    void close() throws Exception;
}
