package org.jw.annotation;

import java.lang.annotation.*;

/**
 * 切面
 * Created by CaiDongYu on 2016/4/11.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aspect {
    Class<? extends Annotation>[] value();
}