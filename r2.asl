// Translation Agent 2

/* Initial beliefs */

at(P) :- pos(P,X,Y) & pos(r2,X,Y).

+match(A,B)[source(percept)]
    <-+match(A,B)[source(self)].

+seen(X,Y,A) : .count(match(A,_)[source(self)], 0) & not .count(matched(A,_),1)
    <- .findall(B, match(_,B)[source(self)], L);
        .print("The object ",A," was seen for the first time.",L);
        query(X,Y,A,L,1).

+seen(X,Y,A) : not .count(match(A,_)[source(self)],0) & not .count(matched(A,_),1)
    <- .findall(B, match(A,B)[source(self)], R);
        .print("The object ",A," was seen again.");
        query(X,Y,A,R,2).

+seen(X,Y,A) : .count(matched(A,_), 1)
    <- .print("Matched",A).

+matched(A,B)[source(percept)]
    <-+matched(A,B)[source(self)].

+matched(A,B)[source(self)] : .count(matched(_,_)[source(self)],10)
    <-.print("Ten Items Have been Matched Successfully").

+check(R)
    <-.findall(A, match(A,R)[source(self)], Q);
    .print("The object ",R," has been unmatched with these items",Q);
    remove(R,Q).

+checkL(R)
    <-.findall(B, match(R,B)[source(self)], Q);
    .length(Q,P);
    assess(R,Q,P).

+removeMatch(A,B)[source(percept)]
    <--match(A,B)[source(self)].

+finished[source(r1)]
    <- .findall(A, matched(A,B), Q);
        .findall(B, matched(A,B), Z);
        .length(Z, P);
        declare(Q,Z);
        .print("The number of items matched at finish is ",P).
