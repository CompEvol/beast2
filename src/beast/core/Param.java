package beast.core;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be used to provide information about parameters in the constructor of a BEAST object,
 * as an alternative way to represent inputs.
 * 
 * Note that any object with a constructor with Param annotations should also have a 
 * public default constructor without arguments to facilitate cloning models in BEAUti.
 * 
 * Furthermore, every Param annotation should come with a public getter and setter, using 
 * camelcase for name, with annotation CTor(@Param(name="shape",...) double shape) these 
 * getter and setter signatures should be in the class: 
 * 
 * public double getShape() 
 * public void setShape(double shape)
 * 
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {

    /**
     * The name of this parameter, typically the same as the name
     * of the constructor argument.
     */
    String name();


    /**
     * The description of this parameter. Must be specified and contain
     * at least 4 words to pass the unit tests.
     */
    String description();

    /**
     * @return the default value as a string. An attempt is made to 
     * convert the String value defaultValue() to the type associated 
     * with the constructor argument.
     */
    String defaultValue() default "";

	/**
	 * Indicates the value can be omitted, in which case the default 
	 * value is used (an attempt is made to convert the String value 
	 * defaultValue() to the type associated with the constructor 
	 * argument).
	 * 
	 * Note that if there are different constructors and an argument 
	 * does not appear in the other constructor it has to be marked 
	 * as optional.
	 */
    boolean optional() default false;

    /**
     * @return true to indicate this description applies to all its 
     * inherited classes as well, false otherwise
     */
    boolean isInheritable() default true;
}