package com.faken.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Model {
    /**
     * 指定Controller的模块名
     */
    String value();
}
