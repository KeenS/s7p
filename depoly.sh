#!/bin/sh
usage() {
    cat <<HELP
NAME:
   $0 -- depoly s7p

SYNOPSIS:
  $0
  $0 [-h|--help]
  $0 [--verbose]

DESCRIPTION:
   Deploy s7p to all the hosts and restart slave.
   You need to restart master manually

  -h  --help      Print this help.
      --verbose   Enables verbose mode.
HELP
}

deploy_one() {
    deploy_one_local_host="$1"
    cat <<SHELL
      scp ./s7p-0.0.1-standalone.jar "${deploy_one_local_host}:/opt/s7p"
      [ ${deploy_one_local_host} != ${MASTER} ] && ssh "${deploy_one_local_host}" sudo service datadog-agent restart
SHELL
}
MASTER="ssp01-master.compe"
SLAVES="
ssp01-slave01.compe
ssp01-slave02.compe
ssp01-slave03.compe
ssp01-slave04.compe
ssp01-slave05.compe
ssp01-slave06.compe
ssp01-slave07.compe
ssp01-slave08.compe
ssp01-slave09.compe
ssp01-slave10.compe
ssp01-slave11.compe
ssp01-slave12.compe
ssp01-slave13.compe
ssp01-slave14.compe
ssp01-slave15.compe
ssp01-slave16.compe
ssp01-slave17.compe
ssp01-slave18.compe
"
# MASTER="ssp00-master.compe"
# SLAVES="
# ssp00-slave01.compe
# ssp00-slave02.compe
# ssp00-slave03.compe
# ssp00-slave04.compe"


HOSTS="${MASTER} ${SLAVES}"

deploy_all() {
    scp ./target/s7p-0.0.1-standalone.jar compe:
    for deploy_all_local_host in "$@"
    do
        echo  "[Host]: ${deploy_all_local_host}"
        deploy_one ${deploy_all_local_host} | ssh -t compe
        echo
    done
}

main() {
    SCRIPT_DIR="$(cd $(dirname "$0"); pwd)"

    for ARG; do
        case "$ARG" in
            --help) usage; exit 0;;
            --verbose) set -x;;
            --) break;;
            -*) 
                OPTIND=1
                while getopts h OPT "$ARG"; do
                    case "$OPT" in
                        h) usage; exit 0;;
                    esac
                done
                ;;
        esac
    done
    
    case "$1" in
        slaves)
            deploy_all ${SLAVES}
            ;;
        master)
            deploy_all ${MASTER}
            ;;
        *)
            deploy_all ${HOSTS}
            ;;
        
    esac
}

main "$@"

