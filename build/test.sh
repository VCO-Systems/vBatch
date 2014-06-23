allParameters=$@
for eachArgument in $allParameters; do
    case `echo $eachArgument | cut -d '-' -f 2` in
    set_job_date)
        newdt=${allParameters/*-set_job_date/}
        echo "Dt: $newdt"
    esac
done
