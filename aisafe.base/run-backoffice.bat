REM set the class path,
REM assumes the build was executed with maven copy-dependencies
SET BASE_CP=app\target\aisafe.app-1.4.0-SNAPSHOT.jar;app\target\dependency\*;

REM call the java VM
java -cp %BASE_CP% -Daisafe.simulator.host=localhost -Daisafe.simulator.port=9999 -Daisafe.logging.host=localhost eapli.aisafe.ui.AISafeBackoffice
