package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {
	
	private ArrayList<TDItem> fieldDescs;

    /**
     * A help class to facilitate organizing the information of each field
     * */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         * */
        public final Type fieldType;
        
        /**
         * The name of the field
         * */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    /**
     * @return
     *        An iterator which iterates over all the field TDItems
     *        that are included in this TupleDesc
     * */
    public Iterator<TDItem> iterator() {
        return fieldDescs.iterator();
    }

    private static final long serialVersionUID = 1L;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     * @param fieldAr
     *            array specifying the names of the fields. Note that names may
     *            be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
    	fieldDescs = new ArrayList<TDItem>();
        for (int i = 0; i < typeAr.length ; ++i) {
        	TDItem item = new TDItem(typeAr[i], fieldAr[i]);
			fieldDescs.add(item);
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
    	fieldDescs = new ArrayList<TDItem>();
        for (int i = 0; i < typeAr.length ; ++i) {
        	TDItem item = new TDItem(typeAr[i], null);
			fieldDescs.add(item);
        }
    }
    
    public void addTDItem(TDItem toAdd) {
    	fieldDescs.add(toAdd);
    }
    
    /**
     * Constructor. Creates an empty tuple desc.
     */
    public TupleDesc() {
    	fieldDescs = new ArrayList<TDItem>();
    	return;
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        return fieldDescs.size();
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     * 
     * @param i
     *            index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        if (i < 0) {
        	throw new NoSuchElementException("The index: " + i + " of the field can't be negative!");
        } else if (i > this.numFields() - 1) {
			throw new NoSuchElementException("The index: " + i + " of the field is invalid!");
        } else {
        	Iterator<TDItem> iterator = iterator();
        	for (int j = 0; j < i; ++j) {
        		iterator.next();
        	}
        	TDItem fieldToGet = iterator.next();
        	return fieldToGet.fieldName;
        }
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     * 
     * @param i
     *            The index of the field to get the type of. It must be a valid
     *            index.
     * @return the type of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        if (i < 0) {
        	throw new NoSuchElementException("The index: " + i + " of the field can't be negative!");
        } else if (i > this.numFields() - 1){
        	throw new NoSuchElementException("The index: " + i + " of the field can't be greater"
        			+ " than the number of fields in the TupleDesc - 1!");
        } else {
        	return this.fieldDescs.get(i).fieldType;
        }
    }

    /**
     * Find the index of the field with a given name.
     * 
     * @param name
     *            name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException
     *             if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
    	if (name == null) {
    		throw new NoSuchElementException("You can't search for the field name null!");
    	} else {
    		Iterator<TDItem> iterator = iterator();
    		for (int i = 0; i < fieldDescs.size(); ++i) {
    			TDItem temp = iterator.next();
    			if ((temp.fieldName != null) && temp.fieldName.equals(name)) {
    				return i;
    			}
    		}
    		throw new NoSuchElementException("No field with field name: " + name + " could be found!");
    	}
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
    	Iterator<TDItem> iterator = iterator();
    	int size = 0;
    	while (iterator.hasNext()) {
    		size += iterator.next().fieldType.getLen();
    	}
        return size;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     * 
     * @param td1
     *            The TupleDesc with the first fields of the new TupleDesc
     * @param td2
     *            The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
    	TupleDesc result = new TupleDesc();
        Iterator<TDItem> iterator1 = td1.iterator();
        while (iterator1.hasNext()) {
        	result.fieldDescs.add(iterator1.next());
        }
        Iterator<TDItem> iterator2 = td2.iterator();
        while (iterator2.hasNext()) {
        	result.fieldDescs.add(iterator2.next());
        }
        return result;
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they are the same size and if the n-th
     * type in this TupleDesc is equal to the n-th type in td.
     * 
     * @param o
     *            the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */
    public boolean equals(Object o) {
        if (!(o instanceof TupleDesc)) {
        	return false;
        } else {
        	TupleDesc candidate = (TupleDesc) o;
        	if (candidate.numFields() != this.numFields()) {
        		return false;
        	}
        	for (int i = 0; i < this.numFields(); ++i) {
        		if (this.getFieldType(i) != candidate.getFieldType(i)) {
        			return false;
        		}
        	}
        	return true;
        }
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     * 
     * @return String describing this descriptor.
     */
    public String toString() {
        String result = "";
        Iterator<TDItem> iterator = iterator();
        while (iterator.hasNext()) {
        	result += iterator.next().toString();
        }
        return result;
    }
}
