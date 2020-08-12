mkdir ~/master-ramp/classes
find -name "*.java" > sources.txt
javac -d classes -cp "./lib/*" @sources.txt -Xlint

rm ~/ramp/12-node/test/ -r
rm ~/ramp/12-node/it/ -r

cp ~/master-ramp/classes/it ~/ramp/12-node/ -r
cp ~/master-ramp/classes/test ~/ramp/12-node/ -r