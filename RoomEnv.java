import jason.asSyntax.*;
import jason.environment.*;
import jason.bb.*;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;
import java.util.*;
import java.io.File;
import java.text.ParseException;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RoomEnv extends Environment {

    public static final int GSize = 5; // grid size
    public static final int ONum = 10; // number of objects;
    public static final int ORoom = 4; // number of objects in each room;
    public static final int NRoom = 4; // number of rooms with Objects;
    public static final Integer[][][] rooms = new Integer[GSize][GSize][ORoom];//represents the coordingates and object# in room
    public static final int Obj  = 16; // object code in grid model showing room has objects

    public static final Term mr = Literal.parseLiteral("next(room)");
    public static final Term sr = Literal.parseLiteral("scan(room)");

    static Logger logger = Logger.getLogger(RoomEnv.class.getName());

    private RoomModel model;
    private RoomView  view;
    private Objects[] objects = new Objects[ONum];
    private ArrayList<String> r2OntMatched = new ArrayList<String>();

    @Override
    public void init(String[] args) {
        model = new RoomModel();
        view  = new RoomView(model);
        model.setView(view);
        model.objectGenerator();
        updatePercepts();
        //createObjects();
    }

    public boolean executeAction(String ag, Structure action) {
        logger.info(ag+" doing: "+ action);
        updatePercepts();
        try {
            if (action.equals(mr)) {
                model.moveRoom();
                logger.info("Agent 1 is located at : " + model.getAgPos(0));
            } else if (action.equals(sr)) {
                model.scanRoom();
                try {
                    Thread.sleep(400);
                } catch (Exception e) {}
            } else if (action.getFunctor().equals("query")) {
                int x = (int)((NumberTerm)action.getTerm(0)).solve();
                int y = (int)((NumberTerm)action.getTerm(1)).solve();
                String ObjSeen = action.getTerm(2).toString();
                String Past = action.getTerm(3).toString();
                int n = (int)((NumberTerm)action.getTerm(4)).solve();
                model.query(x, y, ObjSeen, Past, n);
            } else if (action.getFunctor().equals("remove")) {
                String taken = action.getTerm(0).toString();
                String incorrect = action.getTerm(1).toString();
                model.removeAg2Ont(taken, incorrect);
            } else if (action.getFunctor().equals("assess")) {
                String Ag1Ass = action.getTerm(0).toString();
                String Ag2Ass = action.getTerm(1).toString();
                int p = (int)((NumberTerm)action.getTerm(2)).solve();
                if (p == 1){
                    model.assess(Ag1Ass, Ag2Ass);
                }
            } else if (action.getFunctor().equals("match")) {
                String Ag1Term = action.getTerm(0).toString();
                String Ag2Term = action.getTerm(1).toString();
                model.match(Ag1Term, Ag2Term);
            } else if (action.getFunctor().equals("declare")) {
                String Ag1Terms = action.getTerm(0).toString();
                String Ag2Terms = action.getTerm(1).toString();
                model.declare(Ag1Terms, Ag2Terms);
            } else if (action.getFunctor().equals("clear")) {
                clearAllPercepts();
            } else {
                logger.info("executing: "+action+", but not implemented!");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(100);
        } catch (Exception e) {}
        informAgsEnvironmentChanged();
        return true;
    }

    void updatePercepts() {
        clearPercepts();

        Location r1Loc = model.getAgPos(0);
        Location r2Loc = model.getAgPos(1);

        Literal pos1 = Literal.parseLiteral("pos(r1," + r1Loc.x + "," + r1Loc.y + ")");
        Literal pos2 = Literal.parseLiteral("pos(r2," + r2Loc.x + "," + r2Loc.y + ")");

        addPercept(pos1);
        addPercept(pos2);

    }

    class RoomModel extends GridWorldModel {


        Random random = new Random(System.currentTimeMillis());


        private RoomModel() {
            super(GSize, GSize, 2);

            int random_x = 0;
            int random_y = 0;
            int[] og;

            // initial location of agents
            try {
                //set position of agent 1 randomly within Grid
                random_x = (int)(Math.random() * (GSize - 0 + 1) + 0);
                random_y = (int)(Math.random() * (GSize - 0 + 1) + 0);
                Location r1Loc = new Location(random_x/2, random_y/2);
                setAgPos(0, 0, 0);

                //set position of agent 2 randomly within Grid
                random_x = (int)(Math.random() * (GSize - 0 + 1) + 0);
                random_y = (int)(Math.random() * (GSize - 0 + 1) + 0);
                Location r2Loc = new Location(random_x/2, random_y/2);
                setAgPos(1, 0, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        void moveRoom() throws Exception {
            Location r1 = getAgPos(0);
            r1.x++;
            if (r1.x == getWidth()) {
                r1.x = 0;
                r1.y++;
            }
            // finished searching the whole grid
            if (r1.y == getHeight()) {
                return;
            }
            setAgPos(0, r1);
            setAgPos(1, getAgPos(1)); // to keep it in view
        }


        void objectGenerator() {
            try {
                File myObj = new File("AgObjects.txt");
                Scanner input = new Scanner(myObj);
                for (int i=0; i<ONum; i++) {
                    String data = input.nextLine();
                    objects[i] = new Objects(data);
                }
            input.close();
            } catch (FileNotFoundException e) {
                logger.info("An error occurred.");
                e.printStackTrace();
            }
            /*Array can be shuffled so that two of the same object wont
            appear in a room*/
            Integer[] intArray = new Integer[ONum];
            for (int i=0; i < ONum; i++){
                intArray[i]=i;
            }
            /*Array represents the grid itself, changes have been made to place
            objects in only certain rooms. Temp fix set empty rooms = -1*/
            for (int r=0; r < GSize; r++){
                for(int c=0; c < GSize; c++){
                    for(int z=0; z < ORoom; z++){
                        rooms[r][c][z] = -1;
                        //logger.info(rooms[r][c][z]);
                    }
                }
            }
        List<Integer> randomNumX = new ArrayList<>();
        List<Integer> randomNumY = new ArrayList<>();
        for (int i=0;i<GSize;i++){
            randomNumX.add(i);
            randomNumY.add(i);
        }
        Collections.shuffle(randomNumX);
        Collections.shuffle(randomNumY);
        for (int i=0;i<NRoom;i++){
            int q = randomNumX.get(i);
            int r = randomNumY.get(i);
            shuffleObj(q,r, intArray);
        }
        //put 4 random objects in 8 rooms
        }

        void shuffleObj(int n, int q, Integer[] r) {
            List<Integer> intList = Arrays.asList(r);
            Collections.shuffle(intList);
            for(int z=0; z < ORoom; z++){
                rooms[n][q][z] = r[z];
                int r2in = r[z];
                //logger.info(rooms[r][c][z]);
            }
            add(Obj, n, q);
        }

        void scanRoom() throws Exception {
            //view the objects in room and add whats there to percepts
            //get the agents current location
            int xyz;
            Location lc = getAgPos(0);
            xyz = rooms[lc.x][lc.y][0];
            if (xyz > -1) {
                addPercept(ASSyntax.parseLiteral("scanRoom(success)"));
                for (int i=0;i<ORoom;i++){
                    xyz = rooms[lc.x][lc.y][i];
                    try {
                        addPercept("r1",ASSyntax.parseLiteral("room("+lc+","+objects[xyz].getAg1Obj()+")"));
                        //logger.info("Hello There");
                    } catch (Exception e){
                        logger.info("Error has occurred");
                    }
                }
            } else {
                logger.info("Room is empty");
            }
        }

        void query(int x, int y, String o, String l, int n) throws Exception {
            String tidy = l.replaceAll("[\\[\\]]", "");
            String[] splitString = tidy.split(",");
            ArrayList<String> seenR2Ont = new ArrayList<String>();
            switch (n) {
                case 1:
                    matchQuery(x, y, o, splitString);
                    break;
                case 2:
                    memoryQuery(x, y, o, splitString);
                    break;
            }
        }

        //matches every Ag2OntTerm with the Object Term from Agent 1
        //on the condition that the Ag2OntTerm hasnt been matched 4 times before
        void matchQuery(int x, int y, String o, String[] l) throws Exception {
            ArrayList<String> seenR2Ont = new ArrayList<String>();
            ArrayList<String> finalMatches = new ArrayList<String>();
            logger.info("I made it here 1");
            for(int k=0; k < l.length;k++){
                seenR2Ont.add(l[k]);
            }
            for (int z=0; z<ORoom;z++){
                logger.info("I made it here 2");
                String r2Ont = objects[rooms[x][y][z]].getAg2Obj();
                //this checks if an object in ag2's terms has already been seen
                //if this is true that object must be disjoint
                int matchedPrior = Collections.frequency(r2OntMatched, r2Ont);
                //this checks if an object in ag2's terms has already been matched
                int occurrences = Collections.frequency(seenR2Ont, r2Ont);
                //if a term isnt matched or seen before then its a match
                //matches are added to a list so they can be counted prior to being
                //finalised
                if (occurrences < 1 & matchedPrior < 1){
                    logger.info("I was seen in this location");
                    finalMatches.add(r2Ont);
                }
            }


            if (finalMatches.size() > 1){
                for (int z = 0; z < finalMatches.size(); z++){
                    logger.info("The initial matches were created as such: "+o+finalMatches.get(z));
                    addPercept("r2",ASSyntax.parseLiteral("match("+o+","+finalMatches.get(z)+")"));
                }
            //if a term only has one match its a 1to1 match
            } else {
                addPercept("r2",ASSyntax.parseLiteral("matched("+o+","+finalMatches.get(0)+")"));
                addPercept("r2",ASSyntax.parseLiteral("check("+finalMatches.get(0)+")"));
                r2OntMatched.add(finalMatches.get(0));
            }

        }


        //
        void memoryQuery(int x, int y, String o, String[] l) throws Exception {
            //creates a temporary list to hold which terms have matched before
            ArrayList<String> matched = new ArrayList<String>();
            ArrayList<String> unmatched = new ArrayList<String>();
            ArrayList<String> seenR2Ont = new ArrayList<String>();
            for(int k=0; k < l.length;k++){
                seenR2Ont.add(l[k]);
            }


            for (int q=0; q<ORoom;q++){
                for(int i=0; i < l.length;i++){
                    String r2Ont = objects[rooms[x][y][q]].getAg2Obj();
                    if (r2Ont.equals(l[i])){
                        matched.add(l[i]);
                    }
                }
            }

            for (int q=0; q<l.length;q++){
                unmatched.add(l[q]);
            }

            for(int j=0; j < matched.size();j++){
                unmatched.remove(matched.get(j));
            }
            //logger.info(o+"===="+unmatched);
            //logger.info(o+"----"+matched);
            if (unmatched.size() > 0 & matched.size() > 1){
                //remove matches from ag1sont
                for(int m=0;m<unmatched.size();m++){
                    addPercept("r2",ASSyntax.parseLiteral("removeMatch("+o+","+unmatched.get(m)+")"));
                }
            } else if (matched.size()==1){
            //    logger.info("performing line 285");
                logger.info(o+" has matched with "+ matched.get(0));
                addPercept("r2",ASSyntax.parseLiteral("matched("+o+","+matched.get(0)+")"));
                addPercept("r2",ASSyntax.parseLiteral("check("+matched.get(0)+")"));
                r2OntMatched.add(matched.get(0));
                //takes the unmatched array and removes incorrect matches
                removeAg1Ont(o, unmatched);
            }
        }

        //removes connections for a term in Ag1's Ont which has matched
        void removeAg1Ont(String Ag1T, ArrayList Ag2I) throws Exception {
            //logger.info("I have arrived at my destination "+Ag1T+Ag2I);
            try{

                for(int k=0; k < Ag2I.size();k++){
                //    logger.info("Test Line 303: "+t+", "+incorrect[k]);
                    addPercept("r2",ASSyntax.parseLiteral("removeMatch("+Ag1T+","+Ag2I.get(k)+")"));
                    //addPercept("r2",ASSyntax.parseLiteral("check("+incorrect[k]+")"));
                }

            }
            catch(Exception e) {
                logger.info("There are no incorrect matches");
            }
        }

        //removes connections for a term in Ag2's Ont which has matched
        void removeAg2Ont(String Ag2T, String Ag1I) throws Exception {
            try{
                String tidy = Ag1I.replaceAll("[\\[\\]]", "");
                String[] incorrect = tidy.split(",");

                for(int k=0; k < incorrect.length;k++){
                //    logger.info("Test Line 303: "+t+", "+incorrect[k]);
                    logger.info("Check remove "+incorrect[k]);
                    addPercept("r2",ASSyntax.parseLiteral("removeMatch("+incorrect[k]+","+Ag2T+")"));
                    addPercept("r2",ASSyntax.parseLiteral("checkL("+incorrect[k]+")"));
                }

            }
            catch(Exception e) {
                logger.info("There are no incorrect matches");
            }
        }

        void match(String t, String i) throws Exception {
            try{
                String tidy = i.replaceAll("[\\[\\]]", "");
                //logger.info("line 320: Matched "+t+ " with "+tidy);
                //addPercept("r2",ASSyntax.parseLiteral("removeMatch("+t+","+tidy+")"));
                addPercept("r2",ASSyntax.parseLiteral("matched("+t+","+tidy+")"));
                logger.info(t+" has matched with "+ tidy);
                r2OntMatched.add(tidy);
                addPercept("r2",ASSyntax.parseLiteral("check("+tidy+")"));
            }
            catch(Exception e) {
                logger.info("Error");
            }
        }

        void assess(String o, String l) throws Exception {
            String tidy = l.replaceAll("[\\[\\]]", "");
            addPercept("r2",ASSyntax.parseLiteral("matched("+o+","+tidy+")"));
            addPercept("r2",ASSyntax.parseLiteral("check("+tidy+")"));
            r2OntMatched.add(tidy);
        }

        void declare(String ag1, String ag2) throws Exception {
            String tidy1 = ag1.replaceAll("[\\[\\]]", "");
            String[] splitString1 = tidy1.split(",");
            String tidy2 = ag2.replaceAll("[\\[\\]]", "");
            String[] splitString2 = tidy2.split(",");
            for (int i=0;i<splitString1.length;i++){
                logger.info("Agent 2 has matched the term "+splitString1[i]+" with "+splitString2[i]);
            }
        }

    }

    /*some code taken from Jason examples*/
    class RoomView extends GridWorldView {

        public RoomView(RoomModel model) {
            super(model, "Room World", 600);
            defaultFont = new Font("Arial", Font.BOLD, 18); // change default font
            setVisible(true);
            repaint();
        }

        /** draw application objects */
        @Override
        public void draw(Graphics g, int x, int y, int object) {
            switch (object) {
            case RoomEnv.Obj:
                drawObject(g, x, y);
                break;
           }
        }

        public void drawObject(Graphics g, int x, int y) {
            super.drawObstacle(g, x, y);
            g.setColor(Color.white);
            drawString(g, x, y, defaultFont, "Obj");
        }

        @Override
        public void drawAgent(Graphics g, int x, int y, Color c, int id) {
            String label = "Ag"+(id+1);
            c = Color.blue;
            if (id == 0) {
                c = Color.green;
            }
            super.drawAgent(g, x, y, c, -1);
            if (id == 0) {
                g.setColor(Color.blue);
            } else {
                g.setColor(Color.white);
            }
            super.drawString(g, x, y, defaultFont, label);
            repaint();
        }

    }
}
