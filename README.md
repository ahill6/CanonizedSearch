# CanonizedSearchRepair

## Instructions
To run, overwrite TestPaths.java and TestPaths.jpf with the files from this repo, all others should go in a new package in SPF main (gov.nasa.jpf.symbc.repairproject).

- If using the DB, make sure that MySQL is installed and up to date on your machine.  Then use creation_script.sql to create the DB.
- If you are not using the database, change the method call at the end of main in Runner.java to "demo" (commented out)
- Run Runner.java

This demo takes a handful of sample methods and finds the symbolic constraints, populates the database, then searches for the five nearest matches according to both Edit distance and Longest Common Substring.  These matches are displayed to stdout, although the capacity exists to print them to a file instead.
