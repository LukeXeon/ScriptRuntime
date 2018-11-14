package edu.guet.apicoc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *registerHandler方法会查找所有添加此注解的非静态方法
 * @see ScriptRuntime#registerHandler(Object)
 * Created by Mr.小世界 on 2018/11/12.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HandlerTarget
{
    /**
     * @return 不能有重名的签名,否则会抛出异常
     */
    String value();
}
