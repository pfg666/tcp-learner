package util;

import java.io.IOException;
import java.io.Writer;

public class JsonWriter extends Writer {


	private int indent = 0;
    
    
    private Writer writer;
    
    public JsonWriter(Writer writer) {
    	this.writer=writer;
    }
    
    @Override
    public void	close() throws IOException {
			this.writer.close();
    }
    
    @Override
    public void	flush() throws IOException{
			this.writer.flush();
    }
  
    @Override
    public void	write(char[] cbuf, int off, int len) throws IOException{
    		for(int x = off; x < len; x = x+1){
    			 this.charWrite(cbuf[x]);
    		}
    }

    public void charWrite(char c)   throws IOException  {
        if (((char)c) == '[' || ((char)c) == '{') {
            this.writer.write(c);
            this.writer.write('\n');
            indent++;
            writeIndentation();
        } else if (((char)c) == ',') {
            this.writer.write(c);
            this.writer.write('\n');
            writeIndentation();
        } else if (((char)c) == ']' || ((char)c) == '}') {
            this.writer.write('\n');
            indent--;
            writeIndentation();
            this.writer.write(c);
        } else {
            this.writer.write(c);
        }

    }

    private void writeIndentation()  throws IOException {
        for (int i = 0; i < indent; i++) {
            this.writer.write("   ");
        }
    }
}
