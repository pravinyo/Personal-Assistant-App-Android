package Utils.DataStructure;

import java.util.ArrayList;

import Utils.DataStructure.Field;
import Utils.Libs.formFillLib.FormFillLibException;

/**
 * Created by Pravinyo on 3/15/2017.
 */

public class Form {
    //Collection of fields
    private ArrayList<Field> fields = new ArrayList<Field>();

    /**
     * Adds a new field to the collection of fields of the form
     * @throws Exception when the field is not complete (it does not have all the information needed to process it
     */
    public void addField(Field f) throws FormFillLibException {
        if(f.isComplete())
            fields.add(f);
        else
            throw new FormFillLibException("The field is not complete: name or prompt missing");
    }

    /**
     * Obtains the field in the indicated position
     */
    public Field getField(int position){
        return fields.get(position);
    }

    /**
     * Computes the number of fields in the form
     */
    public int numberOfFields(){
        return fields.size();
    }
}
