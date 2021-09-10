package beast.app.treeannotator;


/**
*  <p> This object represents the attributes assigned to a
*      contour path.  Typically, the same attributes are
*      assigned to all the contour paths of a given contour
*      level.
*  </p>
*
*  <p> Right now, the only attribute used is "level", but
*      in the future I may add more.
*  </p>
*
*  <p>  Modified by:  Joseph A. Huwaldt  </p>
*
*  @author  Joseph A. Huwaldt   Date:  November 11, 2000
*  @version November 17, 2000
*
*
*  @author Marc Suchard
**/
public class ContourAttrib implements Cloneable, java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//	The level (altitude) of a contour path.
	private double level;


	/**
	*  Create a contour attribute object where only
	*  the contour level is specified.
	**/
	public ContourAttrib(double level) {
		this.level = level;
	}

	/**
	*  Return the level stored in this contour attribute.
	**/
	public double getLevel() {
		return level;
	}

	/**
	*  Set or change the level stored in this contour attribute.
	**/
	public void setLevel(double level) {
		this.level = level;
	}

	/**
	*  Make a copy of this ContourAttrib object.
	*
	*  @return  Returns a clone of this object.
	**/
	@Override
	public Object clone() {
		ContourAttrib newObject = null;

		try {
			// Make a shallow copy of this object.
			newObject = (ContourAttrib) super.clone();

			// There is no "deep" data to be cloned.

		} catch (CloneNotSupportedException e) {
			// Can't happen.
			e.printStackTrace();
		}

		// Output the newly cloned object.
		return newObject;
	}

}

