#!/bin/sh
# vbatch.sh

# initialize vars
abort_job=false
PROBLEMS=()

#Save the default IFS into a variable
OIFS=$IFS

allParameters=$@

IFS=" "

echo "Starting vBatch process at $(date)"

for eachArgument in $allParameters; do
    case `echo $eachArgument | cut -d '-' -f 2` in
    
    j)
        jArgs=${allParameters/*-j/}

            for eachArg in $jArgs; do
                    jobs="$eachArg"
                    break
            done

        echo "Job IDs: $jobs"
        ;;

    db) 
                dArgs=${allParameters/*-db/}

                for eachArg in $dArgs; do
                        dbConfig="$eachArg"
                        break
                done

        echo "DB Config: $dbConfig"
        ;;

    b)
        java -jar vbatch.jar $@
        IFS=$OIFS
        exit
        ;;
    set_job_date)
        job_date=${allParameters/*-set_job_date/}
        ;;
    esac
done

if [ "$jobs" == "" ]; then
    echo "Required parameter -j is missing."
    exit 1
fi

if [ "$dbConfig" == "" ]; then
    echo "Required parameter -d is missing."
    exit 1
fi

currJobIDs=$jobs

# If we're setting effective date, skip the
# concurrent-job checks, and just fire up vbatch
if [[ ! -z $job_date ]]; then
    java -jar vbatch.jar $@
    IFS=$OIFS
    exit
fi

#Get list of currently running jobs
psCommandOutput=`ps -eo cmd | grep vbatch.jar | grep -v grep`

OIFS=$IFS

IFS=$'\n'
proceedWithJob=true
prevJobIDs=""

for eachProcess in $psCommandOutput; do
    echo "Checking running process: $eachProcess"
    jParameter=${eachProcess/*-j/}

    IFS=" "

    for eachParameter in $jParameter; do
            prevJobIDs="$eachParameter"
            break
    done

    IFS=","
    
    for eachPrevJob in $prevJobIDs; do
            for eachCurrJob in $currJobIDs; do
                    if [ "$eachCurrJob" == "$eachPrevJob" ]; then
                            prob="Job $eachPrevJob is already running."
                            echo $prob
                            PROBLEMS="${PROBLEMS[@]} $prob"
                            proceedWithJob=false
                    fi
            done
    done
done

if [ "$proceedWithJob" == false ]; then
    FNAME_DATE=$(date +"d%Y%m%d_t%H%M%S")
    LOG_ENTRY_DATE=$(date +"%H-%M-%S - %H:%M:%S")
    log_contents=()
    FNAME="logs/vbatch_fatal_$FNAME_DATE.log"
    echo "*** Job not scheduled ***"
    for probl in $PROBLEMS; do
        msg="$LOG_ENTRY_DATE - vBatch - $probl"
        log_contents="${log_contents[@]} $msg" 
    done
    echo "$log_contents" > $FNAME
    
else
    java -jar vbatch.jar $@
fi

#Reset IFS back to the original value
IFS=$OIFS

echo "Ending vBatch process at $(date)"

exit
