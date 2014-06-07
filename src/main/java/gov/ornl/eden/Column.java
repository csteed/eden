package gov.ornl.eden;

import java.io.Serializable;
import java.util.ArrayList;

public class Column implements Serializable {
	private static final long serialVersionUID = 3845044779917948350L;

	private String name;
	private boolean querySet = false;
	private float mean;
	private float median;
	private float variance;
	private float standardDeviation;
	private float maxValue;
	private float minValue;
	private float maxQueryValue;
	private float minQueryValue;
	private float q1;
	private float q3;
	private float lowerWhisker;
	private float upperWhisker;
	private ArrayList<Float> corrCoefs = new ArrayList<Float>();
	private ArrayList<Float> queryCorrCoefs = new ArrayList<Float>();
	private float skewness;
	private float kurtosis;
	private boolean enabled = true;
	private Histogram histogram;
	private Histogram queryHistogram;

	private float queryMin;
	private float queryMax;
	private float queryMean;
	private float queryMedian;
	private float queryVariance;
	private float queryStandardDeviation;
	private float queryQ1;
	private float queryQ3;
	private float querySkewness;
	private float queryKurtosis;
	private float queryUpperWhisker;
	private float queryLowerWhisker;

	public Column() {

	}

	public Column(String name) {
		this.name = name;
	}

	public float getQueryMaxValue() {
		return queryMax;
	}

	public void setQueryMaxValue(float queryMax) {
		this.queryMax = queryMax;
	}

	public float getQueryMinValue() {
		return queryMin;
	}

	public void setQueryMinValue(float queryMin) {
		this.queryMin = queryMin;
	}

	public float getQueryMean() {
		return queryMean;
	}

	public void setQueryMean(float queryMean) {
		this.queryMean = queryMean;
	}

	public float getQueryMedian() {
		return queryMedian;
	}

	public void setQueryMedian(float queryMedian) {
		this.queryMedian = queryMedian;
	}

	public float getQueryVariance() {
		return queryVariance;
	}

	public void setQueryVariance(float queryVariance) {
		this.queryVariance = queryVariance;
	}

	public float getQueryStandardDeviation() {
		return queryStandardDeviation;
	}

	public void setQueryStandardDeviation(float queryStandardDeviation) {
		this.queryStandardDeviation = queryStandardDeviation;
	}

	public float getQueryQ1() {
		return queryQ1;
	}

	public void setQueryQ1(float queryQ1) {
		this.queryQ1 = queryQ1;
	}

	public float getQueryQ3() {
		return queryQ3;
	}

	public void setQueryQ3(float queryQ3) {
		this.queryQ3 = queryQ3;
	}

	public float getQuerySkewness() {
		return querySkewness;
	}

	public void setQuerySkewness(float querySkewness) {
		this.querySkewness = querySkewness;
	}

	public float getQueryKurtosis() {
		return queryKurtosis;
	}

	public void setQueryKurtosis(float queryKurtosis) {
		this.queryKurtosis = queryKurtosis;
	}

	public float getQueryUpperWhisker() {
		return queryUpperWhisker;
	}

	public void setQueryUpperWhisker(float queryUpperWhisker) {
		this.queryUpperWhisker = queryUpperWhisker;
	}

	public float getQueryLowerWhisker() {
		return queryLowerWhisker;
	}

	public void setQueryLowerWhisker(float queryLowerWhisker) {
		this.queryLowerWhisker = queryLowerWhisker;
	}

	public void setHistogram(Histogram histogram) {
		this.histogram = histogram;
	}

	public Histogram getHistogram() {
		return histogram;
	}

	public void setQueryHistogram(Histogram histogram) {
		this.queryHistogram = histogram;
	}

	public Histogram getQueryHistogram() {
		return queryHistogram;
	}

	public float getLowerWhisker() {
		return lowerWhisker;
	}

	public void setLowerWhisker(float lowerWhisker) {
		this.lowerWhisker = lowerWhisker;
	}

	public float getUpperWhisker() {
		return upperWhisker;
	}

	public void setUpperWhisker(float upperWhisker) {
		this.upperWhisker = upperWhisker;
	}

	public float getSkewness() {
		return skewness;
	}

	public void setSkewness(float skewness) {
		this.skewness = skewness;
	}

	public float getKurtosis() {
		return kurtosis;
	}

	public void setKurtosis(float kurtosis) {
		this.kurtosis = kurtosis;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public float getQ3() {
		return q3;
	}

	public void setQ3(float q3) {
		this.q3 = q3;
	}

	public float getQ1() {
		return q1;
	}

	public void setQ1(float q1) {
		this.q1 = q1;
	}

	public float getIQR() {
		return q3 - q1;
	}

	public float getQueryIQR() {
		return queryQ3 - queryQ1;
	}

	public float getVariance() {
		return variance;
	}

	public void setVariance(float variance) {
		this.variance = variance;
	}

	public float getStandardDeviation() {
		return standardDeviation;
	}

	public void setStandardDeviation(float standardDeviation) {
		this.standardDeviation = standardDeviation;
	}

	public float getMean() {
		return mean;
	}

	public void setMean(float mean) {
		this.mean = mean;
	}

	public float getMedian() {
		return median;
	}

	public void setMedian(float median) {
		this.median = median;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isQuerySet() {
		return querySet;
	}

	public void setQueryFlag(boolean querySet) {
		this.querySet = querySet;
	}

	public float getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(float maxValue) {
		this.maxValue = maxValue;
	}

	public float getMinValue() {
		return minValue;
	}

	public void setMinValue(float minValue) {
		this.minValue = minValue;
	}

	public float getMaxQueryValue() {
		return maxQueryValue;
	}

	public void setMaxQueryValue(float maxQueryValue) {
		this.maxQueryValue = maxQueryValue;
	}

	public float getMinQueryValue() {
		return minQueryValue;
	}

	public void setMinQueryValue(float minQueryValue) {
		this.minQueryValue = minQueryValue;
	}

	public void setCorrelationCoefficients(ArrayList<Float> coefs) {
		corrCoefs.clear();
		corrCoefs.addAll(coefs);
	}

	public ArrayList<Float> getCorrelationCoefficients() {
		return corrCoefs;
	}

	public void setQueryCorrelationCoefficients(ArrayList<Float> coefs) {
		queryCorrCoefs.clear();
		queryCorrCoefs.addAll(coefs);
	}

	public ArrayList<Float> getQueryCorrelationCoefficients() {
		return queryCorrCoefs;
	}
}
