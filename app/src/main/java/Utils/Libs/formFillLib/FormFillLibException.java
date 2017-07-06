package Utils.Libs.formFillLib;

/**
 * Created by Pravinyo on 3/15/2017.
 */

public class FormFillLibException extends Exception{

    private static final long serialVersionUID = 1L;

    private String reason;

    public FormFillLibException(String message){
        super(message);
    }

    public FormFillLibException(String message, String reason){
        super(message);
        this.reason = reason;
    }

    public String getReason(){
        return reason;
    }
}
