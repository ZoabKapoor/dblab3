package simpledb;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query. 
 * 
 * This class is not needed in implementing lab1 and lab2.
 */
public class TableStats {

    private static final ConcurrentHashMap<String, TableStats> statsMap = new ConcurrentHashMap<String, TableStats>();

    static final int IOCOSTPERPAGE = 1000;

    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }

    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }
    
    public static void setStatsMap(HashMap<String,TableStats> s)
    {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }

    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();

        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }

    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     */
    static final int NUM_HIST_BINS = 100;
    
    private int ioCostPerPage;
    private int totalTuples;
    private TupleDesc tableDesc;
    private Object[] histograms;
    
    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     * 
     * @param tableid
     *            The table over which to compute statistics
     * @param ioCostPerPage
     *            The cost per page of IO. This doesn't differentiate between
     *            sequential-scan IO and disk seeks.
     */
    // Implementation algorithm: Go through entire table once to populate Range[], then go
    // through table again to populate Object[] (that contains histograms). 
    public TableStats(int tableid, int ioCostPerPage) {
    	this.ioCostPerPage = ioCostPerPage;
    	Transaction t = new Transaction(); 
    	t.start(); 
    	try {
        	SeqScan scan1 = new SeqScan(t.getId(), tableid, "t");
        	tableDesc = scan1.getTupleDesc();
        	int numHistograms = tableDesc.numFields();
        	totalTuples = 0;
        	Range[] ranges = new Range[numHistograms];
    		scan1.open();
    		while (scan1.hasNext()) {
    			Tuple next = scan1.next();
    			totalTuples++;
    			for (int i = 0; i < ranges.length; ++i) {
    				Field f = next.getField(i);
    				if (f instanceof IntField) {
    					int toAdd = ((IntField) f).getValue();
    					if (!(ranges[i] instanceof Range)) {
    						ranges[i] = new Range(toAdd, toAdd);
    					} else {
    						if (toAdd > ranges[i].max) {
    							ranges[i].max = toAdd;
    						} 
    						if (toAdd < ranges[i].min) {
    							ranges[i].min = toAdd;
    						}
    					}
    				}
    			}
    		}
        	scan1.close();
        	histograms = new Object[numHistograms];
        	for (int i = 0; i < histograms.length; ++i) {
        		if (tableDesc.getFieldType(i).equals(Type.INT_TYPE)) {
        			histograms[i] = new IntHistogram(NUM_HIST_BINS, ranges[i].min, ranges[i].max);
        		} else if (tableDesc.getFieldType(i).equals(Type.STRING_TYPE)) {
        			histograms[i] = new StringHistogram(NUM_HIST_BINS);
        		}
        	}
        	SeqScan scan2 = new SeqScan(t.getId(), tableid, "t");
        	scan2.open();
        	while (scan2.hasNext()) {
        		Tuple next = scan2.next();
        		for (int i = 0; i < histograms.length; ++i) {
        			if (tableDesc.getFieldType(i).equals(Type.INT_TYPE)) {
        				int intToAdd = ((IntField) next.getField(i)).getValue();
        				IntHistogram hist = (IntHistogram) histograms[i];
        				hist.addValue(intToAdd);
        				histograms[i] = hist;
        			} else if (tableDesc.getFieldType(i).equals(Type.STRING_TYPE)) {
        				String stringToAdd = ((StringField) next.getField(i)).getValue();
        				StringHistogram hist = (StringHistogram) histograms[i];
        				hist.addValue(stringToAdd);
        				histograms[i] = hist;
        			}
        		}
        	}
        	scan2.close();
    	} catch (Exception e) {
    		throw new RuntimeException("Couldn't get the data needed from table with id: " + tableid);
    	}
    	try {
			t.commit();
		} catch (IOException e) {
			// tried throwing a DbException but apparently those have to be caught
			throw new RuntimeException("Couldn't commit the transaction: " + t);
		}
    }

    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * 
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     * 
     * @return The estimated cost of scanning the table.
     */
    public double estimateScanCost() {
        return (double) totalTuples*ioCostPerPage;
    }

    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     * 
     * @param selectivityFactor
     *            The selectivity of any predicates over the table
     * @return The estimated cardinality of the scan with the specified
     *         selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
        return (int) (totalTuples*selectivityFactor);
    }

    /**
     * The average selectivity of the field under op.
     * @param field
     *        the index of the field
     * @param op
     *        the operator in the predicate
     * The semantic of the method is that, given the table, and then given a
     * tuple, of which we do not know the value of the field, return the
     * expected selectivity. You may estimate this value from the histograms.
     *
     * Not necessary for lab 3
     * */
    public double avgSelectivity(int field, Predicate.Op op) {
        return 0.5;
    }

    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     * 
     * @param field
     *            The field over which the predicate ranges
     * @param op
     *            The logical operation in the predicate
     * @param constant
     *            The value against which the field is compared
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     *         predicate
     */
    public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
        if (tableDesc.getFieldType(field).equals(Type.INT_TYPE)) {
        	IntHistogram hist = (IntHistogram) histograms[field];
        	return hist.estimateSelectivity(op, ((IntField) constant).getValue());
        } else if (tableDesc.getFieldType(field).equals(Type.STRING_TYPE)) {
        	StringHistogram hist = (StringHistogram) histograms[field];
        	return hist.estimateSelectivity(op, ((StringField) constant).getValue());
        } else {
        	throw new Error("Field num: " + field + " has a type that is neither int nor field!");
        }
    }

    /**
     * return the total number of tuples in this table
     * */
    public int totalTuples() {
        return totalTuples;
    }
    
    private class Range {
    	
    	private int max;
    	private int min;
    	
    	public Range(int min, int max) {
    		this.min = min;
    		this.max = max;
    	}
    }

}
