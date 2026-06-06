REM set the class path,
REM assumes the build was executed with maven copy-dependencies
SET BASE_CP=app\target\aisafe.app-1.4.0-SNAPSHOT.jar;app\target\dependency\*;

REM TCP simulator host/port — change SIM_HOST to your VM's IP for team use
REM   localhost  = simulator runs on same machine (WSL)
REM   <VM-IP>    = simulator runs on a VM accessible on the network
SET SIM_HOST=localhost
SET SIM_PORT=9999

REM call the java VM
java -cp %BASE_CP% -Daisafe.simulator.host=%SIM_HOST% -Daisafe.simulator.port=%SIM_PORT% -Daisafe.logging.host=localhost eapli.aisafe.ui.AISafeBackoffice
