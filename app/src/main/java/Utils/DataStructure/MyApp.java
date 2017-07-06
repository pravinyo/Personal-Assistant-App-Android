package Utils.DataStructure;

/**
 * Created by Pravinyo on 3/7/2017.
 */

public class MyApp {
    private String name; 			//User-friendly name
    private String packageName;		//Full name
    private double similarity;		//Similarity of its user-friendly name with the recognized input

    public MyApp(String name, String packageName, double similarity){
        this.name = name;
        this.packageName = packageName;
        this.similarity = similarity;
    }

    public String getName(){ return name; }
    public  String getPackageName(){ return packageName; }
    public  double getSimilarity(){ return similarity; }
}
