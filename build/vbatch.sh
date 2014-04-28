#!/bin/sh
# vbatch.sh

# initialize vars
abort_job=false

#Save the default IFS into a variable
OIFS=$IFS

allParameters=$@

IFS=" "

echo "Starting vBatch process at $(date)"

for eachArgument in $allParameters; do
    case `echo $eachArgument | cut -d '-' -f 2` i 
    
    j)
        jArgs=${allParameters/*-j/}

            for eachArg in $jArgs; do
                    jobs="$eachArg"
                    break
            done

        echo "Job IDs: $jobs"
        ;;

    d)  
                dArgs=${allParameters/*-d/}

                for eachArg in $dArgs; do
                        dbConfig="$eachArg"
                        break
                done

        echo "DB Config: $dbConfig"
        ;;

    b)
        echo "Re-batch not supported yet."
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
                            echo "Job $eachPrevJob is already running, cannot schedule this job!"
                            proceedWithJob=false
                    fi
            done
    done
done

if [ "$proceedWithJob" == false ]; then
    echo "*** Job not scheduled ***"
else
    echo "Job $currJobIDs scheduled successfully!"
    java -jar vbatch.jar "$@"
fi

#Reset IFS back to the original value
IFS=$OIFS

echo "Ending vBatch process at $(date)"

exit