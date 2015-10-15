package simpledb;

import java.util.*;

/**
 * Filter is an operator that implements a relational select.
 */
public class Filter extends Operator {

    private static final long serialVersionUID = 1L;
    
    private Predicate predicate;
    private DbIterator childIterator;
    private TupleDesc resultTupleDesc;

    /**
     * Constructor accepts a predicate to apply and a child operator to read
     * tuples to filter from.
     * 
     * @param p
     *            The predicate to filter tuples with
     * @param child
     *            The child operator
     */
    public Filter(Predicate p, DbIterator child) {
        predicate = p;
        childIterator = child;
        resultTupleDesc = child.getTupleDesc();
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public TupleDesc getTupleDesc() {
        return resultTupleDesc;
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
    	childIterator.open();
    	super.open();
    }

    public void close() {
    	super.close();
    	childIterator.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
    	childIterator.rewind();
    }

    /**
     * AbstractDbIterator.readNext implementation. Iterates over tuples from the
     * child operator, applying the predicate to them and returning those that
     * pass the predicate (i.e. for which the Predicate.filter() returns true.)
     * 
     * @return The next tuple that passes the filter, or null if there are no
     *         more tuples
     * @see Predicate#filter
     */
    protected Tuple fetchNext() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        while (childIterator.hasNext()) {
        	Tuple candidate = childIterator.next();
        	if (predicate.filter(candidate)) {
        		return candidate;
        	}
        }
        return null;
    }

    @Override
    public DbIterator[] getChildren() {
    	return new DbIterator[] { childIterator };
    }

    @Override
    public void setChildren(DbIterator[] children) {
        childIterator = children[0];
        resultTupleDesc = childIterator.getTupleDesc();
    }

}
