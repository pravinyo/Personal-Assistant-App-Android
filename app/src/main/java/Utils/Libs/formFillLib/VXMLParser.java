package Utils.Libs.formFillLib;

/**
 * Created by Pravinyo on 3/15/2017.
 */

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import Utils.DataStructure.Field;
import Utils.DataStructure.Form;

/**
 * Parser that processes the contents of a VXML file and creates a Form object with a collection of Field objects.
 *
 * @author Zoraida Callejas
 * @author Michael McTear
 * @version 1.3, 08/18/13
 *
 */
public class VXMLParser{

    /**
     * Returns the attributes of the current tag (e.g. field name), or null if it has no attributes.
     * The parser must be placed in the current tag before invoking this method
     *
     * Idea from: http://stackoverflow.com/questions/4827168/how-to-parse-the-value-in-the-attribute-in-xml-parsing (code changed)
     */
    private static HashMap<String,String> getAttributes(XmlPullParser parser) {

        HashMap<String,String> attributes=null;

        int numAttributes = parser.getAttributeCount();

        if(numAttributes != -1) {
            attributes = new HashMap<String,String>(numAttributes);

            for(int i=0; i<numAttributes; i++)
                attributes.put(parser.getAttributeName(i), parser.getAttributeValue(i));
        }

        return attributes;
    }


    /**
     * Parses the string corresponding to a VXML file into a Form and several Fields.
     * @input vxmlContent The contents of the file, which must be in the following format:
     *
     * <form id = "flight">
     * 		<field name="destination">
     * 			<prompt>where would you like to travel to?</prompt>
     * 			<nomatch> I did not understand your destination </nomatch>
     * 			<noinput> I am sorry, I did not hear your clearly </noinput>
     * 		</field>
     *
     * 		<field name="date">
     * 			<prompt>what day would you like to travel?</prompt>
     * 			<nomatch> I did not understand the date </nomatch>
     * 			<noinput> I am sorry, I did not hear your clearly </noinput>
     * 		</field>
     * </form>
     *
     * The nesting of the tags must be strictly as in the example, though the order
     * of the prompt, nomatch and noinput elements inside a field is not important
     *
     * The prompt field is mandatory.
     *
     * If there are more than 1 form in the file, only the last one will be taken into account.
     *
     *
     * @throws Exception If there are errors during parsing, mainly because of ill-formed files that do not follow the previous indications
     */
    public static Form parseVXML(String vxmlContent) throws XmlPullParserException, FormFillLibException {

        Form form = null;
        Field field = null;
        String tagContents = null;
        HashMap<String, String> attributes = null;
        int eventType;


        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(); //May throw XmlPullParserException
        XmlPullParser parser = factory.newPullParser();					   //May throw XmlPullParserException


        StringReader xmlReader = new StringReader(vxmlContent);

        try{
            parser.setInput(xmlReader); //We are reading the xml from a string, but we could read it directly from the streaminput
        }
        catch(XmlPullParserException ex){
            throw new FormFillLibException(ex.getMessage(), "VXML not accessible, check Internet connection and accesibility of the URL");
        }

        try{
            eventType = parser.getEventType();	//May throw a XMLPullParserException
            while (eventType != XmlPullParser.END_DOCUMENT) {

                String tagname = parser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:

                        if (tagname.equalsIgnoreCase("field")){
                            field = new Field();
                            attributes = getAttributes(parser);
                            field.setName(attributes.get("name"));

                        } else if (tagname.equalsIgnoreCase("form"))
                            form = new Form();
                        break;

                    case XmlPullParser.TEXT:
                        tagContents = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagname.equalsIgnoreCase("prompt")) {
                            field.setPrompt(tagContents);
                        } else if (tagname.equalsIgnoreCase("nomatch")){
                            field.setNomatch(tagContents);
                        } else if (tagname.equalsIgnoreCase("noinput")){
                            field.setNoinput(tagContents);
                        } else if (tagname.equalsIgnoreCase("field")){
                            form.addField(field);	//May throw a FormFillLibException if the field is not complete
                        }
                        break;

                    default:
                        break;
                }

                eventType = parser.next(); //May throw a XMLPullParseException or a IOException
            }

        } catch(XmlPullParserException ex){
            throw new FormFillLibException(ex.getMessage(), "VXML could not be read, check Internet connection and accesibility of the URL");
        } catch(IOException ex){
            throw new FormFillLibException(ex.getMessage(), "VXML could not be read, check Internet connection and accesibility of the URL");
        }
        return form;
    }

}
