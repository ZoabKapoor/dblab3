package simpledb;

import java.util.*;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;
    
    private Aggregator aggregator;
    private DbIterator aggregatorIterator;
    private DbIterator childIterator;
    private int aggregateFieldNum;
    private int groupByFieldNum;
    private Aggregator.Op aggregatorOperation;
    

    /**
     * Constructor.
     * 
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     * 
     * 
     * @param child
     *            The DbIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    public Aggregate(DbIterator child, int afield, int gfield, Aggregator.Op aop) {
    	childIterator = child;
    	aggregateFieldNum = afield;
    	groupByFieldNum = gfield;
    	aggregatorOperation = aop;
    	Type groupByFieldType = (gfield == Aggregator.NO_GROUPING) ? null : childIterator.getTupleDesc().getFieldType(gfield);
    	if (childIterator.getTupleDesc().getFieldType(afield).equals(Type.INT_TYPE)) {
    		aggregator = new IntegerAggregator(groupByFieldNum, groupByFieldType, aggregateFieldNum, aggregatorOperation);
    	} else if (childIterator.getTupleDesc().getFieldType(afield).equals(Type.STRING_TYPE)) {
    		aggregator = new StringAggregator(groupByFieldNum, groupByFieldType, aggregateFieldNum, aggregatorOperation);
    	} else {
    		throw new UnsupportedOperationException("Can't aggregate over a field of type: " +
    			child.getTupleDesc().getFieldType(afield) + " , only STRING_TYPE or INT_TYPE aggregates are supported.");
    	}
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
    	return groupByFieldNum;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples If not, return
     *         null;
     * */
    public String groupFieldName() {
		if (groupByFieldNum == Aggregator.NO_GROUPING) {
			return null;
		} else {
			return childIterator.getTupleDesc().getFieldName(groupByFieldNum);
		}
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
		return aggregateFieldNum;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
    	return childIterator.getTupleDesc().getFieldName(aggregateFieldNum);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
    	return aggregatorOperation;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
	return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
	    TransactionAbortedException {
    	super.open();
    	childIterator.open();
    	while (childIterator.hasNext()) {
    		aggregator.mergeTupleIntoGroup(childIterator.next());
    	}
    	aggregatorIterator = aggregator.iterator();
    	aggregatorIterator.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate, If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
    	if (aggregatorIterator.hasNext()) {
    		return aggregatorIterator.next();
    	} else {
    		return null;
    	}
    }

    public void rewind() throws DbException, TransactionAbortedException {
    	aggregatorIterator.rewind();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * 
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
    	if (groupByFieldNum == Aggregator.NO_GROUPING) {
    		return new TupleDesc(new Type[]{childIterator.getTupleDesc().getFieldType(aggregateFieldNum)}, 
    			new String[]{aggregateFieldName()});
    	} else {
    		return new TupleDesc(new Type[] {childIterator.getTupleDesc().getFieldType(groupByFieldNum), 
    			childIterator.getTupleDesc().getFieldType(aggregateFieldNum)}, 
    			new String[] {groupFieldName(), aggregateFieldName()});
    	}
    }

    public void close() {
    	aggregatorIterator.close();
    	childIterator.close();
    	super.close();
    }

    @Override
    public DbIterator[] getChildren() {
    	return new DbIterator[] { childIterator };
    }

    @Override
    public void setChildren(DbIterator[] children) {
    	childIterator = children[0];
    }
    
}
