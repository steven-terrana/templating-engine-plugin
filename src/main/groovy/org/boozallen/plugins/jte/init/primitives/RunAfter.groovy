package org.boozallen.plugins.jte.init.primitives

import static java.lang.annotation.RetentionPolicy.RUNTIME
import java.lang.annotation.ElementType
import java.lang.annotation.Target
import java.lang.annotation.Retention

/**
 *
 */
@Retention(RUNTIME)
@Target(ElementType.METHOD)
@interface RunAfter{

    Class<? extends TemplatePrimitiveInjector>[] value();

}
