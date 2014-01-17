package beast.core;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be used to provide information about parameters in the constructor of a BEAST object,
 * as an alternative way to represent inputs -- still under development!
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {

    /**
     * The name of this parameter
     *
     * @return
     */
    String name();


    /**
     * The description of this parameter
     *
     * @return
     */
    String description();

    /**
     * @return the default value as a string
     */
    String defaultValue() default "";

    boolean optional() default false;

    /**
     * @return true to indicate this description applies to all its inherited classes as well, false otherwise
     */
    boolean isInheritable() default true;
}