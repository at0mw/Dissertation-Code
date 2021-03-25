//University of Liverpool
//Thomas Williams
//Dissertation Comtrial Code

public class Objects {
    //private int id;
    private String Ag1Obj;
    private String Ag2Obj;
    //private String Ag1Col;
    //private String Ag2Col;

    public Objects(String input){
        String[] splitString = input.split(":");
        //id=i;
        this.Ag1Obj = splitString[0];
        //Ag1Col=Ag1C;
        this.Ag2Obj = splitString[1];
        //Ag2Col=Ag2C;
    }

    public String getAg1Obj(){
        return Ag1Obj;
    }

    public String getAg2Obj(){
        return Ag2Obj;
    }
}
