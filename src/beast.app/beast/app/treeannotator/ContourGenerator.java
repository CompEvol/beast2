package beast.app.treeannotator;

import java.util.ArrayList;
import java.util.List;

import beast.base.core.Log;


/**
*  <p> An object used to generate a list of contour lines
*      or paths from a set of gridded three dimensional data.
*  </p>
*
*  <p> Based on contour_plot.c from NeXTcontour1.4 by Thomas H. Pulliam,
*      pulliam@rft29.nas.nasa.gov, MS 202A-1 NASA Ames Research Center,
*      Moffett Field, CA 94035.
*      I don't know how the original Fortran code looked like or where it came from,
*      other than that NeXTcontour1.4 is based on Pieter Bunings' PLOT3D package
*      for Computational Fluid Dynamics.
*  </p>
*
*  <p> Ported from C to Java by Joseph A. Huwaldt, November 16, 2000.  </p>
*
*  <p>  Modified by:  Joseph A. Huwaldt  </p>
*
*  @author  Joseph A. Huwaldt   Date:  November 11, 2000
*  @version November 23, 2000
*
*  @author Marc Suchard
*
**/
public class ContourGenerator {

	//	Debug flag.
	private static final boolean DEBUG = false;

	//	Error messages.
	private static final String kCancelMsg = "Method ContourGenerator.getContours() canceled by user.";
	private static final String kInconsistantArrMsg = "Inconsistant array sizes.";
	private static final String kArrSizeMsg = "Data arrays must have more than one row or column.";
	private static final String kNegLogDataMsg = "Function data must be > 0 for logarithmic intervals.";

	//	Path buffer size.
	private static final int kBufSize = 1000;

	//	The minimum number of points allowed in a contour path.
	private static final int kMinNumPoints = 3;

	//	A list of contour paths.
	private List<ContourPath> pathList = new ArrayList<>();

	//	A flag to indicate that the contours have been computed or not.
	private boolean cCalculated = false;

	//	Data arrays used for generating the contours.
	private double[][] xArray, yArray, funcArray;

	//	Data arrays used when generating contours for 1D X & Y arrays.
	private double[] xArr1D, yArr1D;

	//	Array of contour attributes, one for each contour level.
	private ContourAttrib[] cAttr;

	//	The fraction of the task that is completed.
	private float fracComplete = 0;

	/**
	*  Used to indicate that the user wishes to cancel the calculation
	*  of contours.
	**/
	private boolean isCanceled = false;


	//	Variables in the original FORTRAN program.
	private double[] pathbufxt, pathbufyt;
	private int[] pathbufia;
	private int lnstrt;				//	lnstrt=1 indicates starting a new line.
	private int ignext;
	private int icont;				//	Current contour level index.
	private double cont;			//	The current contour level.
	private int iss, iee, jss, jee;	//	i & j start and end index values.
	private int ima;				//	ima tells which boundary region we are on.
	private int iae;				//	Index to last element in the IA list.
	private int ibeg, jbeg;
	private int gi, gj;				//	Indexes into data arrays.
	private double fij;				//	Data value at i,j in data array.
	private int idir;				//	Indicates current direction.
	private int np=0;				//	Number of points in current contour line.
	private double wx=0, wy=0;		//	Starting point of a contour line.


	/**
	*  Construct a ContourGenerator object using the specified data arrays
	*  and the specified attribute array.  This constructor allows you
	*  to use data on an uneven X, Y grid.
	*
	*  @param  xArr   2D array containing the grid x coordinate data.
	*  @param  yArr   2D array containing the grid y coordinate data.
	*  @param  zArr   2D array containing the grid function (z) data.
	*  @param  cAttr  Array containing attributes of the contour levels.
	**/
	public ContourGenerator(double[][] xArr, double[][] yArr, double[][] zArr, ContourAttrib[] cAttr) {

		//	Make sure input data is reasonable.
		if (yArr.length != xArr.length || yArr.length != zArr.length)
			throw new IllegalArgumentException(kInconsistantArrMsg);
		if (yArr[0].length != xArr[0].length || yArr[0].length != zArr[0].length)
			throw new IllegalArgumentException(kInconsistantArrMsg);
		if (xArr.length <= 1 || xArr[0].length <= 1)
			throw new IllegalArgumentException(kArrSizeMsg);

		this.cAttr = cAttr;
		xArray = xArr;
		yArray = yArr;
		funcArray = zArr;

	}

	/**
	*  Construct a ContourGenerator object using the specified data arrays
	*  and the specified attribute array.  This constructor allows you
	*  to use data on an evenly spaced grid where "X" values are invarient
	*  with "Y" and "Y" values are invarient with "X".  This often occures
	*  where the data is on an evenly spaced cartesian grid.
	*
	*  @param  xArr   1D array containing the grid x coordinate data.
	*  @param  yArr   1D array containing the grid y coordinate data.
	*  @param  zArr   2D array containing the grid function (z) data.
	*  @param  cAttr  Array containing attributes of the contour levels.
	**/
	public ContourGenerator(double[] xArr, double[] yArr, double[][] zArr, ContourAttrib[] cAttr) {

		//	Make sure input data is reasonable.
		if (yArr.length != zArr.length || xArr.length != zArr[0].length)
			throw new IllegalArgumentException(kInconsistantArrMsg);
		if (xArr.length <= 1)
			throw new IllegalArgumentException(kArrSizeMsg);

		this.cAttr = cAttr;
		xArr1D = xArr;
		yArr1D = yArr;
		funcArray = zArr;
	}

	/**
	*  Construct a ContourGenerator object using the specified data arrays.
	*  Contour attributes, including the interval, are generated
	*  automatically.  This constructor allows you to use data on an
	*  uneven X, Y grid.
	*
	*  @param  xArr   2D array containing the grid x coordinate data.
	*  @param  yArr   2D array containing the grid y coordinate data.
	*  @param  zArr   2D array containing the grid function (z) data.
	*  @param  nc     The number of contour levels to generate.
	*  @param  logInterval  Uses a logarithmic contour interval if true, and
	*                       uses a linear interval if false.
	**/
	public ContourGenerator(double[][] xArr, double[][] yArr, double[][] zArr,
								int nc, boolean logInterval) {

		//	Make sure input data is reasonable.
		if (yArr.length != xArr.length || yArr.length != zArr.length)
			throw new IllegalArgumentException(kInconsistantArrMsg);
		if (yArr[0].length != xArr[0].length || yArr[0].length != zArr[0].length)
			throw new IllegalArgumentException(kInconsistantArrMsg);
		if (xArr.length <= 1 || xArr[0].length <= 1)
			throw new IllegalArgumentException(kArrSizeMsg);

		xArray = xArr;
		yArray = yArr;
		funcArray = zArr;

		if (logInterval)
			findLogIntervals(nc);
		else
			findLinearIntervals(nc);
	}

	/**
	*  Construct a ContourGenerator object using the specified data arrays.
	*  Contour attributes, including the interval, are generated
	*  automatically.  This constructor allows you
	*  to use data on an evenly spaced grid where "X" values are invarient
	*  with "Y" and "Y" values are invarient with "X".  This often occures
	*  where the data is on an evenly spaced cartesian grid.
	*
	*  @param  xArr   1D array containing the grid x coordinate data.
	*  @param  yArr   1D array containing the grid y coordinate data.
	*  @param  zArr   2D array containing the grid function (z) data.
	*  @param  nc     The number of contour levels to generate.
	*  @param  logInterval  Uses a logarithmic contour interval if true, and
	*                       uses a linear interval if false.
	**/
	public ContourGenerator(double[] xArr, double[] yArr, double[][] zArr,
								int nc, boolean logInterval) {

		//	Make sure input data is reasonable.
		if (yArr.length != zArr.length || xArr.length != zArr[0].length)
			throw new IllegalArgumentException(kInconsistantArrMsg);
		if (xArr.length <= 1)
			throw new IllegalArgumentException(kArrSizeMsg);

		xArr1D = xArr;
		yArr1D = yArr;
		funcArray = zArr;

		if (logInterval)
			findLogIntervals(nc);
		else
			findLinearIntervals(nc);
	}


	/**
	*  Generate the contour paths and return them as an array
	*  of ContourPath objects. If there is a lot of data, this method
	*  method may take a long time, so be patient.  Progress can be
	*  checked from another thread by calling "getProgress()".
	*
	*  @return An array of contour path objects.
	*  @throws InterruptedException if the user cancels this process
	*          (by calling "cancel()" from another thread).
	**/
	public ContourPath[] getContours() throws InterruptedException {

		if (!cCalculated) {
			isCanceled = false;
			pathList.clear();

			//	Go off an compute the contour paths.
			computeContours();

			//	Now turn loose all our data arrays to be garbage collected.
			cAttr = null;
			xArray = yArray = funcArray = null;
			xArr1D = yArr1D = null;

			//	Set our "done" flags.
			cCalculated = true;
			fracComplete = 1;
		}

		//	Turn our pathList into an array and return the array.
		int size = pathList.size();
		ContourPath[] arr = new ContourPath[size];
		for (int i=0; i < size; ++i)
			arr[i] = pathList.get(i);

		return arr;
	}

	/**
	*  Returns true if the contour generation process is done.  False if it is not.
	**/
	public boolean done() {
		return cCalculated;
	}

	/**
	*  Call this method to cancel the generation of contours.
	**/
	public void cancel() {
		isCanceled = true;
	}

	/**
	*  Returns the progress of the currently executing contour generation
	*  process: 0.0 (just starting) to 1.0 (done).
	**/
	public float getProgress() {
		return fracComplete;
	}


	/**
	*  Find contour intervals that are linearly spaced through the data.
	**/
	private void findLinearIntervals(int nc) {

		//	Find min and max Z values.
		double zMin = Double.MAX_VALUE;
		double zMax = -zMin;
		int ni = funcArray.length;
		for (int i=0; i < ni; ++i) {
			int nj = funcArray[i].length;
			for (int j=0; j < nj; ++j) {
				double zVal = funcArray[i][j];
				zMin = Math.min(zMin, zVal);
				zMax = Math.max(zMax, zVal);
			}
		}

		//	Allocate memory for contour attribute array.
		cAttr = new ContourAttrib[nc];

		//	Determine contour levels.
		double delta = (zMax-zMin)/(nc+1);
		for (int i=0; i < nc; i++) {
			cAttr[i] = new ContourAttrib( zMin + (i+1)*delta );
			if (DEBUG)
				Log.info.println("level[" + i + "] = " + (zMin + (i+1)*delta));
		}

	}

	/**
	*  Find contour intervals that are logarithmically spaced through the data.
	**/
	private void findLogIntervals(int nc) {

		//	Find min and max Z values.
		double zMin = Double.MAX_VALUE;
		double zMax = -zMin;
		int ni = funcArray.length;
		for (int i=0; i < ni; ++i) {
			int nj = funcArray[i].length;
			for (int j=0; j < nj; ++j) {
				double zVal = funcArray[i][j];
				zMin = Math.min(zMin, zVal);
				zMax = Math.max(zMax, zVal);
			}
		}

		if (zMin < 0)
			throw new IllegalArgumentException(kNegLogDataMsg);

		//	Allocate memory for contour attribute array.
		cAttr = new ContourAttrib[nc];

		//	Determine contour levels.
		double temp = Math.log(zMin);
		double delta = (Math.log(zMax) - temp)/(nc+1);
		for (int i=0; i < nc; i++)
			cAttr[i] = new ContourAttrib( Math.exp(temp + (i+1)*delta) );

	}


	/**
	*  Computes contour lines for gridded data and stores information about
	*  those contours.  The result of this routine is a list of contour lines
	*  or paths.
	**/
	private void computeContours() throws InterruptedException {

		int ncont = cAttr.length;		//	Number of contour levels.

		//	Find the number of data points in "I" and "J" directions.
		int nx=0, ny=0;
		if (xArray != null) {
			ny = xArray.length;
			nx = xArray[0].length;
		} else {
			nx = xArr1D.length;
			ny = yArr1D.length;
		}

		//	Allocate temporary storage space for path buffers.
		pathbufxt = new double[kBufSize];
		pathbufyt = new double[kBufSize];
		pathbufia = new int[kBufSize*3];

		//	lnstrt=1 (line start) means we're starting a new line.
		lnstrt = 1;
		ignext = 0;

		//	Loop through each contour level.
		for (icont = 0; icont < ncont; ++icont) {

			//	Check to see if the user has canceled.
			if (isCanceled)
				throw new InterruptedException(kCancelMsg);

			//	Begin working on this contour level.
			cont = cAttr[icont].getLevel();
			iss = 1;
			iee = nx;
			jss = 1;
			jee = ny;

			boolean subDivFlg = false;
/*L110*/	do {
				//	Find where function increases through the contour level.
				FlagContourPassings();

				boolean L10flg = false;
/*L210*/		do {

					if (!L10flg) {
						/*	Search along the boundaries for contour line starts.
						*	IMA tells which boundary of the region we're on.
						*/
						ima = 1;
						ibeg = iss - 1;
						jbeg = jss;
					}

/*L6*/				imaLoop:
					do {

						if (!L10flg) {
							boolean imb = false;
							boolean doneFlg = false;
							do {

								switch(ima) {
									case 1:
										++ibeg;
										if (ibeg == iee)
											ima = 2;
										break;

									case 2:
										++jbeg;
										if (jbeg == jee)
											ima = 3;
										break;

									case 3:
										--ibeg;
										if (ibeg == iss)
											ima = 4;
										break;

									case 4:
										--jbeg;
										if (jbeg == jss)
											ima = 5;
										break;

									case 5:
										continue imaLoop;
								}

								if (funcArray[jbeg -1][ibeg -1] <= cont) {
									imb = true;
									doneFlg = false;

								} else if (imb == true)
									doneFlg = true;

							} while (!doneFlg);

							//	Got a start point.
							gi = ibeg;							//	x index of starting point.
							gj = jbeg;							//	y index of starting point.
							fij = funcArray[jbeg -1][ibeg -1];	//	z value of starting point.

							//	Round the corner if necessary.
							/*	Look different directions to see which way the contour line
							*	went:
							*			  4
							*			1-|-3
							*			  2
							*/
							switch (ima) {
								case 1:
									Routine_L21();
									break;

								case 2:
									if (gj != jss) {
										if (!Routine_L31())
											Routine_L21();
									} else
										Routine_L21();
									break;

								case 3:
									if (gi != iee) {
										if (!Routine_L41())
											Routine_L21();
									} else {
										if (!Routine_L31())
											Routine_L21();
									}
									break;

								case 4:
									if (gj != jee) {
										if (!Routine_L51())
											Routine_L21();
									} else {
										if (!Routine_L41())
											Routine_L21();
									}
									break;

								case 5:
									if (!Routine_L51())
										Routine_L21();
									break;
							}

						}	//	end if(!L10flg)


						//	This is the end of a contour line.  After this, we'll start a
						//	new line.
						L10flg = false;
/*L90*/					lnstrt = 1;						//	Contour line start flag.
						ignext = 0;
						accumContour(np, icont, pathbufxt, pathbufyt, cAttr[icont]);

						//	If we're not done looking along the boundaries,
						//	go look there some more.
					} while (ima != 5);


					//	Otherwise, get the next start out of IA.
/*L91*/				if (iae != 0) {
						int ntmp3 = iae;
						for (int iia = 1; iia <= ntmp3; ++iia) {
							if (pathbufia[iia -1] != 0) {
								//	This is how we start in the middle of the region, using IA.
								gi = pathbufia[iia - 1]/1000;
								gj = pathbufia[iia - 1] - gi*1000;
								fij = funcArray[gj -1][gi -1];
								pathbufia[iia - 1] = 0;

								Routine_L21();

								L10flg = true;
								break;
							}
						}
					}

				} while ( L10flg );

				/*	And if there are no more of these, we're done with this region.
				*   If we've subdivided, update the region pointers and go back for more.
				*/
				subDivFlg = false;
				if (iee == nx) {
					if (jee != ny) {
						jss = jee;
						jee = ny;
						subDivFlg = true;
					}
				} else {
					iss = iee;
					iee = nx;
					subDivFlg = true;
				}

			} while (subDivFlg);


			//	Update progress information.
			fracComplete = (float)(icont+1)/(float)(ncont);

			//	Loop back for the next contour level.
		}	// Next icont


		//	Turn loose temporary arrays used to generate contours.
		pathbufxt = null;
		pathbufyt = null;
		pathbufia = null;

	}


	/**
	*  Flag points in IA where the the function increases through the contour
	*  level, not including the boundaries.  This is so we have a list of at least
	*  one point on each contour line that doesn't intersect a boundary.
	**/
	private void FlagContourPassings() {

		iae = 0;
		int ntmp2 = jee - 1;
		for (int j=jss + 1; j <= ntmp2; ++j) {
			boolean imb = false;
			int iaend = iae;
			int ntmp3 = iee;
			for (int i=iss; i <= ntmp3; ++i) {
				if (funcArray[j -1][i -1] <= cont)
					imb = true;
				else if (imb == true) {
					++iae;
					pathbufia[iae - 1] = i*1000 + j;
					imb = false;

					/*  Check if the IA array is full.  If so, the subdividing
					*   algorithm goes like this:  if we've marked at least one
					*   J row, drop back to the last completed J and call that
					*   the region.  If we haven't even finished one J row, our
					*   region just extends to this I location.
					*/
					if (iae == kBufSize*3) {
						if (j > jss + 1) {
							iae = iaend;
							jee = j;
						} else {
							//	Compute minimum.
							jee = Math.min(j+1, jee);
							iee = i;
						}

						//	Break out of i & j loops.
						return;
					}
				}
			}	//	Next i
		}	//	Next j

	}

	/**
	*  This function represents the block of code in the original
	*  FORTRAN program that comes after line 21.
	**/
	private void Routine_L21() {
		while (true) {
			--gi;
			if (gi < iss)
				return;						//	Goto L90.

			idir = 1;
			if (funcArray[gj -1][gi -1] <= cont) {
				//	Wipe this point out of IA if it's in the list.
/*L52*/			if (iae != 0) {
					int ij = gi*1000 + gj + 1000;
					int ntmp3 = iae;
					for (int iia = 1; iia <= ntmp3; ++iia) {
						if (pathbufia[iia - 1] == ij) {
							pathbufia[iia - 1] = 0;
							break;
						}
					}
				}
				doInterpolation();
				return;						//	Goto L90.
			}

			fij = funcArray[gj -1][gi -1];

			if (Routine_L31())	return;		//	Goto L90
		}
	}

	/**
	*  This function represents the block of code in the original
	*  FORTRAN program that comes after line 31.
	**/
	private boolean Routine_L31() {
		--gj;
		if (gj < jss)
			return true;

		idir = 2;
		if (funcArray[gj -1][gi -1] <= cont) {
			doInterpolation();
			return true;
		}

		fij = funcArray[gj -1][gi -1];

		return (Routine_L41());
	}

	/**
	*  This function represents the block of code in the original
	*  FORTRAN program that comes after line 41.
	**/
	private boolean Routine_L41() {
		++gi;
		if (gi > iee)
			return true;

		idir = 3;
		if (funcArray[gj -1][gi -1] <= cont) {
			doInterpolation();
			return true;
		}

		fij = funcArray[gj -1][gi -1];

		return (Routine_L51());
	}

	/**
	*  This function represents the block of code in the original
	*  FORTRAN program that comes after line 51.
	**/
	private boolean Routine_L51() {
		++gj;
		idir = 4;
		if (gj > jee)
			return true;

		if (funcArray[gj -1][gi -1] <= cont) {
			doInterpolation();
			return true;
		}

		fij = funcArray[gj -1][gi -1];

		return false;
	}

	/**
	*  Do interpolation for X, Y coordinates.
	*
	*  This function represents the block of code in the original
	*  FORTRAN program that comes after line 60.
	**/
	private void doInterpolation() {

		//	Do interpolation for X,Y coordinates.
		double func = funcArray[gj -1][gi -1];
		double xyf = (cont - func)/(fij - func);

		/*  This tests for a contour point coinciding with a grid point.  In this case
		 *  the contour routine comes up with the same physical coordinate twice.  If
		 *  If we don't trap it, it can (in some cases significantly) increase the
		 *  number of points in a contour line.  Also, if this happens on the first
		 *  point in a line, the second point could be misinterpreted as the end of a
		 *   (circling) contour line.
		 */
		if (xyf == 0)
			++ignext;

		double wxx=0, wyy=0;
		double xVal=0, yVal=0;
		if (xArray != null) {
			//	We have 2D arrays for the X & Y grid points.
			xVal = xArray[gj -1][gi -1];
			yVal = yArray[gj -1][gi -1];
			switch (idir) {
				case 1:				//	East
					wxx = xVal + xyf*(xArray[gj -1][gi + 1 -1] - xVal);
					wyy = yVal + xyf*(yArray[gj -1][gi + 1 -1] - yVal);
					break;

				case 2:				//	North
					wxx = xVal + xyf*(xArray[gj + 1 -1][gi -1] - xVal);
					wyy = yVal + xyf*(yArray[gj + 1 -1][gi -1] - yVal);
					break;

				case 3:				//	West
					wxx = xVal + xyf*(xArray[gj -1][gi - 1 -1] - xVal);
					wyy = yVal + xyf*(yArray[gj -1][gi - 1 -1] - yVal);
					break;

				case 4:				//	South
					wxx = xVal + xyf*(xArray[gj - 1 -1][gi -1] - xVal);
					wyy = yVal + xyf*(yArray[gj - 1 -1][gi -1] - yVal);
					break;
			}

		} else {
			//	We have 1D arrays for the X & Y grid points.
			xVal = xArr1D[gi -1];
			yVal = yArr1D[gj -1];
			switch (idir) {
				case 1:				//	East
					wxx = xVal + xyf*(xArr1D[gi + 1 -1] - xVal);
					wyy = yVal;
					break;

				case 2:				//	North
					wxx = xVal;
					wyy = yVal + xyf*(yArr1D[gj + 1 -1] - yVal);
					break;

				case 3:				//	West
					wxx = xVal + xyf*(xArr1D[gi - 1 -1] - xVal);
					wyy = yVal;
					break;

				case 4:				//	South
					wxx = xVal;
					wyy = yVal + xyf*(yArr1D[gj - 1 -1] - yVal);
					break;
			}
		}

		if (DEBUG) {
			Log.info.println("i, j = " + gi + "," + gj);
			Log.info.println("cont = " + (float)cont + ",  fij = " + (float)fij +
									",  func = " + (float)func + ",  xyf = " + (float)xyf);
			Log.info.println("xVal = " + (float)xVal + ",  yVal = " + (float)yVal);
			Log.info.println("wxx = " + (float)wxx + ",  wyy = " + (float)wyy);
		}

		//	Figure out what to do with this point.
		if (lnstrt == 1) {
			//	This is the 1st point in the contour line.

			np = 1;
			pathbufxt[np -1] = wxx;
			pathbufyt[np -1] = wyy;

			//	Save starting point as wx, wy.
			wx = wxx;
			wy = wyy;

			//	Clear the first point flag, we've got one now.
			lnstrt = 0;

		} else {

			boolean skipFlg = false;

			//	Second point and after comes here.
			//	Add a point to this line.  Check for duplicate point first.
			if (ignext == 2) {
				if (wxx == pathbufxt[np -1] && wyy == pathbufyt[np -1]) {
					ignext = 0;
					skipFlg = true;

				} else
					ignext = 1;
			}

			if (!skipFlg) {

				//	Increment # of points in contour.
				++np;
				pathbufxt[np -1] = wxx;
				pathbufyt[np -1] = wyy;

				//	See if the temporary array xt, yt are full.
				if (np == kBufSize) {
					accumContour(np, icont, pathbufxt, pathbufyt, cAttr[icont]);

					//	Last point becomes 1st point to continue.
					pathbufxt[0] = pathbufxt[np -1];
					pathbufyt[0] = pathbufyt[np -1];
					np =1;
				}

				//	Check to see if we're back to the intial point.
				if (wxx == wx && wyy == wy)
					return;
			}

		}

		//	Search for the next point on this line.
/*L67*/		switch(idir) {
			case 1:
				++gi;
				if (!Routine_L51())
					Routine_L21();
				break;

			case 2:
				++gj;
				Routine_L21();
				break;

			case 3:
				--gi;
				if (!Routine_L31())
					Routine_L21();
				break;

			case 4:
				--gj;
				if (!Routine_L41())
					Routine_L21();
				break;
		}

		return;
	}

	/**
	*  Accumulate contour paths, as they are generated, into
	*  an overall list of contours.
	*
	*  @param  np      The number of points in the contour path buffers.
	*  @param  icont   The index to the current contour level.
	*  @param  x,y     Buffers containing x & y coordinates of contour points.
	*  @param  cAttr   The attributes for this particular contour level.
	**/
	private void accumContour(int np, int icont, double[] x, double[] y, ContourAttrib cAttr) {

		//	To few points for a contour line.
		if (np < kMinNumPoints)	return;

		//	Copy over coordinate points from buffers to their own arrays.
		double[] xArr = new double[np];
		double[] yArr = new double[np];
		System.arraycopy(x, 0, xArr, 0, np);
		System.arraycopy(y, 0, yArr, 0, np);

		//	Create a new contour path and add it to the list.
		ContourPath path = new ContourPath(cAttr, icont, xArr, yArr);
		pathList.add(path);

	}

}

