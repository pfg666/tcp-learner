package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class Filesystem {
	

	/*
     *  get  directory of program's path 
     */
    public static String  getDirName(String filepath) {
            	
       String dir=(new File(filepath)).getParent();   
       return dir;
    }   
    
    /*
     *  get  filename of program's path 
     */    
    public static String  getFileName(String filepath) {
        String dir=(new File(filepath)).getName();   
        return dir;
     }   
    
    /*
     *  get  extension of a program from its program's path 
     */
    public static String  getFileExtension(String filepath) {
    	assert(filepath!=null);
    	String filename=getFileName(filepath);
    	int extIndex=filename.lastIndexOf(".");
    	if ( extIndex == -1 ) {
    		return null;
    	} else {
    		return filename.substring(extIndex);  		
    	}
    }


    

	
	/*
	 * cmpBytes - compares two file by reading per byte
	 */
	public static boolean cmpBytes(String filename1, String filename2) {

		FileInputStream file1;
		try {
			file1 = new FileInputStream(filename1);
		} catch (FileNotFoundException e1) {
			throw new ExceptionAdapter(e1);
		}
		FileInputStream file2;
		try {
			file2 = new FileInputStream(filename2);
		} catch (FileNotFoundException e1) {
			throw new ExceptionAdapter(e1);
		}

		try {
			int a, b;
			while (true) {
				a = file1.read(); // read per byte
				b = file2.read();
				if (a != b) {
					return false;
				} // Files do not match
				if (a == -1)
					break; // both files reach end of file!
			}
		} catch (IOException ioe) {
			throw new ExceptionAdapter(ioe);
		}
		return true;
	}

	/*
	 * cmpText - compares two file by reading per character and ignoring eol
	 * differences
	 */
	public static boolean cmpText(String filename1, String filename2) {

		BufferedReader file1;
		try {
			file1 = new BufferedReader(new FileReader(filename1));
		} catch (FileNotFoundException e1) {
			throw new ExceptionAdapter(e1);
		}
		BufferedReader file2;
		try {
			file2 = new BufferedReader(new FileReader(filename2));
		} catch (FileNotFoundException e1) {
			throw new ExceptionAdapter(e1);
		}

		try {
			String line1, line2;
			while (true) {
				line1 = file1.readLine(); // read per line
				line2 = file2.readLine();
				if (line1 != null && line2 != null && !line1.equals(line2)) {
					return false;
				} // Files do not match
				if (line1 == null && line2 != null)
					return false;
				if (line1 != null && line2 == null)
					return false;
				if (line1 == null && line2 == null)
					break; // both files reach end of file!
			}
		} catch (IOException ioe) {
			throw new ExceptionAdapter(ioe);
		}
		return true;
	}

	// source from
	// http://commons.apache.org/io/api-release/org/apache/commons/io/FileUtils.html
	/**
	 * Makes a directory, including any necessary but nonexistent parent
	 * directories. If a file already exists with specified name but it is not a
	 * directory then an IOException is thrown. If the directory cannot be
	 * created (or does not already exist) then an IOException is thrown.
	 * 
	 * @param directory
	 *            directory to create, must not be {@code null}
	 * @throws NullPointerException
	 *             if the directory is {@code null}
	 * @throws unchecked
	 *             IOException if the directory cannot be created or the file
	 *             already exists but is not a directory note: IOException is by
	 *             default checked, but by encapsalation it in ExceptionAdapter
	 *             we made it unchecked!
	 */
	public static void mkdirhier(String dir) {
		File directory = new File(dir);		
		if (directory.exists()) {
			if (!directory.isDirectory()) {
				String message =
								"File "
												+ directory
												+ " exists and is "
												+ "not a directory. Unable to create directory.";
				throw new ExceptionAdapter(new IOException(message));
			}
		} else {
			if (!directory.mkdirs()) {
				// Double-check that some other thread or process hasn't made
				// the directory in the background
				if (!directory.isDirectory())
				{
					String message =
									"Unable to create directory " + directory;
					throw new ExceptionAdapter(new IOException(message));
				}
			}
		}
	}

	static public boolean isdir(String pathstr) {
		File path = new File(pathstr);
		return path.isDirectory();		
	}
	
	static public boolean isexisting(String pathstr) {
		File path = new File(pathstr);
		return path.exists();	
	}	
	
	static public boolean isfile(String pathstr) {
		File path = new File(pathstr);		
		return path.isFile();	
	}
	
	static public boolean isreadablefile(String pathstr) {
		File path = new File(pathstr);		
		return path.canRead();	
	}	
	static public boolean iswritablefile(String pathstr) {
		File path = new File(pathstr);		
		return path.canWrite();	
	}	
	
	
	/*
	 * isexecutable - checks file is executable
	 */
    public static boolean isexecutable(String pathstr) {
    	// note: on windows File.canExecute() always gives true even if ntfs execute permission is not set (in cmd.exe you get "Access is denied" message if you try to execute it)
    	//       Solution: just accept that this test always gives true on windows
    	
		File path = new File(pathstr);		
		return path.canExecute();	    	
    }	
    	
	
	// source:http://stackoverflow.com/questions/1272130/checking-for-write-access-in-a-directory-before-creating-files-inside-it
	static public boolean iswritabledir(String pathstr) {
		File path = new File(pathstr);		
		File sample = new File(path,"empty.txt"); 
		try
		{
		     /*
		      * Create and delete a dummy file in order to check file permissions. Maybe 
		      * there is a safer way for this check.
		      */
		      sample.createNewFile();
		      sample.delete();
		      return true;
		}
		catch(IOException e)
		{
		      //Error message shown to user. Operation is aborted
			  return false;
		}	
	}		
	
	/* 
	 *  remove directory with all its contents,
	 *  where subdirectories are recursively removed
	 */
	static public void rmdirhier(String dir)
	{

		File path = new File(dir);
		_rmdir(path);
	}

	static protected void _rmdir(File path)
	{
		if (path == null)
			return;
		if (path.exists())
		{
			for (File f : path.listFiles())
			{
				if (f.isDirectory())
				{
					_rmdir(f);
					f.delete();
				}
				else
				{
					f.delete();
				}
			}
			path.delete();
		}
	}

	static public void mkdir(String dir) {
		new File(dir).mkdir();
	}

	static public void copyfile(String srFile, String dtFile) {
		try {
			File f1 = new File(srFile);
			File f2 = new File(dtFile);
			InputStream in = new FileInputStream(f1);

			// For Append the file.
			// OutputStream out = new FileOutputStream(f2,true);

			// For Overwrite the file.
			OutputStream out = new FileOutputStream(f2);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			//System.out.println("File copied.");
		} catch (FileNotFoundException ex) {
			System.out.println(ex.getMessage() + " in the specified directory.");
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	static public Writer getUtf8FileWriter(String filename, boolean append) throws  FileNotFoundException {
		Writer result = null;
		try {
			result =  new OutputStreamWriter(new FileOutputStream(filename,append), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.out.println( "Warning: switch to default char encoding, because 'UTF-8' not supported" );
			result =  new OutputStreamWriter(new FileOutputStream(filename,append)); // same as FileWriter
		} 
		return result;
	}
	
	/*
	 * get a Writer object to an UTF8 encoded file
	 */
	static public Writer getUtf8FileWriter(String filename) throws  FileNotFoundException {
        return getUtf8FileWriter(filename, false);
	}	

	/*
	 * get a Writer object to an UTF8 encoded file
	 * 
	 * params:
	 *     append: append newly written data at the end of the file
	 *     autolineflush: automatically flush stream after each newline written
	 */	
	static public PrintWriter getUtf8FilePrintWriter(String filename, boolean append,boolean autolineflush) throws  FileNotFoundException {
		return  new PrintWriter(getUtf8FileWriter(filename,append),autolineflush);
	}		 

	static public PrintWriter getUtf8FilePrintWriter(String filename) throws  FileNotFoundException {
	    return  getUtf8FilePrintWriter(filename,false,false);
	}
	
	static public PrintWriter getUtf8FilePrintWriter(String filename, boolean append) throws  FileNotFoundException {
	    return  getUtf8FilePrintWriter(filename,append,false);
	}

	

}
