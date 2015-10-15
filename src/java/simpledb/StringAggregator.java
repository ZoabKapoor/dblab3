package simpledb;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    
    private int groupByFieldNum;
    
    private ConcurrentHashMap<Field,Tuple> aggregatesWithGrouping;
    private Tuple aggregateNoGrouping;
    private TupleDesc resultTupleDesc;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        if (what != Op.COUNT) {
        	throw new IllegalArgumentException("Can't aggregate over strings unless using operator COUNT!");
        }
        if (gbfield != Aggregator.NO_GROUPING) {
        	aggregatesWithGrouping = new ConcurrentHashMap<Field, Tuple>();
        	resultTupleDesc = new TupleDesc( new Type[] {gbfieldtype, Type.INT_TYPE});
        } else {
        	resultTupleDesc = new TupleDesc( new Type[] {Type.INT_TYPE});
        	aggregateNoGrouping = new Tuple(resultTupleDesc);
        	aggregateNoGrouping.setField(0, new IntField(0));
        }
    	groupByFieldNum = gbfield;
    	// Note that we can throw away afield here, because a string aggregator basically just
    	// counts tuples, so we don't really need a particular field to count over. 
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        if (groupByFieldNum == Aggregator.NO_GROUPING) {
        	IntField currentCount = (IntField) aggregateNoGrouping.getField(0);
        	IntField newCount = new IntField(currentCount.getValue()+1);
        	aggregateNoGrouping.setField(0, newCount);
        } else if (aggregatesWithGrouping.containsKey(tup.getField(groupByFieldNum))) {
        	Tuple groupCounter = aggregatesWithGrouping.get(tup.getField(groupByFieldNum));
        	IntField currentCount = (IntField) groupCounter.getField(1);
        	IntField newCount = new IntField(currentCount.getValue()+1);
        	groupCounter.setField(1, newCount);
        } else {
        	Tuple groupCounter = new Tuple(resultTupleDesc);
        	groupCounter.setField(0, tup.getField(groupByFieldNum));
        	groupCounter.setField(1, new IntField(1));
        	aggregatesWithGrouping.put(groupCounter.getField(0), groupCounter);
        }
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public DbIterator iterator() {
        if (groupByFieldNum == Aggregator.NO_GROUPING) {
        	ArrayList<Tuple> resultList = new ArrayList<Tuple>();
        	resultList.add(aggregateNoGrouping);
        	return new TupleIterator(resultTupleDesc, resultList);
        } else {
        	return new TupleIterator(resultTupleDesc, aggregatesWithGrouping.values());
        }
    }

}
