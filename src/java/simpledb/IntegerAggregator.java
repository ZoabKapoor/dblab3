package simpledb;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    
    private Op operator;
    private TupleDesc resultTupleDesc;
    private ConcurrentHashMap<Field,Integer> aggregateNumsWithGrouping;
    private ConcurrentHashMap<Field,Integer> aggregateDenomsWithGrouping;
    private Integer aggregateNumNoGrouping;
    private Integer aggregateDenomNoGrouping;
    private int groupByFieldNum;
    private int aggregateFieldNum;
    private ArrayList<Tuple> resultTuples;

    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
    	aggregateFieldNum = afield;
    	groupByFieldNum = gbfield;
    	operator = what;
    	if (gbfield != Aggregator.NO_GROUPING) {
    		resultTupleDesc = new TupleDesc( new Type[] {gbfieldtype, Type.INT_TYPE});
    		aggregateNumsWithGrouping = new ConcurrentHashMap<Field,Integer>();
    		if (what.equals(Op.AVG)) {
        		aggregateDenomsWithGrouping = new ConcurrentHashMap<Field,Integer>();
    			}
    		} else {
    		resultTupleDesc = new TupleDesc( new Type[] {Type.INT_TYPE});
    		aggregateNumNoGrouping = null;
    		if (what.equals(Op.AVG)) {
    			aggregateDenomNoGrouping = 0;
    		}
    	}
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
		int toAdd = ((IntField) tup.getField(aggregateFieldNum)).getValue();
    	if (groupByFieldNum == Aggregator.NO_GROUPING) {
    		if (aggregateNumNoGrouping == null) {
    			if (operator.equals(Op.COUNT)) {
    				aggregateNumNoGrouping = 1;
    			} else {
    				aggregateNumNoGrouping = ((IntField) tup.getField(aggregateFieldNum)).getValue();
    			}
    		} else {
    			int currentAgg = aggregateNumNoGrouping;
    			switch (operator) {
    			case AVG:
    				aggregateDenomNoGrouping++;
    			case SUM:
    				aggregateNumNoGrouping += toAdd;
    				break;
    			case COUNT:
    				aggregateNumNoGrouping++;
    				break;
    			case MAX:
    				aggregateNumNoGrouping = Math.max(currentAgg, toAdd);
    				break;
    			case MIN:
    				aggregateNumNoGrouping = Math.min(currentAgg, toAdd);
    				break;
    			default:
    				throw new IllegalStateException("Operator must be one of SUM, COUNT, AVG, MAX, MIN!");
    			}
    		}
    	} else {
    		Field group = tup.getField(groupByFieldNum);
    		int newAgg;
    		if (aggregateNumsWithGrouping.containsKey(group)) {
    			int currentAgg = aggregateNumsWithGrouping.get(group);
    			switch (operator) {
    			case AVG:
    				int currentDenom = aggregateDenomsWithGrouping.get(group);
					aggregateDenomsWithGrouping.put(group, currentDenom+1);
    			case SUM:
    				newAgg = currentAgg + toAdd;
    				break;
    			case COUNT:
    				newAgg = currentAgg + 1;
    				break;
    			case MAX:
    				newAgg = Math.max(toAdd, currentAgg);
    				break;
    			case MIN:
    				newAgg = Math.min(toAdd, currentAgg);
    				break;
    			default:
    				throw new IllegalStateException("Operator must be one of SUM, COUNT, AVG, MAX, MIN!");
    			}
    			aggregateNumsWithGrouping.put(group, newAgg);
    		} else {
    			if (operator.equals(Op.COUNT)) {
    				newAgg = 1;
    			} else {
    				newAgg = toAdd;
    				if (operator.equals(Op.AVG)) {
    					aggregateDenomsWithGrouping.put(group, 1);
    				}
    			}
    			aggregateNumsWithGrouping.put(group, newAgg);
    		}
    	}
    }

    /**
     * Create a DbIterator over group aggregate results.
     * 
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public DbIterator iterator() {
		resultTuples = new ArrayList<Tuple>();
    	if (groupByFieldNum == Aggregator.NO_GROUPING) {
    		Tuple toAdd = new Tuple(resultTupleDesc);
    		if (operator.equals(Op.AVG)) {
    			toAdd.setField(0, new IntField(aggregateNumNoGrouping/aggregateDenomNoGrouping));
    		} else {
    			toAdd.setField(0, new IntField(aggregateNumNoGrouping));
    		}
    		resultTuples.add(toAdd);
    	} else {
    		for (Field group : aggregateNumsWithGrouping.keySet()) {
    			int numVal = aggregateNumsWithGrouping.get(group);
    			Tuple toAdd = new Tuple(resultTupleDesc);
    			toAdd.setField(0, group);
    			if (operator.equals(Op.AVG)) {
    				int denomVal = aggregateDenomsWithGrouping.get(group);
    				toAdd.setField(1, new IntField(numVal/denomVal));
    			} else {
    				toAdd.setField(1, new IntField(numVal));
    			}
    			resultTuples.add(toAdd);
    		}
    	}
		return new TupleIterator(resultTupleDesc, resultTuples);
    }

}
