package beast.core;

import java.lang.annotation.*;

/**
 * An annotation used for specifying a BEAST object without needing to extend BEASTObject directly.
 * Useful when one wants to extend a different object, since there is no multiple inheritance in Java.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface BEASTObjectAnnotation {}
