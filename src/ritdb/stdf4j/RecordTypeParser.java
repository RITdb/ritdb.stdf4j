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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

public class RecordTypeParser extends DefaultHandler {
  private HashMap<RecordType, RecordDescriptor> types=new HashMap<RecordType, RecordDescriptor>();

  private Stack<String> tags=new Stack<String>();
  private StringBuilder sb=new StringBuilder();

  private String name=null;
  private String type=null;
  private String subType=null;
  private ArrayList<Field> fields=new ArrayList<Field>();

  protected RecordTypeParser() {}

  public HashMap<RecordType, RecordDescriptor> getTypeDescriptors() {
    return types;
  }

  /**@Override*/
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    tags.push(qName);

    if(qName.equals("RecordType")) {
      name=attributes.getValue("name").intern();
      type=attributes.getValue("type");
      subType=attributes.getValue("subType");
    }
    else if(qName.equals("Field")) fields.add(new Field(attributes.getValue("name"), attributes.getValue("type").intern(),attributes.getValue("default")));
  }

  /**@Override*/
  public void endElement(String uri, String localName, String qName) {
    tags.pop();

    if(qName.equals("RecordType")) {
      RecordType rt=new RecordType(Integer.parseInt(type), Integer.parseInt(subType));
      types.put(rt, new RecordDescriptor(name, rt, fields));

      fields=new ArrayList<Field>();
    }

    sb.delete(0, sb.length());
  }

  /**@Override*/
  public void characters(char[] ch, int start, int length) { sb.append(ch, start, length); }

  public static HashMap<RecordType, RecordDescriptor> parse() {
    RecordTypeParser handler=new RecordTypeParser();

    InputStream stream=null;
    try { stream=handler.getClass().getResourceAsStream("stdf_v4_types.xml"); }
    catch(Exception e) { e.printStackTrace(); }
    
    try {
      InputSource source=new InputSource(stream);
      SAXParser parser=SAXParserFactory.newInstance().newSAXParser();
      parser.parse(source, handler);
    }
    catch(Exception e) { e.printStackTrace(); }

    return handler.getTypeDescriptors();
  }
}
