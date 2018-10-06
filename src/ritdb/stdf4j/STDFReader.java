/**
 * Copyright 2009-2012 tragicphantom
 *
 * This file is part of stdf4j.
 *
 * Stdf4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stdf4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with stdf4j.  If not, see <http://www.gnu.org/licenses/>.
**/
package ritdb.stdf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import ritdb.stdf4j.util.ByteArray;
import rtalk.util.RtalkLoggerInterface;

/**
 * STDFReader
 * Based on pystdf <http://code.google.com/p/pystdf/>
 *     and libstdf <http://freestdf.sourceforge.net/>
 */
public class STDFReader {
  private InputStream stream=null;
  private int available=0;
  private int totalBytes=0;
  private ByteArray byteArray=new ByteArray();
  private boolean errorOnUnknown=false;
  private RtalkLoggerInterface _logger ;

  public STDFReader(String fileName, RtalkLoggerInterface logger) throws FileNotFoundException, IOException {
    this(new FileInputStream(fileName), logger);
  }

  public STDFReader(File file,RtalkLoggerInterface logger) throws FileNotFoundException, IOException {
    this(new FileInputStream(file) ,logger);
  }

  public STDFReader(InputStream stream,RtalkLoggerInterface logger) throws IOException {
	_logger = logger;
    InputStream bufis=new BufferedInputStream(stream);
    bufis.mark(2);
    int header=((bufis.read() & 0xFF) << 8) + (bufis.read() & 0xFF);
    bufis.reset();
    if(header == 0x1F8B /*GZIP*/) this.stream=new BufferedInputStream(new GZIPInputStream(bufis));
    else this.stream=bufis;
  }

  public void setErrorOnUnknown(boolean errorOnUnknown) {
    this.errorOnUnknown=errorOnUnknown;
  }

  public void parse(RecordVisitor visitor) throws FileNotFoundException, IOException, ParseException {
    // default to v4 types if none specified
    parse(visitor, ritdb.stdf4j.v4.Types.getRecordDescriptors());
  }

  public void parse(RecordVisitor visitor, Map<RecordType, RecordDescriptor> records) throws FileNotFoundException, IOException, ParseException {
    if(stream == null) throw new FileNotFoundException();

    byteArray.setByteOrder(ByteOrder.nativeOrder());

    visitor.beforeFile();

    try {
      Header header=new Header();

      // verify first record in file is a FAR
      readFirstHeader(header);
      if(header.getType() != 0 && header.getSubType() != 10) throw new ParseException("Invalid header sequence", 0);

      Record record=readRecord(header, records);

      if(record == null) throw new ParseException("Unknown record type cannot be first in file", 0);

      // set byte order based on FAR contents
      //RecordData far=record.getData();
      //long cpuType=(Long)far.getField("CPU_TYPE");
      //byteArray.setByteOrder((cpuType == 1) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

      visitor.handleRecord(record);

      // read until IOException
      while(true) {
        readHeader(header);
        record=readRecord(header, records);
        if(record != null){
          visitor.handleRecord(record);
        }else{
          _logger.log("WARNING: Unknown record type at " + totalBytes); 
          visitor.handleUnknownRecord(header, getBytes(header.getLength()));
        }
      }
    }
    catch(IOException e) {
      // Ignore
      //e.printStackTrace();
      System.out.println(e.getMessage());
    }
    finally {
      stream.close();
      stream=null;
    }

    visitor.afterFile();
  }

  protected void readHeader(Header header) throws IOException, ParseException {
    available=4;
    header.set(readUnsignedInt(2), readUnsignedInt(1), readUnsignedInt(1));
  }
  
  protected void readFirstHeader(Header header) throws IOException, ParseException {
    // issue if the byte swap messes up the size here
    available=4;
    if(readUnsignedInt(2) == 2)
      byteArray.setByteOrder(ByteOrder.LITTLE_ENDIAN);
    else{
      byteArray.setByteOrder(ByteOrder.BIG_ENDIAN);      
    }
    header.set(2, readUnsignedInt(1), readUnsignedInt(1));
  }

  protected Record readRecord(Header header, Map<RecordType, RecordDescriptor> records) throws IOException, ParseException {
    Record record=null;

    available=header.getLength();

    //System.err.println(totalBytes + "[" + String.format("0x%x", totalBytes) + "]: " + header.getType() + ", " + header.getSubType() + ": " + available + " bytes");

    if(records.containsKey(header.getRecordType())) {
      //TODO need to add support for extension byte here and perhaps in the header
      record=new Record(records.get(header.getRecordType()), totalBytes, getBytes(header.getLength()), byteArray.getByteOrder());
    }
    else {
      // unknown record type
      return null;
    }

    return record;
  }

  protected int readUnsignedInt(int length) throws IOException {
    return byteArray.toUnsignedInt(getBytes(length), length);
  }

  protected byte[] getBytes(int numBytes) throws IOException {
    available-=numBytes;
    totalBytes+=numBytes;

    if(available < 0) {
      numBytes+=available;
      totalBytes+=available;
      available=0;
    }

    byte[] bytes=new byte[numBytes];
    int actualBytes=0;
    if((actualBytes=stream.read(bytes, 0, numBytes)) != numBytes) {
      int offset=0;
      while(actualBytes > 0) {
        numBytes-=actualBytes;
        offset+=actualBytes;
        if((actualBytes=stream.read(bytes, offset, numBytes)) == numBytes) break;
      }
      if(actualBytes != numBytes){
        if(numBytes == 2){
          throw new IOException("File read finished");
        }else{
          _logger.log("WARNING: Unexpected file end, short by "+numBytes);
        }
      }
    }

    return bytes;
  }
}
