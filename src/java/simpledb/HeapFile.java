package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {
	
	private File file;
	private TupleDesc schema;

    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        file = f;
        schema = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        return file.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        return schema;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
    	RandomAccessFile fileToRead = null;
        try {
			fileToRead = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Couldn't find the file: " + file.toString());
		}
        byte[] bytes = new byte[BufferPool.PAGE_SIZE];
        try {
        	fileToRead.seek((long) pid.pageNumber()*BufferPool.PAGE_SIZE); 
			fileToRead.readFully(bytes, 0, BufferPool.PAGE_SIZE);
		} catch (IOException e) {
			throw new IllegalArgumentException("Page :" + pid.pageNumber() + "does not exist in the file!");
		} finally {
			try {
				fileToRead.close();
			} catch (IOException e) {
				throw new RuntimeException("Can't close the file: " + file.toString());
			}
		}
        try {
			return new HeapPage((HeapPageId) pid, bytes);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't cast PageId: " + pid.toString() + "to a HeapPageId!");
		}
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
    	RandomAccessFile fileToWrite = null;
        try {
			fileToWrite = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Couldn't find the file: " + file.toString());
		}
        fileToWrite.seek((long) page.getId().pageNumber()*BufferPool.PAGE_SIZE); 
		fileToWrite.write(page.getPageData(), 0, BufferPool.PAGE_SIZE);
		fileToWrite.close();
		
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
    	return (int) file.length()/BufferPool.PAGE_SIZE;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
    	ArrayList<Page> result = new ArrayList<Page>();
    	for (int i = 0; i < numPages(); ++i) {
    		HeapPageId pid = new HeapPageId(getId(), i);
    		HeapPage candidate = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
    		if (candidate.getNumEmptySlots() != 0)  {
    			candidate.insertTuple(t);
    			result.add(candidate);
    			return result;
    		}
    	}
    	HeapPage newPage = new HeapPage(new HeapPageId(getId(), numPages()), new byte[BufferPool.PAGE_SIZE]);
    	newPage.insertTuple(t);
    	writePage(newPage);
    	result.add(newPage);
    	return result;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
    	ArrayList<Page> result = new ArrayList<Page>();
    	if (t.getRecordId().getPageId().getTableId() != getId()) {
    		throw new DbException("The tuple to delete isn't in this HeapFile!");
    	} else {
    		PageId pid = t.getRecordId().getPageId();
    		HeapPage toModify = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
    		toModify.deleteTuple(t);
    		result.add(toModify);
    		return result;	
    	}
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
    	return new HeapFileIterator(tid);
    }
    
    public class HeapFileIterator implements DbFileIterator {
    	
    	private TransactionId tid;
    	private Iterator<Tuple> tupleIterator;
    	// The page number of the next page to get.
    	private int nextPageNo = 0;
    	private boolean isOpen = false;

    	public HeapFileIterator(TransactionId tid) {
    		this.tid = tid;
    	}
    	
		@Override
		public void open() throws DbException, TransactionAbortedException {
			if (!isOpen) {
			HeapPage myPage = (HeapPage) Database.getBufferPool().getPage(tid, new HeapPageId(getId(), nextPageNo), Permissions.READ_ONLY);
	    	nextPageNo++;
			tupleIterator = myPage.iterator();
	    	isOpen = true;
			}
		}

		@Override
		public boolean hasNext() throws DbException, TransactionAbortedException {
			if (isOpen) {
				if (tupleIterator.hasNext()) {
					return true;
				} else {
					while (nextPageNo < numPages()) {
						HeapPageId idForNextPage = new HeapPageId(getId(), nextPageNo);
						HeapPage nextPage = (HeapPage) Database.getBufferPool().getPage(tid, idForNextPage, Permissions.READ_ONLY);
						nextPageNo++;
						tupleIterator = nextPage.iterator();
						if (tupleIterator.hasNext()) {
							return true;
						}
					}
					return false;
				}
			} else {
				return false;
			}
		}

		@Override
		public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
			if (isOpen) {
				if (!hasNext()) {
					throw new NoSuchElementException("There is no next tuple!");
				} else {
					return tupleIterator.next();
				} 
			} else {
				throw new NoSuchElementException("Iterator is not open!");
			}
		}

		@Override
		public void rewind() throws DbException, TransactionAbortedException {
			nextPageNo = 0;
			this.close();
			this.open();
		}

		@Override
		public void close() {
			tupleIterator = null;
			isOpen = false;
		}
    	
    }

}

