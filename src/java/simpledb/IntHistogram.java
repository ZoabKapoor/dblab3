package simpledb;

import java.util.Arrays;

import simpledb.Predicate.Op;

/** A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {
	
	private int[] histogram;
	private int maxVal;
	private int minVal;
	private double bucketWidth;
	private int numVals;

    /**
     * Create a new IntHistogram.
     * 
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     *
     * Note: if the number of buckets exceeds the number of distinct integers between min and max, 
     * some buckets may remain empty (don't create buckets with non-integer widths).
     * 
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * 
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't 
     * simply store every value that you see in a sorted list.
     * 
     * @param buckets The number of buckets to split the input value into.
     * @param min The minimum integer value that will ever be passed to this class for histogramming
     * @param max The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
    	if (max < min) {
    		throw new IllegalArgumentException("Max is: " + max + " which is less than min, which is:" + min + "!");
    	} else if (buckets <= 0) {
    		throw new IllegalArgumentException("Can't have a histogram with <= 0 buckets");
    	} else {
    		maxVal = max;
    		minVal = min;
    		numVals = 0;
    		bucketWidth = (max-min+1)*1.0/buckets;
    		histogram = new int[buckets];
    		for (int i = 0; i < histogram.length; ++i) {
    			histogram[i] = 0;
    		}
    	}
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
    	if (v > maxVal || v < minVal) {
    		throw new IllegalArgumentException("Value to add: " + v + " is outside the acceptable range of " + minVal + " <= v <= " + maxVal);
    	} else {
    		int bucket = getBucketIndex(v);
    		histogram[bucket]++;
    		numVals++;
    	}
    }
    
    /**
     * Gets the index for the bucket that an int belongs in.
     * 
     * @param v     The int to get the bucket for.
     * @return      The index of the bucket to put v in.
     */
    private int getBucketIndex(int v) {
    	return (int) ((v-minVal)/bucketWidth);
    }
    
    /**
     * Gets the minimum possible value in the bucket with index bucketNum.
     * 
     * @param bucketNum    The index of the bucket we're considering.
     * @return    The minimum value for that bucket.
     */
    private int getMinInBucket(int bucketNum) {
    	return (int) (Math.ceil(bucketNum*bucketWidth)) + minVal;
    }
    
    /**
     * Gets the maximum possible value in the bucket with index bucketNum.
     * 
     * @param bucketNum    The index of the bucket we're considering.
     * @return    The maximum value for that bucket. 
     */
    private int getMaxInBucket(int bucketNum) {
    	return (getMinInBucket(bucketNum) == getMinInBucket(bucketNum+1) ? 
    			getMinInBucket(bucketNum) : getMinInBucket(bucketNum+1)-1);
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * 
     * For example, if "op" is "GREATER_THAN" and "v" is 5, 
     * return your estimate of the fraction of elements that are greater than 5.
     * 
     * @param op Operator
     * @param v Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
    	switch (op) {
    	case EQUALS:
    	case LIKE:
    		return estimateSelectivityEqual(v);
    	case NOT_EQUALS:
    		return 1.0 - estimateSelectivityEqual(v);
    	case GREATER_THAN:
    		return estimateSelectivityGreater(v);
    	case GREATER_THAN_OR_EQ:
    		return estimateSelectivityEqual(v) + estimateSelectivityGreater(v);
    	case LESS_THAN:
    		return estimateSelectivityLess(v);
    	case LESS_THAN_OR_EQ:
    		return estimateSelectivityLess(v) + estimateSelectivityEqual(v);
    	default:
    		throw new IllegalStateException("Proposed operator: " + op + "isn't valid!");
    	}
    }
    
    /**
     * Estimates the selectivity of a particular operand on this table if the predicate is Op.EQUALS
     * 
     * @param v    Value
     * @return    Predicted selectivity of this value and Op.EQUALS
     */
    private double estimateSelectivityEqual(int v) {
    	if (v < minVal || v > maxVal) {
    		return 0.0;
    	} else {
    		int bucket = getBucketIndex(v);
    		int height = histogram[bucket];
    		int width = getMaxInBucket(bucket)-getMinInBucket(bucket)+1;
    		return height*1.0/(width*numVals);
    	}
    }
    
    /**
     * Estimates the selectivity of a particular operand on this table if the predicate is Op.GREATER_THAN
     * 
     * @param v    Value
     * @return    Predicted selectivity of this value and Op.GREATER_THAN
     */
    private double estimateSelectivityGreater(int v) {
    	if (v > maxVal) {
    		return 0.0;
    	} else if (v < minVal) {
    		return 1.0;
    	} else {
    		double selectivity = 0.0;
    		int bucket = getBucketIndex(v);
    		int height = histogram[bucket];
    		int width = getMaxInBucket(bucket)-getMinInBucket(bucket)+1;
    		selectivity += (getMaxInBucket(bucket)-v)*height*1.0/(width*numVals);
    		for (int i = bucket + 1; i < histogram.length; ++i) {
    			selectivity += histogram[i]*1.0/numVals;
    		}
    		return selectivity;
    	}
    }

    /**
     * Estimates the selectivity of a particular operand on this table if the predicate is Op.LESS_THAN
     * 
     * @param v    Value
     * @return    Predicted selectivity of this value and Op.LESS_THAN
     */
    private double estimateSelectivityLess(int v) {
    	if (v < minVal) {
    		return 0.0;
    	} else if (v > maxVal) {
    		return 1.0;
    	} else {
    		double selectivity = 0.0;
    		int bucket = getBucketIndex(v);
    		int height = histogram[bucket];
    		int width = getMaxInBucket(bucket)-getMinInBucket(bucket)+1;
    		selectivity += (v-getMinInBucket(bucket))*height*1.0/(width*numVals);
    		for (int i = 0; i < bucket; ++i) {
    			selectivity += histogram[i]*1.0/numVals;
    		}
    		return selectivity;
    	}
    }
    
    /**
     * @return
     *     the average selectivity of this histogram.
     *     
     *     This is not an indispensable method to implement the basic
     *     join optimization. It could be used to
     *     implement a more efficient optimization
     *
     * Not necessary for lab 3
     * */
    public double avgSelectivity()
    {
        return 0.5;
    }
    
    /**
     * (Optional) A String representation of the contents of this histogram
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
    	String max = "maximum is: " + maxVal + ", ";
    	String min = "minimum is: " + minVal + ", ";
    	String vals = "histogram values are: " + Arrays.toString(histogram);
        return max + min + vals;
    }
}
