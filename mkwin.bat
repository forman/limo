"C:\Program Files\Java\jdk1.8.0_144\bin\javapackager.exe" -createjar -v ^
   -outdir build ^
   -outfile limo ^
   -srcdir out\production\limo ^
   -appclass com.forman.limo.Main

"C:\Program Files\Java\jdk1.8.0_144\bin\javapackager.exe" -deploy -v ^
   -native installer ^
   -name "limo" ^
   -title "Limo" ^
   -description "Lowly Image Organizer" ^
   -vendor "Norman Fomferra" ^
   -outdir dist ^
   -outfile limo-installer ^
   -srcfiles lib\metadata-extractor-2.10.1.jar ^
   -srcfiles lib\xmpcore-5.1.3.jar ^
   -srcfiles LICENSE.md ^
   -srcfiles build\limo.jar ^
   -appclass com.forman.limo.Main ^
   -BappVersion=0.5 ^
   "-Bruntime=C:\Program Files\Java\jre1.8.0_144" ^
   -Bicon=src\com\forman\limo\resources\limo.ico ^
   -Bidentifier=com.forman.limo
