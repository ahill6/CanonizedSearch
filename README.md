# CanonizedSearchRepair

## Instructions
To run, overwrite TestPaths.java and TestPaths.jpf with the files from this repo, all others should go in a new package in SPF main (gov.nasa.jpf.symbc.repairproject).

- Make sure that MySQL is installed and up to date on your machine.
- Use creation_script.sql to create the DB.
- Change the "database" and "password" class variables in DistanceCalculations.java to match your MySQL database path and password.
- Run Runner.java

N.B. If desired to speed future runs, setting the databaseExists variable in Runner's main to true will skip the database population step.

## Discussion
This demo takes a handful of sample methods and finds the symbolic constraints, populates the database, then searches for the five nearest matches according to both Edit distance.  These matches are displayed to stdout, although the capacity exists to print them to a file instead.

The methods include several instance of methods of the form "copies" and "copies2" where "copies2" is either a slightly altered or rewritten version of "copies".  

Notice that on the whole this technique does an excellent job finding matches, though the example of makeChange shows that it is not perfect and struggles with Path Constraints containing modular arithmetic.  returnMin also seems to be flawed, since the correct match is fourth of five.  However, looking at the distances, it is clear that the first four results are essentially tied for closest.  Compare this to results such as median, where the closest result is nearly an order of magnitude closer than the second.

As a note, on the IntroClass dataset Longest Common Substring shows better performance than Edit distance, but Edit is always more discerning.  After over 300 test methods, Edit distance has never registered a false positive.  It either struggles to find a nearest neighbor (i.e. many results are essentially the same - in which case LCSubstring nearly always succeeds), or else it gives one answer that is distinct from the second best by 70% or more of the second's value (c.f. results of this demo).  I have never found an instance in which there was a difference of more than 70% between the first two results and Edit distance was wrong.  Edit distance seems to indicate either that it definitely has the right answer, or else is inconclusive.  It never (so far) gives a false positive.
