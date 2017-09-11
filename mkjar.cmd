"C:/Program Files/Java/jdk1.8.0_144/bin/javapackager.exe" -createjar -v ^
   -outdir build ^
   -outfile limo ^
   -srcdir out/production/limo ^
   -classpath metadata-extractor-2.10.1.jar,xmpcore-5.1.3.jar ^
   -appclass com.forman.limo.Main
