package Utils.DataStructure;

/**
 * Created by Pravinyo on 3/15/2017.
 */

public class Field {
    private String name = null; 							//Name of the field (e.g. "Destination")
    private String prompt = null;							//String used to prompt the user for the field (e.g. "What is your destination?")
    private String nomatch = "I did not understand";		//String used to tell the user that the system could not understand what they said
    private String noinput = "I did not hear anything";		//String used to tell the user that the system could not hear them
    private String value = null;							//Value for the field provided by the user (e.g. "Paris")


    public void setValue(String value){
        this.value=value;
    }

    public String getValue(){
        return value;
    }

    public void setPrompt(String prompt){
        this.prompt=prompt;
    }

    public String getPrompt(){
        return prompt;
    }

    public void setName(String name){
        this.name=name;
    }

    public String getName(){
        return name;
    }

    public void setNomatch(String nomatch){
        this.nomatch=nomatch;
    }

    public String getNomatch(){
        return nomatch;
    }

    public void setNoinput(String noinput){
        this.noinput=noinput;
    }

    public String getNoinput(){
        return noinput;
    }

    /**
     * A field is complete if the prompt and name are not null, and thus it is possible to ask the user for the field
     */
    public boolean isComplete() {
        if(prompt==null || name==null)
            return false;
        return true;
    }

    /**
     * A field is filled when the user has provided a valid value for it
     */
    public Boolean isFilled(){
        return value!=null;
    }

}
