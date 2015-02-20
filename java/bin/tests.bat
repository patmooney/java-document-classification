cd src

javac -cp ".\;.\lib\junit-4.12.jar;.\lib\hamcrest-core-1.3.jar" tests\*.java


java -cp ".\;.\lib\junit-4.12.jar;.\lib\hamcrest-core-1.3.jar" tests/TestRunner


del tests\*.class

cd ..