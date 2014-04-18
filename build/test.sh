# vbatch.sh

# initialize vars
abort_job=false
args=`getopt hj:b:db: $*`
set -- $args
b="vbatch -j 1,3,5 -db config/wm_ohl_dev.ini"
for i; do
  case "$i" in
    -h  ) shift ;;
    -j  ) jobs="$2"
          # echo $jobs
          shift ; shift  ;;
    -b  ) echo "re-batch not supported yet."
          shift ; shift ;;
    --  ) shift; break ;;
  esac
done

# Get list of currently running jobs
instances=($(ps -eo cmd | grep vbatch.jar | grep -v grep))



# requested jobs
if [[ -n "$jobs" ]]; then
  for requested_job in $(echo $jobs | tr "," "\n")
  do
    # check each job
    echo "Checking job: $requested_job"
    # if job is already running, abort request
    
  done
  
fi

# run vbatch
