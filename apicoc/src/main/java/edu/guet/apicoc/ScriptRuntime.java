package edu.guet.apicoc;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;


/**
 * C Script 脚本运行时
 * 支持的基本类型
 * @see String boolean int long double float char short byte void
 * Created by Mr.小世界 on 2018/10/31.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@SuppressWarnings("JniMissingFunction")
public final class ScriptRuntime
        implements AutoCloseable,
        ScriptingIO
{
    private final static String TAG = "ScriptRuntime";

    private final static Map<Class<?>, String> SCRIPT_PARAMETER_TYPE
            = new HashMap<Class<?>, String>()
    {
        {
            put(String.class, "char *");
            put(boolean.class, "bool");
            put(int.class, "int");
            put(long.class, "long");
            put(double.class, "double");
            put(float.class, "float");
            put(char.class, "unsigned short");
            put(short.class, "short");
            put(byte.class, "char");
        }
    };

    private final static Map<Class<?>, String> SCRIPT_RETURN_TYPE
            = new HashMap<Class<?>, String>()
    {
        {
            put(void.class, "void");
            put(String.class, "char *");
            put(boolean.class, "boolean");
            put(int.class, "int");
            put(long.class, "long");
            put(double.class, "double");
            put(float.class, "float");
            put(char.class, "unsigned short");
            put(short.class, "short");
            put(byte.class, "char");
        }
    };

    private final static Map<Class<?>, String> SCRIPT_TYPE_SIG
            = new HashMap<Class<?>, String>()
    {
        {
            put(void.class, "V");
            put(boolean.class, "Z");
            put(int.class, "I");
            put(long.class, "J");
            put(double.class, "D");
            put(float.class, "F");
            put(char.class, "C");
            put(short.class, "S");
            put(byte.class, "B");
            put(String.class, "L");
        }
    };

    private List<MethodHandler> methodHandlers = new ArrayList<>();

    private PipedInputStream stderr;
    private PipedInputStream stdout;
    private PipedOutputStream stdin;

    /**
     * 本地对象的指针
     */
    private long handler;

    static
    {
        System.loadLibrary("apicoc");
    }

    /**
     * 使用无参构造函数,构造一个新的解释器实例
     */
    public ScriptRuntime()
    {
        AccessController.doPrivileged(new PrivilegedAction<Void>()
        {
            @Override
            public Void run()
            {
                FileDescriptor stdinFd = new FileDescriptor();
                FileDescriptor stdoutFd = new FileDescriptor();
                FileDescriptor stderrFd = new FileDescriptor();
                handler = init0(stdinFd, stdoutFd, stderrFd);
                stdin = new PipedOutputStream(stdinFd);
                stdout = new PipedInputStream(stdoutFd);
                stderr = new PipedInputStream(stderrFd);
                return null;
            }
        });
    }

    /**
     * @return 返回解释器的stdin输出端
     */
    @Override
    public OutputStream getOutputStream()
    {
        return stdin;
    }

    /**
     * @return 返回解释器的stdout输入端
     */
    @Override
    public InputStream getInputStream()
    {
        return stdout;
    }

    /**
     * @return 返回解释器的stderr输出端
     */
    @Override
    public InputStream getErrorStream()
    {
        return stderr;
    }

    /**
     * 在子进程中启动解释器
     * @param files 要执行的文件
     * @param args 调用时的参数
     * @return 返回执行解释器的子进程
     * @see ScriptingIOProcess
     */
    public static ScriptingIOProcess exec(File[] files, String[] args)
    {
        List<String> fileList = null;
        List<String> argList = null;
        if (files != null)
        {
            fileList = new ArrayList<>();
            for (File file : files)
            {
                if (file != null && file.exists() && file.isFile())
                {
                    fileList.add(file.getAbsolutePath());
                }
            }
        } else
        {
            fileList = Collections.emptyList();
        }
        if (fileList.size() == 0)
        {
            throw new RuntimeException();
        }
        if (args != null)
        {
            argList = new ArrayList<>();
            for (String arg : args)
            {
                if (TextUtils.isEmpty(arg))
                {
                    argList.add(arg);
                }
            }
        } else
        {
            argList = Collections.emptyList();
        }
        return new ScriptingProcess(ScriptingProcess.FILE_MODE,
                fileList.toArray(new String[fileList.size()]),
                argList.toArray(new String[argList.size()]));
    }

    /**
     * 在子进程中启动解释器
     * @param scripts 要执行的源代码
     * @param args 调用时的参数
     * @return 返回执行解释器的子进程
     * @see ScriptingIOProcess
     */
    public static ScriptingIOProcess exec(String[] scripts, String[] args)
    {
        List<String> scriptList = null;
        List<String> argList = null;
        if (scripts != null)
        {
            scriptList = new ArrayList<>();
            for (String script : scripts)
            {
                if (TextUtils.isEmpty(script))
                {
                    scriptList.add(script);
                }
            }
        } else
        {
            scriptList = Collections.emptyList();
        }
        if (args != null)
        {
            argList = new ArrayList<>();
            for (String arg : args)
            {
                if (TextUtils.isEmpty(arg))
                {
                    argList.add(arg);
                }
            }
        } else
        {
            argList = Collections.emptyList();
        }
        return new ScriptingProcess(ScriptingProcess.SOURCE_MODE,
                scriptList.toArray(new String[scriptList.size()]),
                argList.toArray(new String[argList.size()]));
    }

    /**
     * 直接执行一段脚本代码,此方法只能以同步的方式执行
     * @param source 要执行的一段代码
     * @return 返回是否执行成功
     */
    @WorkerThread
    public synchronized boolean doSomething(final String source)
    {
        selfCheck();
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>()
        {
            @Override
            public Boolean run()
            {
                return doSomething0(handler, source);
            }
        });
    }

    /**
     * 注册所有有@HandlerTarget注解修饰的非静态方法
     * @param target 注册的目标对象
     * @see HandlerTarget
     * @exception IllegalArgumentException 非法参数
     */
    public synchronized void registerHandler(Object target)
    {
        selfCheck();
        Pattern pattern = Pattern.compile("[_a-zA-z][_a-zA-z0-9]*");
        Method[] methods = target.getClass().getMethods();
        if (methods != null && methods.length != 0)
        {
            for (Method method : methods)
            {
                HandlerTarget handlerTarget
                        = method.getAnnotation(HandlerTarget.class);
                if (Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers())
                        && handlerTarget != null)
                {
                    String name = handlerTarget.value();
                    if (!pattern.matcher(TextUtils.isEmpty(name)
                            ? Objects.requireNonNull(name) : name)
                            .matches()
                            && !containsName0(handler, name))
                    {
                        throw new IllegalArgumentException("this value is illegal '" + name + "'");
                    }
                    if (!SCRIPT_RETURN_TYPE.containsKey(method.getReturnType()))
                    {
                        throw new IllegalArgumentException(method.getReturnType().getName());
                    }
                    for (Class<?> pramType : method.getParameterTypes())
                    {
                        if (void.class.equals(pramType)
                                || !SCRIPT_PARAMETER_TYPE.containsKey(pramType))
                        {
                            throw new IllegalArgumentException(pramType.getName());
                        }
                    }
                    methodHandlers.add(new MethodHandler(target, method));
                    registerHandler0(handler, generateScript(name, methodHandlers.size() - 1, method));
                }
            }
        }
    }

    /**
     * @return 返回已分配的native堆大小以byte为单位
     */

    /**
     *关闭解释器并清理资源
     * @throws IOException
     */
    @Override
    public synchronized void close() throws IOException
    {
        if (handler == 0)
        {
            return;
        }
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>()
            {
                @Override
                public Void run() throws IOException
                {
                    methodHandlers.clear();
                    methodHandlers = null;
                    stdin.processExited();
                    stdout.processExited();
                    stderr.processExited();
                    close0(handler);
                    handler = 0;
                    return null;
                }
            });
        } catch (PrivilegedActionException e)
        {
            throw (IOException) e.getException();
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        close();
    }

    private static final class MethodHandler
    {
        private final Method method;
        private final Object target;

        private MethodHandler(Object target, Method method)
        {
            this.target = target;
            this.method = method;
        }
    }

    private static final class ScriptingProcess extends ScriptingIOProcess
    {
        private final static int SOURCE_MODE = 0;
        private final static int FILE_MODE = 1;

        private static final String TAG = "ScriptingProcess";
        private static final ExecutorService processReaperExecutor
                = AccessController.doPrivileged(new PrivilegedAction<ExecutorService>()
        {
            @Override
            public ExecutorService run()
            {
                return Executors.newCachedThreadPool();
            }
        });

        private int pid;
        private int exitCode;
        private boolean hasExited;
        private PipedInputStream stderr;
        private PipedInputStream stdout;
        private PipedOutputStream stdin;

        private ScriptingProcess(final int mode,
                                 final String[] srcOrFile,
                                 final String[] args)
        {
            AccessController.doPrivileged(new PrivilegedAction<Object>()
            {
                @Override
                public Object run()
                {
                    FileDescriptor stdinFd = new FileDescriptor();
                    final FileDescriptor stdoutFd = new FileDescriptor();
                    FileDescriptor stderrFd = new FileDescriptor();
                    pid = createSub0(mode, srcOrFile, args, stdinFd, stdoutFd, stderrFd);
                    stdin = new PipedOutputStream(stdinFd);
                    stdout = new PipedInputStream(stdoutFd);
                    stderr = new PipedInputStream(stderrFd);
                    processReaperExecutor.execute(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            int exitCode = waitSub0(pid);
                            hasExited = true;
                            synchronized (ScriptingProcess.this)
                            {
                                hasExited = true;
                                ScriptingProcess.this.exitCode = exitCode;
                                ScriptingProcess.this.notifyAll();
                            }
                            stdin.processExited();
                            stdout.processExited();
                            stderr.processExited();
                        }
                    });
                    return null;
                }
            });
        }

        @Override
        public OutputStream getOutputStream()
        {
            return stdin;
        }

        @Override
        public InputStream getInputStream()
        {
            return stdout;
        }

        @Override
        public InputStream getErrorStream()
        {
            return stderr;
        }

        @Override
        public synchronized int waitFor() throws InterruptedException
        {
            while (!hasExited)
            {
                wait();
            }
            return exitCode;
        }

        @Override
        public synchronized int exitValue()
        {
            if (!hasExited)
            {
                throw new IllegalThreadStateException("process hasn't exited");
            }
            return exitCode;
        }

        @Override
        public void destroy()
        {
            synchronized (this)
            {
                if (!hasExited)
                {
                    killSub0(pid);
                }
            }
            try
            {
                stdin.close();
            } catch (IOException ignored)
            {
            }
            try
            {
                stdout.close();
            } catch (IOException ignored)
            {
            }
            try
            {
                stderr.close();
            } catch (IOException ignored)
            {
            }
        }
    }

    private static class PipedOutputStream extends BufferedOutputStream
    {
        private PipedOutputStream(FileDescriptor fileDescriptor)
        {
            super(formFd(fileDescriptor));
        }

        private static FileOutputStream formFd(final FileDescriptor fileDescriptor)
        {
            return AccessController.doPrivileged(new PrivilegedAction<FileOutputStream>()
            {
                @Override
                public FileOutputStream run()
                {
                    try
                    {
                        return FileOutputStream.class
                                .getConstructor(FileDescriptor.class, boolean.class)
                                .newInstance(fileDescriptor, true);
                    } catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        private synchronized void processExited()
        {
            OutputStream out = this.out;
            if (out != null)
            {
                try
                {
                    out.close();
                } catch (IOException ignored)
                {
                    // We know of no reason to get an IOException, but if
                    // we do, there's nothing else to do but carry on.
                }
                this.out = NullOutputStream.INSTANCE;
            }
        }
    }

    private static class PipedInputStream extends BufferedInputStream
    {
        private PipedInputStream(FileDescriptor fileDescriptor)
        {
            super(formFd(fileDescriptor));
        }

        private static FileInputStream formFd(final FileDescriptor fileDescriptor)
        {
            return AccessController.doPrivileged(new PrivilegedAction<FileInputStream>()
            {
                @Override
                public FileInputStream run()
                {
                    try
                    {
                        return FileInputStream.class.getConstructor(FileDescriptor.class, boolean.class)
                                .newInstance(fileDescriptor, true);
                    } catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        private static byte[] drainInputStream(InputStream in)
                throws IOException
        {
            if (in == null) return null;
            int n = 0;
            int j;
            byte[] a = null;
            while ((j = in.available()) > 0)
            {
                a = (a == null) ? new byte[j] : Arrays.copyOf(a, n + j);
                n += in.read(a, n, j);
            }
            return (a == null || n == a.length) ? a : Arrays.copyOf(a, n);
        }

        private synchronized void processExited()
        {
            // Most BufferedInputStream methods are synchronized, but close()
            // is not, and so we have to handle concurrent racing close().
            try
            {
                InputStream in = this.in;
                if (in != null)
                {
                    byte[] stragglers = drainInputStream(in);
                    in.close();
                    this.in = (stragglers == null) ? NullInputStream.INSTANCE :
                            new ByteArrayInputStream(stragglers);
                    if (buf == null) // asynchronous close()?
                    {
                        this.in = null;
                    }
                }
            } catch (IOException ignored)
            {
                // probably an asynchronous close().
            }
        }
    }

    private static class NullInputStream extends InputStream
    {
        private static final NullInputStream INSTANCE = new NullInputStream();

        private NullInputStream()
        {
        }

        @Override
        public int read()
        {
            return -1;
        }

        @Override
        public int available()
        {
            return 0;
        }
    }

    private static class NullOutputStream extends OutputStream
    {
        private static final NullOutputStream INSTANCE = new NullOutputStream();

        private NullOutputStream()
        {
        }

        @Override
        public void write(int b) throws IOException
        {
            throw new IOException("Stream closed");
        }
    }

    private void selfCheck()
    {
        if (handler == 0)
        {
            throw new IllegalStateException("is close");
        }
    }

    private String generateScript(String name, int handlerIndex, Method method)
    {
        StringBuilder builder = new StringBuilder(128);
        Class<?> returnType = method.getReturnType();
        builder.append(SCRIPT_RETURN_TYPE
                .get(returnType))
                .append(' ')
                .append(name)
                .append('(');
        Class<?>[] pramType = method.getParameterTypes();
        if (pramType != null && pramType.length != 0)
        {
            for (int index = 0; index < pramType.length; index++)
            {
                builder.append(SCRIPT_PARAMETER_TYPE
                        .get(pramType[index]))
                        .append(' ')
                        .append('_')
                        .append(index);
                if (index != pramType.length - 1)
                {
                    builder.append(',');
                }
            }
        }
        builder.append(')').append('{');
        //返回值
        if (returnType.equals(void.class))
        {
            builder.append("__internal_call((void*)")
                    .append(handler)
                    .append(',')
                    .append(handlerIndex)
                    .append(',')
                    .append('\"')
                    .append(SCRIPT_TYPE_SIG.get(returnType));
            if (pramType != null && pramType.length != 0)
            {
                for (Class<?> item : pramType)
                {
                    builder.append(SCRIPT_TYPE_SIG.get(item));
                }
            }
            builder.append('\"')
                    .append(',')
                    .append("NULL")
                    .append(',');
        } else
        {
            builder.append(SCRIPT_RETURN_TYPE.get(returnType))
                    .append(' ')
                    .append('_')
                    .append('r')
                    .append(';')
                    .append("__internal_call((void*)")
                    .append(handler)
                    .append(',')
                    .append(handlerIndex)
                    .append(',')
                    .append('\"')
                    .append(SCRIPT_TYPE_SIG.get(returnType));
            if (pramType != null && pramType.length != 0)
            {
                for (Class<?> item : pramType)
                {
                    builder.append(SCRIPT_TYPE_SIG.get(item));
                }
            }
            builder.append('\"')
                    .append(',')
                    .append('&')
                    .append('_')
                    .append('r')
                    .append(',');
        }
        //参数值
        if (pramType != null && pramType.length != 0)
        {
            for (int index = 0; index < pramType.length; index++)
            {
                builder.append('_')
                        .append(index);
                if (index != pramType.length - 1)
                {
                    builder.append(',');
                }
            }
        }
        if (returnType.equals(void.class))
        {
            builder.append(')')
                    .append(';')
                    .append('}');
        } else
        {
            builder.append(')')
                    .append(';')
                    .append("return")
                    .append(' ')
                    .append('_')
                    .append('r')
                    .append(';')
                    .append('}');
        }
        Log.i(TAG, "generateScript: " + builder);
        return builder.toString();
    }

    private Object onInvoke(int index, Object[] args)
    {
        selfCheck();
        try
        {
            MethodHandler methodHandler = methodHandlers.get(index);
            return methodHandler.method.invoke(methodHandler.target, args);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static native int createSub0(int mode,
                                         String[] srcOrFile,
                                         String[] args,
                                         FileDescriptor stdinFd,
                                         FileDescriptor stdoutFd,
                                         FileDescriptor stderrFd);

    private static native boolean containsName0(long handler, String name);

    private static native int waitSub0(int pid);

    private static native void killSub0(int pid);

    private native long init0(FileDescriptor stdinFd,
                              FileDescriptor stdoutFd,
                              FileDescriptor stderrFd);

    private static native void close0(long handler);

    private static native boolean doSomething0(long handler, String source);

    private static native void registerHandler0(long handler, String registerText);

}