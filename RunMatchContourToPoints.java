package benWhitesharkPkg;


// code to show example usage of MatchContourToPoints2
// example uses data generated for image accessible at url:
// 'http://ec2-52-213-8-97.eu-west-1.compute.amazonaws.com/wildbook_data_dir/8/8/88ec3e40-08c2-4d95-addc-5d933562c3dd/S33I83.JPG'
// nb 'fdo', standing for 'fin detection object', is basically synonymous with 'regions'


public class RunMatchContourToPoints {

	public static void main(String[] args) {
		
		// TODO Auto-generated method stub
		double[] regionContourx, regionContoury, finDetectionx, finDetectiony;
		double[] tipyx={0,0}, trailingyx={0,0}, leadingyx={0,0};
		int[] imSzYx;
		
		
		MatchContourToPoints2 m = new MatchContourToPoints2();
		
		
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// IMPORT FIN DETECTION DATA (FDO / REGIONS)
		
		
		// import fin detection data from S3
		S3tools_mctp s3_tools = new S3tools_mctp();
		
		//initialise on ec2 server with iam role attached allowing s3 read / write
		//s3_tools.initialise();
		
		//alternative: initialise using credentials
		//s3_tools.initialise("User@account");

		
		// call S3 import method with inputs: s3_tools, bucket, key
		m.importFdoJsonS3(s3_tools, "sosf", "sampleFdoJson");
		
		
		


		// alternatively call method to import fin detection object json from file with input: path-to-file
		//m.importFdoJsonFile(<path-to-sample-file>);

		
		// alternatively, set fin detection object (currently untested)
		//FinDetectObjects.Regions regions = new FinDetectObjects.Regions();
		//m.setFdo(regions);
		
		
		// alternatively, set regions using json formatted string (currently untested)
		//String fdoInJsonString = "someJsonString";
		//m.setFdoWithJsonString(fdoInJsonString);
		
		
		
		// other alternatives....
		
		

		
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//BASIC USAGE
		
		
		// get image size to match image size at which contours were initially detected
		imSzYx = m.getImSzYx();
		
		// resize image
		
		// collect start of leading edge (le), end of trailing edge (te) and fin tip (tip) keypoint coordinates from user
		
		// set keypoints
		m.setLePt(leadingyx);
		m.setTePt(trailingyx);
		m.setTipPt(tipyx);
		
		// do matching / recompute contours
		m.matchPointsToContours();
		
		
		
		// send fin detection object back to S3
		m.exportFdoJsonS3(s3_tools, "sosf", "sampleFdoJson");
		
		
		
		
		
		
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//'INTERACTIVE' USAGE
		
		
		// get image size to match image size at which contours were initially detected
		imSzYx = m.getImSzYx();
		
		// resize image
		
		// get the currently selected (closed) region contour (to be displayed in one colour on image)
		regionContourx = m.getSelectedRegionContour("x");
		regionContoury = m.getSelectedRegionContour("y");
		
		
		// get the current (open) fin contour detection (to be dislpayed in a different colour)
		finDetectionx = m.getFinDetectionContour("x");
		finDetectiony = m.getFinDetectionContour("y");
		
		
		// check to see if any keypoints already stored - if keypoints, get them and display on image
		if (m.getTipPt().length==0) {
			tipyx = m.getTipPt();
			leadingyx = m.getLePt();
			trailingyx = m.getTePt();
		}
		
		
		
		boolean quitLoop = false;
		
		while (quitLoop == false) {
			
			// collect keypoint coordinates / updated keypoint coordinates from user

			// set keypoints
			m.setLePt(leadingyx);
			m.setTePt(trailingyx);
			m.setTipPt(tipyx);

			// do matching / recompute contours
			m.matchPointsToContours();

			// get the updated (closed) region contour (to be displayed in one colour on image)
			regionContourx = m.getSelectedRegionContour("x");
			regionContoury = m.getSelectedRegionContour("y");

			// get the updated (open) fin contour detection (to be displayed in a different colour)
			finDetectionx = m.getFinDetectionContour("x");
			finDetectiony = m.getFinDetectionContour("y");
			
			
			//display keypoints

			
			// if user is happy with result
			m.setDetectionIsBad(0);
			quitLoop = true;
			
			
			// if user concludes good detection not possible
			m.setDetectionIsBad(1);
			quitLoop = true;
		}
		
		
		// send fin detection object back to S3
		m.exportFdoJsonS3(s3_tools, "sosf", "sampleFdoJson");
		
		// alternatives:
		// write to file
		//m.exportFdoJsonFile(<path-to-file>);
		// return object as json string
		//String fdoInJsonString = m.getFdoAsJsonString();
		// return java object
		//FinDetectObjects.Regions regions = m.getFdo();
		
		
		// finally, update 'state' variable for annotation / media asset / encounter as discussed in Ctrl-h

	}

}






