package benWhitesharkPkg;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;

public class MatchContourToPoints2 {

	private FinDetectObjects.Regions regions;
	private double[] selectedRegx, selectedRegy;
	private double[] sec1x, sec2x, sec1y, sec2y, bigx, bigy;
	private Gson gson = new Gson();

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Constructors

	public MatchContourToPoints2() {
	}

	public MatchContourToPoints2(String pathOrUuid, String type) {
		switch (type) {
		case "path":
			this.importFdoJsonFile(pathOrUuid);
			break;
		case "uuid":
			this.doSomethingWithUuid(pathOrUuid);
			break;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// relevant getters and setters
	
	public void setFdo(FinDetectObjects.Regions regions) {
		this.regions = regions;
	}
	
	public String getFdoAsJsonString() {
		return convertRegionsToJsonString();
	}
	
	public FinDetectObjects.Regions getFdo(){
		return this.regions;
	}
	
	public void setFdoWithJsonString(String str) {
		regions = gson.fromJson(str, FinDetectObjects.Regions.class);
	}
	
	
	public void setDetectionIsBad(int val) {
		regions.detectionbad = val;
	}

	public int getDetectionIsBad() {
		return regions.detectionbad;
	}

	public void setTipPt(double[] pt) {
		regions.tippointyx = pt;
	}

	public void setLePt(double[] pt) {
		regions.leadingpointyx = pt;
	}

	public void setTePt(double[] pt) {
		regions.trailingpointyx = pt;
	}

	public double[] getTipPt() {
		return regions.tippointyx;
	}

	public double[] getTePt() {
		return regions.trailingpointyx;
	}

	public double[] getLePt() {
		return regions.leadingpointyx;
	}

	public int[] getImSzYx() {
		return regions.imsizeyx;
	}

	public double[] getFinDetectionContour(String dim) {
		if (dim.equals("x")) {
			return regions.detectedfincontourx;
		} else {
			return regions.detectedfincontoury;
		}
	}

	public double[] getSelectedRegionContour(String dim) {
		if (dim.equals("x")) {
			return selectedRegx;
		} else {
			return selectedRegy;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Import / export fin detection object json
	public String importFdoJsonS3(S3tools_mctp s3_tools, String bucket, String key) {
		String success;
		try {
			regions = gson.fromJson(s3_tools.downloadJsonString(bucket, key), FinDetectObjects.Regions.class);
			this.selectedRegx = regions.closedregionboundariesx.get(regions.selectedregion);
			this.selectedRegy = regions.closedregionboundariesy.get(regions.selectedregion);
			success = "success";
		} catch (Exception e) {
			System.out.println("Error Message: " + e.getMessage());
			e.printStackTrace();
			success = "fail";
		}
		return success;
	}
	
	
	public void exportFdoJsonS3(S3tools_mctp s3_tools, String bucket, String key) {
		String jsonInString = convertRegionsToJsonString();
		s3_tools.uploadJsonString(bucket, key, jsonInString);
	}
	

	public String importFdoJsonFile(String pathToJson) {
		final String EoL = System.getProperty("line.separator");
		final String content;
		String success;
		try {
			List<String> lines = Files.readAllLines(Paths.get(pathToJson), Charset.defaultCharset());
			StringBuilder sb = new StringBuilder();
			for (String line : lines) {
				sb.append(line).append(EoL);
			}
			content = sb.toString();
			regions = gson.fromJson(content, FinDetectObjects.Regions.class);


			this.selectedRegx = regions.closedregionboundariesx.get(regions.selectedregion);
			this.selectedRegy = regions.closedregionboundariesy.get(regions.selectedregion);
			success = "success";

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			success = "fail";
		}
		return success;
	}



	
	public void exportFdoJsonFile(String pathToJson) {
		String jsonInString = convertRegionsToJsonString();
		try (PrintWriter out = new PrintWriter(pathToJson)) {
			out.println(jsonInString);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/*public void exportJson() {
		// take the time of export as the dateModified time: want latest possible
		// time...
		//Date dateNow = new Date();
		//this.regions.datedetectionmodified = dateNow.getTime();

		String jsonInString = convertRegionsToJsonString();
		try (PrintWriter out = new PrintWriter(pathToJson)) {
			out.println(jsonInString);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// utility stuff
	
	
	public String convertRegionsToJsonString() {
		return gson.toJson(regions);
	}


	private void doSomethingWithUuid(String uuid) {
		// TODO Convert image uuid to S3 key and get json from there
	}

	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Matching

	public String matchPointsToContours() {
		String rtn;
		double[] dists;
		double maxDist = 0;
		double[] maxDists = new double[regions.closedregionboundariesx.size()];
		int[] closestTip, closestLe, closestTe;
		closestTip = new int[regions.closedregionboundariesx.size()];
		closestLe = new int[regions.closedregionboundariesx.size()];
		closestTe = new int[regions.closedregionboundariesx.size()];
		MinIdx minIdxTip, minIdxLe, minIdxTe, minIdxReg;

		// if matching is called, we assume that manual editing of detection has taken
		// place....
		this.regions.ismanual = 1;

		// Step 1: C* = min_c max_p dist(Reg_c,keypoint_p)
		// Find region that minimises the maximum distance you'd have to travel to from
		// any keypoint to that region
		try {
			for (int j = 0; j < regions.closedregionboundariesx.size(); j++) {

				maxDist = 0;

				dists = distsPtToContour(regions.tippointyx[1], regions.tippointyx[0],
						regions.closedregionboundariesx.get(j), regions.closedregionboundariesy.get(j));
				minIdxTip = getMinIdx(dists);
				closestTip[j] = minIdxTip.idx;
				if (minIdxTip.min > maxDist)
					maxDist = minIdxTip.min;

				dists = distsPtToContour(regions.leadingpointyx[1], regions.leadingpointyx[0],
						regions.closedregionboundariesx.get(j), regions.closedregionboundariesy.get(j));
				minIdxLe = getMinIdx(dists);
				closestLe[j] = minIdxLe.idx;
				if (minIdxLe.min > maxDist)
					maxDist = minIdxLe.min;

				dists = distsPtToContour(regions.trailingpointyx[1], regions.trailingpointyx[0],
						regions.closedregionboundariesx.get(j), regions.closedregionboundariesy.get(j));
				minIdxTe = getMinIdx(dists);
				closestTe[j] = minIdxTe.idx;
				if (minIdxTe.min > maxDist)
					maxDist = minIdxTe.min;


				maxDists[j] = maxDist;
			}

			minIdxReg = getMinIdx(maxDists);
			regions.selectedregion = minIdxReg.idx;


			// Step2: split region into two open contour parts. Find part that minimises min
			// distance to tip.

			// now need to subsection the region and get best matching part
			int idxOfRegContVertexNearestLePt = closestLe[minIdxReg.idx];
			int idxOfRegContVertexNearestTePt = closestTe[minIdxReg.idx];

			int smallIdx = Math.min(idxOfRegContVertexNearestLePt, idxOfRegContVertexNearestTePt);
			int bigIdx = Math.max(idxOfRegContVertexNearestLePt, idxOfRegContVertexNearestTePt);

			bigx = concatDbl(regions.closedregionboundariesx.get(minIdxReg.idx),
					regions.closedregionboundariesx.get(minIdxReg.idx));
			bigy = concatDbl(regions.closedregionboundariesy.get(minIdxReg.idx),
					regions.closedregionboundariesy.get(minIdxReg.idx));

			sec1x = Arrays.copyOfRange(bigx, smallIdx, bigIdx);
			sec1y = Arrays.copyOfRange(bigy, smallIdx, bigIdx);

			sec2x = Arrays.copyOfRange(bigx, bigIdx,
					regions.closedregionboundariesx.get(minIdxReg.idx).length + bigIdx - sec1x.length);
			sec2y = Arrays.copyOfRange(bigy, bigIdx,
					regions.closedregionboundariesx.get(minIdxReg.idx).length + bigIdx - sec1x.length);

			MinIdx minIdx1, minIdx2;

			dists = distsPtToContour(regions.tippointyx[1], regions.tippointyx[0], sec1x, sec1y);
			minIdx1 = getMinIdx(dists);
			dists = distsPtToContour(regions.tippointyx[1], regions.tippointyx[1], sec2x, sec2y);
			minIdx2 = getMinIdx(dists);

			if (minIdx1.min < minIdx2.min) {
				regions.detectedfincontourx = sec1x;
				regions.detectedfincontoury = sec1y;
			} else {
				regions.detectedfincontourx = sec2x;
				regions.detectedfincontoury = sec2y;
			}

			this.selectedRegx = regions.closedregionboundariesx.get(minIdxReg.idx);
			this.selectedRegy = regions.closedregionboundariesy.get(minIdxReg.idx);

			rtn = "success";
		} catch (Exception e) {
			rtn = "fail";

		}
		return rtn;
	}

	private double[] concatDbl(double[] a, double[] b) {
		int aLen = a.length;
		int bLen = b.length;
		double[] c = new double[aLen + bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

	private MinIdx getMinIdx(double[] vals) {
		MinIdx rtn = new MinIdx();
		rtn.min = 999999999;
		rtn.idx = -1;

		for (int j = 0; j < vals.length; j++) {
			if (vals[j] <= rtn.min) {
				rtn.min = vals[j];
				rtn.idx = j;
			}
		}
		return rtn;
	}

	private double[] distsPtToContour(double ptx, double pty, double[] regionsx, double[] regionsy) {
		double[] dists = new double[regionsx.length];
		double dx, dy;
		for (int j = 0; j < regionsx.length; j++) {
			dx = regionsx[j] - ptx;
			dy = regionsy[j] - pty;
			dists[j] = Math.sqrt((dx * dx) + (dy * dy));
		}

		return dists;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// For testing only

	public void setRegion(double[] x, double[] y) {
		this.selectedRegx = x;
		this.selectedRegy = y;
	}

	public void setDetection(double[] x, double[] y) {
		regions.detectedfincontourx = x;
		regions.detectedfincontoury = y;
	}

	public void setRegionWithIndex(int idx) {
		this.selectedRegx = regions.closedregionboundariesx.get(idx);
		this.selectedRegy = regions.closedregionboundariesy.get(idx);
	}

	public void addRegion(double[] x, double[] y) {
		System.out.println("received contour of length " + x.length);
		regions.closedregionboundariesx.add(x);
		regions.closedregionboundariesy.add(y);
	}

	public double[] getContour(String str) {
		switch (str) {
		case "bigx":
			return bigx;
		case "bigy":
			return bigy;
		case "sec1x":
			return sec1x;
		case "sec1y":
			return sec1y;
		case "sec2x":
			return sec2x;
		case "sec2y":
			return sec2y;
		}
		double[] blank = { -1 };
		return blank;
	}
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class MinIdx {
	double min;
	int idx;
}

class FinDetectObjects {
	static class Regions {
		int[] imsizeyx;
		String fininstancequality;
		long datedetectionmodified;
		int detectionbad;
		double[] tippointyx, leadingpointyx, trailingpointyx;
		int ismanual, selectedregion;
		List<double[]> closedregionboundariesy, closedregionboundariesx;
		double[] detectedfincontoury, detectedfincontourx, autofincontoury, autofincontourx;
	}
}
