find -name "*.java" > sources.txt
javac -d classes -cp "./lib/*" @sources.txt -Xlint

rm ~/ramp/2-node/it/ -r
rm ~/ramp/3-node/it/ -r
rm ~/ramp/4-node/it/ -r


cp ~/master-ramp/classes/it/ ~/ramp/2-node/ -r
cp ~/master-ramp/classes/it/ ~/ramp/3-node/ -r
cp ~/master-ramp/classes/it/ ~/ramp/4-node/ -r
