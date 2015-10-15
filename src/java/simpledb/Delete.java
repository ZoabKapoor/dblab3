package simpledb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;
    
    private DbIterator childIterator;
    private TransactionId tid;
    private TupleDesc resultTD;
    private ArrayList<Tuple> result;
    private Iterator<Tuple>	resultIterator;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     * 
     * @param t
     *            The transaction this delete runs in
     * @param child
     *            The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, DbIterator child) {
        tid = t;
        childIterator = child;
        resultTD = new TupleDesc(new Type[] {Type.INT_TYPE}, new String[] {"numTups deleted"});
    }

    public TupleDesc getTupleDesc() {
        return resultTD;
    }

    public void open() throws DbException, TransactionAbortedException {
        super.open();
        childIterator.open();
        int count = 0;
        while (childIterator.hasNext()) {
    		Tuple toDelete = childIterator.next();
        	try {
				Database.getBufferPool().deleteTuple(tid, toDelete);
        		count++;
			} catch (NoSuchElementException e) {
				throw new DbException("Tuple: " + toDelete + "couldn't be found!");
			} catch (IOException e) {
				throw new DbException("Failed to delete tuple: " + toDelete);
			}
        }
        Tuple resultTuple = new Tuple(resultTD);
        resultTuple.setField(0, new IntField(count));
        result = new ArrayList<Tuple>();
        result.add(resultTuple);
        resultIterator = result.iterator();
    }

    public void close() {
        childIterator.close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        childIterator.rewind();
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     * 
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if (resultIterator.hasNext()) {
        	return resultIterator.next();
        } else {
        	return null;
        }
    }

    @Override
    public DbIterator[] getChildren() {
    	return new DbIterator[] { childIterator };
    }

    @Override
    public void setChildren(DbIterator[] children) {
        childIterator = children[0];
        result = null;
        resultIterator = null;
    }

}
