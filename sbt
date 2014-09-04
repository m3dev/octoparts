#!/bin/bash
if [ ! -f $HOME/.sbtconfig ]; then
  echo "export SBT_OPTS=\"-XX:+CMSClassUnloadingEnabled\"" > ${HOME}/.sbtconfig
fi
source $HOME/.sbtconfig
cd `dirname $0`
current_dir=`pwd`
exec java -Xmx1024M ${SBT_OPTS} -jar ${current_dir}/bin/sbt-launch.jar "$@"

