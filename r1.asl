// Adventuring Agent 1

/* Initial beliefs */

at(P) :- pos(P,X,Y) & pos(r1,X,Y).

/* rules */


/* Initial goal */

!check(rooms).

/* Plans */

+!check(rooms)
    <- scan(room);
        //.wait(1000);
        next(room);
        !check(rooms).
+!check(rooms).

//If the agent sees new objects, add to beliefs.
//to do, add beliefs
+room(X,Y,A)[source(percept)] : true
    <-+room(X,Y,A)[source(self)];
    //tells r2 what was seen in the room.
    .send(r2, tell, seen(X,Y,A)).

//this value needs editing when the grid size is altered in java
+pos(r1,4,4)
    <-.drop_all_intentions;
        .print("Agent 1 has scanned every room.");
        .wait(2000);
        clear;
        .send(r2, tell, finished).
