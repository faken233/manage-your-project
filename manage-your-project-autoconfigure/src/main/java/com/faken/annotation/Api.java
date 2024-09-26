package com.faken.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Api {
    /**
     * 指定标记的接口的接口名
     */
    String value();
}
