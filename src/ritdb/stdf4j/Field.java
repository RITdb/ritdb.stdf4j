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

import java.io.IOException;

public class Field {
  private String name;
  private char type;
  private int length;
  private int lengthFieldIndex;
  private char arrayType;
  private int arraySizeFieldIndex;
  private int flagIndex;
  private byte[] flagBits;
  private byte[] flagOmit;
  private Object defValue;

  public Field(String name, String typeCode, String dValue) {
    assert typeCode.length() >= 2 : "Invalid type code: " + typeCode;

    this.name=name;
    this.type=typeCode.charAt(0);
    //TODO need to convert the default to the correct type
    this.defValue = dValue;
    if(defValue != null){
    	System.out.println(defValue+" "+ name);
    }
    
    
    this.flagIndex=-1;
    if(typeCode.indexOf('_') > 0) {
      String[] pieces=typeCode.split("_");
      if(pieces.length == 4) {
        typeCode=pieces[0];
        flagIndex=Integer.parseInt(pieces[1]);
        flagBits=pieces[2].getBytes();
        flagOmit=pieces[3].getBytes();
        assert flagBits.length == flagOmit.length;

        for(int i=0; i < flagBits.length; i++) {
          flagBits[i]-='0';
          flagOmit[i]-='0';
        }
      }
    }

    int lenPos=1;
    if(this.type == 'k') {
      // not using a regex here since this seems like it should be faster
      // may change if necessary
      int pos=1;
      int len=typeCode.length();
      for(; pos < len; pos++) {
        char c=typeCode.charAt(pos);
        if(c >= 'A' && c <= 'Z') break;
      }

      this.arraySizeFieldIndex=Integer.parseInt(typeCode.substring(1, pos));
      this.arrayType=typeCode.charAt(pos);

      lenPos=pos + 1;
    }
    else {
      this.arraySizeFieldIndex=-1;
      this.arrayType=' ';
    }

    this.lengthFieldIndex=-1;

    String lengthCode=typeCode.substring(lenPos);
    if(lengthCode.equals("n")) this.length=-1;
    else if(lengthCode.startsWith("f")) {
      this.length=-1;
      this.lengthFieldIndex=Integer.parseInt(lengthCode.substring(1));
    }
    else this.length=Integer.parseInt(lengthCode);
  }

  public String getName() {
    return name;
  }
  
  public Object getDefaultValue(){
	  return defValue;
  }

  public char getType() {
    return type;
  }

  public int getLength() {
    return length;
  }

  public int getLengthFieldIndex() {
    return lengthFieldIndex;
  }

  public char getArrayType() {
    return arrayType;
  }

  public int getArraySizeFieldIndex() {
    return arraySizeFieldIndex;
  }

  public int getFlagIndex() {
    return flagIndex;
  }

  // check to see if flag bits match omit flags from spec xml
  public boolean isValid(byte flag) {
    boolean valid=true;
    if(flagBits != null && flagOmit != null) {
      for(int i=0; i < flagBits.length; i++) {
        int shift=flagBits[i];
        byte check=(byte)((flag & (0x1 << shift)) >> shift);
        if(check == flagOmit[i]) {
          valid=false;
          break;
        }
      }
    }
    return valid;
  }
  
  protected int getFieldLength(char typeN, int lengthN, char arrayTypeN, Object value) throws IOException {
	  if( typeN == 'k'){
		  return getFieldLength(arrayTypeN, lengthN,value);
	  }else{
		  return getFieldLength(typeN, lengthN,value);		  
	  }
	  }
  
  private int getFieldLength(char typeN, int lengthN, Object value) throws IOException {
	  //TODO add default handler here
	    int fieldLength=0;
	    if(typeN == 'U' || typeN == 'I') {
	      fieldLength+=lengthN;
	    }
	    else if(typeN == 'R') {
	      fieldLength+=lengthN;
	    }
	    else if(typeN == 'B' && length == 1) {
	      fieldLength++;
	    }
	    else if(typeN == 'B' || typeN == 'N' || typeN == 'D') {
	      if(value instanceof Integer) fieldLength+=4;
	      else if(lengthN < 0) fieldLength+=1 + ((byte[])value).length;
	      else fieldLength+=lengthN;
	    }
	    else if(typeN == 'C') {
	      if(length < 0) fieldLength+=1 + ((String)value).length();
	      else fieldLength+=lengthN;
	    }
	    else if(typeN == 'V') {
	      fieldLength+=2;
	      for(Object o : (Object[])value) {
	        if(o != null) {
	          if(o instanceof Integer) fieldLength+=2;
	          else if(o instanceof Long) fieldLength+=4;
	          else if(o instanceof String) fieldLength+=((String)o).length();
	          else if(o instanceof Byte) fieldLength++;
	          else if(o instanceof Float) fieldLength+=4;
	          else if(o instanceof Double) fieldLength+=8;
	        }
	      }
	    }
	    return fieldLength;
  }
}
