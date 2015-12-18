package beast.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used for specifying a BEAST object without needing to extend BEASTObject directly.
 * Useful when one wants to extend a different object, since there is no multiple inheritance in Java.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface BEASTObjectAnnotation {}
